(() => {
  const hero = document.getElementById('hero');
  const slides = document.getElementById('slides');

  let cur = 0, timer = null;
  const INTERVAL = 4000, SWIPE_THRESHOLD = 40;

  const getDots = () => Array.from(document.querySelectorAll('.dot'));
  const getTotal = () => getDots().length;

  function goTo(i) {
    const total = getTotal();
    if (!slides || total === 0) return;
    cur = (i + total) % total;
    slides.style.transform = `translateX(-${cur * 100}%)`;
    const dots = getDots();
    dots.forEach(d => d.classList.remove('is-active'));
    dots[cur]?.classList.add('is-active');
  }
  function next() { goTo(cur + 1); }
  function prev() { goTo(cur - 1); }
  function start() { stop(); timer = setInterval(next, INTERVAL); }
  function stop() { if (timer) { clearInterval(timer); timer = null; } }

  // 도트 클릭(동적 생성 대응: 문서에 위임)
  document.addEventListener('click', (e) => {
    const dot = e.target.closest('.dot');
    if (!dot) return;
    const i = Number(dot.dataset.i);
    if (!Number.isFinite(i)) return;
    goTo(i); start();
  });

  // hero가 있을 때만 모든 이벤트 바인딩
  if (hero) {
    hero.addEventListener('mouseenter', stop);
    hero.addEventListener('mouseleave', start);

    let sx = 0, dx = 0, dragging = false;

    hero.addEventListener('touchstart', (e) => {
      stop(); sx = e.touches[0].clientX; dx = 0; dragging = true;
      hero.classList.add('dragging');
    }, { passive: true });

    hero.addEventListener('touchmove', (e) => {
      if (!dragging || !slides) return;
      dx = e.touches[0].clientX - sx;
      slides.style.transform = `translateX(calc(-${cur * 100}% + ${dx}px))`;
    }, { passive: true });

    hero.addEventListener('touchend', () => {
      hero.classList.remove('dragging');
      Math.abs(dx) > SWIPE_THRESHOLD ? (dx < 0 ? next() : prev()) : goTo(cur);
      dragging = false; start();
    });

    let isDown = false;
    hero.addEventListener('mousedown', (e) => {
      isDown = true; stop(); sx = e.clientX; dx = 0;
      hero.classList.add('dragging');
    });
    window.addEventListener('mousemove', (e) => {
      if (!isDown || !slides) return;
      dx = e.clientX - sx;
      slides.style.transform = `translateX(calc(-${cur * 100}% + ${dx}px))`;
    });
    window.addEventListener('mouseup', () => {
      if (!isDown) return;
      hero.classList.remove('dragging');
      Math.abs(dx) > SWIPE_THRESHOLD ? (dx < 0 ? next() : prev()) : goTo(cur);
      isDown = false; start();
    });
  }

  // —— 초기화 타이밍: DOMContentLoaded 이후, .dots 변화만 관찰 ——
  document.addEventListener('DOMContentLoaded', () => {
    const dotsWrap = document.querySelector('.dots');

    // 즉시 가능한지 시도
    if (getTotal() > 0) { goTo(0); start(); return; }

    if (dotsWrap) {
      const observer = new MutationObserver(() => {
        if (getTotal() > 0) { goTo(0); start(); observer.disconnect(); }
      });
      observer.observe(dotsWrap, { childList: true });
    } else {
      // 폴백: 짧은 폴링으로 2초간 재시도
      let tries = 0;
      const id = setInterval(() => {
        if (getTotal() > 0) { goTo(0); start(); clearInterval(id); }
        else if (++tries > 20) { clearInterval(id); /* 포기 */ }
      }, 100);
    }
  });

})(); // IIFE 끝; 위 줄 앞 문장 끝에는 세미콜론이 이미 있음(호출 안전)

// ----- Bottom Sheet Controls (Menu) -----
function openSlideMenu() {
  const sheet = document.getElementById('slideMenu');
  if (!sheet) return;

  // 먼저 보여주고 다음 프레임에 active → 트랜지션 확실
  sheet.style.visibility = 'visible';
  sheet.style.opacity = '1';
  requestAnimationFrame(() => {
    sheet.classList.add('active');
    document.body.style.overflow = 'hidden'; // 배경 스크롤 잠금
  });
}

function closeSlideMenu() {
  const sheet = document.getElementById('slideMenu');
  if (!sheet) return;

  sheet.classList.remove('active');
  document.body.style.overflow = '';
  // 애니 끝나고 완전 숨김
  setTimeout(() => {
    if (!sheet.classList.contains('active')) {
      sheet.style.opacity = '0';
      sheet.style.visibility = 'hidden';
    }
  }, 300);
}

function toggleSlideMenu() {
  const sheet = document.getElementById('slideMenu');
  if (!sheet) return;
  sheet.classList.contains('active') ? closeSlideMenu() : openSlideMenu();
}

// 초기 바인딩 (오버레이/ESC 닫기)
document.addEventListener('DOMContentLoaded', () => {
  const sheet = document.getElementById('slideMenu');
  if (!sheet) return;

  const overlay = sheet.querySelector('.slide-menu-overlay');
  if (overlay) overlay.addEventListener('click', closeSlideMenu);

  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeSlideMenu();
  });
});
