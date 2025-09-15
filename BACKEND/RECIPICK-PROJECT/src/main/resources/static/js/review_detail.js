// API 연동된 댓글 시스템
let currentUserRating = 0;
let hasUserCommented = false;
let currentPostId = null; // 실제로는 JSP/Thymeleaf에서 전달받거나 URL에서 추출

// 페이지 로드 시 초기화
document.addEventListener('DOMContentLoaded', function() {
    // 현재 게시글 ID 설정 (실제로는 서버에서 전달받아야 함)
    currentPostId = getCurrentPostId(); // 구현 필요
    
    initializeCommentSystem();
    loadReviewStats();
    loadReviews();
    checkUserReviewStatus();
});

// 현재 게시글 ID 가져오기 (실제 구현에서는 서버에서 전달받거나 URL에서 추출)
function getCurrentPostId() {
    // 예: URL에서 추출 /post/123 -> 123
    const pathParts = window.location.pathname.split('/');
    const postId = pathParts[pathParts.length - 1];
    return parseInt(postId) || 1; // 기본값 1
}

// 댓글 시스템 초기화
function initializeCommentSystem() {
    const stars = document.querySelectorAll(".star");
    const ratingText = document.getElementById("ratingText");
    const submitButton = document.getElementById("submitComment");

    // 별점 클릭 이벤트
    stars.forEach((star, index) => {
        star.addEventListener("click", function (e) {
            const rect = star.getBoundingClientRect();
            const clickX = e.clientX - rect.left;
            const starWidth = rect.width;

            const isHalf = clickX < starWidth / 2;
            const rating = index + (isHalf ? 0.5 : 1);

            currentUserRating = rating;
            setRating(rating);
            updateRatingText(rating);
        });

        // 호버 효과
        star.addEventListener("mouseenter", function (e) {
            const rect = star.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            const starWidth = rect.width;
            const isHalf = mouseX < starWidth / 2;
            const hoverRating = index + (isHalf ? 0.5 : 1);

            previewRating(hoverRating);
        });

        star.addEventListener("mousemove", function (e) {
            const rect = star.getBoundingClientRect();
            const mouseX = e.clientX - rect.left;
            const starWidth = rect.width;
            const isHalf = mouseX < starWidth / 2;
            const hoverRating = index + (isHalf ? 0.5 : 1);

            previewRating(hoverRating);
        });
    });

    // 별점 영역을 벗어날 때 원래 상태로 복원
    document.getElementById("starRating").addEventListener("mouseleave", function () {
        setRating(currentUserRating);
        updateRatingText(currentUserRating);
    });

    // 댓글 제출
    submitButton.addEventListener("click", function () {
        submitComment();
    });
}

// API: 리뷰 통계 로드
async function loadReviewStats() {
    try {
        const response = await fetch(`/api/reviews/post/${currentPostId}/stats`);
        if (response.ok) {
            const stats = await response.json();
            updateReviewStats(stats);
        }
    } catch (error) {
        console.error('리뷰 통계 로드 실패:', error);
    }
}

// API: 리뷰 목록 로드
async function loadReviews(page = 0, size = 10) {
    try {
        const response = await fetch(`/api/reviews/post/${currentPostId}?page=${page}&size=${size}`);
        if (response.ok) {
            const reviewsPage = await response.json();
            renderComments(reviewsPage.content);
            updatePagination(reviewsPage);
        }
    } catch (error) {
        console.error('리뷰 목록 로드 실패:', error);
        document.getElementById("commentsList").innerHTML = 
            '<div class="error-message">리뷰를 불러오는데 실패했습니다.</div>';
    }
}

// API: 사용자 리뷰 작성 여부 확인
async function checkUserReviewStatus() {
    try {
        const response = await fetch(`/api/reviews/post/${currentPostId}/user-status`);
        if (response.ok) {
            hasUserCommented = await response.json();
            
            if (hasUserCommented) {
                // 기존 리뷰 정보 가져오기
                loadUserReview();
            }
        }
    } catch (error) {
        console.error('사용자 리뷰 상태 확인 실패:', error);
    }
}

// API: 사용자의 기존 리뷰 로드
async function loadUserReview() {
    try {
        const response = await fetch(`/api/reviews/post/${currentPostId}/user-review`);
        if (response.ok) {
            const review = await response.json();
            showExistingReview(review);
        }
    } catch (error) {
        console.error('기존 리뷰 로드 실패:', error);
        showAlreadyCommentedMessage();
    }
}

// API: 리뷰 작성
async function submitComment() {
    if (hasUserCommented) {
        alert("이미 댓글을 작성하셨습니다.");
        return;
    }

    const rating = currentUserRating;
    const commentText = document.getElementById("commentText").value.trim();

    if (rating === 0) {
        alert("별점을 선택해주세요.");
        return;
    }

    if (commentText === "") {
        alert("댓글을 입력해주세요.");
        return;
    }

    try {
        const response = await fetch('/api/reviews', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                postId: currentPostId,
                rating: rating,
                comment: commentText
            })
        });

        if (response.ok) {
            const newReview = await response.json();
            
            // 성공 처리
            hasUserCommented = true;
            showExistingReview(newReview);
            
            // 데이터 새로고침
            loadReviews();
            loadReviewStats();
            
            alert("댓글이 등록되었습니다!");
        } else {
            const error = await response.text();
            alert("댓글 등록 실패: " + error);
        }
    } catch (error) {
        console.error('댓글 등록 실패:', error);
        alert("서버 연결에 실패했습니다.");
    }
}

// API: 리뷰 수정
async function updateReview(reviewId) {
    const rating = currentUserRating;
    const commentText = document.getElementById("commentText").value.trim();

    if (rating === 0) {
        alert("별점을 선택해주세요.");
        return;
    }

    if (commentText === "") {
        alert("댓글을 입력해주세요.");
        return;
    }

    try {
        const response = await fetch(`/api/reviews/${reviewId}`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                postId: currentPostId,
                rating: rating,
                comment: commentText
            })
        });

        if (response.ok) {
            const updatedReview = await response.json();
            showExistingReview(updatedReview);
            
            // 데이터 새로고침
            loadReviews();
            loadReviewStats();
            
            alert("댓글이 수정되었습니다!");
        } else {
            const error = await response.text();
            alert("댓글 수정 실패: " + error);
        }
    } catch (error) {
        console.error('댓글 수정 실패:', error);
        alert("서버 연결에 실패했습니다.");
    }
}

// API: 리뷰 삭제
async function deleteReview(reviewId) {
    if (!confirm("정말로 댓글을 삭제하시겠습니까?")) {
        return;
    }

    try {
        const response = await fetch(`/api/reviews/${reviewId}`, {
            method: 'DELETE'
        });

        if (response.ok) {
            hasUserCommented = false;
            resetCommentForm();
            
            // 데이터 새로고침
            loadReviews();
            loadReviewStats();
            
            alert("댓글이 삭제되었습니다!");
        } else {
            const error = await response.text();
            alert("댓글 삭제 실패: " + error);
        }
    } catch (error) {
        console.error('댓글 삭제 실패:', error);
        alert("서버 연결에 실패했습니다.");
    }
}

// API: 리뷰 신고
async function reportReview(reviewId) {
    if (!confirm("이 댓글을 신고하시겠습니까?")) {
        return;
    }

    try {
        const response = await fetch(`/api/reviews/${reviewId}/report`, {
            method: 'POST'
        });

        if (response.ok) {
            alert("신고가 접수되었습니다.");
        } else {
            alert("신고 처리에 실패했습니다.");
        }
    } catch (error) {
        console.error('신고 실패:', error);
        alert("서버 연결에 실패했습니다.");
    }
}

// 리뷰 통계 업데이트
function updateReviewStats(stats) {
    const avgRating = stats.averageRating || 0;
    const totalReviews = stats.totalReviews || 0;
    
    // 평균 별점 표시
    document.getElementById("averageRating").textContent = avgRating.toFixed(1);
    document.getElementById("totalReviews").textContent = `(${totalReviews}개)`;
    
    // 별점 분포 차트 업데이트 (구현 필요시)
    updateRatingDistribution(stats);
}

// 별점 분포 업데이트
function updateRatingDistribution(stats) {
    const total = stats.totalReviews || 1;
    
    // 각 별점별 비율 계산 및 표시
    for (let i = 1; i <= 5; i++) {
        const count = stats[`ratingCount${i}`] || 0;
        const percentage = (count / total * 100).toFixed(1);
        
        const barElement = document.getElementById(`rating-bar-${i}`);
        const countElement = document.getElementById(`rating-count-${i}`);
        
        if (barElement) barElement.style.width = `${percentage}%`;
        if (countElement) countElement.textContent = count;
    }
}

// 기존 리뷰 표시 (수정 가능한 형태)
function showExistingReview(review) {
    const commentForm = document.getElementById("commentForm");
    
    currentUserRating = parseFloat(review.rating);
    
    commentForm.innerHTML = `
        <div class="existing-review">
            <div class="review-header">
                <h4>작성한 리뷰</h4>
                <div class="review-actions">
                    <button onclick="enableEditMode(${review.reviewId})" class="edit-btn">수정</button>
                    <button onclick="deleteReview(${review.reviewId})" class="delete-btn">삭제</button>
                </div>
            </div>
            <div class="review-content">
                <div class="rating-display">${renderCommentStars(review.rating)}</div>
                <div class="comment-text">${review.comment}</div>
                <div class="review-date">작성일: ${formatDate(review.createdAt)}</div>
            </div>
        </div>
    `;
}

// 수정 모드 관련 함수들 제거됨

// 이미 댓글 작성 완료 메시지 표시 (API 에러시 fallback)
function showAlreadyCommentedMessage() {
    const commentForm = document.getElementById("commentForm");
    commentForm.innerHTML = `
        <div class="already-commented">
            이미 이 레시피에 댓글을 작성하셨습니다.
        </div>
    `;
    commentForm.classList.add("disabled");
}

// 댓글 폼 리셋
function resetCommentForm() {
    const commentForm = document.getElementById("commentForm");
    commentForm.innerHTML = `
        <div class="comment-form-content">
            <h4>리뷰 작성</h4>
            <div id="starRating" class="star-rating">
                ${Array.from({length: 5}, (_, i) => `
                    <div class="star">
                        <svg viewBox="0 0 24 24">
                            <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                        </svg>
                    </div>
                `).join('')}
            </div>
            <div id="ratingText">별점을 선택해주세요</div>
            <textarea id="commentText" placeholder="리뷰를 입력해주세요..." rows="4"></textarea>
            <button id="submitComment">리뷰 등록</button>
        </div>
    `;
    
    currentUserRating = 0;
    commentForm.classList.remove("disabled");
    
    // 이벤트 리스너 재설정
    initializeCommentSystem();
}

// 별점 설정
function setRating(rating) {
    const stars = document.querySelectorAll(".star");
    
    stars.forEach((star, index) => {
        const starRating = index + 1;
        star.classList.remove("filled", "half-filled");

        if (rating >= starRating) {
            star.classList.add("filled");
        } else if (rating >= starRating - 0.5) {
            star.classList.add("half-filled");
        }
    });
}

// 별점 미리보기
function previewRating(rating) {
    setRating(rating);
    updateRatingText(rating);
}

// 별점 텍스트 업데이트
function updateRatingText(rating) {
    const ratingText = document.getElementById("ratingText");
    if (rating === 0) {
        ratingText.textContent = "별점을 선택해주세요";
    } else {
        ratingText.textContent = `${rating}점 - ${getRatingText(rating)}`;
    }
}

// 평점에 따른 텍스트 반환
function getRatingText(rating) {
    const ratingTexts = {
        0.5: "별로예요", 1: "별로예요", 1.5: "그저 그래요", 2: "그저 그래요",
        2.5: "괜찮아요", 3: "괜찮아요", 3.5: "좋아요", 4: "좋아요",
        4.5: "훌륭해요", 5: "최고예요!"
    };
    return ratingTexts[rating] || "괜찮아요";
}

// 댓글 목록 렌더링
function renderComments(reviews) {
    const container = document.getElementById("commentsList");

    if (!reviews || reviews.length === 0) {
        container.innerHTML = '<div class="no-comments">첫 댓글을 작성해보세요!</div>';
        return;
    }

    container.innerHTML = "";

    reviews.forEach((review) => {
        const commentItem = document.createElement("div");
        commentItem.className = "comment-item";
        commentItem.innerHTML = `
            <div class="comment-header">
                <span class="comment-user">${review.nickname}</span>
                <div class="comment-rating">${renderCommentStars(review.rating)}</div>
                <span class="comment-date">${formatDate(review.createdAt)}</span>
                <button onclick="reportReview(${review.reviewId})" class="report-btn">신고</button>
            </div>
            <div class="comment-text">${review.comment}</div>
        `;
        container.appendChild(commentItem);
    });
}

// 댓글의 별점 렌더링
function renderCommentStars(rating) {
    let starsHtml = "";

    for (let i = 1; i <= 5; i++) {
        let starClass = "comment-star";
        if (rating >= i) {
            starClass += " filled";
        } else if (rating >= i - 0.5) {
            starClass += " half-filled";
        }

        starsHtml += `
            <div class="${starClass}">
                <svg viewBox="0 0 24 24">
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
                </svg>
            </div>
        `;
    }

    return starsHtml;
}

// 페이지네이션 업데이트
function updatePagination(reviewsPage) {
    const pagination = document.getElementById("pagination");
    if (!pagination) return;
    
    const { number, totalPages, first, last } = reviewsPage;
    
    let paginationHtml = '';
    
    // 이전 페이지
    if (!first) {
        paginationHtml += `<button onclick="loadReviews(${number - 1})">이전</button>`;
    }
    
    // 페이지 번호들
    for (let i = Math.max(0, number - 2); i <= Math.min(totalPages - 1, number + 2); i++) {
        const isActive = i === number ? 'active' : '';
        paginationHtml += `<button class="${isActive}" onclick="loadReviews(${i})">${i + 1}</button>`;
    }
    
    // 다음 페이지
    if (!last) {
        paginationHtml += `<button onclick="loadReviews(${number + 1})">다음</button>`;
    }
    
    pagination.innerHTML = paginationHtml;
}

// 날짜 포맷팅
function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
    });
}