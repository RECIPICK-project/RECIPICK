// 맨 위에 추가
const API_BASE_URL = '/api/posts';

// 더미 카테고리/태그 목록
const RAW_TAGS = [
  "소고기", "돼지고기", "닭고기", "해물류", "채소류", "과일류", "버섯류", "건어물류", "곡류", "쌀",
  "밀가루", "달걀/유제품", "콩/견과류", "가공식품류", "육류", "기타"
];

// 상태
const selected = [];    // [{name, main:boolean}]
const mainSlot = document.getElementById('mainSlot');
const chipList = document.getElementById('chipList');
const searchBtn = document.getElementById('searchBtn');

// 메인 슬롯 UI 업데이트
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

// 선택 리스트 렌더
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
    // 체크 클릭/엔터: 메인 설정(하나만)
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

    // 삭제
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
  // 검색 버튼 활성화: 최소 1개 선택
  searchBtn.disabled = selected.length === 0;
}

// 초기 렌더
render();

/* ============================
   재료 선택 모달 (가운데)
============================ */
const picker = document.getElementById('picker');
const pickerInput = document.getElementById('pickerInput');
const pickerChips = document.getElementById('pickerChips');
document.getElementById('openPicker').addEventListener('click', () => {
  picker.setAttribute('aria-hidden', 'false');
  pickerInput.value = '';
  renderPicker();
  setTimeout(() => pickerInput.focus(), 0);
});
document.getElementById('closePicker').addEventListener('click', () => {
  picker.setAttribute('aria-hidden', 'true');
});
picker.addEventListener('click', (e) => {
  if (e.target.hasAttribute('data-close')) {
    picker.setAttribute('aria-hidden',
        'true');
  }
});

function renderPicker() {
  const q = pickerInput.value.trim();
  const list = RAW_TAGS
  .filter(t => q ? t.toLowerCase().includes(q.toLowerCase()) : true);

  pickerChips.innerHTML = '';
  list.forEach(name => {
    const chip = document.createElement('button');
    chip.className = 'chip';
    chip.type = 'button';
    chip.textContent = name;
    chip.addEventListener('click', () => {
      // 이미 있으면 메인으로 승격만, 없으면 추가
      const exist = selected.find(x => x.name === name);
      selected.forEach(x => x.main = false);
      if (exist) {
        exist.main = true;
      } else {
        selected.push({name, main: true});
      }
      picker.setAttribute('aria-hidden', 'true');
      render();
    });
    pickerChips.appendChild(chip);
  });
}

pickerInput.addEventListener('input', renderPicker);

/* ============================
   CTA: 검색 버튼 (실제 API 연동)
============================ */
searchBtn.addEventListener('click', () => {
  if (searchBtn.disabled) {
    return;
  }

  const mainIngredients = selected.filter(x => x.main).map(x => x.name);
  const subIngredients = selected.filter(x => !x.main).map(x => x.name);

  // search_home.html로 이동하여 재료 검색 결과 표시
  const params = new URLSearchParams();
  mainIngredients.forEach(ingredient => params.append('main', ingredient));
  subIngredients.forEach(ingredient => params.append('sub', ingredient));
  params.append('searchType', 'ingredients'); // 재료 검색임을 표시

  window.location.href = `search_home.html?${params.toString()}`;
});

/* ============================
   카메라 FAB (항상 클릭되게 z-index↑)
============================ */
document.getElementById('cameraBtn').addEventListener('click', () => {
  // TODO: 사진 업로드(영수증) 페이지로 이동
  alert('영수증 업로드 화면으로 이동 (더미)');
});
