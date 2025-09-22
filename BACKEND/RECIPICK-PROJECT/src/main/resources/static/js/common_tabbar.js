
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

  // 통합 메뉴 시트 생성 함수
  function createUnifiedMenuSheet() {
    // 기존 시트가 있으면 제거
    const existingSheet = document.getElementById('commonMenuSheet');
    if (existingSheet) {
      existingSheet.remove();
    }

    const sheet = document.createElement('div');
    sheet.id = 'commonMenuSheet';
    sheet.className = 'sheet';
    sheet.setAttribute('aria-hidden', 'true');
    
    sheet.innerHTML = `
      <div class="sheet-handle" aria-hidden="true"></div>
      <nav class="sheet-list" aria-label="메뉴 목록">
        <a href="/pages/faq.html" class="item">자주 묻는 질문</a>
        <a href="/pages/profile.html#activity" class="item">내 리뷰</a>
        <a href="/pages/official_recipes.html" class="item">정식 레시피</a>
        <a href="/pages/setting.html" class="item">설정</a>
        <button id="commonLogoutBtn" class="item danger" type="button">로그아웃</button>
      </nav>
    `;

    document.body.appendChild(sheet);

    // 이벤트 리스너 등록 - 바깥 영역 클릭으로 닫기
    sheet.addEventListener('click', (e) => {
      if (e.target === sheet) closeMenuSheet();
    });

    const logoutBtn = sheet.querySelector('#commonLogoutBtn');
    logoutBtn.addEventListener('click', handleLogout);
  }

  // 통합 CSS 스타일 추가
  function injectUnifiedStyles() {
    // 기존 스타일이 있으면 제거
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

      /* 하단 고정 탭바 */
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

      /* 바텀 시트 스타일 */
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
        border-bottom: 1px solid var(--line);
        color: var(--text);
        text-decoration: none;
        background: transparent;
        transition: background-color 0.2s ease;
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

  function openMenuSheet() {
    const sheet = document.getElementById('commonMenuSheet');
    if (sheet) {
      sheet.setAttribute('aria-hidden', 'false');
    }
  }

  function closeMenuSheet() {
    const sheet = document.getElementById('commonMenuSheet');
    if (sheet) {
      sheet.setAttribute('aria-hidden', 'true');
    }
  }

  async function handleLogout() {
    try {
      const res = await fx('/logout', { method: 'POST' });
      localStorage.removeItem('ACCESS_TOKEN');
      toast('로그아웃 되었습니다.');
      setTimeout(() => {
        location.href = '/pages/login.html';
      }, 1000);
    } catch (err) {
      console.error('로그아웃 오류:', err);
      localStorage.removeItem('ACCESS_TOKEN');
      toast('로그아웃 되었습니다.');
      setTimeout(() => {
        location.href = '/pages/login.html';
      }, 1000);
    }
    closeMenuSheet();
  }

  // 전역 함수들
  window.toggleSlideMenu = function() {
    const sheet = document.getElementById('commonMenuSheet');
    if (!sheet) {
      createUnifiedMenuSheet();
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

  // 초기화 함수
  function initUnifiedTabbar() {
    injectUnifiedStyles();
    createUnifiedTabbar();
    createUnifiedMenuSheet();
  }

  // DOM 준비 시 초기화
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initUnifiedTabbar);
  } else {
    initUnifiedTabbar();
  }
})();