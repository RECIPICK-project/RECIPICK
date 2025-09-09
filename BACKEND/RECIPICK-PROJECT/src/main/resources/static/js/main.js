  (() => {
  const slides = document.getElementById('slides');
  const dots = Array.from(document.querySelectorAll('.dot'));
  const hero = document.getElementById('hero');

  const TOTAL = dots.length;
  let cur = 0;
  let timer = null;
  const INTERVAL = 4000;   // 자동 전환 주기 (ms)
  const SWIPE_THRESHOLD = 40; // 스와이프 판정 픽셀

  function goTo(i){
  cur = (i + TOTAL) % TOTAL;
  slides.style.transform = `translateX(-${cur * 100}%)`;
  dots.forEach(d => d.classList.remove('is-active'));
  dots[cur].classList.add('is-active');
}
  function next(){ goTo(cur + 1); }
  function prev(){ goTo(cur - 1); }

  function start(){
  stop();
  timer = setInterval(next, INTERVAL);
}
  function stop(){
  if(timer){ clearInterval(timer); timer = null; }
}

  // dots 클릭
  dots.forEach(d => d.addEventListener('click', () => {
  goTo(+d.dataset.i);
  start();
}));

  // 호버 시 일시정지(모바일에선 영향 거의 없음)
  hero.addEventListener('mouseenter', stop);
  hero.addEventListener('mouseleave', start);

  // 스와이프(터치)
  let sx = 0, dx = 0, dragging = false;

  hero.addEventListener('touchstart', (e)=>{
  stop();
  sx = e.touches[0].clientX; dx = 0; dragging = true;
  hero.classList.add('dragging');
}, {passive:true});

  hero.addEventListener('touchmove', (e)=>{
  if(!dragging) return;
  const x = e.touches[0].clientX;
  dx = x - sx;
  slides.style.transform = `translateX(calc(-${cur*100}% + ${dx}px))`;
}, {passive:true});

  hero.addEventListener('touchend', ()=>{
  hero.classList.remove('dragging');
  if(Math.abs(dx) > SWIPE_THRESHOLD){
  dx < 0 ? next() : prev();
}else{
  goTo(cur); // 원위치
}
  dragging = false; start();
});

  // 드래그(마우스) - 선택 사항
  let isDown = false;
  hero.addEventListener('mousedown', (e)=>{
  isDown = true; stop(); sx = e.clientX; dx = 0;
  hero.classList.add('dragging');
});
  window.addEventListener('mousemove', (e)=>{
  if(!isDown) return;
  dx = e.clientX - sx;
  slides.style.transform = `translateX(calc(-${cur*100}% + ${dx}px))`;
});
  window.addEventListener('mouseup', ()=>{
  if(!isDown) return;
  hero.classList.remove('dragging');
  if(Math.abs(dx) > SWIPE_THRESHOLD){
  dx < 0 ? next() : prev();
}else{
  goTo(cur);
}
  isDown = false; start();
});

  // 초기 시작
  goTo(0);
  start();
})();