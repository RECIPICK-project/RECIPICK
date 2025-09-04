// 백엔드 API 기본 URL
const API_BASE_URL = '/api/posts';
// 이미지 로드 실패 시 표시할 기본 이미지 URL
const DEFAULT_IMAGE_URL = 'https://via.placeholder.com/300x200.png?text=No+Image';

// DOM 요소
const listEl = document.getElementById('cardList');
const countEl = document.getElementById('resultCount');
const loadMoreBtn = document.getElementById('loadMore');
const sortSel = document.getElementById('sortSel');
const form = document.getElementById('recipeSearchForm');
const qInput = document.getElementById('recipeQuery');

// 상태 관리
let currentQuery = '';
let currentSearchType = '';
let currentIngredients = {main: [], sub: []};
let isLoading = false;
let nextPage = 0;
let hasMoreData = true;
const PAGE_SIZE = 10;

// 로딩 상태 제어 함수
function showLoading() {
  isLoading = true;
  listEl.innerHTML = '<div class="loading">레시피를 불러오는 중...</div>';
  countEl.textContent = '총 0개 레시피';
  loadMoreBtn.hidden = true;
}

function hideLoading() {
  isLoading = false;
}

// API 호출 함수들
async function searchRecipesByTitle(query, sort = 'latest', page = 0,
    size = PAGE_SIZE) {
  const params = new URLSearchParams({
    title: query,
    sort: sort,
    page: page.toString(),
    size: size.toString()
  });

  const response = await fetch(
      `${API_BASE_URL}/search/by-title?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  return await response.json();
}

async function searchRecipesByIngredients(mainIngredients, subIngredients,
    sort = 'latest', page = 0, size = PAGE_SIZE) {
  const params = new URLSearchParams();
  mainIngredients.forEach(ingredient => params.append('main', ingredient));
  subIngredients.forEach(ingredient => params.append('sub', ingredient));
  params.append('sort', sort);
  params.append('page', page.toString());
  params.append('size', size.toString());

  const response = await fetch(`${API_BASE_URL}/search?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  return await response.json();
}

async function getPopularRecipes(sort = 'latest', page = 0, size = PAGE_SIZE) {
  const params = new URLSearchParams({
    sort: sort,
    page: page.toString(),
    size: size.toString()
  });

  const response = await fetch(
      `${API_BASE_URL}/search/popular?${params.toString()}`);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }

  return await response.json();
}

// 이미지 URL 유효성 검사 및 처리
function processImageUrl(imageUrl) {
  // null, undefined, 빈 문자열 체크
  if (!imageUrl || imageUrl.trim() === '') {
    return DEFAULT_IMAGE_URL;
  }

  // HTTP/HTTPS 프로토콜 체크
  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }

  // 상대 경로인 경우 기본 이미지 반환
  return DEFAULT_IMAGE_URL;
}

// 레시피 카드 생성
function makeCard(recipe) {
  // 디버깅: 레시피 데이터 확인
  console.log('Recipe data:', recipe);
  console.log('rcpImgUrl:', recipe.rcpImgUrl);

  const li = document.createElement('li');
  li.className = 'card';
  li.onclick = () => window.location.href = `/post/${recipe.postId}`;

  const imgWrapper = document.createElement('div');
  imgWrapper.className = 'card-img';
  const img = document.createElement('img');

  // 이미지 URL 처리 - 썸네일용 rcpImgUrl 우선 사용
  const imageUrl = recipe.rcpImgUrl;
  const processedUrl = processImageUrl(imageUrl);

  console.log('Original URL:', imageUrl);
  console.log('Processed URL:', processedUrl);

  img.src = processedUrl;
  img.alt = recipe.foodName;

  // 이미지 로드 성공/실패 이벤트
  img.onload = function () {
    console.log('Image loaded successfully:', this.src);
  };

  img.onerror = function () {
    console.log('Image load failed, using default:', this.src);
    this.src = DEFAULT_IMAGE_URL;
  };

  imgWrapper.appendChild(img);
  li.appendChild(imgWrapper);

  const meta = document.createElement('div');
  meta.className = 'meta';

  const title = document.createElement('p');
  title.className = 'title';
  title.textContent = recipe.title;
  meta.appendChild(title);

  const sub = document.createElement('p');
  sub.className = 'sub';

  const likeCount = recipe.likeCount !== undefined ? `❤ ${recipe.likeCount}`
      : '❤ 0';
  const viewCount = recipe.viewCount !== undefined ? `👀 ${recipe.viewCount}`
      : '👀 0';
  sub.innerHTML = `${recipe.foodName} · ${likeCount} · ${viewCount}`;
  meta.appendChild(sub);

  const date = document.createElement('p');
  date.className = 'sub';
  if (recipe.createdAt) {
    date.textContent = `등록일: ${new Date(
        recipe.createdAt).toLocaleDateString()}`;
  }
  meta.appendChild(date);

  li.appendChild(meta);
  return li;
}

// 상태 초기화
function resetState() {
  nextPage = 0;
  hasMoreData = true;
  listEl.innerHTML = '';
  loadMoreBtn.hidden = true;
}

// 결과 수 표시
function updateCount(count) {
  countEl.textContent = `총 ${count}개 레시피`;
}

// 새 레시피들을 리스트에 추가
function addRecipesToList(recipes) {
  if (nextPage === 0) {
    listEl.innerHTML = '';
  }

  if (recipes.length === 0) {
    hasMoreData = false;
    loadMoreBtn.hidden = true;
    return;
  }

  recipes.forEach(recipe => {
    const card = makeCard(recipe);
    listEl.appendChild(card);
  });
  loadMoreBtn.hidden = false;
}

// 빈 결과 표시
function showNoResults(message) {
  listEl.innerHTML = `<div class="no-results">${message}</div>`;
  countEl.textContent = '총 0개 레시피';
  loadMoreBtn.hidden = true;
}

// 에러 메시지 표시
function showError(error) {
  console.error(error);
  listEl.innerHTML = `<div class="error-msg">오류가 발생했습니다.<br>다시 시도해주세요.</div>`;
}

// 정렬 변경 이벤트 핸들러
async function applySort() {
  resetState();
  showLoading();
  const sort = sortSel.value;
  let response = {};

  try {
    if (currentSearchType === 'ingredients') {
      response = await searchRecipesByIngredients(
          currentIngredients.main,
          currentIngredients.sub,
          sort,
          0,
          PAGE_SIZE
      );
    } else if (currentSearchType === 'title') {
      response = await searchRecipesByTitle(currentQuery, sort, 0, PAGE_SIZE);
    } else {
      response = await getPopularRecipes(sort, 0, PAGE_SIZE);
    }

    if (response.recipes && response.recipes.length === 0) {
      showNoResults('검색 결과가 없습니다.');
    } else {
      addRecipesToList(response.recipes);
      if (currentSearchType === 'ingredients') {
        updateCount(response.recipeCount);
      } else {
        updateCount(response.recipes.length);
      }
    }
    nextPage = 1;

  } catch (error) {
    showError(error);
  } finally {
    hideLoading();
  }
}

// 더보기 버튼 로딩
async function handleLoadMore() {
  if (isLoading || !hasMoreData) {
    return;
  }

  isLoading = true;
  loadMoreBtn.disabled = true;

  try {
    let recipes = [];
    let response = {};
    const sort = sortSel.value;

    if (currentSearchType === 'ingredients') {
      response = await searchRecipesByIngredients(
          currentIngredients.main,
          currentIngredients.sub,
          sort,
          nextPage,
          PAGE_SIZE
      );
      recipes = response.recipes;
    } else if (currentSearchType === 'title') {
      response = await searchRecipesByTitle(
          currentQuery,
          sort,
          nextPage,
          PAGE_SIZE
      );
      recipes = response.recipes;
    } else {
      response = await getPopularRecipes(
          sort,
          nextPage,
          PAGE_SIZE
      );
      recipes = response.recipes;
    }

    if (recipes && recipes.length > 0) {
      addRecipesToList(recipes);
      nextPage++;
      hasMoreData = recipes.length === PAGE_SIZE;
    } else {
      hasMoreData = false;
      loadMoreBtn.hidden = true;
    }

  } catch (error) {
    console.error('더보기 로딩 중 오류 발생:', error);
    showError(error);
  } finally {
    isLoading = false;
    loadMoreBtn.disabled = !hasMoreData;
  }
}

// 초기화
async function init() {
  const urlParams = new URLSearchParams(window.location.search);
  const searchType = urlParams.get('searchType');
  const query = urlParams.get('q');
  const sort = sortSel.value;

  try {
    resetState();
    showLoading();
    let response = {};

    if (searchType === 'ingredients') {
      const mainIngredients = urlParams.getAll('main');
      const subIngredients = urlParams.getAll('sub');

      if (mainIngredients.length > 0) {
        currentSearchType = 'ingredients';
        currentIngredients = {main: mainIngredients, sub: subIngredients};
        response = await searchRecipesByIngredients(mainIngredients,
            subIngredients, sort, 0, PAGE_SIZE);
      } else {
        currentSearchType = '';
        response = await getPopularRecipes(sort, 0, PAGE_SIZE);
      }
    } else if (searchType === 'title') {
      currentSearchType = 'title';
      currentQuery = query;
      response = await searchRecipesByTitle(query, sort, 0, PAGE_SIZE);
    } else {
      currentSearchType = '';
      response = await getPopularRecipes(sort, 0, PAGE_SIZE);
    }

    if (response.recipes && response.recipes.length > 0) {
      addRecipesToList(response.recipes);
      nextPage = 1;
      if (currentSearchType === 'ingredients') {
        updateCount(response.recipeCount);
      } else {
        updateCount(response.recipes.length);
      }
    } else {
      showNoResults('검색 결과가 없습니다.');
    }

  } catch (error) {
    showError(error);
  } finally {
    hideLoading();
  }
}

// 이벤트 리스너
sortSel.addEventListener('change', applySort);
loadMoreBtn.addEventListener('click', handleLoadMore);

form.addEventListener('submit', async (e) => {
  e.preventDefault();
  const query = qInput.value.trim();
  if (!query) {
    alert('검색어를 입력해주세요.');
    return;
  }

  resetState();
  showLoading();
  currentSearchType = 'title';
  currentQuery = query;

  try {
    const sort = sortSel.value;
    const response = await searchRecipesByTitle(query, sort, 0, PAGE_SIZE);
    addRecipesToList(response.recipes);
    if (response.recipes.length === 0) {
      showNoResults('검색 결과가 없습니다.');
    }
    updateCount(response.recipes.length);
    nextPage = 1;
  } catch (error) {
    showError(error);
  } finally {
    hideLoading();
  }
});

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', init);