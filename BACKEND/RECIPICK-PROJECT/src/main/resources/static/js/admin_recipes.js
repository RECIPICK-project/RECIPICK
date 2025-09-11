// admin_recipes.js — 레시피/댓글 관리 (더미/실서버 겸용)
(() => {
  const $  = (s, c=document) => c.querySelector(s);
  const $$ = (s, c=document) => Array.from(c.querySelectorAll(s));

  /* =========================
   * API (컨트롤러 시그니처 맞춤)
   * ========================= */
  // 임시(미승인) 레시피 목록
  const postsAPI = (q) => {
    const p = new URLSearchParams();
    p.set('offset', q.offset ?? 0);
    p.set('limit',  q.limit  ?? 20);
    if (q.query) p.set('q', q.query); // 검색 파라미터는 서비스에서 선택
    return `/admin/posts/pending?${p.toString()}`; // List<PostDto>
  };

  // 임시 → 정식 승격
  const publishAPI = (postId) => `/admin/posts/${postId}/publish`; // POST

  // 레시피 삭제
  const deletePostAPI = (postId) => `/admin/posts/${postId}`; // DELETE

  // 댓글 목록(관리용) — 컨트롤러에 명시 없어서 더미 우선, 필요 시 별도 엔드포인트 연결
  // const commentsAPI = (q) => `/admin/comments?...`

  /* =========================
   * Utils
   * ========================= */
  const esc = (s) => String(s)
      .replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')
      .replaceAll('"','&quot;').replaceAll("'",'&#39;');

  const fmtDate = (iso) => {
    if (!iso) return '-';
    const t = String(iso).replace('T',' ');
    return t.slice(0,16);
  };

  const daysBetween = (iso) => {
    try {
      const d = new Date(iso);
      const now = new Date();
      return Math.floor((now - d) / 86400000);
    } catch { return 0; }
  };

  /* =========================
   * 승격 규칙 (세팅에서 저장한 값)
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
    if(r.mode === 'manual') return false; // 수동이면 자동 판정 없음
    const fails = [];
    if(+m.days > +r.windowDays) fails.push('days');
    if(+m.likes < +r.minLikes) fails.push('likes');
    if(+m.saves < +r.minSaves) fails.push('saves');
    if(+m.rating < +r.minRating) fails.push('rating');
    if(+m.reviews < +r.minReviews) fails.push('reviews');
    if(+m.views < +r.minViews) fails.push('views');
    return fails.length === 0;
  }

  /* =========================
   * 렌더: 레시피 카드
   * ========================= */
  function renderPosts(list){
    const ul = $('#postList'); if (!ul) return;
    ul.innerHTML = '';

    list.forEach(row => {
      // PostDto 추정 필드: id, title, authorNickname/email, createdAt, likes, saves,
      // ratingAvg, ratingCount, views, status("TEMP"/"OFFICIAL" 등) …
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
          <a class="btn-ghost small" href="recipe-detail.html?id=${encodeURIComponent(id)}" target="_blank" rel="noopener">보기</a>
        </div>
      `;
      ul.appendChild(li);
    });

    // 규칙 적용/버튼 바인딩
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

      // 규칙 충족 여부
      const ok = qualifiesByRule(rule, metrics);
      if(ok){
        chip?.classList.remove('wait'); chip?.classList.add('ok');
        chip.textContent = '승격 가능';
        if (btn) btn.disabled = false;
      }else{
        chip?.classList.remove('ok'); chip?.classList.add('wait');
        chip.textContent = '대기';
        if (btn) btn.disabled = false; // 수동 승격 케이스
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
   * 댓글 섹션 (더미 우선)
   * ========================= */
  function renderComments(list){
    const ul = $('#commentList'); if (!ul) return;
    ul.innerHTML = '';
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

    // 선택 비공개(더미)
    $('#hidePosts')?.addEventListener('click', ()=>{
      const ids = checkedPostIds();
      if(!ids.length) return alert('선택된 레시피가 없습니다.');
      alert('선택 레시피 비공개(더미): '+ids.join(', '));
    });

    // 선택 삭제
    $('#delPosts')?.addEventListener('click', async ()=>{
      const ids = checkedPostIds();
      if(!ids.length) return alert('선택된 레시피가 없습니다.');
      if(!confirm(`삭제하시겠습니까? (${ids.length}개)`)) return;
      if (window.ADMIN_RECIPES_USE_DUMMY){
        alert('삭제(더미): '+ids.join(', '));
        // 화면에서 제거
        ids.forEach(id => $('#postList .item[data-id="'+CSS.escape(id)+'"]')?.remove());
        return;
      }
      try{
        for (const id of ids){
          const res = await fetch(deletePostAPI(id), { method:'DELETE' });
          if(!res.ok) throw new Error('HTTP '+res.status);
        }
        loadPosts();
      }catch(e){
        console.error(e); alert('삭제 실패: '+e.message);
      }
    });

    // 댓글 삭제(더미/실서버 훅)
    document.addEventListener('click', async (e)=>{
      const btn = e.target.closest('[data-del-comment]');
      if(!btn) return;
      const id = btn.getAttribute('data-del-comment');
      if(!confirm(`댓글 ${id} 삭제?`)) return;
      if (window.ADMIN_RECIPES_USE_DUMMY){
        alert('댓글 삭제(더미): '+id);
        btn.closest('.item')?.remove();
        return;
      }
      // TODO: 실제 삭제 API 연결 시 사용
      // await fetch(`/admin/reports/comments/${id}`, { method:'DELETE' })
      //   .then(r=>{ if(!r.ok) throw new Error('HTTP '+r.status); });
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

      if (window.ADMIN_RECIPES_USE_DUMMY){
        alert('승격(더미): '+ids.join(', '));
        // 화면에서 상태 업데이트
        ids.forEach(id=>{
          const li = $(`#postList .item[data-id="${CSS.escape(id)}"]`);
          if(!li) return;
          li.dataset.status = 'official';
          li.querySelector('.status-badge')?.classList.replace('warn','ok');
          li.querySelector('.status-badge').textContent = '정식';
          li.querySelector('.promote-chip')?.classList.replace('wait','ok');
          li.querySelector('.promote-chip').textContent = '충족';
          const b = li.querySelector('.promote-btn'); if (b) b.disabled = true;
        });
        return;
      }

      try{
        for (const id of ids){
          const res = await fetch(publishAPI(id), { method:'POST' });
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
    if (window.ADMIN_RECIPES_USE_DUMMY){
      renderPosts(DUMMY_POSTS.slice(state.post.offset, state.post.offset+state.post.limit));
      renderPostPager(DUMMY_POSTS.length);
      return;
    }
    const res = await fetch(postsAPI(state.post), { headers:{ 'Accept':'application/json' }});
    if (!res.ok) throw new Error('HTTP '+res.status);
    const list = await res.json(); // List<PostDto>
    renderPosts(list);
    // TODO: 총량 API가 따로 없으면 페이지네이션은 '다음 버튼' 방식 처리
    renderPostPager(list.length < state.post.limit ? state.post.offset + list.length : state.post.offset + state.post.limit + 1);
  }

  function renderPostPager(totalLike){
    const pager = $('#postPager'); if (!pager) return;
    const disablePrev = state.post.offset <= 0;
    const more = (window.ADMIN_RECIPES_USE_DUMMY)
        ? (state.post.offset + state.post.limit < DUMMY_POSTS.length)
        : (totalLike > state.post.offset + state.post.limit); // 대충 더 있음 판단
    pager.innerHTML = `
      <button class="mini" ${disablePrev?'disabled':''} id="postPrev">이전</button>
      <span class="page-info">${Math.floor(state.post.offset/state.post.limit)+1}</span>
      <button class="mini" ${more?'':'disabled'} id="postNext">다음</button>
    `;
    $('#postPrev')?.addEventListener('click', ()=>{ if (state.post.offset>0){ state.post.offset -= state.post.limit; loadPosts().catch(console.error); }});
    $('#postNext')?.addEventListener('click', ()=>{ if (more){ state.post.offset += state.post.limit; loadPosts().catch(console.error); }});
  }

  async function loadComments(){
    if (window.ADMIN_RECIPES_USE_DUMMY){
      renderComments(DUMMY_COMMENTS.slice(state.comment.offset, state.comment.offset+state.comment.limit));
      renderCommentPager(DUMMY_COMMENTS.length);
      return;
    }
    // TODO: 실제 댓글 목록 API 연결하면 여기서 fetch
    renderComments([]);
    renderCommentPager(0);
  }

  function renderCommentPager(total){
    const pager = $('#commentPager'); if (!pager) return;
    const disablePrev = state.comment.offset <= 0;
    const more = state.comment.offset + state.comment.limit < total;
    pager.innerHTML = `
      <button class="mini" ${disablePrev?'disabled':''} id="cPrev">이전</button>
      <span class="page-info">${Math.floor(state.comment.offset/state.comment.limit)+1}</span>
      <button class="mini" ${more?'':'disabled'} id="cNext">다음</button>
    `;
    $('#cPrev')?.addEventListener('click', ()=>{ if (state.comment.offset>0){ state.comment.offset -= state.comment.limit; loadComments().catch(console.error); }});
    $('#cNext')?.addEventListener('click', ()=>{ if (more){ state.comment.offset += state.comment.limit; loadComments().catch(console.error); }});
  }

  function bindGlobal(){
    bindBulkActions();
  }

  // ===== 더미 데이터 =====
  const DUMMY_POSTS = [
    { id:'r_550', title:'새송이 버터간장볶음', author:'cook99', createdAt:'2025-08-10T12:00:00',
      likes:124, saves:60, ratingAvg:4.6, ratingCount:22, views:1500, status:'TEMP' },
    { id:'r_551', title:'간단 토스트', author:'homecook', createdAt:'2025-08-06T09:00:00',
      likes:20, saves:8, ratingAvg:3.9, ratingCount:5, views:200, status:'TEMP' },
    { id:'r_100', title:'정식 레시피 예시', author:'master', createdAt:'2025-08-12T08:30:00',
      likes:230, saves:120, ratingAvg:4.8, ratingCount:88, views:12000, status:'OFFICIAL' },
  ];
  const DUMMY_COMMENTS = [
    { id:'c_901', recipeId:'r_550', preview:'맛있어요 👍', author:'user77', createdAt:'2025-08-20T10:10:00' },
    { id:'c_777', recipeId:'r_551', preview:'다음엔 설탕 줄이는 게 좋겠어요', author:'guest1', createdAt:'2025-08-21T09:40:00' },
  ];

  // ===== Init =====
  bindGlobal();
  Promise.all([loadPosts(), loadComments()]).catch(console.error);
})();
