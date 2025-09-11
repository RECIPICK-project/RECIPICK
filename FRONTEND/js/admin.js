// 색 팔레트 (프로젝트 컬러 고정)
const GREEN   = '#2E7D32';
const GREEN_L = '#C8E6C9';
const AMBER   = '#F4B942';
const GRAY    = '#9CA3AF';

// 디바이스 픽셀 비율(레티나 대응) — 2배까지만
const DPR = Math.min(window.devicePixelRatio || 1, 2);

// 캔버스를 부모(.chart-box) 크기에 맞추고 실제 픽셀 스케일도 맞춤
function fitCanvasToBox(canvas){
  const box = canvas.parentElement;
  const rect = box.getBoundingClientRect();
  const dpr  = DPR;
  canvas.style.width  = rect.width + 'px';
  canvas.style.height = rect.height + 'px';
  canvas.width  = Math.round(rect.width  * dpr);
  canvas.height = Math.round(rect.height * dpr);
}

// 공통 차트 옵션
function baseOptions(extra={}) {
  return Object.assign({
    responsive: true,
    maintainAspectRatio: false,
    devicePixelRatio: DPR,
    plugins: { legend: { display: false } },
  }, extra);
}

// 방문자 추이 (Line)
(function(){
  const el = document.getElementById('visitorsChart');
  if (!el) return;
  fitCanvasToBox(el);
  new Chart(el, {
    type: 'line',
    data: {
      labels: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'],
      datasets: [
        { label: '방문자', data: [820, 930, 880, 1020, 1200, 1500, 1340],
          borderColor: GREEN, backgroundColor: 'transparent', tension: .35, borderWidth: 2 },
        { label: '가입', data: [90, 110, 100, 120, 160, 210, 180],
          borderColor: AMBER, backgroundColor: 'transparent', tension: .35, borderDash: [4,4], borderWidth: 2 }
      ]
    },
    options: baseOptions({
      scales: {
        x: { grid: { display:false } },
        y: { grid: { color: 'rgba(0,0,0,.06)' }, ticks: { stepSize: 200 } }
      }
    })
  });
})();

// 카테고리 업로드 (Stacked Bar)
(function(){
  const el = document.getElementById('categoryChart');
  if (!el) return;
  fitCanvasToBox(el);
  new Chart(el, {
    type: 'bar',
    data: {
      labels: ['월','화','수','목','금','토','일'],
      datasets: [
        { label:'육류', data:[6,7,5,8,9,10,7], backgroundColor: GREEN },
        { label:'해물', data:[3,4,3,5,6,6,4], backgroundColor: AMBER },
        { label:'채소', data:[4,5,4,6,5,7,5], backgroundColor: GREEN_L }
      ]
    },
    options: baseOptions({
      scales: { x: { stacked:true }, y:{ stacked:true, grid:{ color:'rgba(0,0,0,.06)'}} }
    })
  });
})();

// 도넛 공통
function doughnut(id, labels, data, colors){
  const el = document.getElementById(id);
  if (!el) return null;
  fitCanvasToBox(el);
  return new Chart(el, {
    type: 'doughnut',
    data: { labels, datasets: [{ data, backgroundColor: colors, borderWidth: 0 }] },
    options: baseOptions({ cutout: '60%' })
  });
}
const deviceChart  = doughnut('deviceChart',  ['모바일','데스크탑'], [76,24], [GREEN, GREEN_L]);
const channelChart = doughnut('channelChart', ['검색','SNS','직접'],   [52,28,20], [AMBER, '#ffe08a', '#ffd166']);

// 기간 버튼(더미 이벤트)
document.querySelectorAll('.pill').forEach(p=>{
  p.addEventListener('click',()=>{
    document.querySelectorAll('.pill').forEach(x=>x.classList.remove('is-active'));
    p.classList.add('is-active');
    // TODO: 여기서 API 재호출 → 차트 data 갱신 → chart.update()
  });
});

// 리사이즈 시 캔버스 재적용(선명도 유지)
let resizeTimer;
window.addEventListener('resize', ()=>{
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(()=>{
    document.querySelectorAll('.chart-box canvas').forEach(c=>fitCanvasToBox(c));
    // Chart.js는 responsive:true라 컨테이너 변경을 인지하고 리사이즈됨
  }, 120);
});

// === 사이드바 접기/펴기 ===
const root = document.body; // .admin-layout
const toggleBtn = document.getElementById('toggleSide');
if (toggleBtn) {
  toggleBtn.addEventListener('click', ()=>{
    root.classList.toggle('side-collapsed');
    // 리사이즈 트리거해서 차트도 즉시 리레이아웃
    window.dispatchEvent(new Event('resize'));
  });
}

// === 신고 상세 모달 ===
const modal = document.getElementById('reportModal');
const fill = (sel, val)=>{ const el = document.querySelector(sel); if(el) el.textContent = val; };

function openReportModal(data){
  if (!modal) return;
  fill('#m-id', data.id || '-');
  fill('#m-reason', data.reason || '-');
  fill('#m-user', data.user || '-');
  fill('#m-time', data.time || '-');
  fill('#m-detail', data.detail || '-');
  modal.classList.remove('hidden');
}
function closeReportModal(){
  if (!modal) return;
  modal.classList.add('hidden');
}

// 보기 버튼 클릭 → 모달 오픈
document.querySelectorAll('.view-report').forEach(btn=>{
  btn.addEventListener('click', ()=>{
    openReportModal({
      id: btn.dataset.id,
      reason: btn.dataset.reason,
      user: btn.dataset.user,
      time: btn.dataset.time,
      detail: btn.dataset.detail
    });
  });
});

// 오버레이/닫기 버튼
modal?.addEventListener('click', (e)=>{
  if (e.target.dataset.close) closeReportModal();
});

// Esc 키로 닫기
window.addEventListener('keydown', (e)=>{
  if (e.key === 'Escape' && !modal?.classList.contains('hidden')) closeReportModal();
});
