// Admin Dashboard · AdminDashboardResponse DTO 연동
(() => {
  const $  = (s, c=document) => c.querySelector(s);
  const $$ = (s, c=document) => Array.from(c.querySelectorAll(s));
  const DPR = Math.min(window.devicePixelRatio || 1, 2);
  // ✅ 컨트롤러 시그니처에 맞춤: days / minReports / top
  const API = (days=7, minReports=3, top=5) =>
      `/admin/dashboard?days=${days}&minReports=${minReports}&top=${top}`;
  const charts = { visitors: null, category: null };

  const fitCanvas = (canvas) => {
    if (!canvas || !canvas.parentElement) return;
    const rect = canvas.parentElement.getBoundingClientRect();
    canvas.style.width  = rect.width + 'px';
    canvas.style.height = rect.height + 'px';
    canvas.width  = Math.round(rect.width  * DPR);
    canvas.height = Math.round(rect.height * DPR);
  };
  const fmtInt = (n) => (n==null ? '-' : Number(n).toLocaleString('ko-KR'));
  const fmtDate = (v) => {
    if (typeof v === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(v)) return v.slice(5).replace('-', '.');
    const d = new Date(v);
    return isNaN(d) ? String(v) : `${String(d.getMonth()+1).padStart(2,'0')}.${String(d.getDate()).padStart(2,'0')}`;
  };
  const esc = (s) => String(s)
      .replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;')
      .replaceAll('"','&quot;').replaceAll("'",'&#39;');

  // ===== KPI =====
  function renderKpis(dto){
    const lastVisitors = dto?.visitorTrend?.data?.slice(-1)[0] ?? null;
    const signupCount  = dto?.recentSignups?.length ?? 0;
    const recipeSum    = (dto?.categoryUploads||[]).reduce((a,b)=> a + (b?.count||0), 0);
    const reportCount  = dto?.recentReports?.length ?? 0;

    $('#kpi-dau')?.replaceChildren(document.createTextNode(fmtInt(lastVisitors)));
    $('#kpi-signup')?.replaceChildren(document.createTextNode(fmtInt(signupCount)));
    $('#kpi-recipes')?.replaceChildren(document.createTextNode(fmtInt(recipeSum)));
    $('#kpi-reports')?.replaceChildren(document.createTextNode(fmtInt(reportCount)));

    // 보조 문구
    $('#kpi-signup-sub') && ($('#kpi-signup-sub').textContent =
        (typeof dto?.totalUsers === 'number') ? `총 사용자 ${fmtInt(dto.totalUsers)}` : '');

    $('#kpi-recipes-sub') && ($('#kpi-recipes-sub').textContent =
        (typeof dto?.totalRecipes === 'number') ? `총 레시피 ${fmtInt(dto.totalRecipes)}` : '');

    $('#kpi-reports-sub') && ($('#kpi-reports-sub').textContent =
        (typeof dto?.reportedRecipesOverThreshold === 'number') ? `임계 초과 ${fmtInt(dto.reportedRecipesOverThreshold)}` : '');
  }

  // ===== Charts =====
  function renderCharts(dto){
    // 방문자 추이(Line)
    const vEl = $('#visitorsChart');
    if (vEl) {
      fitCanvas(vEl);
      charts.visitors?.destroy?.();
      charts.visitors = new Chart(vEl, {
        type: 'line',
        data: {
          labels: (dto?.visitorTrend?.labels||[]).map(fmtDate),
          datasets: [{
            label: '방문자',
            data: dto?.visitorTrend?.data || [],
            borderColor: '#2E7D32', backgroundColor: 'transparent', borderWidth: 2, tension: .35
          }]
        },
        options: {
          responsive:true, maintainAspectRatio:false, devicePixelRatio:DPR,
          plugins:{ legend:{ display:false } },
          scales:{ x:{ grid:{ display:false } }, y:{ grid:{ color:'rgba(0,0,0,.06)'} } }
        }
      });
    }

    // 카테고리 업로드(Bar)
    const cEl = $('#categoryChart');
    if (cEl) {
      fitCanvas(cEl);
      charts.category?.destroy?.();
      const cats = dto?.categoryUploads || [];
      charts.category = new Chart(cEl, {
        type: 'bar',
        data: {
          labels: cats.map(x=>x?.category ?? '-'),
          datasets: [{ label:'업로드', data: cats.map(x=>x?.count ?? 0), backgroundColor:'#4CAF50' }]
        },
        options: {
          responsive:true, maintainAspectRatio:false, devicePixelRatio:DPR,
          plugins:{ legend:{ display:false } },
          scales:{ x:{ grid:{ display:false } }, y:{ grid:{ color:'rgba(0,0,0,.06)'} } }
        }
      });
    }
  }

  // ===== Tables =====
  function renderTables(dto){
    // 최근 신고
    const $r = $('#recentReportsTbody');
    if ($r) {
      $r.innerHTML = '';
      (dto?.recentReports || []).forEach(it=>{
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>#${esc(it?.id ?? '-')}</td>
          <td>${esc(it?.reason ?? '')}</td>
          <td>${esc(it?.targetType ?? '')} · ${esc(String(it?.targetId ?? ''))}</td>
          <td>${esc(String(it?.createdAt ?? ''))}</td>
          <td class="act">
            <button class="mini view-report"
              data-id="${esc(it?.id ?? '')}"
              data-reason="${esc(it?.reason ?? '')}"
              data-user="-"
              data-time="${esc(String(it?.createdAt ?? ''))}"
              data-detail="${esc(it?.targetType ?? '')} · ${esc(String(it?.targetId ?? ''))}"
            >보기</button>
            <button class="mini warn">차단</button>
            <button class="mini ghost">무시</button>
          </td>
        `;
        $r.appendChild(tr);
      });
    }

    // 최근 가입
    const $s = $('#recentSignupsTbody');
    if ($s) {
      $s.innerHTML = '';
      (dto?.recentSignups || []).forEach(it=>{
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${esc(it?.nicknameOrEmail ?? '')}</td>
          <td>${esc(String(it?.userId ?? ''))}</td>
          <td>${esc(String(it?.createdAt ?? ''))}</td>
          <td><span class="badge ok">정상</span></td>
        `;
        $s.appendChild(tr);
      });
    }
  }

  // ===== Report Modal =====
  function bindReportModal(){
    const modal = $('#reportModal');
    if (!modal) return;
    const open = (btn) => {
      $('#m-id').textContent     = btn.dataset.id || '-';
      $('#m-reason').textContent = btn.dataset.reason || '-';
      $('#m-user').textContent   = btn.dataset.user || '-';
      $('#m-time').textContent   = btn.dataset.time || '-';
      $('#m-detail').textContent = btn.dataset.detail || '-';
      modal.classList.remove('hidden');
    };
    document.addEventListener('click', (e)=>{
      const btn = e.target.closest('.view-report');
      if (btn) open(btn);
      if (e.target.closest('[data-close]')) modal.classList.add('hidden');
    });
  }

  // ===== Load =====
  async function loadByDays(days){
    try{
      const res = await fetch(API(days), { headers:{ 'Accept':'application/json' }});
      if(!res.ok) throw new Error('HTTP '+res.status);
      const dto = await res.json();
      renderKpis(dto); renderCharts(dto); renderTables(dto);
    }catch(e){
      console.error(e);
      renderKpis(null); renderCharts({}); renderTables({});
    }
  }
  // pill → days 매핑
  const rangeToDays = (r) => (r==='1d'?1 : r==='7d'?7 : 30);

  // ===== Init =====
  bindReportModal();
  $$('.pill').forEach(p=>{
    p.addEventListener('click', ()=>{
      $$('.pill').forEach(x=>x.classList.remove('is-active'));
      p.classList.add('is-active');
      const days = rangeToDays(p.dataset.range || '7d');
      // 더미 사용 시
      if (window.ADMIN_USE_DUMMY) return window.renderDashboard?.(window.ADMIN_DUMMY);
      loadByDays(days);
    });
  });

  const initialDays = rangeToDays(document.querySelector('.pill.is-active')?.dataset.range || '7d');
  if (window.ADMIN_USE_DUMMY) window.renderDashboard?.(window.ADMIN_DUMMY);
  else loadByDays(initialDays);



  // 리사이즈 선명도
  let rTimer;
  window.addEventListener('resize', ()=>{
    clearTimeout(rTimer);
    rTimer = setTimeout(()=>{
      ['#visitorsChart','#categoryChart'].forEach(sel=>{
        const c = $(sel); if (c) fitCanvas(c);
      });
    }, 120);
  });
})();
