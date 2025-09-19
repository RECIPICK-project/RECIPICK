// ====== review_detail.js  ======
// - 리뷰 목록: GET /api/reviews/post/{postId}
// - 리뷰 작성: POST /api/reviews
// - 리뷰 삭제: DELETE /api/reviews/{reviewId}
// - 리뷰 작성 여부: GET /api/reviews/post/{postId}/user-status

let currentUserRating = 0;

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
      return { loggedIn: false };
    }
  } catch (error) {
    console.warn("사용자 정보 조회 실패:", error);
  }
  return { loggedIn: false };
}

/* -------------------------------
 * DOMContentLoaded - 리뷰 시스템 초기화
 * ------------------------------- */
document.addEventListener("DOMContentLoaded", async function () {
  const postId = getPostIdFromUrl();
  if (!postId) return;
  await initializeReviewSystem(postId);
});

async function initializeReviewSystem(postId) {
  await loadReviews(postId);
  await checkUserReviewStatus(postId);
  setupReviewForm(postId);
}

async function loadReviews(postId) {
  const container = document.getElementById("commentsList");
  try {
    const res = await fx(`/api/reviews/post/${postId}`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const page = await res.json();
    renderReviews(page.content || []);
  } catch (error) {
    console.error("리뷰 목록 로딩 실패:", error);
    if (container) container.innerHTML = '<div class="no-comments">리뷰를 불러오는 데 실패했습니다.</div>';
  }
}

async function renderReviews(reviews) {
  const container = document.getElementById("commentsList");
  if (!container) return;

  if (!reviews || reviews.length === 0) {
    container.innerHTML = '<div class="no-comments">첫 댓글을 작성해보세요!</div>';
    return;
  }

  const currentUser = await getCurrentUserInfo();
  console.log("Current user info:", currentUser); 
  
  container.innerHTML = "";
  
  reviews.forEach((review) => {
    const el = document.createElement("div");
    el.className = "comment-item";
    const formattedDate = review.createdAt ? new Date(review.createdAt).toISOString().split("T")[0] : '';

    console.log("Review data:", review); 
    
    // 이메일 기반으로만 비교하도록 변경 (userId 대신)
    const canDelete = currentUser.loggedIn && 
                     currentUser.email && 
                     review.nickname && 
                     (currentUser.nickname === review.nickname || 
                      currentUser.email.split('@')[0] === review.nickname);
    
    console.log("Delete permission check:", {
      currentUserEmail: currentUser.email,
      currentUserNickname: currentUser.nickname,
      reviewNickname: review.nickname,
      canDelete: canDelete
    });
    
    const deleteButtonHtml = canDelete
  ? `<button class="btn-delete-review" data-review-id="${review.reviewId}">
       <svg viewBox="0 0 24 24" stroke="currentColor" stroke-width="2" fill="none">
         <path d="M18 6L6 18M6 6l12 12"/>
       </svg>
     </button>`
  : '';

    el.innerHTML = `
      <div class="comment-header">
        <span class="comment-user">${review.nickname || '익명'}</span>
        <div class="comment-rating">${renderCommentStars(review.reviewRating)}</div>
        <span class="comment-date">${formattedDate}</span>
        ${deleteButtonHtml}
      </div>
      <div class="comment-text">${review.comment}</div>
    `;
    container.appendChild(el);
  });

  // 삭제 버튼 이벤트 리스너 추가
  container.querySelectorAll('.btn-delete-review').forEach(button => {
    button.addEventListener('click', (e) => {
      const reviewId = e.currentTarget.dataset.reviewId;
      handleDeleteReview(reviewId);
    });
  });
}

async function handleDeleteReview(reviewId) {
    if (!confirm("정말로 리뷰를 삭제하시겠습니까?")) {
        return;
    }

    try {
        const res = await fx(`/api/reviews/${reviewId}`, { method: 'DELETE' });
        if (res.status === 204) {
            alert("리뷰가 삭제되었습니다.");
            const postId = getPostIdFromUrl();
            await loadReviews(postId);
            await checkUserReviewStatus(postId);
        } else if (res.status === 403) {
            alert("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        } else if (res.status === 401) {
            alert("로그인이 필요합니다.");
        } else {
            throw new Error(`서버 오류: ${res.status}`);
        }
    } catch (error) {
        console.error("리뷰 삭제 실패:", error);
        alert("리뷰 삭제 중 오류가 발생했습니다.");
    }
}

async function checkUserReviewStatus(postId) {
  try {
    const currentUser = await getCurrentUserInfo();
    if (!currentUser.loggedIn) {
      lockCommentForm("로그인 후 리뷰를 작성할 수 있습니다.");
      return;
    }
    
    // 1. 본인이 작성한 레시피인지 확인 (이메일/닉네임 기반)
    try {
      const postRes = await fx(`/post/${encodeURIComponent(postId)}`);
      if (postRes.ok) {
        const postData = await postRes.json();
        const post = (postData && typeof postData === "object" && "data" in postData) ? postData.data : postData;
        
        console.log("Post data:", post); 
        console.log("Current user:", currentUser); 
        
        // 닉네임 또는 이메일 기반으로 본인 게시글 확인
        const isOwnPost = (post.nickname && currentUser.nickname && post.nickname === currentUser.nickname) ||
                         (post.userEmail && currentUser.email && post.userEmail === currentUser.email) ||
                         (post.author && currentUser.nickname && post.author === currentUser.nickname);
        
        if (isOwnPost) {
          lockCommentForm("본인이 작성한 레시피에는 댓글을 작성할 수 없습니다.");
          return;
        }
      }
    } catch (postError) {
      console.warn("게시글 정보 조회 실패:", postError);
    }
    
    const res = await fx(`/api/reviews/post/${postId}/user-status`);
    if (res.status === 401) {
      lockCommentForm("로그인 후 리뷰를 작성할 수 있습니다.");
      return;
    }
    if (!res.ok) {
      lockCommentForm("리뷰 작성 상태를 확인할 수 없습니다.");
      return;
    }
    
    const hasReviewed = await res.json();
    if (hasReviewed) {
      lockCommentForm("이미 이 레시피에 댓글을 작성하셨습니다.");
    } else {
      unlockCommentForm();
    }
  } catch (error) {
    console.error("리뷰 작성 상태 확인 중 오류:", error);
    lockCommentForm("네트워크 오류로 인해 리뷰 작성 상태를 확인할 수 없습니다.");
  }
}

function lockCommentForm(message) {
  const commentForm = document.getElementById("commentForm");
  if (!commentForm) return;
  commentForm.innerHTML = `<div class="already-commented">${message}</div>`;
  commentForm.classList.add("disabled");
}

function unlockCommentForm() {
    const commentForm = document.getElementById('commentForm');
    if (!commentForm) return;
    commentForm.classList.remove('disabled');
    commentForm.innerHTML = `
        <div class="comment-form-title">이 레시피는 어떠셨나요?</div>
        <div class="rating-section">
            <label class="rating-label">별점</label>
            <div class="star-rating" id="starRating">
                ${[1, 2, 3, 4, 5].map(i => `
                    <div class="star" data-rating="${i}">
                        <svg viewBox="0 0 24 24">
                            <defs>
                                <linearGradient id="half-fill">
                                    <stop offset="50%" stop-color="#ffc107"/>
                                    <stop offset="50%" stop-color="#e0e0e0"/>
                                </linearGradient>
                            </defs>
                            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                        </svg>
                    </div>
                `).join('')}
            </div>
            <span class="rating-text" id="ratingText">별점을 선택해주세요</span>
        </div>
        <textarea class="comment-input" id="commentText" placeholder="이 레시피에 대한 경험을 공유해주세요..."></textarea>
        <button class="comment-submit" id="submitComment">댓글 등록</button>
    `;
    const postId = getPostIdFromUrl();
    setupReviewForm(postId);
}

function setupReviewForm(postId) {
  setTimeout(() => {
    const stars = document.querySelectorAll(".star-rating .star");
    const submitButton = document.getElementById("submitComment");
    const starRatingContainer = document.getElementById("starRating");
    
    if (!stars.length || !submitButton) return;
    
    starRatingContainer.addEventListener("mousemove", (e) => {
        const star = e.target.closest(".star");
        if (!star) return;
        const index = Array.from(star.parentNode.children).indexOf(star);
        const rect = star.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const rating = mouseX < rect.width / 2 ? index + 0.5 : index + 1;
        previewRating(rating);
    });

    starRatingContainer.addEventListener("mouseleave", () => {
        setRating(currentUserRating);
        updateRatingText(currentUserRating);
    });

    starRatingContainer.addEventListener("click", (e) => {
        const star = e.target.closest(".star");
        if (!star) return;
        const index = Array.from(star.parentNode.children).indexOf(star);
        const rect = star.getBoundingClientRect();
        const clickX = e.clientX - rect.left;
        currentUserRating = clickX < rect.width / 2 ? index + 0.5 : index + 1;
        setRating(currentUserRating);
        updateRatingText(currentUserRating);
    });
    
    submitButton.addEventListener("click", () => submitReview(postId));
  }, 100);
}

let isSubmitting = false; // 전역 변수로 추가

async function submitReview(postId) {
  // 이미 제출 중이면 무시
  if (isSubmitting) {
    console.log("이미 제출 중입니다.");
    return;
  }

  const commentText = document.getElementById("commentText")?.value.trim() || "";
  if (currentUserRating === 0) return alert("별점을 선택해주세요.");
  if (!commentText) return alert("댓글을 입력해주세요.");
  if (commentText.length > 255) return alert("댓글은 255자를 초과할 수 없습니다.");

  const reviewDto = { 
    postId: parseInt(postId, 10), 
    reviewRating: parseFloat(currentUserRating.toFixed(1)),
    comment: commentText 
  };
  
  console.log("Submitting review:", reviewDto);
  
  // 제출 상태로 설정
  isSubmitting = true;
  
  // 버튼 비활성화
  const submitButton = document.getElementById("submitComment");
  if (submitButton) {
    submitButton.disabled = true;
    submitButton.textContent = "등록 중...";
  }
  
  try {
    const res = await fx('/api/reviews', { 
      method: 'POST', 
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(reviewDto) 
    });
    
    console.log("Response status:", res.status);
    
    if (res.status === 400) {
      const errorData = await res.json();
      return alert(errorData.error || "리뷰를 등록할 수 없습니다.");
    }
    if (res.status === 401) {
      return alert("로그인이 필요합니다.");
    }
    if (!res.ok) {
      const errorText = await res.text();
      console.error("Server error:", errorText);
      throw new Error(`서버 오류: ${res.status}`);
    }
    
    alert("댓글이 성공적으로 등록되었습니다!");
    currentUserRating = 0;
    await loadReviews(postId);
    await checkUserReviewStatus(postId);
  } catch (error) {
    console.error("댓글 등록 실패:", error);
    alert("댓글 등록 중 오류가 발생했습니다.");
  } finally {
    // 제출 완료 후 상태 복원
    isSubmitting = false;
    if (submitButton) {
      submitButton.disabled = false;
      submitButton.textContent = "댓글 등록";
    }
  }
}

/* -------------------------------
 * 별점 UI 헬퍼 함수
 * ------------------------------- */
function setRating(rating) {
    const stars = document.querySelectorAll("#starRating .star");
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 !== 0;

    stars.forEach((star, index) => {
        const path = star.querySelector("path");
        if (!path) return;
        
        if (index < fullStars) {
            path.setAttribute("fill", "#ffc107");
        } else if (index === fullStars && hasHalfStar) {
            path.setAttribute("fill", "url(#half-fill)");
        } else {
            path.setAttribute("fill", "#e0e0e0");
        }
    });
}

function previewRating(rating) {
    const stars = document.querySelectorAll("#starRating .star");
    const fullStars = Math.floor(rating);
    const hasHalfStar = rating % 1 !== 0;

    stars.forEach((star, index) => {
        const path = star.querySelector("path");
        if (!path) return;

        if (index < fullStars) {
            path.setAttribute("fill", "#ffc107");
        } else if (index === fullStars && hasHalfStar) {
            path.setAttribute("fill", "url(#half-fill)");
        } else {
            path.setAttribute("fill", "#e0e0e0");
        }
    });
    updateRatingText(rating);
}

function updateRatingText(rating) {
    const el = document.getElementById("ratingText");
    if (!el) return;
    if (rating === 0) return el.textContent = "별점을 선택해주세요";
    const map = { 0.5: "별로예요", 1: "별로예요", 1.5: "그저 그래요", 2: "그저 그래요", 2.5: "괜찮아요", 3: "괜찮아요", 3.5: "좋아요", 4: "좋아요", 4.5: "훌륭해요", 5: "최고예요!" };
    el.textContent = `${rating}점 - ${map[rating]}`;
}

function renderCommentStars(rating) {
    let html = "";
    for (let i = 1; i <= 5; i++) {
        let fill = "#e0e0e0";
        if (rating >= i) {
            fill = "#ffc107";
        } else if (rating >= i - 0.5) {
            fill = "url(#half-fill)";
        }
        
        html += `<div class="comment-star">
                    <svg viewBox="0 0 24 24">
                        <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" fill="${fill}"/>
                    </svg>
                </div>`;
    }
    return html;
}

function getPostIdFromUrl() {
  const urlParams = new URLSearchParams(window.location.search);
  const idFromQuery = urlParams.get('postId');
  if (idFromQuery) {
    return parseInt(idFromQuery, 10);
  }
  
  // URL 경로에서도 postId 추출 시도
  const pathSegments = window.location.pathname.split('/').filter(Boolean);
  const lastSegment = pathSegments[pathSegments.length - 1];
  if (/^\d+$/.test(lastSegment)) {
    return parseInt(lastSegment, 10);
  }
  
  return null;
}

// 유틸리티 함수: 공통 fetch 헬퍼 (fx 함수가 정의되어 있지 않은 경우)
if (typeof fx === 'undefined') {
  window.fx = async function(path, options = {}) {
    const { method = 'GET', headers = {}, body, credentials = 'include' } = options;
    
    // 토큰 가져오기
    const token = localStorage.getItem('ACCESS_TOKEN');
    const authHeaders = token ? { 'Authorization': `Bearer ${token}` } : {};
    
    // CSRF 토큰 가져오기 (POST/PUT/DELETE의 경우)
    const csrfToken = getCookie('XSRF-TOKEN');
    const csrfHeaders = ['POST', 'PUT', 'DELETE'].includes(method) && csrfToken 
      ? { 'X-XSRF-TOKEN': decodeURIComponent(csrfToken) } : {};
    
    const finalHeaders = {
      'Accept': 'application/json',
      ...authHeaders,
      ...csrfHeaders,
      ...headers
    };async function getCurrentUserInfo() {
  try {
    const res = await fx('/api/users/profile');
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
      return { loggedIn: false };
    }
  } catch (error) {
    console.warn("사용자 정보 조회 실패:", error);
  }
  return { loggedIn: false };
}
    
    return fetch(path, {
      method,
      headers: finalHeaders,
      body,
      credentials
    });
  };
  
  // 쿠키 헬퍼 함수
  function getCookie(name) {
    return document.cookie
      .split('; ')
      .find(row => row.startsWith(name + '='))
      ?.split('=')[1];
  }
}