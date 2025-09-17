// 백엔드 API 기본 URL
const API_BASE_URL = 'http://localhost:8080'; 

// 페이지 로드 시 실행
document.addEventListener("DOMContentLoaded", function () {
    // URL에서 'postId' 파라미터 값 추출
    const urlParams = new URLSearchParams(window.location.search);
    const recipeId = urlParams.get('postId');

    if (recipeId) {
        // postId가 있으면 API를 호출하여 레시피 데이터를 불러옴
        loadRecipeData(recipeId);
    } else {
        console.error("레시피 ID를 찾을 수 없습니다.");
        alert("잘못된 접근입니다.");
    }

    // 댓글 시스템 초기화
    initializeReviewSystem(recipeId);

    initializeSlideMenu();
    initializeLikeButton();
});


// 실제 레시피 데이터 로드 함수 (API 호출)
async function loadRecipeData(recipeId) {
    try {
        // 백엔드의 GET /post/{postId} 엔드포인트 호출
        const response = await fetch(`${API_BASE_URL}/post/${recipeId}`);
        if (!response.ok) {
            throw new Error("레시피를 불러올 수 없습니다.");
        }
        const result = await response.json();

        if (result.success && result.data) {
            renderRecipeData(result.data);
        } else {
            throw new Error(result.message || "데이터 형식이 올바르지 않습니다.");
        }

    } catch (error) {
        console.error("레시피 로드 에러:", error);
        alert("레시피를 불러오는데 실패했습니다: " + error.message);
    }
}

// 레시피 데이터 렌더링
function renderRecipeData(recipe) {
    // 기본 정보 업데이트
    document.title = recipe.title; // 브라우저 탭 제목 변경
    document.getElementById("recipeTitle").textContent = recipe.title;
    document.getElementById("authorName").textContent = recipe.author;
    document.getElementById("difficulty").textContent = recipe.ckgLevel;
    document.getElementById("servings").textContent = recipe.ckgInbun;
    document.getElementById("cookingTime").textContent = recipe.cookingTimeString;

    // 썸네일 이미지
    if (recipe.rcpImgUrl) {
        const thumbnail = document.getElementById("thumbnailBox");
        thumbnail.innerHTML = `<img src="${recipe.rcpImgUrl}" alt="${recipe.title} 이미지">`;
        thumbnail.classList.add("has-img");
    }

    // 재료 렌더링 (PostDto의 ingredientsString 사용)
    renderIngredients(recipe.ingredientsString);

    // 조리순서 렌더링 (PostDto의 rcpSteps 사용)
    renderCookingSteps(recipe.rcpSteps, recipe.rcpStepsImg);
}

// 재료 리스트 렌더링 (백엔드 데이터 형식에 맞게 수정)
function renderIngredients(ingredientsString) {
    if (!ingredientsString) return;
    const ingredientsArray = ingredientsString.split("|");
    const container = document.getElementById("ingredientsList");
    container.innerHTML = "";

    for (let i = 0; i < ingredientsArray.length; i += 2) {
        const name = ingredientsArray[i];
        const amount = ingredientsArray[i + 1] || "";

        const ingredientItem = document.createElement("div");
        ingredientItem.className = "ingredient-item";
        ingredientItem.innerHTML = `
          <span class="ingredient-name">${name}</span>
          <span class="ingredient-amount">${amount}</span>
          <button class="btn-small btn-substitute" onclick="getSubstituteIngredient('${name}', this)">대체</button>
          <button class="btn-small" onclick="goToCoupang('${name}')">구매</button>
        `;
        container.appendChild(ingredientItem);
    }
}

// 조리순서 렌더링 (백엔드 데이터 형식에 맞게 수정)
function renderCookingSteps(steps, stepImages) {
    const container = document.getElementById("cookingStepsList");
    container.innerHTML = "";

    steps.forEach((stepDescription, index) => {
        const stepItem = document.createElement("div");
        stepItem.className = "step-item";
        const imageUrl = (stepImages && stepImages[index]) ? stepImages[index] : '';

        stepItem.innerHTML = `
          <div class="step-header">${index + 1}단계</div>
          <div class="step-content">
            <div class="step-description">${stepDescription}</div>
            <div class="step-image">
              ${imageUrl ? `<img src="${imageUrl}" alt="${index + 1}단계 이미지">` : '<div class="step-image-placeholder">사진</div>'}
            </div>
          </div>
        `;
        container.appendChild(stepItem);
    });
}


// GPT API를 통한 대체 재료 추천
async function getSubstituteIngredient(ingredientName, button) {
    try {
        button.disabled = true;
        button.textContent = "로딩...";

        // 데모용 대체 재료 추천 (실제로는 API 호출)
        // const response = await fetch("/api/gpt/substitute", {
        const response = await new Promise(resolve => {
            setTimeout(() => {
                resolve({
                    ok: true,
                    json: () => Promise.resolve({
                        substitute: `${ingredientName} 대신 사용할 수 있는 대체 재료:\n\n1. 비슷한 맛의 재료\n2. 영양가가 비슷한 재료\n3. 식감이 비슷한 재료`
                    })
                });
            }, 1000);
        });
        
        /*
        const response = await fetch("/api/gpt/substitute", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                ingredient: ingredientName,
            }),
        });
        */

        if (!response.ok) {
            throw new Error("대체 재료 추천을 받을 수 없습니다.");
        }

        const result = await response.json();
        alert(`${ingredientName} 대체 재료 추천:\n\n${result.substitute}`);
    } catch (error) {
        console.error("대체 재료 추천 에러:", error);
        alert("대체 재료 추천에 실패했습니다: " + error.message);
    } finally {
        button.disabled = false;
        button.textContent = "대체";
    }
}

// 쿠팡 구매 링크로 이동
function goToCoupang(ingredientName) {
    const coupangUrl = `https://www.coupang.com/np/search?component=&q=${encodeURIComponent(
        ingredientName
    )}`;
    window.open(coupangUrl, "_blank");
}

// 슬라이드 메뉴 열기
function toggleSlideMenu() {
    const slideMenu = document.getElementById('slideMenu');
    slideMenu.classList.toggle('active');
    
    // body 스크롤 방지
    if (slideMenu.classList.contains('active')) {
        document.body.style.overflow = 'hidden';
    } else {
        document.body.style.overflow = '';
    }
}

// 슬라이드 메뉴 닫기
function closeSlideMenu() {
    const slideMenu = document.getElementById('slideMenu');
    slideMenu.classList.remove('active');
    document.body.style.overflow = '';
}

// 로그아웃 처리
function handleLogout() {
    if (confirm('로그아웃하시겠습니까?')) {
        // 실제로는 로그아웃 API 호출
        // 예: await logout();
        
        // 데모용으로 로컬스토리지 정리 및 리다이렉트
        localStorage.clear();
        alert('로그아웃되었습니다.');
        window.location.href = '/login';
    }
}

// 페이지 이동 함수
function goToPage(url) {
    closeSlideMenu(); // 메뉴 먼저 닫기
    window.location.href = url;
}

// 탭바 활성화 상태 변경
function setActiveTab(tabName) {
    const tabItems = document.querySelectorAll(".tab-item");
    tabItems.forEach((item) => {
        if (item.dataset.tab === tabName) {
            item.classList.add("active");
        } else {
            item.classList.remove("active");
        }
    });
}

// ESC 키로 메뉴 닫기
document.addEventListener('keydown', function(e) {
    if (e.key === 'Escape') {
        closeSlideMenu();
    }
});

// 좋아요 버튼 초기화
function initializeLikeButton() {
    // 현재 레시피의 좋아요 상태를 로컬스토리지에서 확인 (실제로는 서버 API 호출)
    const recipeId = "sample-recipe"; // 실제로는 현재 레시피 ID
    const likedRecipes = JSON.parse(localStorage.getItem('likedRecipes') || '[]');

    isLiked = likedRecipes.includes(recipeId);
    updateLikeButtonState();
}

// 좋아요 토글 함수
function toggleLike() {
    const recipeId = "sample-recipe"; // 실제로는 현재 레시피 ID

    // 좋아요 상태 토글
    isLiked = !isLiked;

    // 로컬스토리지 업데이트 (실제로는 서버 API 호출)
    let likedRecipes = JSON.parse(localStorage.getItem('likedRecipes') || '[]');

    if (isLiked) {
        // 좋아요 추가
        if (!likedRecipes.includes(recipeId)) {
            likedRecipes.push(recipeId);
        }
        console.log('레시피 좋아요 추가');
    } else {
        // 좋아요 제거
        likedRecipes = likedRecipes.filter(id => id !== recipeId);
        console.log('레시피 좋아요 제거');
    }

    localStorage.setItem('likedRecipes', JSON.stringify(likedRecipes));

    // UI 업데이트
    updateLikeButtonState();

    // 실제 환경에서는 서버에 좋아요 상태 전송
    // await updateLikeStatus(recipeId, isLiked);
}

// 좋아요 버튼 상태 업데이트
function updateLikeButtonState() {
    const likeButton = document.getElementById('likeButton');

    if (isLiked) {
        likeButton.classList.add('liked');
    } else {
        likeButton.classList.remove('liked');
    }
}

// 서버에 좋아요 상태 업데이트 (실제 구현 시 사용)
async function updateLikeStatus(recipeId, liked) {
    try {
        const response = await fetch(`/api/recipes/${recipeId}/like`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ liked: liked }),
        });

        if (!response.ok) {
            throw new Error('좋아요 상태 업데이트 실패');
        }

        console.log('서버에 좋아요 상태 업데이트 완료');
    } catch (error) {
        console.error('좋아요 상태 업데이트 에러:', error);
        // 에러 발생 시 상태 되돌리기
        isLiked = !isLiked;
        updateLikeButtonState();
        alert('좋아요 처리 중 오류가 발생했습니다.');
    }
}
