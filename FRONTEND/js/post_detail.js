// 예시 레시피 데이터
const sampleRecipeData = {
    title: "소고기콩나물솥밥",
    author: "맛있는요리사",
    difficulty: 4,
    servings: 4,
    cookingTime: "45 min",
    thumbnailUrl: "", // 이미지가 있으면 URL 넣기
    ingredients: "쌀|4컵|콩나물|200g|소고기다짐육|100g|다시마|3조각|부추|넉넉히|설탕|1/2스푼",
    steps: [
        {
            description: "소고기콩나물솥밥을 소개합니다.",
            imageUrl: "",
        },
        {
            description:
                "쌀은 깨끗하게 씻어준후 30분간 불린후 다시다 우린물에 밥을 지어주세요.냄비",
            imageUrl: "",
        },
        {
            description:
                "소고기다짐육을 후라이팬에 볶아가면서 설탕을 조금 넣어서 볶아주세요.프라이팬,요리스푼",
            imageUrl: "",
        },
        {
            description:
                "밥을 지어준후 뜸을 들일때쯤 뚜껑을 열어서 콩나물을 넉넉히 넣어서 뜸을 들여주세요.위생장갑",
            imageUrl: "",
        },
        {
            description: "10분정도 약불로 뜸을 들여준후 볶은 소고기를 올려주세요.",
            imageUrl: "",
        },
        {
            description: "부추를 송송송 썰어준후 뚜껑을 닫아주세요.",
            imageUrl: "",
        },
        {
            description:
                "주걱을 이용해서 고슬고슬 지어진 밥을 콩나물과 잘 섞이도록 잘 섞어주세요.밥주걱",
            imageUrl: "",
        },
        {
            description: "부추의 향이 가득 배인 소고기콩나물솥밥 이랍니다.",
            imageUrl: "",
        },
        {
            description: "소고기콩나물솥밥이 완성이 되었습니다.",
            imageUrl: "",
        },
    ],
};

// 댓글 데이터 (예시)
let commentsData = [
    {
        id: 1,
        user: "김요리사",
        rating: 4.5,
        text: "정말 맛있었어요! 콩나물이 아삭아삭하고 소고기와 잘 어울리네요. 다만 불 조절을 조심해야 할 것 같아요.",
        date: "2024-03-15",
    },
    {
        id: 2,
        user: "박주부",
        rating: 5,
        text: "가족들이 너무 좋아해요. 특히 아이들이 맛있다고 하네요. 레시피 감사합니다!",
        date: "2024-03-14",
    },
];

// 현재 사용자의 댓글 여부 (실제로는 서버에서 확인)
let hasUserCommented = false;
let currentUserRating = 0;

// 좋아요 상태 (실제로는 서버에서 확인)
let isLiked = false;

// 페이지 로드시 실행
document.addEventListener("DOMContentLoaded", function () {
    // URL에서 레시피 ID 추출 (예: /recipe/123)
    const pathArray = window.location.pathname.split("/");
    const recipeId = pathArray[pathArray.length - 1];

    // 실제 환경에서는 API 호출
    // loadRecipeData(recipeId);

    // 데모용으로 샘플 데이터 사용
    renderRecipeData(sampleRecipeData);
    initializeCommentSystem();
    renderComments();

    // 슬라이드 메뉴 이벤트 리스너 추가
    initializeSlideMenu();

    // 좋아요 상태 초기화
    initializeLikeButton();
});

// 슬라이드 메뉴 초기화
function initializeSlideMenu() {
    const slideMenu = document.getElementById('slideMenu');
    
    // 오버레이 클릭 시 메뉴 닫기
    slideMenu.addEventListener('click', function(e) {
        if (e.target.classList.contains('slide-menu-overlay')) {
            closeSlideMenu();
        }
    });
}
// 실제 레시피 데이터 로드 함수
async function loadRecipeData(recipeId) {
    try {
        const response = await fetch(`/api/recipes/${recipeId}`);
        if (!response.ok) {
            throw new Error("레시피를 불러올 수 없습니다.");
        }
        const recipeData = await response.json();
        renderRecipeData(recipeData);
    } catch (error) {
        console.error("레시피 로드 에러:", error);
        alert("레시피를 불러오는데 실패했습니다: " + error.message);
    }
}

// 레시피 데이터 렌더링
function renderRecipeData(data) {
    // 기본 정보 업데이트
    document.getElementById("recipeTitle").textContent = data.title;
    document.getElementById("authorName").textContent = data.author;
    document.getElementById("difficulty").textContent = data.difficulty;
    document.getElementById("servings").textContent = data.servings;
    document.getElementById("cookingTime").textContent = data.cookingTime;

    // 썸네일 이미지
    if (data.thumbnailUrl) {
        const thumbnail = document.getElementById("thumbnailBox");
        thumbnail.innerHTML = `<img src="${data.thumbnailUrl}" alt="레시피 이미지">`;
        thumbnail.classList.add("has-img");
    }

    // 재료 렌더링
    renderIngredients(data.ingredients);

    // 조리순서 렌더링
    renderCookingSteps(data.steps);
}

// 재료 리스트 렌더링
function renderIngredients(ingredientsString) {
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

// 조리순서 렌더링
function renderCookingSteps(steps) {
    const container = document.getElementById("cookingStepsList");
    container.innerHTML = "";

    steps.forEach((step, index) => {
        const stepItem = document.createElement("div");
        stepItem.className = "step-item";
        stepItem.innerHTML = `
      <div class="step-header">${index + 1}단계</div>
      <div class="step-content">
        <div class="step-description">${step.description}</div>
        <div class="step-image">
          ${
              step.imageUrl
                  ? `<img src="${step.imageUrl}" alt="${index + 1}단계 이미지">`
                  : '<div class="step-image-placeholder">사진</div>'
          }
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
