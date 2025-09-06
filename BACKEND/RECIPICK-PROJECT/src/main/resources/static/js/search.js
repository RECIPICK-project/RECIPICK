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

// --- UI 렌더링 함수들 (기존과 동일) ---

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
      <div class="name">${it.name}</div>
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
      }
      setMain();
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

// --- 재료 선택 모달 (최종 수정) ---

/**
 * 고정된 섹션 내부에 결과를 렌더링하는 함수
 * @param {string[]} apiResults - API로부터 받은 재료 추천 목록
 * @param {string[]} allCategories - 항상 표시될 전체 카테고리 목록
 */
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
      selected.push({name, main: true});
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

/**
 * 사용자의 입력을 기반으로 추천 재료만 업데이트하는 함수
 * @param {string} keyword - 사용자가 입력한 검색어
 */
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

/**
 * 모달을 열 때 넉넉한 크기의 고정 UI 구조를 생성하는 함수
 */
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
      /* 핵심: 높이 제한과 스크롤을 제거하고 최소 높이만 설정 */
      .chip-container {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        min-height: 38px; /* 결과가 없을 때 최소 높이 유지 */
        padding: 4px;
      }
      .placeholder { color:#aaa; font-size: 14px; text-align:center; width:100%; }
      .picker-divider { width: 100%; border: none; border-top: 1px solid #e5e7eb; margin: 16px 0; }
    </style>
  `;
}

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
    picker.setAttribute('aria-hidden',
        'true');
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

/* ============================
   CTA 및 FAB 버튼 (기존과 동일)
============================ */
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

document.getElementById('cameraBtn').addEventListener('click',
    () => alert('영수증 업로드 화면으로 이동 (더미)'));

// 초기 렌더링
render();