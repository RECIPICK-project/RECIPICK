/* profile.js (stable, hardened)
 * 마이페이지: 탭 전환, 프로필 편집, 목록 로드(내 글/저장/활동), 정식 레시피 예외 처리
 */
(function () {
  'use strict';

  document.addEventListener('DOMContentLoaded', () => {
    window.__origNickname = document.querySelector('[name="nickname"]')?.value?.trim() || '';
  });

  // ✅ 폼 있을 때만 등록
  const profileForm = document.getElementById('profileForm');
  if (profileForm) profileForm.addEventListener('submit', async (e) => {
    e.preventDefault();

    const btn = e.currentTarget.querySelector('button[type="submit"]');
    btn && (btn.disabled = true, btn.textContent = '저장 중...');

    try {
      const nicknameInput = document.querySelector('[name="nickname"]');
      const newNickname   = nicknameInput?.value?.trim() || '';
      const file          = document.getElementById('profileImageInput')?.files?.[0];

      // 변경된 필드만
      const payload = {};
      if (newNickname && newNickname !== window.__origNickname) {
        payload.nickname = newNickname;
      }
      if (file) {
        const url = await uploadImageToS3(file, 'profile-images');
        payload.profileImageUrl = url;
      }

      if (!Object.keys(payload).length) {
        alert('변경된 내용이 없습니다.');
        return;
      }

      // PATCH 시도
      let res = await fx('/me/profile', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });

      // 닉네임 쿨다운이면 사진만 재시도
      if (!res.ok) {
        let err = {};
        try { err = await res.json(); } catch (_) {}
        const isCooldown =
            err?.code === 'NICKNAME_COOLDOWN' ||
            err?.error === 'NICKNAME_COOLDOWN' ||
            res.status === 429 || res.status === 409;

        if (isCooldown && 'nickname' in payload && 'profileImageUrl' in payload) {
          const onlyPhoto = { profileImageUrl: payload.profileImageUrl };
          res = await fx('/me/profile', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(onlyPhoto),
          });
          if (res.ok) {
            alert('프로필 사진만 변경되었습니다. 닉네임은 쿨다운 이후에 변경할 수 있어요.');
            location.reload();
            return;
          }
        }
        throw new Error(err?.message || `HTTP ${res.status}`);
      }

      alert('프로필이 저장되었습니다.');
      location.reload();
    } catch (e2) {
      console.error('[profile save] error:', e2);
      alert('저장 실패: ' + (e2?.message || '알 수 없는 오류'));
    } finally {
      btn && (btn.disabled = false, btn.textContent = '저장');
    }
  });

  /* ================================
   * 0) 공통 유틸 (API/인증/토스트/확인/CSRF)
   * ================================ */
  const API_BASE = ''; // 같은 도메인이면 '', 아니면 'http://localhost:8080'
  const USE_CREDENTIALS = true;
  const OFFICIAL_DETAIL_PAGE = '/pages/post_detail.html';

  const API = (p) => `${API_BASE}${p}`;

  function authHeader() {
    const token = localStorage.getItem('ACCESS_TOKEN');
    return token ? { Authorization: `Bearer ${token}` } : {};
  }

  function getCookie(name) {
    return document.cookie
        .split('; ')
        .map((s) => s.trim())
        .find((row) => row.startsWith(name + '='))?.split('=')[1];
  }

  // Spring Security: meta 또는 Cookie(XSRF-TOKEN)에서 CSRF 추출
  function getCsrfHeaders(method = 'GET') {
    const m = method.toUpperCase();
    if (['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(m)) return {};

    const rawHeader = document.querySelector('meta[name="_csrf_header"]')?.content || '';
    const rawToken  = document.querySelector('meta[name="_csrf"]')?.content || '';
    const header = rawHeader.trim();
    const token  = rawToken.trim();

    // 헤더 키로 쓸 수 있는지 최소 검증
    const TOKEN_NAME_RE = /^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/;
    if (header && token && TOKEN_NAME_RE.test(header)) {
      return { [header]: token };
    }

    const xsrf = getCookie('XSRF-TOKEN');
    if (xsrf) return { 'X-XSRF-TOKEN': decodeURIComponent(xsrf) };

    return {};
  }

  function fx(path, opts = {}) {
    const method = (opts.method || 'GET').toUpperCase();

    // FormData일 때는 Content-Type을 절대 강제 설정하지 않음
    const isFormData = (opts.body && typeof FormData !== 'undefined' && opts.body instanceof FormData);

    const headers = {
      Accept: 'application/json',
      ...authHeader(),
      ...getCsrfHeaders(method),
      ...(opts.headers || {}),
    };

    // FormData가 아닌데 JSON 보낼 때만 Content-Type을 유지(호출부에서 넣음)
    if (isFormData && headers['Content-Type']) {
      delete headers['Content-Type'];
    }

    const cred = USE_CREDENTIALS ? { credentials: 'include' } : {};
    return fetch(API(path), { ...opts, headers, ...cred });
  }

  function toast(msg) {
    let t = document.getElementById('toast');
    if (!t) {
      t = document.createElement('div');
      t.id = 'toast';
      t.style.cssText =
          'position:fixed;left:50%;bottom:24px;transform:translateX(-50%);padding:10px 14px;border-radius:8px;background:#222;color:#fff;font-size:14px;z-index:9999;opacity:0;transition:opacity .2s';
      document.body.appendChild(t);
    }
    t.textContent = String(msg || '');
    t.style.opacity = '1';
    setTimeout(() => (t.style.opacity = '0'), 1800);
  }

  const confirmAsync = (message) => Promise.resolve(confirm(message));

  function getPostId(it) {
    if (!it || typeof it !== 'object') return null;

    // 1) 가장 흔한 키 우선 시도
    const directKeys = ['id','postId','post_id','postID','postid','postNo','post_no','recipeId'];
    for (const k of directKeys) {
      const v = it[k];
      if (v !== undefined && v !== null && String(v).trim?.() !== '') return v;
    }

    // 2) "id"가 들어가는 모든 직속 키 스캔
    for (const [k, v] of Object.entries(it)) {
      if (/(^|_)(id|Id|ID)$/.test(k) || /id$/i.test(k) || k.toLowerCase().includes('id')) {
        const val = (typeof v === 'object' && v !== null) ? null : v;
        if (val !== null && val !== undefined && String(val).trim?.() !== '') return val;
      }
    }

    // 3) 1-depth 중첩 객체도 훑어보기
    for (const [k, v] of Object.entries(it)) {
      if (v && typeof v === 'object') {
        for (const dk of directKeys) {
          const vv = v[dk];
          if (vv !== undefined && vv !== null && String(vv).trim?.() !== '') return vv;
        }
        for (const [nk, nv] of Object.entries(v)) {
          if (/(^|_)(id|Id|ID)$/.test(nk) || /id$/i.test(nk) || nk.toLowerCase().includes('id')) {
            if (nv !== undefined && nv !== null && String(nv).trim?.() !== '') return nv;
          }
        }
      }
    }

    return null;
  }

  /* ================================
   * 1) 데이터 로더들
   * ================================ */
  async function loadProfile() {
    try {
      const res = await fx('/me/profile');
      if (res.status === 401) { location.href = '/pages/login.html'; return; }
      if (res.status === 404) { console.warn('PROFILE_NOT_FOUND'); return; }
      if (res.status === 405) { console.warn('PROFILE_ENDPOINT_NO_GET'); return; } // GET 미지원일 때 스킵
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const p = await res.json();
      window.initProfile?.({
        nickname: p.nickname,
        avatarUrl: p.profileImg || 'https://placehold.co/128x128?text=User',
        grade: p.grade,
      });
    } catch (err) {
      console.error('프로필 로드 실패:', err);
    }
  }

  async function loadMine() {
    try {
      const r = await fx('/me/posts?type=temp&limit=20');
      if (r.status === 401) { location.href = '/pages/login.html'; return; }
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const items = await r.json();
      if (Array.isArray(items) && items[0]) {
        console.log('[DEBUG] first item keys:', Object.keys(items[0]), items[0]);
      }
      window.renderMine?.(items || []);
    } catch (e) {
      console.warn('내 레시피 로드 실패:', e);
      window.renderMine?.([]);
    }
  }

  // ✅ 변경 1: 저장한 레시피 → /me/likes 호출 + 필드 보정
  async function loadSaved() {
    try {
      const r = await fx('/me/likes?offset=0&limit=20');
      if (r.status === 401) { location.href = '/pages/login.html'; return; }
      if (!r.ok) throw new Error(`HTTP ${r.status}`);
      const raw = await r.json(); // List<PostDto>

      const items = Array.isArray(raw) ? raw.map(it => ({
        ...it,
        title: it.title ?? it.foodName ?? '',
        thumb: it.rcpImgUrl ?? it.thumb ?? '',
        postId: it.postId ?? it.id ?? it.recipeId ?? null,
      })) : [];

      window.renderLinkList?.('listSaved', items || []);
    } catch (e) {
      console.warn('저장(좋아요) 로드 실패:', e);
      window.renderLinkList?.('listSaved', []);
    }
  }

  async function loadActivity() {
  try {
    const r = await fx('/me/reviews?offset=0&limit=20');
    if (r.status === 401) { location.href = '/pages/login.html'; return; }
    if (!r.ok) throw new Error(`HTTP ${r.status}`);
    const items = await r.json();
    window.renderActivityList?.('listActivity', items || [], loadActivity);
  } catch (e) {
    console.warn('리뷰 활동 로드 실패:', e);
    window.renderActivityList?.('listActivity', [], loadActivity);
  }
}

  /* ================================
   * 2) 탭 전환
   * ================================ */
  const VALID = ['my', 'saved', 'activity'];

  function selectTab(target) {
    document.querySelectorAll('.tab-btn').forEach((b) => {
      const on = b.dataset.target === target;
      b.classList.toggle('active', on);
      b.setAttribute('aria-selected', on ? 'true' : 'false');
    });
    document.querySelectorAll('.tab-panel').forEach((p) => {
      const on = p.id === `tab-${target}`;
      p.classList.toggle('active', on);
      p.hidden = !on;
    });
  }

  async function navigateTo(target, { push = false } = {}) {
    if (!VALID.includes(target)) target = 'my';
    const current = (location.hash || '').replace('#', '');
    if (current !== target) {
      const url = `#${target}`;
      push ? history.pushState(null, '', url) : history.replaceState(null, '', url);
    }
    selectTab(target);

    if (target === 'my')      await loadMine();
    if (target === 'saved')   await loadSaved();
    if (target === 'activity')await loadActivity();
  }

  function bindTabButtons() {
    document.querySelectorAll('.tab-btn').forEach((a) => {
      a.addEventListener('click', (e) => {
        e.preventDefault();
        navigateTo(a.dataset.target, { push: false });
      });
    });
    window.addEventListener('hashchange', () => {
      const t = (location.hash || '').replace('#', '');
      navigateTo(VALID.includes(t) ? t : 'my', { push: false });
    });
  }

  /* ================================
   * 3) 메뉴/로그아웃(데모)
   * ================================ */
  function bindMenuSheet() {
    const sheet = document.getElementById('menuSheet');
    const openMenu = () => sheet?.setAttribute('aria-hidden', 'false');
    const closeMenu = () => sheet?.setAttribute('aria-hidden', 'true');

    document.getElementById('openMenu')?.addEventListener('click', openMenu);
    sheet?.addEventListener('click', (e) => { if (e.target === sheet) closeMenu(); });

    document.getElementById('logoutBtn')?.addEventListener('click', () => {
      // TODO: POST /logout
      toast('로그아웃 되었습니다.');
    });
  }

  /* ================================
   * 4) 프로필 편집 모달 + 저장 (수정판)
   * ================================ */
  let savingProfile = false;

  function buildProfileModal() {
    const wrap = document.createElement('div');
    wrap.className = 'modal';
    wrap.setAttribute('aria-hidden', 'true');
    wrap.innerHTML = `
    <div class="overlay" data-close></div>
    <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="editTitle">
      <div class="dialog-head">
        <h3 id="editTitle">프로필 편집</h3>
        <button class="btn-ghost" data-close aria-label="닫기">닫기</button>
      </div>
      <div class="dialog-body">
        <label class="row"><span>닉네임</span><input class="input" id="editName" maxlength="20" placeholder="닉네임" /></label>
        <label class="row"><span>프로필 사진</span><input class="input" id="editAvatar" type="file" accept="image/*" /></label>
      </div>
      <div class="dialog-foot">
        <button class="btn-ghost" data-close>취소</button>
        <button class="btn success" id="saveProfile">저장</button>
      </div>
    </div>`;
    document.body.appendChild(wrap);

    wrap.addEventListener('click', (e) => {
      if (e.target && e.target.dataset.close !== undefined) {
        wrap.setAttribute('aria-hidden', 'true');
      }
    });

    document.getElementById('openEdit')?.addEventListener('click', () => {
      const cur = document.getElementById('userName')?.textContent?.trim() || '';
      const input = wrap.querySelector('#editName');
      if (input) input.value = cur;
      wrap.setAttribute('aria-hidden', 'false');
    });

    wrap.addEventListener('change', (e) => {
      if (e.target && e.target.id === 'editAvatar') {
        const f = e.target.files?.[0];
        if (!f) return;
        const img = document.getElementById('avatarImg');
        if (img) img.src = URL.createObjectURL(f);
      }
    });

    const saveBtn = wrap.querySelector('#saveProfile');
    saveBtn?.addEventListener('click', onSaveProfile);

    async function onSaveProfile() {
      if (savingProfile) return;
      savingProfile = true;

      const btn = saveBtn;
      const orig = btn.textContent;
      btn.disabled = true;
      btn.textContent = '저장 중...';

      try {
        const inputEl = wrap.querySelector('#editName');
        const typedNickname = inputEl?.value?.trim() || '';
        const currentNickname = document.getElementById('userName')?.textContent?.trim() || '';
        const shouldPatchNickname = !!typedNickname && typedNickname !== currentNickname;

        // (1) 아바타 업로드
        let avatarUrl;
        const file = document.getElementById('editAvatar')?.files?.[0] || null;
        if (file) {
          const up = await uploadAvatar(file);
          if (up.status === 401) { location.href = '/pages/login.html'; return; }
          if (up.ok && up.url) {
            avatarUrl = up.url;
            const img = document.getElementById('avatarImg');
            if (img) img.src = avatarUrl;
          } else if (up.status !== 0) {
            toast('프로필 사진 저장에 실패했어요. 닉네임만 저장합니다.');
          }
        }

        // (2) 닉네임 PATCH (변경된 경우에만)
        if (shouldPatchNickname) {
          const nickRes = await fx('/me/profile/nickname', {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ newNickname: typedNickname })
          });

          const rawText = await nickRes.text().catch(() => '');
          if (nickRes.status === 401) { location.href = '/pages/login.html'; return; }
          if (nickRes.status === 409 || nickRes.status === 429) {
            toast(rawText || '닉네임 변경이 제한되었습니다. 쿨다운 이후 다시 시도해 주세요.');
            return; // 닉네임만 실패 → 사진은 이미 반영됨
          }
          if (!nickRes.ok) { toast(rawText || `닉네임 변경 실패(${nickRes.status})`); return; }

          document.getElementById('userName').textContent = typedNickname;
        }

        wrap.setAttribute('aria-hidden', 'true');
        loadProfile();
        toast('저장되었습니다.');
      } catch (err) {
        console.error('프로필 저장 실패:', err);
        toast('프로필 저장에 실패했습니다.');
      } finally {
        btn.disabled = false;
        btn.textContent = orig;
        savingProfile = false;
      }
    }
  }

// === 프로필 아바타 업로드 (S3 presign → PUT → 서버에 URL 저장) ===
  async function uploadAvatar(file) {
    if (!file) return { ok: false, status: 0 }; // 파일 없으면 사진 없이 진행

    try {
      // 1) presign 요청: POST /me/profile/image/presign  (filename, contentType form-urlencoded)
      const presignRes = await fx('/me/profile/image/presign', {
        method: 'POST',
        body: new URLSearchParams({
          filename: file.name,
          contentType: file.type || 'application/octet-stream',
        }),
      });
      if (presignRes.status === 401) return { ok: false, status: 401 };
      if (!presignRes.ok)          return { ok: false, status: presignRes.status };

      const { putUrl, publicUrl } = await presignRes.json();
      if (!putUrl || !publicUrl)   return { ok: false, status: 500 };

      // 2) 브라우저 → S3 업로드 (PUT)
      const put = await fetch(putUrl, { method: 'PUT', body: file });
      if (!put.ok) return { ok: false, status: put.status };

      // 3) 최종 URL 저장: PATCH /me/profile/image  (JSON: { profileImg })
      const save = await fx('/me/profile/image', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ profileImg: publicUrl }),
      });
      if (save.status === 401) return { ok: false, status: 401 };
      if (!save.ok)            return { ok: false, status: save.status };

      return { ok: true, status: save.status, url: publicUrl };
    } catch (e) {
      console.warn('[uploadAvatar] error:', e);
      return { ok: false, status: 0 };
    }
  }





  /* ================================
   * 5) 등급 표시 & 프로필 초기화
   * ================================ */
  window.setTier = function (grade) {
    const el = document.getElementById('userTier');
    if (!el) return;
    const key = String(grade || '').toUpperCase();
    const shown = { BRONZE:'Bronze', SILVER:'Silver', GOLD:'Gold', PLATINUM:'Platinum', DIAMOND:'Diamond' }[key];
    if (!shown) return;
    el.dataset.tier = key;
    el.textContent = shown;
  };

  window.initProfile = function ({ nickname, avatarUrl, grade } = {}) {
    if (nickname) {
      const el = document.getElementById('userName');
      if (el) el.textContent = nickname;
    }
    if (avatarUrl) {
      const img = document.getElementById('avatarImg');
      if (img) img.src = avatarUrl;
    }
    if (grade) window.setTier(grade);
  };

  /* ================================
   * 6) 목록 렌더 (정식 레시피 처리 + 안전 id)
   * ================================ */
  window.renderMine = function (items) {
    const ul = document.getElementById('listMine');
    if (!ul) return;
    ul.innerHTML = '';

    const emptyEl = document.querySelector('[data-empty-mine]');
    if (emptyEl) emptyEl.hidden = (Array.isArray(items) && items.length > 0);

    (items || []).forEach((it, idx) => {
      const li = document.createElement('li');
      li.className = 'card';

      const rid = getPostId(it);
      const isOfficialFlag = typeof it?.official === 'boolean' ? it.official : Number(it?.rcpIsOfficial) === 1;

      // 항상 인덱스 저장 (id가 없어도 클릭 시 역추적)
      li.dataset.idx = String(idx);
      if (rid != null) li.dataset.id = String(rid);
      li.dataset.official = isOfficialFlag ? '1' : '0';

      const safeBg = (it?.thumb || '').replace(/'/g, '&#39;');
      const officialBadge = isOfficialFlag ? `<span class="badge official" title="정식 레시피">OFFICIAL</span>` : '';
      const editDisabled  = isOfficialFlag ? 'disabled aria-disabled="true" title="정식 레시피는 수정할 수 없어요."' : '';
      const delDisabled   = isOfficialFlag ? 'disabled aria-disabled="true" title="정식 레시피는 삭제할 수 없어요."' : '';

      li.innerHTML = `
      <div class="thumb" style="background-image:url('${safeBg}')"></div>
      <div class="meta">
        <div class="title">${it?.title || '레시피 제목'} ${officialBadge}</div>
        <div class="sub">${it?.date || ''}</div>
      </div>
      <div class="rating">${it?.rating ? '⭐ ' + it.rating : ''}</div>
      <div class="actions">
        <button class="icon btn-edit" data-id="${rid ?? ''}" ${editDisabled}>✏️</button>
        <button class="icon btn-del"  data-id="${rid ?? ''}" ${delDisabled}>🗑️</button>
      </div>`;

      if (rid == null) {
        console.warn('[renderMine] id가 비어있는 아이템', { index: idx, item: it, keys: Object.keys(it || {}) });
      }

      ul.appendChild(li);
    });

    ul.onclick = async (e) => {
      const btn = e.target.closest('button');
      if (!btn) return;

      const card = btn.closest('li.card');
      const idx  = card?.dataset.idx ? Number(card.dataset.idx) : -1;

      // 1) 버튼 → 카드 → 데이터 순으로 id 추적
      let id = btn.dataset.id && btn.dataset.id.trim() !== '' ? btn.dataset.id : (card?.dataset.id || '');
      let isOfficial = (card?.dataset.official === '1');

      // 2) 여전히 id 없으면, items[idx]에서 다시 파생
      if ((!id || id === '') && idx >= 0 && (items || [])[idx]) {
        const backId = getPostId(items[idx]);
        if (backId != null) {
          id = String(backId);
          // 카드/버튼에도 심어 재발 방지
          card.dataset.id = id;
          btn.dataset.id = id;
          // 공식 여부도 보정
          if (!isOfficial) {
            const flag = typeof items[idx]?.official === 'boolean' ? items[idx].official : Number(items[idx]?.rcpIsOfficial) === 1;
            isOfficial = !!flag;
            card.dataset.official = isOfficial ? '1' : '0';
          }
        }
      }

      if (!id) {
        console.warn('data-id 없음', { btn, card, idx, item: (items || [])[idx] });
        toast('잘못된 항목입니다.');
        return;
      }

      if (btn.classList.contains('btn-edit')) {
        if (isOfficial) location.href = `${OFFICIAL_DETAIL_PAGE}?postId=${encodeURIComponent(id)}`; // ✅ 변경 2: ?postId=
        else            location.href = `post_upload.html?edit=${encodeURIComponent(id)}`;
        return;
      }

      if (btn.classList.contains('btn-del')) {
        if (isOfficial) { toast('정식 레시피는 삭제할 수 없어요.'); return; }
        if (!(await confirmAsync('삭제하시겠어요?'))) return;
        try {
          const res = await fx(`/me/posts/${encodeURIComponent(id)}`, { method: 'DELETE' });
          if (res.status === 401) { location.href = '/pages/login.html'; return; }
          if (res.status === 403) { toast('삭제 권한이 없어요.'); return; }
          if (res.status === 409) { toast('정식 레시피로 승격된 글은 삭제할 수 없어요.'); return; }
          if (!res.ok) throw new Error(`HTTP ${res.status}`);
          toast('삭제되었습니다.');
          // 재로드
          const r = await fx('/me/posts?type=temp&limit=20');
          const items2 = r.ok ? await r.json() : [];
          window.renderMine?.(items2 || []);
        } catch (err) {
          console.error('삭제 실패:', err);
          toast('삭제에 실패했습니다.');
        }
      }
    };
  };

  // ✅ 변경 3: 상세 링크를 official_detail.html?postId= 로 통일
  window.renderLinkList = function (listId, items) {
    const ul = document.getElementById(listId);
    if (!ul) return;
    ul.innerHTML = '';

    const emptySel = listId === 'listSaved' ? '[data-empty-saved]' : '[data-empty-activity]';
    const em = document.querySelector(emptySel);
    if (em) em.hidden = (Array.isArray(items) && items.length > 0);

    (items || []).forEach((it) => {
      const li = document.createElement('li');
      li.className = 'card';
      const safeBg = (it?.thumb || '').replace(/'/g, '&#39;');
      const rid = getPostId(it);
      li.innerHTML = `
        <a class="link" href="/pages/post_detail.html?postId=${encodeURIComponent(rid ?? it.recipeId ?? '')}">
          <div class="thumb" style="background-image:url('${safeBg}')"></div>
          <div class="meta">
            <div class="title">${it?.title || ''}</div>
            <div class="sub">${it?.meta || ''}</div>
          </div>
        </a>`;
      ul.appendChild(li);
    });
  };

window.renderActivityList = function (listId, items, onListChange) {
  const ul = document.getElementById(listId);
  if (!ul) return;
  ul.innerHTML = '';

  const emptyEl = document.querySelector('[data-empty-activity]');
  if (emptyEl) emptyEl.hidden = (Array.isArray(items) && items.length > 0);

  (items || []).forEach((it) => {
    const li = document.createElement('li');
    li.className = 'card';

    const post = it?.post || {};
    const thumbUrl = post.rcpImgUrl || '';
    const titleText = post.title || '원본 레시피';
    const postId = post.postId ?? it.postId;
    
    const reviewId = it.reviewId;
    const rating = it.reviewRating?.toFixed(1) || '0.0';
    const comment = it.comment || '';
    
    const safeBg = thumbUrl.replace(/'/g, '&#39;');

    // 3단 구조 (thumb | meta+rating | actions)
    li.innerHTML = `
      <div class="thumb" style="background-image:url('${safeBg}')"></div>
      <div class="meta">
        <div class="title">${comment}</div>
        <div class="sub">⭐ ${rating} · ${titleText}</div>
      </div>
      <div class="actions">
        <button class="icon btn-view" data-post-id="${postId}" title="원본 레시피 보기">👁️</button>
        <button class="icon btn-del" data-review-id="${reviewId}" title="리뷰 삭제">🗑️</button>
      </div>`;
    ul.appendChild(li);
  });

  ul.onclick = async (e) => {
    const btn = e.target.closest('button');
    if (!btn) return;

    if (btn.classList.contains('btn-view')) {
      const postId = btn.dataset.postId;
      if (postId) location.href = `/pages/post_detail.html?postId=${encodeURIComponent(postId)}`;
      return;
    }

    if (btn.classList.contains('btn-del')) {
      const reviewId = btn.dataset.reviewId;
      if (!reviewId) { toast('잘못된 리뷰 ID입니다.'); return; }
      if (!await confirmAsync('리뷰를 삭제하시겠어요?')) return;

      try {
        const res = await fx(`/api/reviews/${reviewId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('삭제 실패');
        toast('리뷰가 삭제되었습니다.');
        if (typeof onListChange === 'function') onListChange();
      } catch (err) {
        toast('삭제에 실패했습니다.');
      }
    }
  };
};
  /* ================================
   * 7) 하단 하트 → 저장 탭
   * ================================ */
  function bindBottomTabHeart() {
    const heart = document.querySelector('.tabbar .heart');
    if (!heart) return;
    heart.addEventListener('click', (e) => {
      if (location.pathname.endsWith('profile.html')) {
        e.preventDefault();
        navigateTo('saved', { push: false });
      }
    });
  }

  /* ================================
   * 8) 초기 진입
   * ================================ */
  function init() {
    bindTabButtons();
    bindMenuSheet();
    buildProfileModal();
    bindBottomTabHeart();

    const t = (location.hash || '').replace('#', '');
    navigateTo(VALID.includes(t) ? t : 'my', { push: false });
    loadProfile(); // 최초 1회
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();