// API 기본 URL 설정
const API_BASE_URL = '/api/posts';

// 고정 카테고리/태그 목록 (절대 변하지 않음)
const RAW_TAGS = [
  "소고기", "돼지고기", "닭고기", "육류", "해물류", "채소류", "과일류", "버섯류", "건어물류", "곡류", "쌀",
  "밀가루", "달걀/유제품", "콩/견과류", "가공식품류", "기타"
];

// 상태 변수들
const selected = [];
const mainSlot = document.getElementById('mainSlot');
const chipList = document.getElementById('chipList');
const searchBtn = document.getElementById('searchBtn');
const picker = document.getElementById('picker');
const pickerInput = document.getElementById('pickerInput');
const pickerChips = document.getElementById('pickerChips');

// Debounce 타이머
let debounceTimer;

// --- UI 렌더링 함수들 ---

function renderMain() {
  const hasMain = selected.find(x => x.main);
  mainSlot.classList.toggle('has-main', !!hasMain);
  const titleEl = mainSlot.querySelector('.label');
  titleEl.textContent = hasMain ? hasMain.name : '메인 자리';
  const minus = mainSlot.querySelector('.minus');
  minus.disabled = !hasMain;
  minus.onclick = () => {
    const idx = selected.findIndex(x => x.main);
    if (idx > -1) {
      selected[idx].main = false;
      render();
    }
  };
}

function renderList() {
  chipList.innerHTML = '';
  selected.forEach((it, idx) => {
    const row = document.createElement('div');
    row.className = 'item' + (it.main ? ' is-main' : '');
    row.innerHTML = `
      <div class="check" role="button" tabindex="0" aria-pressed="${it.main
        ? 'true' : 'false'}" title="${it.main ? '메인' : '서브'}"></div>
      <div class="name">${it.name}${it.fromOCR ? ' 📄' : ''}</div>
      <button class="minus" aria-label="${it.name} 삭제">−</button>
    `;
    const chk = row.querySelector('.check');
    const setMain = () => {
      selected.forEach(x => x.main = false);
      selected[idx].main = true;
      render();
    };
    chk.addEventListener('click', setMain);
    chk.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        setMain();
      }
    });
    row.querySelector('.minus').addEventListener('click', () => {
      selected.splice(idx, 1);
      render();
    });
    chipList.appendChild(row);
  });
}

function render() {
  renderMain();
  renderList();
  searchBtn.disabled = selected.length === 0;
}

// --- 재료 선택 모달 ---

function renderPickerChips(apiResults, allCategories) {
  const recommendationEl = document.getElementById('recommendation-chips');
  const categoryEl = document.getElementById('category-chips');

  if (!recommendationEl || !categoryEl) {
    return;
  }

  const handleChipClick = (name) => {
    const exist = selected.find(x => x.name === name);
    selected.forEach(x => x.main = false);
    if (exist) {
      exist.main = true;
    } else {
      selected.push({name, main: true, fromOCR: false});
    }
    picker.setAttribute('aria-hidden', 'true');
    render();
  };

  const createChip = (name) => {
    const chip = document.createElement('button');
    chip.className = 'chip';
    chip.type = 'button';
    chip.textContent = name;
    chip.addEventListener('click', () => handleChipClick(name));
    return chip;
  };

  // 1. 추천 재료 섹션 렌더링
  recommendationEl.innerHTML = '';
  if (apiResults && apiResults.length > 0) {
    apiResults.forEach(name => recommendationEl.appendChild(createChip(name)));
  } else {
    recommendationEl.innerHTML = '<p class="placeholder">검색어와 일치하는 재료가 없습니다.</p>';
  }

  // 2. 고정 카테고리 섹션 렌더링
  categoryEl.innerHTML = '';
  allCategories.forEach(name => categoryEl.appendChild(createChip(name)));
}

async function updatePicker(keyword) {
  let apiSuggestions = [];

  if (keyword) {
    try {
      const url = new URL(`${API_BASE_URL}/ingredients`,
          window.location.origin);
      url.searchParams.append('keyword', keyword);
      url.searchParams.append('limit', 3);
      const response = await fetch(url);
      if (response.ok) {
        apiSuggestions = await response.json();
      }
    } catch (error) {
      console.error("API suggestions fetch error:", error);
    }
  }

  renderPickerChips(apiSuggestions, RAW_TAGS);
}

function setupPickerLayout() {
  pickerChips.innerHTML = `
    <div class="picker-section">
      <h4 class="picker-title">추천 재료</h4>
      <div id="recommendation-chips" class="chip-container"></div>
    </div>
    <hr class="picker-divider">
    <div class="picker-section">
      <h4 class="picker-title">카테고리</h4>
      <div id="category-chips" class="chip-container"></div>
    </div>
    <style>
      .picker-section { }
      .picker-title { margin: 0 0 10px; font-size: 14px; color: #333; }
      .chip-container {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        min-height: 38px;
        padding: 4px;
      }
      .placeholder { color:#aaa; font-size: 14px; text-align:center; width:100%; }
      .picker-divider { width: 100%; border: none; border-top: 1px solid #e5e7eb; margin: 16px 0; }
    </style>
  `;
}

// --- 영수증 업로드 관련 함수들 ---

function showLoadingState(message = '처리 중...') {
  const loadingDiv = document.createElement('div');
  loadingDiv.id = 'loading-overlay';
  loadingDiv.innerHTML = `
    <div class="loading-content">
      <div class="spinner"></div>
      <p>${message}</p>
    </div>
    <style>
      #loading-overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
      }
      .loading-content {
        background: white;
        padding: 20px;
        border-radius: 12px;
        text-align: center;
      }
      .spinner {
        width: 40px;
        height: 40px;
        border: 4px solid #f3f3f3;
        border-top: 4px solid #4caf50;
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin: 0 auto 10px;
      }
      @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }
    </style>
  `;
  document.body.appendChild(loadingDiv);
}

function hideLoadingState() {
  const loadingDiv = document.getElementById('loading-overlay');
  if (loadingDiv) {
    loadingDiv.remove();
  }
}

function showNotification(message, type = 'info') {
  const notification = document.createElement('div');
  notification.className = `notification ${type}`;
  notification.innerHTML = `
    ${message}
    <style>
      .notification {
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%);
        padding: 12px 20px;
        border-radius: 8px;
        color: white;
        font-weight: 600;
        z-index: 10000;
        animation: slideDown 0.3s ease;
        max-width: 90%;
        text-align: center;
      }
      .notification.success {
        background: #4caf50;
      }
      .notification.error {
        background: #f44336;
      }
      .notification.info {
        background: #2196f3;
      }
      @keyframes slideDown {
        from {
          opacity: 0;
          transform: translateX(-50%) translateY(-20px);
        }
        to {
          opacity: 1;
          transform: translateX(-50%) translateY(0);
        }
      }
    </style>
  `;
  document.body.appendChild(notification);

  setTimeout(() => {
    notification.remove();
  }, 3000);
}

async function handleReceiptUpload(file) {
  if (!file) {
    return;
  }

  // 파일 타입 검증
  if (!file.type.startsWith('image/')) {
    showNotification('이미지 파일만 업로드 가능합니다.', 'error');
    return;
  }

  // 파일 크기 검증 (10MB)
  if (file.size > 10 * 1024 * 1024) {
    showNotification('파일 크기는 10MB 이하여야 합니다.', 'error');
    return;
  }

  try {
    showLoadingState('영수증을 분석하고 있습니다...');

    const formData = new FormData();
    formData.append('image', file);

    // OCR 전용 API 엔드포인트 사용
    const response = await fetch('/api/ocr/extract', {
      method: 'POST',
      body: formData
    });

    const result = await response.json();
    hideLoadingState();

    if (result.success) {
      const extractedIngredients = result.extractedIngredients || [];

      if (extractedIngredients.length > 0) {
        // 추출된 재료들을 선택된 재료 목록에 추가
        extractedIngredients.forEach(ingredientName => {
          const exist = selected.find(x => x.name === ingredientName);
          if (!exist) {
            selected.push({
              name: ingredientName,
              main: false,
              fromOCR: true
            });
          }
        });

        // 첫 번째 재료를 메인으로 설정
        if (selected.length > 0 && !selected.find(x => x.main)) {
          const firstOCRItem = selected.find(x => x.fromOCR);
          if (firstOCRItem) {
            firstOCRItem.main = true;
          }
        }

        render();
        showNotification(`영수증에서 ${extractedIngredients.length}개 재료를 찾았습니다!`,
            'success');
      } else {
        showNotification('영수증에서 재료를 찾을 수 없습니다. 다른 이미지를 시도해보세요.', 'info');
      }
    } else {
      showNotification(`OCR 처리 실패: ${result.message}`, 'error');
    }
  } catch (error) {
    hideLoadingState();
    console.error('OCR 처리 중 오류:', error);
    showNotification('영수증 처리 중 오류가 발생했습니다.', 'error');
  }
}

// --- 이벤트 리스너들 ---

// 재료 검색 모달 열기
document.getElementById('openPicker').addEventListener('click', () => {
  picker.setAttribute('aria-hidden', 'false');
  pickerInput.value = '';
  setupPickerLayout();
  updatePicker('');
  setTimeout(() => pickerInput.focus(), 0);
});

// 재료 검색 모달 닫기
document.getElementById('closePicker').addEventListener('click',
    () => picker.setAttribute('aria-hidden', 'true'));
picker.addEventListener('click', (e) => {
  if (e.target.hasAttribute('data-close')) {
    picker.setAttribute('aria-hidden', 'true');
  }
});

// 재료 검색 인풋 이벤트 리스너
pickerInput.addEventListener('input', () => {
  clearTimeout(debounceTimer);
  const keyword = pickerInput.value.trim();
  debounceTimer = setTimeout(() => {
    updatePicker(keyword);
  }, 300);
});

// 검색 버튼 클릭
searchBtn.addEventListener('click', () => {
  if (searchBtn.disabled) {
    return;
  }

  const mainIngredients = selected.filter(x => x.main).map(x => x.name);
  const subIngredients = selected.filter(x => !x.main).map(x => x.name);
  const params = new URLSearchParams();

  mainIngredients.forEach(ing => params.append('main', ing));
  subIngredients.forEach(ing => params.append('sub', ing));
  params.append('searchType', 'ingredients');

  window.location.href = `search_home.html?${params.toString()}`;
});

// 카메라 버튼 (영수증 업로드)
document.getElementById('cameraBtn').addEventListener('click', () => {
  const input = document.createElement('input');
  input.type = 'file';
  input.accept = 'image/*';
  input.capture = 'camera'; // 모바일에서 카메라 우선 실행

  input.onchange = (e) => {
    const file = e.target.files[0];
    if (file) {
      handleReceiptUpload(file);
    }
  };

  input.click();
});

// 드래그 앤 드롭 지원 (데스크톱)
const dropZone = document.querySelector('.phone.search-page');

dropZone.addEventListener('dragover', (e) => {
  e.preventDefault();
  dropZone.style.backgroundColor = '#f0f8f0';
});

dropZone.addEventListener('dragleave', (e) => {
  e.preventDefault();
  dropZone.style.backgroundColor = '';
});

dropZone.addEventListener('drop', (e) => {
  e.preventDefault();
  dropZone.style.backgroundColor = '';

  const files = e.dataTransfer.files;
  if (files.length > 0) {
    handleReceiptUpload(files[0]);
  }
});

// 초기 렌더링
render();