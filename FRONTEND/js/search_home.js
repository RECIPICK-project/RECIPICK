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
let totalRecipeCount = 0;
const PAGE_SIZE = 10;

// 레시피 상세 페이지로 이동하는 함수
function goToRecipeDetail(postId) {
  // URL에 postId를 포함하여 상세 페이지로 이동
  window.location.href = `/post_detail.html?postId=${postId}`;
}

// 로딩 상태 제어 함수
function showLoading() {
  isLoading = true;
  listEl.innerHTML = '<div class="loading">레시피를 불러오는 중...</div>';
  countEl.textContent = '총 0개 레시피';
  loadMoreBtn.hidden = true;
}

function hideLoading() {
  isLoading = false;
  // 로딩 메시지 제거
  const loadingEl = listEl.querySelector('.loading');
  if (loadingEl) {
    loadingEl.remove();
  }
}

// 검색된 재료 표시 함수
function showSearchedIngredients() {
  // 기존 재료 표시 제거
  const existingInfo = document.querySelector('.search-ingredients-info');
  if (existingInfo) {
    existingInfo.remove();
  }

  if (currentSearchType === 'ingredients' && (currentIngredients.main.length > 0
      || currentIngredients.sub.length > 0)) {
    const ingredientsInfo = document.createElement('div');
    ingredientsInfo.className = 'search-ingredients-info';

    // 메인 재료 칩 생성
    currentIngredients.main.forEach(ingredient => {
      const chip = document.createElement('span');
      chip.className = 'ingredient-chip main';
      chip.textContent = ingredient;
      ingredientsInfo.appendChild(chip);
    });

    // 서브 재료 칩 생성
    currentIngredients.sub.forEach(ingredient => {
      const chip = document.createElement('span');
      chip.className = 'ingredient-chip';
      chip.textContent = ingredient;
      ingredientsInfo.appendChild(chip);
    });

    // countEl 다음에 삽입 (레시피 목록 위에)
    countEl.insertAdjacentElement('afterend', ingredientsInfo);
  }
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
  if (!imageUrl || imageUrl.trim() === '') {
    return DEFAULT_IMAGE_URL;
  }

  if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://')) {
    return imageUrl;
  }

  return DEFAULT_IMAGE_URL;
}

// 레시피 카드 생성 (클릭 이벤트 수정)
function makeCard(recipe) {
  console.log('Recipe data:', recipe);

  const li = document.createElement('li');
  li.className = 'card';
  
  // 클릭 이벤트 수정 - postId를 사용하여 상세 페이지로 이동
  li.onclick = () => goToRecipeDetail(recipe.postId);
  li.style.cursor = 'pointer'; // 클릭 가능함을 시각적으로 표시

  const imgWrapper = document.createElement('div');
  imgWrapper.className = 'card-img';
  const img = document.createElement('img');

  const imageUrl = recipe.rcpImgUrl;
  const processedUrl = processImageUrl(imageUrl);

  img.src = processedUrl;
  img.alt = recipe.foodName;

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
  totalRecipeCount = 0;
}

// 결과 수 표시
function updateCount(count) {
  totalRecipeCount = count;
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

  hasMoreData = recipes.length === PAGE_SIZE;
  loadMoreBtn.hidden = !hasMoreData;
}

// 빈 결과 표시
function showNoResults(message) {
  listEl.innerHTML = `<div class="no-results">${message}</div>`;
  countEl.textContent = '총 0개 레시피';
  loadMoreBtn.hidden = true;
  totalRecipeCount = 0;
}

// 에러 메시지 표시
function showError(error) {
  console.error(error);
  listEl.innerHTML = `<div class="error-msg">오류가 발생했습니다.<br>다시 시도해주세요.</div>`;
}

// 정렬 옵션 동적 업데이트
function updateSortOptions(searchType) {
  const sortSel = document.getElementById('sortSel');

  sortSel.innerHTML = '';

  if (searchType === 'ingredients') {
    // 재료 검색시: defaultsort 포함
    sortSel.innerHTML = `
      <option value="defaultsort">관련도순</option>
      <option value="popular">인기순</option>
      <option value="latest">최신순</option>
      <option value="rating">조회순</option>
    `;
  } else {
    // 제목 검색, 인기 레시피: defaultsort 제외
    sortSel.innerHTML = `
      <option value="latest">최신순</option>
      <option value="popular">인기순</option>
      <option value="rating">조회순</option>
    `;
  }
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
      // 전체 개수 업데이트
      if (currentSearchType === 'ingredients') {
        updateCount(response.recipeCount || response.totalCount || 0);
      } else {
        updateCount(response.totalCount || 0);
      }
    }
    nextPage = 1;

  } catch (error) {
    showError(error);
  } finally {
    hideLoading();
    showSearchedIngredients(); // 검색 재료 표시
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
      response = await searchRecipesByTitle(currentQuery, sort, nextPage,
          PAGE_SIZE);
      recipes = response.recipes;
    } else {
      response = await getPopularRecipes(sort, nextPage, PAGE_SIZE);
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

  // 정렬 옵션 동적 설정
  updateSortOptions(searchType);

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
      // 전체 개수 업데이트
      if (currentSearchType === 'ingredients') {
        updateCount(response.recipeCount || response.totalCount || 0);
      } else {
        updateCount(response.totalCount || 0);
      }
    } else {
      showNoResults('검색 결과가 없습니다.');
    }

  } catch (error) {
    showError(error);
  } finally {
    hideLoading();
    showSearchedIngredients(); // 검색 재료 표시
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

  // 정렬 옵션 업데이트 (제목 검색이므로 defaultsort 제거)
  updateSortOptions('title');

  try {
    const sort = sortSel.value;
    const response = await searchRecipesByTitle(query, sort, 0, PAGE_SIZE);
    addRecipesToList(response.recipes);
    if (response.recipes.length === 0) {
      showNoResults('검색 결과가 없습니다.');
    }
    updateCount(response.totalCount || 0);
    nextPage = 1;
  } catch (error) {
    showError(error);
  } finally {
    hideLoading();
    showSearchedIngredients(); // 검색 재료 표시
  }
});

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', init);