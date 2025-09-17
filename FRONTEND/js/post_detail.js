// ====== post_detail.js (live) ======
// - 상세: GET /post/{postId} (PostRestController#getRecipeById)
//   응답: { success: true, data: { ...PostDto } }  또는  바로 { ...PostDto }
// - 좋아요: /post/{postId}/like  (GET: 여부, POST: 좋아요, DELETE: 취소)

let hasUserCommented = false;
let currentUserRating = 0;
let isLiked = false;

/* -------------------------------
 * 공통 유틸 (토큰/CSRF/Fetch)
 * ------------------------------- */
function getCookie(name) {
  return document.cookie
      .split("; ")
      .map((s) => s.trim())
      .find((row) => row.startsWith(name + "="))
      ?.split("=")[1];
}

function authHeader() {
  const token = localStorage.getItem("ACCESS_TOKEN");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function csrfHeaders(method) {
  const m = (method || "GET").toUpperCase();
  if (["GET", "HEAD", "OPTIONS", "TRACE"].includes(m)) return {};
  const xsrf = getCookie("XSRF-TOKEN");
  return xsrf ? { "X-XSRF-TOKEN": decodeURIComponent(xsrf) } : {};
}

async function fx(path, { method = "GET", headers = {}, body, credentials = "include" } = {}) {
  const isForm = typeof FormData !== "undefined" && body instanceof FormData;
  const h = {
    Accept: "application/json",
    ...authHeader(),
    ...csrfHeaders(method),
    ...headers,
  };
  if (isForm && h["Content-Type"]) delete h["Content-Type"];
  const res = await fetch(path, { method, headers: h, body, credentials });
  return res;
}

/* -------------------------------
 * URL에서 postId 추출 ( /post_detail/123  또는  ?postId=123 )
 * ------------------------------- */
function getPostIdFromUrl() {
  const p = new URLSearchParams(location.search).get("postId");
  if (p && /^\d+$/.test(p)) return p;
  const seg = location.pathname.split("/").filter(Boolean);
  const last = seg[seg.length - 1];
  return /^\d+$/.test(last) ? last : null;
}

/* -------------------------------
 * DOMContentLoaded
 * ------------------------------- */
document.addEventListener("DOMContentLoaded", async function () {
  const postId = getPostIdFromUrl();
  if (!postId) {
    alert("잘못된 접근입니다. (postId 없음)");
    return;
  }

  // 상세 불러오기
  await loadRecipeData(postId);

  // 댓글 (현재 로컬 기반 그대로 유지)
  initializeCommentSystem();
  renderComments();

  // 슬라이드 메뉴
  initializeSlideMenu();

  // 좋아요 버튼 초기화
  await initializeLikeButton(postId);

  // 탭바 표시 갱신이 필요하면 setActiveTab('home') 같은 거 호출
});

/* -------------------------------
 * 상세 불러오기
 * ------------------------------- */
async function loadRecipeData(postId) {
  try {
    const res = await fx(`/post/${encodeURIComponent(postId)}`);
    if (res.status === 401) {
      console.warn("401 Unauthorized");
      // location.href = '/pages/login.html';
      // return;
    }
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const json = await res.json();

    // ★ 응답 래핑/비래핑 모두 지원
    const dto = (json && typeof json === "object" && "data" in json)
        ? json.data
        : json;

    if (!dto || typeof dto !== "object") {
      throw new Error("응답 형식이 올바르지 않습니다.");
    }

    // 서버 DTO → 화면 데이터로 정규화
    const view = normalizeRecipe(dto);
    renderRecipeData(view);
  } catch (e) {
    console.error("레시피 로드 에러:", e);
    alert("레시피를 불러올 수 없습니다.");
  }
}

/* -------------------------------
 * DTO 정규화
 * ------------------------------- */
function normalizeRecipe(dto) {
  const title =
      dto.title ??
      dto.foodName ??
      "";
  const author =
      dto.author ??
      dto.nickname ??
      dto.userName ??
      "";
  const difficulty =
      dto.difficulty ??
      dto.ckgLevel ??
      "";
  const servings =
      dto.servings ??
      dto.ckgInbun ??
      "";
  const cookingTime =
      dto.cookingTimeString ??
      dto.cookingTime ??
      "";
  const thumbnailUrl =
      dto.rcpImgUrl ??
      dto.imageUrl ??
      dto.thumb ??
      "";

  // 재료 문자열: "이름|수량|이름|수량|..."
  const ingredients =
      dto.ingredientsString ??
      dto.rcpIngredients ??
      "";

  // 단계
  let stepsRaw =
      dto.rcpSteps ??
      dto.steps ??
      dto.stepsString ??
      [];

  const steps = parseSteps(stepsRaw);

  return {
    title,
    author,
    difficulty,
    servings,
    cookingTime,
    thumbnailUrl,
    ingredients,
    steps,
  };
}

// 교체: parseSteps
function parseSteps(stepsRaw) {
  const explodePipe = (txt) =>
      String(txt)
          .split("|")
          .map((t) => t.trim())
          .filter(Boolean)
          .map((t) => ({ description: t, imageUrl: "" }));

  if (Array.isArray(stepsRaw)) {
    const out = [];
    for (const s of stepsRaw) {
      if (s == null) continue;
      if (typeof s === "string") {
        // 배열인데 원소가 "1. 준비|2. 조리" 같은 형태면 파이프 분해
        if (s.includes("|")) out.push(...explodePipe(s));
        else out.push({ description: s.trim(), imageUrl: "" });
      } else if (typeof s === "object") {
        const desc = s.description ?? s.desc ?? "";
        const img  = s.imageUrl ?? s.img ?? "";
        if (typeof desc === "string" && desc.includes("|")) {
          out.push(...explodePipe(desc));
        } else if (desc) {
          out.push({ description: String(desc).trim(), imageUrl: img || "" });
        }
      } else {
        out.push({ description: String(s).trim(), imageUrl: "" });
      }
    }
    return out;
  }

  if (typeof stepsRaw === "string") {
    return explodePipe(stepsRaw);
  }

  return [];
}


/* -------------------------------
 * 렌더링
 * ------------------------------- */
// 헬퍼: 여러 후보 셀렉터 중 첫 매칭
function getEl(...selectors) {
  for (const s of selectors) {
    const el = document.querySelector(s);
    if (el) return el;
  }
  return null;
}
function renderRecipeData(data) {
  // 제목/작성자/난이도/인분/시간 — id가 다르면 data-field나 클래스도 시도
  const titleEl = getEl('#recipeTitle', '[data-field="title"]', '.recipe-title');
  const authorEl = getEl('#authorName', '[data-field="author"]', '.recipe-author');
  const diffEl   = getEl('#difficulty', '[data-field="difficulty"]', '.recipe-difficulty');
  const servEl   = getEl('#servings',   '[data-field="servings"]',   '.recipe-servings');
  const timeEl   = getEl('#cookingTime','[data-field="cookingTime"]','.recipe-time');

  if (titleEl)  titleEl.textContent = data.title || '';
  if (authorEl) authorEl.textContent = data.author || '';
  if (diffEl)   diffEl.textContent   = data.difficulty || '';
  if (servEl)   servEl.textContent   = data.servings || '';
  if (timeEl)   timeEl.textContent   = data.cookingTime || '';

  // 썸네일
  const thumbBox = getEl('#thumbnailBox', '.thumbnail-box', '[data-field="thumbnail"]');
  if (thumbBox) {
    if (data.thumbnailUrl) {
      thumbBox.innerHTML = `<img src="${data.thumbnailUrl}" alt="레시피 이미지">`;
      thumbBox.classList.add('has-img');
    } else {
      thumbBox.innerHTML = '';
      thumbBox.classList.remove('has-img');
    }
  }

  renderIngredients(data.ingredients);
  renderCookingSteps(data.steps);
}

function renderIngredients(ingredientsString) {
  const container = document.getElementById("ingredientsList");
  if (!container) return;
  container.innerHTML = "";

  if (!ingredientsString || typeof ingredientsString !== "string") return;

  const arr = ingredientsString.split("|");
  for (let i = 0; i < arr.length; i += 2) {
    const name = arr[i];
    const amount = arr[i + 1] || "";
    if (!name) continue;

    // ★ onclick 인자 안전 처리 (따옴표/특수문자 보정)
    const safeNameArg = JSON.stringify(String(name));

    const el = document.createElement("div");
    el.className = "ingredient-item";
    el.innerHTML = `
      <span class="ingredient-name">${name}</span>
      <span class="ingredient-amount">${amount}</span>
      <button class="btn-small btn-substitute" onclick="getSubstituteIngredient(${safeNameArg}, this)">대체</button>
      <button class="btn-small" onclick="goToCoupang(${safeNameArg})">구매</button>
    `;
    container.appendChild(el);
  }
}

function renderCookingSteps(steps) {
  const container = document.getElementById("cookingStepsList");
  if (!container) return;
  container.innerHTML = "";

  (steps || []).forEach((step, idx) => {
    const desc = (step && step.description) ? step.description : String(step || "");
    const img = (step && step.imageUrl) ? step.imageUrl : "";

    const el = document.createElement("div");
    el.className = "step-item";
    el.innerHTML = `
      <div class="step-header">${idx + 1}단계</div>
      <div class="step-content">
        <div class="step-description">${desc}</div>
        <div class="step-image">
          ${
        img
            ? `<img src="${img}" alt="${idx + 1}단계 이미지">`
            : '<div class="step-image-placeholder">사진</div>'
    }
        </div>
      </div>
    `;
    container.appendChild(el);
  });
}

/* -------------------------------
 * 대체 재료 / 쿠팡
 * ------------------------------- */
async function getSubstituteIngredient(ingredientName, button) {
  try {
    button.disabled = true;
    button.textContent = "로딩...";
    // TODO: 서버 연동 시 교체
    await new Promise((r) => setTimeout(r, 600));
    alert(
        `${ingredientName} 대신 쓸 수 있는 대체 재료 예시\n\n1) 비슷한 맛\n2) 영양 유사\n3) 식감 유사`
    );
  } catch (e) {
    console.error(e);
    alert("대체 재료 추천에 실패했습니다.");
  } finally {
    button.disabled = false;
    button.textContent = "대체";
  }
}

function goToCoupang(ingredientName) {
  const url =
      "https://www.coupang.com/np/search?component=&q=" + encodeURIComponent(ingredientName);
  window.open(url, "_blank");
}

/* -------------------------------
 * 슬라이드 메뉴
 * ------------------------------- */
function initializeSlideMenu() {
  const slideMenu = document.getElementById("slideMenu");
  if (!slideMenu) return;
  slideMenu.addEventListener("click", function (e) {
    if (e.target.classList.contains("slide-menu-overlay")) {
      closeSlideMenu();
    }
  });
}

function toggleSlideMenu() {
  const slideMenu = document.getElementById("slideMenu");
  if (!slideMenu) return;
  slideMenu.classList.toggle("active");
  document.body.style.overflow = slideMenu.classList.contains("active") ? "hidden" : "";
}

function closeSlideMenu() {
  const slideMenu = document.getElementById("slideMenu");
  if (!slideMenu) return;
  slideMenu.classList.remove("active");
  document.body.style.overflow = "";
}

document.addEventListener("keydown", function (e) {
  if (e.key === "Escape") closeSlideMenu();
});

function handleLogout() {
  if (confirm("로그아웃하시겠습니까?")) {
    localStorage.clear();
    alert("로그아웃되었습니다.");
    location.href = "/login";
  }
}

function goToPage(url) {
  closeSlideMenu();
  location.href = url;
}

function setActiveTab(tabName) {
  document.querySelectorAll(".tab-item").forEach((el) => {
    el.classList.toggle("active", el.dataset.tab === tabName);
  });
}

/* -------------------------------
 * 좋아요 (서버 연동)
 * ------------------------------- */
async function initializeLikeButton(postId) {
  try {
    const res = await fx(`/post/${encodeURIComponent(postId)}/like`, { method: "GET" });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    isLiked = await res.json(); // 서버: true/false
  } catch (e) {
    console.warn("좋아요 상태 조회 실패(비로그인 또는 오류):", e);
    isLiked = false;
  } finally {
    updateLikeButtonState();
    const likeButton = document.getElementById("likeButton");
    if (likeButton) {
      likeButton.onclick = () => toggleLike(postId);
    }
  }
}

async function toggleLike(postId) {
  // 낙관적 UI 적용
  isLiked = !isLiked;
  updateLikeButtonState();

  try {
    const method = isLiked ? "POST" : "DELETE";
    const res = await fx(`/post/${encodeURIComponent(postId)}/like`, { method });
    if (res.status === 401) {
      // location.href = '/pages/login.html';
      throw new Error("401 Unauthorized");
    }
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
  } catch (e) {
    console.error("좋아요 처리 실패:", e);
    // 롤백
    isLiked = !isLiked;
    updateLikeButtonState();
    alert("좋아요 처리 중 오류가 발생했습니다.");
  }
}

function updateLikeButtonState() {
  const likeButton = document.getElementById("likeButton");
  if (!likeButton) return;
  likeButton.classList.toggle("liked", !!isLiked);
}

/* -------------------------------
 * 댓글(로컬 유지) - 서버 연동은 추후
 * ------------------------------- */
let commentsData = []; // 샘플 제거, 비워둠

function initializeCommentSystem() {
  const stars = document.querySelectorAll(".star");
  const submitButton = document.getElementById("submitComment");

  stars.forEach((star, index) => {
    star.addEventListener("click", function (e) {
      const rect = star.getBoundingClientRect();
      const clickX = e.clientX - rect.left;
      const isHalf = clickX < rect.width / 2;
      const rating = index + (isHalf ? 0.5 : 1);
      currentUserRating = rating;               // ★ 선택값 보존
      setRating(rating);
      updateRatingText(rating);
    });

    star.addEventListener("mouseenter", function (e) {
      const rect = star.getBoundingClientRect();
      const mouseX = e.clientX - rect.left;
      const isHalf = mouseX < rect.width / 2;
      const hoverRating = index + (isHalf ? 0.5 : 1);
      previewRating(hoverRating);
    });

    star.addEventListener("mousemove", function (e) {
      const rect = star.getBoundingClientRect();
      const mouseX = e.clientX - rect.left;
      const isHalf = mouseX < rect.width / 2;
      const hoverRating = index + (isHalf ? 0.5 : 1);
      previewRating(hoverRating);
    });
  });

  document.getElementById("starRating")?.addEventListener("mouseleave", function () {
    setRating(currentUserRating);
  });

  submitButton?.addEventListener("click", function () {
    submitComment();
  });

  checkUserCommentStatus();
}

function setRating(rating) {
  const stars = document.querySelectorAll(".star");
  stars.forEach((star, index) => {
    const starRating = index + 1;
    star.classList.remove("filled", "half-filled");
    if (rating >= starRating) star.classList.add("filled");
    else if (rating >= starRating - 0.5) star.classList.add("half-filled");
  });
}

function previewRating(rating) {
  setRating(rating);
  updateRatingText(rating);
}

function updateRatingText(rating) {
  const ratingText = document.getElementById("ratingText");
  if (!ratingText) return;
  if (rating === 0) {
    ratingText.textContent = "별점을 선택해주세요";
  } else {
    const map = {
      0.5: "별로예요",
      1: "별로예요",
      1.5: "그저 그래요",
      2: "그저 그래요",
      2.5: "괜찮아요",
      3: "괜찮아요",
      3.5: "좋아요",
      4: "좋아요",
      4.5: "훌륭해요",
      5: "최고예요!",
    };
    ratingText.textContent = `${rating}점 - ${map[rating]}`;
  }
}

function checkUserCommentStatus() {
  const recipeId = getPostIdFromUrl() || "current";
  const userComment = localStorage.getItem(`comment_${recipeId}`);
  if (userComment) {
    hasUserCommented = true;
    showAlreadyCommentedMessage();
  }
}

function showAlreadyCommentedMessage() {
  const commentForm = document.getElementById("commentForm");
  if (!commentForm) return;
  commentForm.innerHTML = `
    <div class="already-commented">
      이미 이 레시피에 댓글을 작성하셨습니다.
    </div>
  `;
  commentForm.classList.add("disabled");
}

function submitComment() {
  if (hasUserCommented) {
    alert("이미 댓글을 작성하셨습니다.");
    return;
  }
  const commentTextEl = document.getElementById("commentText");
  const commentText = commentTextEl ? commentTextEl.value.trim() : "";
  if (currentUserRating === 0) {
    alert("별점을 선택해주세요.");
    return;
  }
  if (!commentText) {
    alert("댓글을 입력해주세요.");
    return;
  }

  const newComment = {
    id: Date.now(),
    user: "현재사용자",
    rating: currentUserRating,
    text: commentText,
    date: new Date().toISOString().split("T")[0],
  };

  commentsData.unshift(newComment);

  const recipeId = getPostIdFromUrl() || "current";
  localStorage.setItem(`comment_${recipeId}`, JSON.stringify(newComment));

  hasUserCommented = true;
  showAlreadyCommentedMessage();
  renderComments();

  alert("댓글이 등록되었습니다!");
}

function renderComments() {
  const container = document.getElementById("commentsList");
  if (!container) return;

  if (!commentsData.length) {
    container.innerHTML = '<div class="no-comments">첫 댓글을 작성해보세요!</div>';
    return;
  }

  container.innerHTML = "";
  commentsData.forEach((c) => {
    const el = document.createElement("div");
    el.className = "comment-item";
    el.innerHTML = `
      <div class="comment-header">
        <span class="comment-user">${c.user}</span>
        <div class="comment-rating">${renderCommentStars(c.rating)}</div>
        <span class="comment-date">${c.date}</span>
      </div>
      <div class="comment-text">${c.text}</div>
    `;
    container.appendChild(el);
  });
}

function renderCommentStars(rating) {
  let html = "";
  for (let i = 1; i <= 5; i++) {
    let cls = "comment-star";
    if (rating >= i) cls += " filled";
    else if (rating >= i - 0.5) cls += " half-filled";
    html += `
      <div class="${cls}">
        <svg viewBox="0 0 24 24">
          <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
        </svg>
      </div>
    `;
  }
  return html;
}

/* ★ 중복/충돌 위험: 전역 클릭 핸들러 제거 (initializeCommentSystem에서 처리함)
document.addEventListener("click", function (e) {
  const star = e.target.closest(".star");
  if (!star) return;
  const stars = document.querySelectorAll(".star");
  const index = Array.from(stars).indexOf(star);
  const rect = star.getBoundingClientRect();
  const clickX = e.clientX - rect.left;
  const isHalf = clickX < rect.width / 2;
  currentUserRating = index + (isHalf ? 0.5 : 1);
});
*/
