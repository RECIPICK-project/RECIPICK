// admin_users.js — 실서버 연동(더미 비활성), 공통 인증/CSRF 헤더 적용
(() => {
  'use strict';

  /* ========= Helpers ========= */
  const $  = (s, c=document) => c.querySelector(s);
  const $$ = (s, c=document) => Array.from(c.querySelectorAll(s));
  const esc = (s) => String(s)
      .replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')
      .replaceAll('"','&quot;').replaceAll("'",'&#39;');

  // ==== Auth / CSRF ====
  function getCookie(name){
    return document.cookie.split('; ').map(v=>v.trim()).find(r=>r.startsWith(name+'='))?.split('=')[1];
  }
  function authHeader(){
    const t = localStorage.getItem('ACCESS_TOKEN'); // 네가 쓰던 키 그대로
    return t ? { Authorization: `Bearer ${t}` } : {};
  }
  function csrfHeader(){
    const token = getCookie('XSRF-TOKEN') || getCookie('X-CSRF-TOKEN');
    return token ? { 'X-XSRF-TOKEN': decodeURIComponent(token) } : {};
  }
  async function jfetch(url, opt={}){
    const base = opt || {};
    base.method  = base.method || 'GET';
    base.headers = {
      'Accept': 'application/json',
      ...(base.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
      ...authHeader(),
      ...csrfHeader(),
      ...(base.headers || {})
    };
    base.credentials = 'include';
    const res = await fetch(url, base);
    return res;
  }

  /* ========= API =========
   * GET   /admin/users?offset=&limit=      -> List<UserSummaryDTO>
   * PATCH /admin/users/{id}/grade           body {grade: BRONZE|SILVER|GOLD|PLATINUM|DIAMOND}
   * PATCH /admin/users/{id}/active          body {active: true|false}
   * PATCH /admin/users/{id}/suspend         body {days:int, reason?:string}  // 0=영구
   * PATCH /admin/users/{id}/unsuspend       body {}
   */
  const API = {
    list     : (o, l) => `/admin/users?offset=${o}&limit=${l}`,
    grade    : (id)   => `/admin/users/${encodeURIComponent(id)}/grade`,
    active   : (id)   => `/admin/users/${encodeURIComponent(id)}/active`,
    suspend  : (id)   => `/admin/users/${encodeURIComponent(id)}/suspend`,
    unsuspend: (id)   => `/admin/users/${encodeURIComponent(id)}/unsuspend`,
  };

  /* ========= Fetchers ========= */
  async function patchGrade(userId, grade){
    const res = await jfetch(API.grade(userId), {
      method:'PATCH',
      body: JSON.stringify({ grade: String(grade||'').toUpperCase() })
    });
    if(!res.ok){
      let msg = `등급 변경 실패 (HTTP ${res.status})`;
      try{ const e = await res.json(); if(e?.message) msg += `: ${e.message}` }catch{}
      throw new Error(msg);
    }
  }
  async function patchActive(userId, active){
    const res = await jfetch(API.active(userId), {
      method:'PATCH',
      body: JSON.stringify({ active: !!active })
    });
    if(!res.ok){
      let msg = `활성 변경 실패 (HTTP ${res.status})`;
      try{ const e = await res.json(); if(e?.message) msg += `: ${e.message}` }catch{}
      throw new Error(msg);
    }
  }
  // 기간 정지(3/7/15일) / 영구(0)
  async function patchSuspend(userId, days, reason){
    const res = await jfetch(API.suspend(userId), {
      method:'PATCH',
      body: JSON.stringify({ days, reason })
    });
    if(!res.ok){
      let msg = `정지 적용 실패 (HTTP ${res.status})`;
      try{ const e = await res.json(); if(e?.message) msg += `: ${e.message}` }catch{}
      throw new Error(msg);
    }
    try { return await res.json(); } catch { return null; }
  }
  async function patchUnsuspend(userId){
    const res = await jfetch(API.unsuspend(userId), { method:'PATCH' });
    if(!res.ok) throw new Error(`정지 해제 실패 (HTTP ${res.status})`);
  }

  /* ========= State ========= */
  const state = { page: 0, size: 20, list: [] };
  let currentUser = null;

  /* ========= Local policy (브라우저 저장) ========= */
  const KEY_POLICY = 'userPolicy';
  const defaultPolicy = {
    tokens: { Bronze:200, Silver:500, Gold:1000 },
    points: { comment:1, recipe:10, promote:100 },
    reportThreshold: 3
  };
  const getPolicy = () => { try{ return JSON.parse(localStorage.getItem(KEY_POLICY)) || defaultPolicy }catch{ return defaultPolicy } };
  const setPolicy = (p) => localStorage.setItem(KEY_POLICY, JSON.stringify(p));

  /* ========= Render: List ========= */
  function renderList(users){
    const ul = $('#userList'); if (!ul) return;
    ul.innerHTML = '';

    const thresh = getPolicy().reportThreshold ?? 3;
    const activeFilter = $('.pill.is-active')?.dataset.filter || 'all';
    const q = $('#search')?.value.trim().toLowerCase() || '';

    const filtered = users.filter(u => {
      const text = `${u.nicknameOrEmail||''} ${u.userId||''}`.toLowerCase();
      const okSearch = !q || text.includes(q);
      let okFilter = true;
      const reports = +u.reportCount || 0;
      const points  = +u.points || 0;
      if (activeFilter === 'reported') okFilter = reports >= thresh;
      if (activeFilter === 'lowPoint') okFilter = points <= 0;
      return okSearch && okFilter;
    });

    if (!filtered.length){
      const li = document.createElement('li');
      li.className = 'user-item';
      li.innerHTML = `<div class="user-main"><div class="user-title">결과가 없습니다.</div></div>`;
      ul.appendChild(li);
      renderPagination();
      return;
    }

    filtered.forEach(u => {
      const li = document.createElement('li');
      li.className = 'user-item';
      li.dataset.uid     = u.userId;
      li.dataset.reports = u.reportCount ?? 0;
      li.dataset.points  = u.points ?? 0;
      li.dataset.tier    = (u.grade || 'BRONZE');
      li.dataset.active  = (u.active === false ? 'false' : 'true');
      li.dataset.suspendedUntil = u.suspendedUntil || ''; // 선택 필드

      const isActive = (u.active !== false);
      const tierText = (u.grade || 'BRONZE');
      const flagWarn = (u.reportCount ?? 0) >= thresh;

      li.innerHTML = `
        <div class="user-main">
          <div class="user-title">${esc(u.nicknameOrEmail || '(무명)')}</div>
          <div class="user-meta">
            <span>UID: ${esc(u.userId)}</span>
            <span>가입일: ${esc((u.createdAt || '').toString().slice(0,10))}</span>
            <span>포인트: <strong class="u-point">${esc(u.points ?? 0)}</strong></span>
            <span>신고 누적: <strong class="u-report">${esc(u.reportCount ?? 0)}</strong>건</span>
          </div>
        </div>
        <div class="user-right">
          <span class="badge tier js-tier">${esc(tierText)}</span>
          ${flagWarn ? '<span class="badge warn js-flag">신고주의</span>' : ''}
          ${!isActive ? '<span class="badge stop">정지</span>' : ''}
          <button class="btn-ghost small js-view">보기</button>
          <button class="btn-ghost small js-edit" data-open="userModal">수정</button>
        </div>
      `;
      if (!isActive) li.classList.add('is-suspended');
      ul.appendChild(li);
    });

    bindRowActions();
    renderPagination();
  }

  function renderPagination(){
    $('#pageInfo') && ($('#pageInfo').textContent = `${state.page+1}`);
    $('#prevPage') && ($('#prevPage').disabled = state.page <= 0);
    // "다음" 비활성 기준: 페이지 사이즈보다 적게 오면 마지막
    $('#nextPage') && ($('#nextPage').disabled = (state.list.length < state.size));
  }

  /* ========= Load ========= */
  async function load(){
    const offset = state.page * state.size;
    const res = await jfetch(API.list(offset, state.size), { headers:{ 'Accept':'application/json' }});
    if (!res.ok) throw new Error('HTTP '+res.status);
    const list = await res.json(); // List<UserSummaryDTO>
    state.list = Array.isArray(list) ? list : [];
    renderList(state.list);
  }

  /* ========= Row actions & Modal ========= */
  function openModal(){
    $('#userModal')?.setAttribute('aria-hidden','false');
    document.documentElement.style.overflow = 'hidden';
  }
  function closeModal(){
    $('#userModal')?.setAttribute('aria-hidden','true');
    document.documentElement.style.overflow = '';
  }
  $$('#userModal [data-close]').forEach(b=> b.onclick = closeModal);

  function bindRowActions(){
    $$('#userList .js-view').forEach(btn=>{
      btn.onclick = (e)=>{
        const li = e.target.closest('.user-item');
        alert(`사용자 상세: ${li.dataset.uid}`);
      };
    });
    $$('#userList .js-edit').forEach(btn=>{
      btn.onclick = (e)=>{
        const li = e.target.closest('.user-item');
        const uid    = li.dataset.uid;
        const title  = $('.user-title', li).textContent;
        const tier   = ($('.js-tier', li).textContent || 'BRONZE').toUpperCase();
        const reps   = +$('.u-report', li).textContent;
        const active = !(li.dataset.active === 'false');

        currentUser = { uid, title, tier, reps, active, li };

        $('#mUid')      && ($('#mUid').textContent = uid);
        $('#mName')     && ($('#mName').textContent = title);
        $('#mTier')     && ($('#mTier').value = tier);
        $('#mActive')   && ($('#mActive').checked = active);
        $('#mReports')  && ($('#mReports').textContent = `최근 30일: ${reps}건`);

        showBanStatus(li);
        openModal();
      };
    });
  }

  function showBanStatus(li){
    const statusEl = $('#mBanStatus'); if (!statusEl || !li) return;
    const until = li.dataset.suspendedUntil || '';
    if(until){
      statusEl.style.display = 'block';
      statusEl.textContent = `현재 정지 상태 · 해제 예정: ${String(until).replace('T',' ').slice(0,16)}`;
      $('#liftSuspendBtn') && ($('#liftSuspendBtn').style.display = 'inline-block');
    }else if(li.dataset.active === 'false'){
      statusEl.style.display = 'block';
      statusEl.textContent = '현재 정지 상태(영구) 또는 비활성화됨';
      $('#liftSuspendBtn') && ($('#liftSuspendBtn').style.display = 'inline-block');
    }else{
      statusEl.style.display = 'none';
      $('#liftSuspendBtn') && ($('#liftSuspendBtn').style.display = 'none');
    }
  }

  // 저장: 등급 + 활성
  $('#saveUser') && ($('#saveUser').onclick = async ()=>{
    if (!currentUser) return;
    const userId   = currentUser.uid;
    const newTier  = ($('#mTier')?.value || 'BRONZE').toUpperCase();
    const newActive= !!$('#mActive')?.checked;

    try{
      await patchGrade(userId, newTier);
      await patchActive(userId, newActive);

      // UI 반영
      const li = currentUser.li || $(`#userList .user-item[data-uid="${CSS.escape(String(userId))}"]`);
      if (li){
        $('.js-tier', li) && ($('.js-tier', li).textContent = newTier);
        li.dataset.active = String(newActive);
        li.classList.toggle('is-suspended', !newActive);

        const stop = li.querySelector('.badge.stop');
        if (!newActive){
          if (!stop){
            const x = document.createElement('span');
            x.className = 'badge stop';
            x.textContent = '정지';
            li.querySelector('.user-right').insertBefore(x, li.querySelector('.js-view'));
          }
        }else{
          stop?.remove();
          li.dataset.suspendedUntil = '';
        }
      }

      closeModal();
      alert('저장되었습니다.');
    }catch(err){
      console.error(err);
      alert(err.message || '저장 실패');
    }
  });

  // 기간 정지 적용
  $('#applySuspendBtn') && ($('#applySuspendBtn').onclick = async ()=>{
    if(!currentUser) return;
    const userId = currentUser.uid;
    const sel = document.querySelector('input[name="banPeriod"]:checked');
    const days = sel ? parseInt(sel.value,10) : NaN; // 0=영구
    if(Number.isNaN(days)){ alert('정지 기간을 선택하세요.'); return; }
    const reason = $('#mBanReason')?.value.trim() || '';

    try{
      const res = await patchSuspend(userId, days, reason);
      const li = currentUser.li || $(`#userList .user-item[data-uid="${CSS.escape(String(userId))}"]`);
      if(li){
        li.classList.add('is-suspended');
        li.dataset.active = 'false';
        li.dataset.suspendedUntil = res?.suspendedUntil || (days===0 ? '' : '');
        if(!li.querySelector('.badge.stop')){
          const stop = document.createElement('span');
          stop.className = 'badge stop';
          stop.textContent = '정지';
          li.querySelector('.user-right').insertBefore(stop, li.querySelector('.js-view'));
        }
      }
      showBanStatus(li);
      alert('정지가 적용되었습니다.');
    }catch(err){
      alert(err.message || '정지 적용 실패');
    }
  });

  // 정지 해제
  $('#liftSuspendBtn') && ($('#liftSuspendBtn').onclick = async ()=>{
    if(!currentUser) return;
    const userId = currentUser.uid;
    try{
      await patchUnsuspend(userId);
      const li = currentUser.li || $(`#userList .user-item[data-uid="${CSS.escape(String(userId))}"]`);
      if(li){
        li.classList.remove('is-suspended');
        li.dataset.active = 'true';
        li.dataset.suspendedUntil = '';
        li.querySelector('.badge.stop')?.remove();
      }
      showBanStatus(li);
      alert('정지 해제되었습니다.');
    }catch(err){
      alert(err.message || '정지 해제 실패');
    }
  });

  /* ========= 검색/필터 ========= */
  function filterList(){ renderList(state.list); }
  $$('.pill').forEach(p=>{
    p.onclick = ()=>{
      $$('.pill').forEach(x=>x.classList.remove('is-active'));
      p.classList.add('is-active');
      filterList();
    };
  });
  $('#searchBtn') && ($('#searchBtn').onclick = filterList);

  /* ========= 정책 폼 ========= */
  function renderPolicyForm(p){
    $('#tokBronze') && ($('#tokBronze').value = p.tokens.Bronze);
    $('#tokSilver') && ($('#tokSilver').value = p.tokens.Silver);
    $('#tokGold')   && ($('#tokGold').value   = p.tokens.Gold);
    $('#ptComment') && ($('#ptComment').value = p.points.comment);
    $('#ptRecipe')  && ($('#ptRecipe').value  = p.points.recipe);
    $('#ptPromote') && ($('#ptPromote').value = p.points.promote);
    $('#reportThreshold') && ($('#reportThreshold').value = p.reportThreshold);
  }
  renderPolicyForm(getPolicy());

  $('#savePolicy') && ($('#savePolicy').onclick = ()=>{
    const p = {
      tokens:{
        Bronze:+($('#tokBronze')?.value ?? 200),
        Silver:+($('#tokSilver')?.value ?? 500),
        Gold:+($('#tokGold')?.value ?? 1000)
      },
      points:{
        comment:+($('#ptComment')?.value ?? 1),
        recipe:+($('#ptRecipe')?.value ?? 10),
        promote:+($('#ptPromote')?.value ?? 100)
      },
      reportThreshold:+($('#reportThreshold')?.value ?? 3)
    };
    setPolicy(p);
    if($('#policySaved')){
      $('#policySaved').style.display='inline-block';
      setTimeout(()=> $('#policySaved').style.display='none', 1400);
    }
    filterList();
  });

  $('#resetPolicy') && ($('#resetPolicy').onclick = ()=>{
    setPolicy(defaultPolicy);
    renderPolicyForm(defaultPolicy);
    filterList();
  });

  /* ========= Pagination ========= */
  $('#prevPage') && ($('#prevPage').onclick = async ()=>{
    if (state.page<=0) return;
    state.page -= 1;
    await load().catch(console.error);
  });
  $('#nextPage') && ($('#nextPage').onclick = async ()=>{
    state.page += 1;
    await load().catch(e=>{
      state.page = Math.max(0, state.page-1);
      console.error(e);
    });
  });

  /* ========= Start ========= */
  load().catch(console.error);
})();
