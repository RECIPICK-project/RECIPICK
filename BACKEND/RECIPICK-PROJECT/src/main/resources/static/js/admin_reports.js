// admin_reports.js — AdminController 시그니처 연동 (하드코딩 없음)
(() => {
  const $  = (s, c=document) => c.querySelector(s);
  const $$ = (s, c=document) => Array.from(c.querySelectorAll(s));

  // ===== Query 상태 =====
  const q = {
    status: 'PENDING',   // PENDING | ACCEPTED | REJECTED
    type:   '',          // POST | REVIEW | COMMENT | USER | ''(전체)
    page:   0,
    size:   20,
  };

  // ===== API =====
  const listAPI = () => {
    const p = new URLSearchParams();
    p.set('status', q.status || 'PENDING');
    if (q.type) p.set('type', q.type);
    p.set('page', q.page);
    p.set('size', q.size);
    return `/admin/reports?${p.toString()}`; // Page<ReportEntity> (권장: Page<AdminReportResponse>)
  };
  const moderateAPI = (id) => `/admin/reports/${id}`; // PATCH {action:ACCEPT|REJECT}

  // ===== Utils =====
  const esc = (s) => String(s)
      .replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')
      .replaceAll('"','&quot;').replaceAll("'",'&#39;');
  const fmtDT = (v) => {
    if (!v) return '-';
    const t = String(v);
    return t.includes('T') ? t.replace('T',' ').slice(0,16) : t;
  };
  const badge = (status) => {
    const s = String(status||'').toUpperCase();
    if (s==='PENDING')  return '<span class="badge warn">대기</span>';
    if (s==='ACCEPTED') return '<span class="badge ok">수리</span>';
    if (s==='REJECTED') return '<span class="badge">반려</span>';
    return '<span class="badge">-</span>';
  };

  // ===== Render: KPI(선택) =====
  function renderKpiFromPage(page){
    // 정확한 KPI API 없으므로, 페이지 콘텐츠 기준으로만 대략 표시 (원하면 백에서 KPI 엔드포인트 추가 권장)
    const todayEl = $('#kpi-today');
    const pendingEl = $('#kpi-pending');
    if (!todayEl && !pendingEl) return;

    const content = page?.content || [];
    // createdAt이 "오늘"인 것만 세고 싶으면 로직 추가 가능 (지금은 단순히 페이지 크기 사용)
    if (todayEl) todayEl.textContent = String(content.length ?? 0);
    if (pendingEl) {
      const cnt = content.filter(r => String(r.status).toUpperCase() === 'PENDING').length;
      pendingEl.textContent = String(cnt);
    }
  }

  // ===== Render: table =====
  function renderTable(page){
    const tbody = $('#reportsTbody'); if (!tbody) return;
    const content = page?.content || [];
    tbody.innerHTML = '';

    content.forEach(row => {
      // AdminReportResponse로 바꾸면 reporter / targetPreview 사용 가능.
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>#${esc(row.id)}</td>
        <td>${esc(row.reason||'')}</td>
        <td>${esc(row.targetType||'')} · ${esc(row.targetId||'')}</td>
        <td>${fmtDT(row.createdAt)}</td>
        <td>${badge(row.status)}</td>
        <td class="act">
          <button class="mini view-report"
            data-id="${esc(row.id)}"
            data-reason="${esc(row.reason||'')}"
            data-user="${esc(row.reporter||'-')}"
            data-time="${fmtDT(row.createdAt)}"
            data-detail="${esc(row.targetType||'')} · ${esc(row.targetId||'')}"
          >보기</button>
          ${String(row.status).toUpperCase()==='PENDING'
          ? `<button class="mini warn" data-accept="${esc(row.id)}">수리</button>
               <button class="mini ghost" data-reject="${esc(row.id)}">반려</button>`
          : `<button class="mini ghost" disabled>처리완료</button>`
      }
        </td>
      `;
      tbody.appendChild(tr);
    });

    renderPagination(page);
    renderKpiFromPage(page);
  }

  // ===== Render: pagination =====
  function renderPagination(page){
    const wrap = $('.pagination'); if (!wrap) return;
    const totalPages = page?.totalPages ?? 1;
    const number     = page?.number ?? 0;

    wrap.innerHTML = `
      <button class="mini" ${number<=0 ? 'disabled' : ''} data-nav="prev">이전</button>
      <span class="page-info">${number+1} / ${totalPages}</span>
      <button class="mini" ${number>=totalPages-1 ? 'disabled' : ''} data-nav="next">다음</button>
    `;
  }

  // ===== Modal =====
  function bindModal(){
    const modal = $('#reportModal'); if (!modal) return;
    const open = (btn) => {
      $('#m-id').textContent     = btn.dataset.id || '-';
      $('#m-reason').textContent = btn.dataset.reason || '-';
      $('#m-user').textContent   = btn.dataset.user || '-';
      $('#m-time').textContent   = btn.dataset.time || '-';
      $('#m-detail').textContent = btn.dataset.detail || '-';

      // 모달에 현재 대상 ID 보관(조치용)
      modal.dataset.currentId = btn.dataset.id || '';

      modal.classList.remove('hidden');
      document.documentElement.style.overflow = 'hidden';
    };
    document.addEventListener('click', (e)=>{
      const openBtn = e.target.closest('.view-report');
      if (openBtn) open(openBtn);
      if (e.target.closest('[data-close]')) {
        modal.classList.add('hidden');
        document.documentElement.style.overflow = '';
      }
    });

    // 모달 내 수리/반려 버튼
    $('#modal-accept')?.addEventListener('click', async ()=>{
      const id = modal.dataset.currentId;
      if (!id) return;
      try {
        await moderate(id, 'ACCEPT');
        alert('수리 처리되었습니다.');
        modal.classList.add('hidden');
        document.documentElement.style.overflow = '';
        reload();
      } catch(e){ alert('처리 실패: '+e.message); }
    });
    $('#modal-reject')?.addEventListener('click', async ()=>{
      const id = modal.dataset.currentId;
      if (!id) return;
      try {
        await moderate(id, 'REJECT');
        alert('반려 처리되었습니다.');
        modal.classList.add('hidden');
        document.documentElement.style.overflow = '';
        reload();
      } catch(e){ alert('처리 실패: '+e.message); }
    });
  }

  // ===== Actions (ACCEPT / REJECT) — 행 버튼 =====
  async function moderate(id, action){
    const res = await fetch(moderateAPI(id), {
      method: 'PATCH',
      headers: { 'Content-Type':'application/json' },
      body: JSON.stringify({ action }) // ReportModerateRequest
    });
    if (!res.ok) throw new Error('HTTP '+res.status);
  }

  function bindRowActions(){
    document.addEventListener('click', async (e)=>{
      const acc = e.target.closest('button[data-accept]');
      const rej = e.target.closest('button[data-reject]');
      try{
        if (acc){
          await moderate(acc.dataset.accept, 'ACCEPT');
          alert('수리 처리되었습니다.');
          reload();
        } else if (rej){
          await moderate(rej.dataset.reject, 'REJECT');
          alert('반려 처리되었습니다.');
          reload();
        }
      }catch(err){
        console.error(err);
        alert('처리 실패: '+err.message);
      }
    });

    // 페이지네이션
    document.addEventListener('click', (e)=>{
      const nav = e.target.closest('button[data-nav]');
      if (!nav) return;
      if (nav.dataset.nav==='prev' && q.page>0) q.page--;
      if (nav.dataset.nav==='next') q.page++;
      reload();
    });
  }

  // ===== Filters =====
  function bindFilters(){
    $('#f-status')?.addEventListener('change', (e)=>{
      q.status = e.target.value || 'PENDING';
      q.page = 0; reload();
    });
    $('#f-type')?.addEventListener('change', (e)=>{
      q.type = e.target.value || '';
      q.page = 0; reload();
    });
  }

  // ===== Load / Reload =====
  async function load(){
    // 더미 모드 지원 (원하면 HTML head에서 window.REPORTS_USE_DUMMY/REPORTS_DUMMY 넣어 테스트 가능)
    if (window.REPORTS_USE_DUMMY) {
      renderTable(window.REPORTS_DUMMY);
      return;
    }
    const res = await fetch(listAPI(), { headers:{ 'Accept':'application/json' }});
    if (!res.ok) throw new Error('HTTP '+res.status);
    const page = await res.json(); // Page<ReportEntity> or Page<AdminReportResponse>
    renderTable(page);
  }
  const reload = () => load().catch(e=>{ console.error(e); });

  // ===== Init =====
  bindModal();
  bindRowActions();
  bindFilters();
  load().catch(e=>console.error(e));
})();
