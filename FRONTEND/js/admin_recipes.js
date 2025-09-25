// admin_recipes.js — 실서버 연동(더미 비활성), 공통 인증/CSRF 헤더 적용
(() => {
  const $  = (s, c=document) => c.querySelector(s);
  const $$ = (s, c=document) => Array.from(c.querySelectorAll(s));

  /* ==== Auth / CSRF ==== */
  function getCookie(name){
    return document.cookie.split('; ').map(v=>v.trim()).find(r=>r.startsWith(name+'='))?.split('=')[1];
  }
  function authHeader(){
    const t = localStorage.getItem('ACCESS_TOKEN');
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

  // === 전역 새탭 방지 (우리 링크는 무조건 같은 탭) ===
  (function(){
    // 혹시 레이아웃에 <base target="_blank">가 있으면 제거
    const base = document.querySelector('base[target]');
    if (base) base.removeAttribute('target');

    // 원본 보관 후 후킹
    const _open = window.open;
    window.__forceSameTab = { active: false };

    window.open = function(url, target, feat){
      // 우리 링크 클릭 직후엔 새창 금지하고 같은 탭으로
      if (window.__forceSameTab.active) {
        window.__forceSameTab.active = false;
        if (url) location.assign(url);
        return null;
      }
      return _open.call(window, url, target, feat);
    };

    // 캡처 단계에서 가장 먼저 플래그 ON + 기본동작 차단
    const arm = (e) => {
      const a = e.target?.closest?.('a[data-detail-link]');
      if (!a) return;
      // 다른 리스너보다 먼저 먹자
      e.preventDefault();
      e.stopPropagation();
      e.stopImmediatePropagation();

      // 우리 링크는 같은 탭으로
      window.__forceSameTab.active = true;
      const url = a.getAttribute('href') || a.href;
      location.assign(url);
      // 다음 틱에 플래그 자동 해제(혹시 중복 방지)
      setTimeout(()=> window.__forceSameTab.active = false, 0);
    };

    // click뿐 아니라 auxclick(휠/중클릭), mousedown도 선점
    window.addEventListener('click', arm, true);
    window.addEventListener('auxclick', arm, true);
    window.addEventListener('mousedown', arm, true);
  })();


  /* =========================
   * API (컨트롤러 시그니처 맞춤)
   * ========================= */
  const postsAPI = (q) => {
    const p = new URLSearchParams();
    p.set('offset', q.offset ?? 0);
    p.set('limit',  q.limit  ?? 20);
    if (q.query) p.set('q', q.query);
    return `/admin/posts/pending?${p.toString()}`; // List<PostDto>
  };
  const publishAPI   = (postId) => `/admin/posts/${postId}/publish`; // POST
  const deletePostAPI= (postId) => `/admin/posts/${postId}`;         // DELETE
  // 댓글 목록/삭제 엔드포인트는 확정되면 여기 연결

  /* ========================= */
  const esc = (s) => String(s)
      .replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')
      .replaceAll('"','&quot;').replaceAll("'",'&#39;');

  const fmtDate = (iso) => {
    if (!iso) return '-';
    const t = String(iso).replace('T',' ');
    return t.slice(0,16);
  };
  const daysBetween = (iso) => {
    try { return Math.floor((Date.now() - new Date(iso)) / 86400000); } catch { return 0; }
  };

  /* =========================
   * 승격 규칙 (로컬 저장된 값 표기용)
   * ========================= */
  const RULE_KEY = 'promotionRule';
  function readRule(){
    try{
      const raw = localStorage.getItem(RULE_KEY);
      return raw ? JSON.parse(raw) : null;
    }catch(e){ return null; }
  }
  function ruleSummaryText(r){
    if(!r) return '규칙이 없습니다. 세팅에서 먼저 정의하세요.';
    const parts = [];
    parts.push(r.mode === 'auto' ? '자동 승격' : '수동 승인');
    parts.push(`${r.windowDays}일 이내`);
    parts.push(`좋아요 ≥ ${r.minLikes}`);
    parts.push(`저장 ≥ ${r.minSaves}`);
    parts.push(`평점 ≥ ${r.minRating} (참여 ≥ ${r.minReviews})`);
    parts.push(`조회 ≥ ${r.minViews}`);
    if(+r.cooldown > 0) parts.push(`쿨다운 ${r.cooldown}시간`);
    return parts.join(' · ');
  }
  function qualifiesByRule(r, m){
    if(!r) return false;
    if(r.mode === 'manual') return false;
    const fails = [];
    if(+m.days   > +r.windowDays) fails.push('days');
    if(+m.likes  < +r.minLikes)   fails.push('likes');
    if(+m.saves  < +r.minSaves)   fails.push('saves');
    if(+m.rating < +r.minRating)  fails.push('rating');
    if(+m.reviews< +r.minReviews) fails.push('reviews');
    if(+m.views  < +r.minViews)   fails.push('views');
    return fails.length === 0;
  }



  /* =========================
   * 렌더: 레시피 카드
   * ========================= */
  function renderPosts(list){
    const ul = $('#postList'); if (!ul) return;
    ul.innerHTML = '';

    list.forEach(row => {
      // 예상 필드: id|postId, title, authorNickname/email, createdAt, likes, saves, ratingAvg|rating, ratingCount|reviews, views, status
      const id      = row.id ?? row.postId ?? '-';
      const title   = row.title ?? '(제목 없음)';
      const author  = row.author ?? row.authorNickname ?? row.authorEmail ?? '-';
      const created = row.createdAt ?? row.created_at ?? null;
      const likes   = row.likes ?? 0;
      const saves   = row.saves ?? 0;
      const rAvg    = row.ratingAvg ?? row.rating ?? 0;
      const rCnt    = row.ratingCount ?? row.reviews ?? 0;
      const views   = row.views ?? 0;
      const days    = created ? daysBetween(created) : 0;
      const status  = (row.status ?? 'TEMP').toString().toLowerCase(); // temp | official

      const li = document.createElement('li');
      li.className = 'item';
      li.dataset.id = id;
      li.dataset.likes = likes;
      li.dataset.saves = saves;
      li.dataset.rating = rAvg;
      li.dataset.reviews = rCnt;
      li.dataset.views = views;
      li.dataset.days = days;
      li.dataset.status = status;

      li.innerHTML = `
        <div class="item-main">
          <div class="item-title">
            <span class="select-box">
              <input type="checkbox" class="rowchk-post" />
              <span>${esc(title)}</span>
            </span>
          </div>
          <div class="item-meta">
            <span>RID: ${esc(id)}</span>
            <span>작성자: ${esc(author)}</span>
            <span>등록일: ${esc(fmtDate(created))}</span>
            <span>좋아요: ${esc(likes)}</span>
            <span>저장: ${esc(saves)}</span>
            <span>평점: ${esc(rAvg)}(${esc(rCnt)})</span>
            <span>조회: ${esc(views)}</span>
            <span>경과일: ${esc(days)}</span>
          </div>
        </div>
        <div class="item-right">
          <span class="badge ${status==='official'?'ok':'warn'} status-badge">${status==='official'?'정식':'임시'}</span>
          <span class="promote-chip wait">대기</span>
          <button class="btn-ghost small promote-btn"${status==='official'?' disabled':''}>승격</button>
         <a class="btn-ghost small"
   data-detail-link
   href="/pages/post_detail.html?postId=${encodeURIComponent(id)}"
   style="text-decoration:none; color:inherit;">보기</a>

        </div>
      `;
      ul.appendChild(li);
    });

    applyRuleToCards();
    bindPostRowActions();
  }



  function applyRuleToCards(){
    const rule = readRule();
    $('#ruleSummary') && ($('#ruleSummary').textContent = ruleSummaryText(rule));

    $$('#postList .item').forEach(li=>{
      const statusBadge = li.querySelector('.status-badge');
      const chip = li.querySelector('.promote-chip');
      const btn  = li.querySelector('.promote-btn');

      const metrics = {
        likes: +li.dataset.likes || 0,
        saves: +li.dataset.saves || 0,
        rating: +li.dataset.rating || 0,
        reviews: +li.dataset.reviews || 0,
        views: +li.dataset.views || 0,
        days: +li.dataset.days || 0
      };

      const isOfficial = (li.dataset.status === 'official');
      if(isOfficial){
        statusBadge?.classList.remove('warn'); statusBadge?.classList.add('ok');
        statusBadge.textContent = '정식';
        chip?.classList.remove('wait'); chip?.classList.add('ok');
        chip.textContent = '충족';
        if (btn) btn.disabled = true;
        return;
      }

      // 임시 상태
      statusBadge?.classList.remove('ok'); statusBadge?.classList.add('warn');
      statusBadge.textContent = '임시';

      // 규칙 충족 여부(표시용)
      const ok = qualifiesByRule(rule, metrics);
      if(ok){
        chip?.classList.remove('wait'); chip?.classList.add('ok');
        chip.textContent = '승격 가능';
        if (btn) btn.disabled = false;
      }else{
        chip?.classList.remove('ok'); chip?.classList.add('wait');
        chip.textContent = '대기';
        if (btn) btn.disabled = false; // 수동 승격 허용
      }
    });
  }

  function bindPostRowActions(){
    // 개별 승격(모달 확인)
    $$('#postList .item .promote-btn').forEach(btn=>{
      btn.addEventListener('click', ()=>{
        const li = btn.closest('.item');
        if (!li) return;
        openPromoteModal([li.dataset.id], {
          likes: li.dataset.likes,
          saves: li.dataset.saves,
          rating: li.dataset.rating,
          reviews: li.dataset.reviews,
          views: li.dataset.views,
          days: li.dataset.days
        });
      });
    });
  }

  /* =========================
   * 댓글 섹션 (엔드포인트 확정되면 연결)
   * ========================= */
  function renderComments(list){
    const ul = $('#commentList'); if (!ul) return;
    ul.innerHTML = '';
    if (!list || !list.length){
      const li = document.createElement('li');
      li.className = 'item';
      li.innerHTML = `<div class="item-main"><div class="item-title">댓글 데이터 없음</div></div>`;
      ul.appendChild(li);
      return;
    }
    list.forEach(it=>{
      const li = document.createElement('li');
      li.className = 'item';
      li.dataset.id = it.id;
      li.innerHTML = `
        <div class="item-main">
          <div class="item-title">
            <span class="select-box">
              <input type="checkbox" class="rowchk-comment" />
              <span>[${esc(it.recipeId)}] ${esc(it.preview)}</span>
            </span>
          </div>
          <div class="item-meta">
            <span>CID: ${esc(it.id)}</span>
            <span>작성자: ${esc(it.author)}</span>
            <span>작성일: ${esc(fmtDate(it.createdAt))}</span>
          </div>
        </div>
        <div class="item-right">
          <button class="btn-ghost small" data-del-comment="${esc(it.id)}">삭제</button>
        </div>
      `;
      ul.appendChild(li);
    });
  }

  /* =========================
   * 선택/일괄 액션
   * ========================= */
  function bindBulkActions(){
    // 전체선택
    $('#checkAllPosts')?.addEventListener('change', (e)=>{
      $$('.rowchk-post').forEach(chk => chk.checked = e.target.checked);
    });
    $('#checkAllComments')?.addEventListener('change', (e)=>{
      $$('.rowchk-comment').forEach(chk => chk.checked = e.target.checked);
    });

    // 검색
    $('#btnSearch')?.addEventListener('click', ()=>{
      state.post.offset = 0;
      state.post.query = $('#q')?.value?.trim() || '';
      loadPosts();
    });

    // 일괄 승격
    $('#bulkPromote')?.addEventListener('click', ()=>{
      const ids = checkedPostIds();
      if(!ids.length) return alert('선택된 레시피가 없습니다.');
      openPromoteModal(ids);
    });

    // 선택 비공개(실서버 연결 필요 시 별도 API 사용)
    $('#hidePosts')?.addEventListener('click', ()=>{
      const ids = checkedPostIds();
      if(!ids.length) return alert('선택된 레시피가 없습니다.');
      alert('비공개 처리 엔드포인트 확정되면 연결 예정');
    });

    // 선택 삭제
    $('#delPosts')?.addEventListener('click', async ()=>{
      const ids = checkedPostIds();
      if(!ids.length) return alert('선택된 레시피가 없습니다.');
      if(!confirm(`삭제하시겠습니까? (${ids.length}개)`)) return;
      try{
        for (const id of ids){
          const res = await jfetch(deletePostAPI(id), { method:'DELETE' });
          if(!res.ok) throw new Error('HTTP '+res.status);
        }
        loadPosts();
      }catch(e){
        console.error(e); alert('삭제 실패: '+e.message);
      }
    });

    // 댓글 삭제(엔드포인트 확정 시 연결)
    document.addEventListener('click', async (e)=>{
      const btn = e.target.closest('[data-del-comment]');
      if(!btn) return;
      const id = btn.getAttribute('data-del-comment');
      if(!confirm(`댓글 ${id} 삭제?`)) return;
      alert('댓글 삭제 API 확정되면 연결 예정');
    });
  }

  function checkedPostIds(){
    const ids = [];
    $$('#postList .item').forEach(li=>{
      const chk = li.querySelector('.rowchk-post');
      if(chk?.checked) ids.push(li.dataset.id);
    });
    return ids;
  }

  /* =========================
   * 승격 모달
   * ========================= */
  function openPromoteModal(ids, m){
    const modal = $('#promoteModal');
    const info  = $('#promoteInfo');
    const title = ids.length === 1 ? ids[0] : `${ids.length}개 레시피`;
    const rule  = readRule();
    let lines = [`<strong>${title}</strong>을(를) 정식으로 승격하시겠습니까?`];
    if(m){
      lines.push(`<div style="margin-top:6px; font-size:13px; color:var(--muted)">
        likes ${m.likes}, saves ${m.saves}, rating ${m.rating}(${m.reviews}), views ${m.views}, days ${m.days}
      </div>`);
      if(rule){
        const ok = qualifiesByRule(rule, m);
        lines.push(`<div style="margin-top:6px; font-size:13px; color:${ok ? 'var(--green-700)' : '#E53935'}">
          규칙 판정: ${ok ? '충족' : '미충족 (수동 가능)'}
        </div>`);
      }
    }
    info.innerHTML = lines.join('');
    modal.setAttribute('aria-hidden','false');
    document.documentElement.style.overflow = 'hidden';

    $('#confirmPromote').onclick = async ()=>{
      modal.setAttribute('aria-hidden','true');
      document.documentElement.style.overflow = '';
      try{
        for (const id of ids){
          const res = await jfetch(publishAPI(id), { method:'POST' });
          if(!res.ok) throw new Error('HTTP '+res.status);
        }
        loadPosts();
      }catch(e){
        console.error(e); alert('승격 실패: '+e.message);
      }
    };

    // 닫기
    $$('[data-close]').forEach(b=> b.onclick = ()=> {
      modal.setAttribute('aria-hidden','true');
      document.documentElement.style.overflow = '';
    });
  }

  /* =========================
   * 로드
   * ========================= */
  const state = {
    post: { offset:0, limit:20, query:'' },
    comment: { offset:0, limit:20, query:'' }
  };

  async function loadPosts(){
    const res = await jfetch(postsAPI(state.post), { headers:{ 'Accept':'application/json' }});
    if (!res.ok) throw new Error('HTTP '+res.status);
    const list = await res.json(); // List<PostDto>
    renderPosts(Array.isArray(list) ? list : []);
    // 총량 API 없으면 '다음' 버튼은 응답 개수로 추정
    renderPostPager((list?.length ?? 0) >= state.post.limit);
  }

  function renderPostPager(hasMore){
    const pager = $('#postPager'); if (!pager) return;
    const disablePrev = state.post.offset <= 0;
    pager.innerHTML = `
      <button class="mini" ${disablePrev?'disabled':''} id="postPrev">이전</button>
      <span class="page-info">${Math.floor(state.post.offset/state.post.limit)+1}</span>
      <button class="mini" ${hasMore?'':'disabled'} id="postNext">다음</button>
    `;
    $('#postPrev')?.addEventListener('click', ()=>{ if (state.post.offset>0){ state.post.offset -= state.post.limit; loadPosts().catch(console.error); }});
    $('#postNext')?.addEventListener('click', ()=>{ if (hasMore){ state.post.offset += state.post.limit; loadPosts().catch(console.error); }});
  }

  async function loadComments(){
    // TODO: 엔드포인트 확정되면 fetch로 교체
    renderComments([]);
    renderCommentPager(false);
  }

  function renderCommentPager(hasMore){
    const pager = $('#commentPager'); if (!pager) return;
    const disablePrev = state.comment.offset <= 0;
    pager.innerHTML = `
      <button class="mini" ${disablePrev?'disabled':''} id="cPrev">이전</button>
      <span class="page-info">${Math.floor(state.comment.offset/state.comment.limit)+1}</span>
      <button class="mini" ${hasMore?'':'disabled'} id="cNext">다음</button>
    `;
    $('#cPrev')?.addEventListener('click', ()=>{ if (state.comment.offset>0){ state.comment.offset -= state.comment.limit; loadComments().catch(console.error); }});
    $('#cNext')?.addEventListener('click', ()=>{ if (hasMore){ state.comment.offset += state.comment.limit; loadComments().catch(console.error); }});
  }

  function bindGlobal(){ bindBulkActions(); }

  // ===== Init =====
  bindGlobal();
  Promise.all([loadPosts(), loadComments()]).catch(console.error);
})();
