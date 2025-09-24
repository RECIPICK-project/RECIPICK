(function() {
  'use strict';

  // API 관련 유틸리티
  const API_BASE = '';
  const USE_CREDENTIALS = true;

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

  function getCsrfHeaders(method = 'GET') {
    const m = method.toUpperCase();
    if (['GET', 'HEAD', 'OPTIONS', 'TRACE'].includes(m)) return {};

    const rawHeader = document.querySelector('meta[name="_csrf_header"]')?.content || '';
    const rawToken  = document.querySelector('meta[name="_csrf"]')?.content || '';
    const header = rawHeader.trim();
    const token  = rawToken.trim();

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
    const isFormData = (opts.body && typeof FormData !== 'undefined' && opts.body instanceof FormData);

    const headers = {
      Accept: 'application/json',
      ...authHeader(),
      ...getCsrfHeaders(method),
      ...(opts.headers || {}),
    };

    if (isFormData && headers['Content-Type']) {
      delete headers['Content-Type'];
    }

    const cred = USE_CREDENTIALS ? { credentials: 'include' } : {};
    return fetch(`${API_BASE}${path}`, { ...opts, headers, ...cred });
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

  // === 로그인 상태 체크 함수들 (무한루프 방지) ===
  let isCheckingLogin = false; // 중복 체크 방지 플래그
  let lastLoginState = null; // 마지막 로그인 상태 캐시

  async function getCurrentUserInfo() {
    try {
      const res = await fx('/api/users/review_check');
      
      if (res.ok) {
        const userProfile = await res.json();
        return {
          loggedIn: true,
          email: userProfile.email,
          role: userProfile.role,
          userId: userProfile.userId,
          nickname: userProfile.nickname
        };
      } else if (res.status === 401) {
        // 401 응답 시 토큰 정리
        localStorage.removeItem('ACCESS_TOKEN');
        sessionStorage.removeItem('ACCESS_TOKEN');
        return { loggedIn: false };
      } else {
        return { loggedIn: false };
      }
    } catch (error) {
      // 네트워크 오류 등의 경우 토큰 정리
      localStorage.removeItem('ACCESS_TOKEN');
      sessionStorage.removeItem('ACCESS_TOKEN');
    }
    return { loggedIn: false };
  }

  function hasValidToken() {
    const token = localStorage.getItem('ACCESS_TOKEN') || sessionStorage.getItem('ACCESS_TOKEN');
    if (!token) return false;
    
    // 간단한 JWT 형식 체크 (3개 부분으로 나뉘어진 구조)
    const parts = token.replace(/^Bearer\s+/i, '').trim().split('.');
    const isValid = parts.length === 3;
    return isValid;
  }

  async function checkLoginStatus() {
    // 중복 체크 방지
    if (isCheckingLogin) {
      return lastLoginState || false;
    }

    isCheckingLogin = true;
    
    try {
      // 서버에 실제 로그인 상태 확인 (가장 확실한 방법)
      const userInfo = await getCurrentUserInfo();
      lastLoginState = userInfo.loggedIn;
      return userInfo.loggedIn;
    } catch (error) {
      lastLoginState = false;
      return false;
    } finally {
      isCheckingLogin = false;
    }
  }

  // === 로그인 상태에 따른 메뉴 업데이트 (무한루프 방지) ===
  function updateMenuButtonOnly(sheet, isLoggedIn) {
    
    const authButton = sheet.querySelector('#commonAuthBtn');
    
    if (authButton) {
      if (isLoggedIn) {
        authButton.textContent = '로그아웃';
        authButton.className = 'item danger';
        authButton.onclick = handleLogout;
      } else {
        authButton.textContent = '로그인';
        authButton.className = 'item';
        authButton.onclick = handleLogin;
      }
    }
  }

  async function updateMenuForLoginState(sheet) {
    // 캐시 무효화하여 최신 상태 확인
    lastLoginState = null;
    isCheckingLogin = false;
    
    const loggedIn = await checkLoginStatus();
    updateMenuButtonOnly(sheet, loggedIn);
  }

  // 통합 하단 탭바 생성 함수
  function createUnifiedTabbar() {
    // 기존 탭바가 있으면 제거
    const existingTabbar = document.querySelector('.bottom-tabbar');
    if (existingTabbar) {
      existingTabbar.remove();
    }

    const tabbar = document.createElement('nav');
    tabbar.className = 'bottom-tabbar';
    tabbar.setAttribute('aria-label', '하단 탭바');
    
    // 현재 페이지에 따라 active 클래스 결정
    const currentPath = window.location.pathname;
    const getActiveClass = (tabName) => {
      switch(tabName) {
        case 'search': return currentPath.includes('search') ? 'active' : '';
        case 'profile': return currentPath.includes('profile') ? 'active' : '';
        case 'add': return currentPath.includes('upload') || currentPath.includes('add') ? 'active' : '';
        default: return '';
      }
    };

    tabbar.innerHTML = `
      <div class="tab-item" data-tab="menu" onclick="toggleSlideMenu()">
        <svg class="tab-icon" viewBox="0 0 24 24">
          <path d="M3 12h18M3 6h18M3 18h18" />
        </svg>
        <span class="tab-label">메뉴</span>
      </div>

      <div class="tab-item" data-tab="favorite" onclick="goToPage('/favorites')">
        <svg class="tab-icon" viewBox="0 0 24 24">
          <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
        </svg>
        <span class="tab-label">찜</span>
      </div>

      <div class="tab-item" data-tab="search" onclick="goToPage('/search')">
        <svg class="tab-icon search" viewBox="0 0 24 24">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
      </div>

      <div class="tab-item" data-tab="add" onclick="goToPage('/add-recipe')">
        <svg class="tab-icon" viewBox="0 0 24 24">
          <path d="M12 5v14M5 12h14" />
        </svg>
        <span class="tab-label">추가</span>
      </div>

      <div class="tab-item" data-tab="profile" onclick="goToPage('/profile')">
        <svg class="tab-icon" viewBox="0 0 24 24">
          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
          <circle cx="12" cy="7" r="4" />
        </svg>
        <span class="tab-label">프로필</span>
      </div>
    `;

    document.body.appendChild(tabbar);
  }

  // 통합 메뉴 시트 생성 함수 (로그인 상태 반영) - 수정된 버전
  async function createUnifiedMenuSheet() {
    // 기존 시트가 있으면 제거
    const existingSheet = document.getElementById('commonMenuSheet');
    if (existingSheet) {
      existingSheet.remove();
    }

    const sheet = document.createElement('div');
    sheet.id = 'commonMenuSheet';
    sheet.className = 'sheet';
    sheet.setAttribute('aria-hidden', 'true');
    
    // 초기 로그인 상태 확인 - 캐시 무효화하여 최신 상태 확인
    lastLoginState = null;
    isCheckingLogin = false;
    const loggedIn = await checkLoginStatus();
    
    const authButtonText = loggedIn ? '로그아웃' : '로그인';
    const authButtonClass = loggedIn ? 'item danger' : 'item';
    
    sheet.innerHTML = `
      <div class="sheet-handle" aria-hidden="true"></div>
      <nav class="sheet-list" aria-label="메뉴 목록">
        <a href="/pages/main.html" class="item">홈으로</a>
        <a href="/pages/faq.html" class="item">자주 묻는 질문</a>
        <a href="/pages/official_recipes.html" class="item">정식 레시피</a>
        <a href="/pages/setting.html" class="item">설정</a>
        <button id="commonAuthBtn" class="${authButtonClass}" type="button">${authButtonText}</button>
      </nav>
    `;

    document.body.appendChild(sheet);

    // 이벤트 리스너 등록
    sheet.addEventListener('click', (e) => {
      if (e.target === sheet) closeMenuSheet();
    });

    // 버튼 클릭 이벤트 설정
    updateMenuButtonOnly(sheet, loggedIn);
  }

  function injectUnifiedStyles() {
    const existingStyle = document.getElementById('unified-tabbar-styles');
    if (existingStyle) {
      existingStyle.remove();
    }

    const style = document.createElement('style');
    style.id = 'unified-tabbar-styles';
    style.textContent = `
      :root {
        --green-700: #2e7d32;
        --green-600: #388e3c;
        --green-500: #4caf50;
        --mint-200: #d1fae5;
        --bg: #f1f8e9;
        --text: #111827;
        --muted: #6b7280;
        --line: #e5e7eb;
        --soft: #f9fbe7;
      }

      .bottom-tabbar {
        position: fixed;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 800px;
        max-width: 95vw;
        height: 80px;
        background: #fff;
        border-top: 1px solid var(--line);
        border-radius: 20px 20px 0 0;
        box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.1);
        display: flex;
        align-items: center;
        justify-content: space-around;
        padding: 0 20px;
        z-index: 1000;
      }

      .tab-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        gap: 4px;
        cursor: pointer;
        transition: all 0.2s ease;
        padding: 8px;
        border-radius: 12px;
        min-width: 50px;
      }

      .tab-item:hover {
        background: var(--soft);
      }

      .tab-item.active {
        background: var(--mint-200);
      }

      .tab-item.active .tab-icon {
        stroke: var(--green-700);
        fill: var(--green-700);
      }

      .tab-icon {
        width: 28px;
        height: 28px;
        stroke: var(--muted);
        fill: none !important; 
        stroke-width: 2;
        transition: all 0.2s ease;
      }

      .tab-icon.search {
        stroke: var(--green-700) !important;
        fill: none !important; 
        padding: 12px !important;
        width: 48px;
        height: 48px;
        border-radius: 50%;
        background: var(--mint-200);
        display: grid;
        place-items: center;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .tab-label {
        font-size: 12px;
        color: var(--muted);
        font-weight: 500;
        transition: all 0.2s ease;
      }

      .tab-item.active .tab-label {
        color: var(--green-700);
        font-weight: 600;
      }

      .sheet[aria-hidden="true"] { display: none; }
      .sheet {
        position: fixed;
        left: 50%;
        transform: translateX(-50%);
        bottom: 80px;
        width: 800px;
        max-width: calc(100vw - 20px);
        background: #fff;
        border: 1px solid var(--line);
        border-radius: 12px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
        padding: 8px 12px 12px;
        z-index: 1100;
      }
      
      .sheet-handle {
        width: 40px;
        height: 4px;
        background: #e5e7eb;
        border-radius: 999px;
        margin: 6px auto 10px;
      }
      
      .sheet-list .item {
        display: block;
        padding: 14px 8px;
        color: var(--text);
        text-decoration: none;
        background: transparent;
        transition: background-color 0.2s ease;
        border-radius: 8px; 
        margin: 2px 0; 
        border: none; 
      }

      .sheet-list .item:hover {
        background: var(--soft);
      }
      
      .sheet-list .item:last-child {
        border-bottom: none;
      }
      
      .sheet-list .item.danger {
        color: #E53935;
        font-weight: 800;
      }
      
      .sheet-list button.item {
        all: unset;
        display: block;
        width: 100%;
        padding: 14px 8px;
        box-sizing: border-box;
        cursor: pointer;
        color: inherit;
        transition: background-color 0.2s ease;
        border-radius: 8px;
        margin: 2px 0;
        border: none; 
      }
      
      .sheet-list button.item:focus-visible {
        outline: 2px solid var(--green-500);
        border-radius: 8px;
      }

      /* 반응형 */
      @media (max-width: 850px) {
        .bottom-tabbar, .sheet {
          width: 95%;
        }
      }

      @media (max-width: 480px) {
        .bottom-tabbar, .sheet {
          width: 100vw;
          border-radius: 0;
        }
        
        .bottom-tabbar {
          border-radius: 0;
        }
        
        .sheet {
          border-radius: 0 0 0 0;
        }
      }
    `;
    
    document.head.appendChild(style);
  }

  // 메뉴 시트 열기 - 로그인 상태 강제 재확인
  function openMenuSheet() {
    const sheet = document.getElementById('commonMenuSheet');
    if (sheet) {
      // 메뉴를 열 때마다 로그인 상태 다시 체크하여 버튼 업데이트
      updateMenuForLoginState(sheet);
      sheet.setAttribute('aria-hidden', 'false');
    }
  }

  function closeMenuSheet() {
    const sheet = document.getElementById('commonMenuSheet');
    if (sheet) {
      sheet.setAttribute('aria-hidden', 'true');
    }
  }

  // 로그아웃 처리 - 개선된 버전
  async function handleLogout() {
    try {
      // 1. 서버에 로그아웃 요청
      const res = await fx('/logout', { method: 'POST' });
      
      // 2. 클라이언트 토큰 정리 (서버 응답과 관계없이)
      localStorage.removeItem('ACCESS_TOKEN');
      sessionStorage.removeItem('ACCESS_TOKEN');
      
      // 3. 로그인 상태 캐시 초기화
      lastLoginState = false;
      isCheckingLogin = false;
      
      toast('로그아웃 되었습니다.');
      
      // 4. 메뉴 시트 업데이트 (로그인 버튼으로 변경)
      const sheet = document.getElementById('commonMenuSheet');
      if (sheet) {
        updateMenuButtonOnly(sheet, false);
      }
      
      // 5. 페이지 이동
      setTimeout(() => {
        location.href = '/pages/login.html';
      }, 1000);
      
    } catch (err) {
      // 오류가 발생해도 클라이언트 상태는 정리
      localStorage.removeItem('ACCESS_TOKEN');
      sessionStorage.removeItem('ACCESS_TOKEN');
      lastLoginState = false;
      isCheckingLogin = false;
      
      toast('로그아웃 되었습니다.');
      
      const sheet = document.getElementById('commonMenuSheet');
      if (sheet) {
        updateMenuButtonOnly(sheet, false);
      }
      
      setTimeout(() => {
        location.href = '/pages/login.html';
      }, 1000);
    }
    closeMenuSheet();
  }

  // 로그인 페이지로 이동
  function handleLogin() {
    closeMenuSheet();
    location.href = '/pages/login.html';
  }

  // 전역 함수들
  window.toggleSlideMenu = async function() {
    const sheet = document.getElementById('commonMenuSheet');
    if (!sheet) {
      await createUnifiedMenuSheet();
      openMenuSheet();
    } else {
      const isHidden = sheet.getAttribute('aria-hidden') === 'true';
      if (isHidden) {
        openMenuSheet();
      } else {
        closeMenuSheet();
      }
    }
  };

  window.goToPage = function(path) {
    const routes = {
      '/favorites': '/pages/profile.html#saved',
      '/search': '/pages/search.html',
      '/add-recipe': '/pages/post_upload.html',
      '/profile': '/pages/profile.html'
    };
    
    const targetUrl = routes[path] || path;
    location.href = targetUrl;
  };

  // ESC 키로 메뉴 시트 닫기
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
      closeMenuSheet();
    }
  });

  // 메뉴 외부 클릭으로 닫기 기능 추가
  document.addEventListener('click', (e) => {
    const sheet = document.getElementById('commonMenuSheet');
    const menuButton = e.target.closest('[data-tab="menu"]');
    if (sheet && sheet.getAttribute('aria-hidden') === 'false' && 
        !menuButton && !sheet.contains(e.target)) {
      closeMenuSheet();
    }
  });

  // 페이지 포커스 시 로그인 상태 다시 체크 (다른 탭에서 로그인/아웃 했을 경우 대응) - 개선
  window.addEventListener('focus', async () => {
    // 캐시 초기화하여 새로 체크하도록 함
    lastLoginState = null;
    isCheckingLogin = false;
    
    const sheet = document.getElementById('commonMenuSheet');
    if (sheet && sheet.getAttribute('aria-hidden') === 'false') {
      await updateMenuForLoginState(sheet);
    }
  });

  // 로그인 상태 캐시 무효화 함수 (다른 스크립트에서 호출 가능) - 개선
  window.refreshLoginState = function() {
    lastLoginState = null;
    isCheckingLogin = false;
    
    // 현재 열린 메뉴 시트가 있다면 즉시 업데이트
    const sheet = document.getElementById('commonMenuSheet');
    if (sheet && sheet.getAttribute('aria-hidden') === 'false') {
      updateMenuForLoginState(sheet);
    }
  };

  // 로그인 성공 후 호출할 수 있는 함수 추가
  window.onLoginSuccess = function() {
    lastLoginState = true; // 로그인 성공 상태로 캐시 업데이트
    
    const sheet = document.getElementById('commonMenuSheet');
    if (sheet) {
      updateMenuButtonOnly(sheet, true);
    }
  };

  // 초기화 함수
  async function initUnifiedTabbar() {
    injectUnifiedStyles();
    createUnifiedTabbar();
    await createUnifiedMenuSheet();
  }

  // DOM 준비 시 초기화
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initUnifiedTabbar);
  } else {
    initUnifiedTabbar();
  }
})();