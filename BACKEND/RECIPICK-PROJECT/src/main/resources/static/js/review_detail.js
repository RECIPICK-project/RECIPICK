// 현재 사용자의 평점 (0~5)
let currentUserRating = 0;
const API_REVIEW_URL = '/api/reviews'; // 리뷰 API 기본 URL

// 리뷰 시스템 초기화 함수
function initializeReviewSystem(postId) {
    if (!postId) return;

    // 기존 리뷰 목록 불러오기
    loadReviews(postId);

    const starRatingContainer = document.getElementById('starRating');
    const ratingText = document.getElementById('ratingText');
    const stars = starRatingContainer.querySelectorAll('.star');

    // 별점 클릭 이벤트
    stars.forEach(star => {
        star.addEventListener('click', () => {
            currentUserRating = parseInt(star.dataset.rating);
            updateStarDisplay(currentUserRating);
            ratingText.textContent = `${currentUserRating}점 선택됨`;
        });

        star.addEventListener('mouseover', () => {
            const rating = parseInt(star.dataset.rating);
            updateStarDisplay(rating, true);
        });
    });

    starRatingContainer.addEventListener('mouseleave', () => {
        updateStarDisplay(currentUserRating);
    });

    // 댓글 등록 버튼 이벤트
    const submitBtn = document.getElementById('submitComment');
    submitBtn.addEventListener('click', () => {
        submitReview(postId);
    });
}

// 별점 표시 업데이트 함수
function updateStarDisplay(rating, isHover = false) {
    const stars = document.querySelectorAll('#starRating .star');
    stars.forEach(star => {
        const starRating = parseInt(star.dataset.rating);
        if (starRating <= rating) {
            star.classList.add('filled');
        } else {
            star.classList.remove('filled');
        }
    });
}


// 특정 게시글의 리뷰 목록을 불러오는 함수
async function loadReviews(postId) {
    const commentsList = document.getElementById('commentsList');
    try {
        // GET /api/reviews/post/{postId} 호출
        const response = await fetch(`${API_REVIEW_URL}/post/${postId}`);
        if (!response.ok) {
            throw new Error('리뷰를 불러오지 못했습니다.');
        }

        const reviewPage = await response.json(); // Page<ReviewResponseDto> 형태
        const reviews = reviewPage.content;

        commentsList.innerHTML = ''; // 기존 목록 초기화

        if (reviews.length === 0) {
            commentsList.innerHTML = '<div class="no-comments">아직 등록된 댓글이 없습니다.</div>';
            return;
        }

        reviews.forEach(review => {
            const commentItem = createCommentElement(review);
            commentsList.appendChild(commentItem);
        });

    } catch (error) {
        console.error('리뷰 로딩 실패:', error);
        commentsList.innerHTML = '<div class="error-msg">리뷰를 불러오는 중 오류가 발생했습니다.</div>';
    }
}

// 리뷰 DOM 요소를 생성하는 함수
function createCommentElement(review) {
    const item = document.createElement('div');
    item.className = 'comment-item';

    const formattedDate = new Date(review.createdAt).toLocaleDateString();

    // ReviewResponseDto 필드에 맞춰서 데이터 바인딩
    item.innerHTML = `
        <div class="comment-header">
            <span class="comment-user">${review.nickname}</span>
            <div class="comment-rating">
                ${'★'.repeat(Math.floor(review.rating))}${'☆'.repeat(5 - Math.floor(review.rating))}
            </div>
            <span class="comment-date">${formattedDate}</span>
        </div>
        <p class="comment-text">${review.comment}</p>
    `;
    return item;
}


// 리뷰를 서버에 제출하는 함수
async function submitReview(postId) {
    const commentText = document.getElementById('commentText').value;

    if (currentUserRating === 0) {
        alert('별점을 선택해주세요.');
        return;
    }
    if (!commentText.trim()) {
        alert('댓글 내용을 입력해주세요.');
        return;
    }

    const reviewData = {
        postId: parseInt(postId),
        rating: currentUserRating,
        comment: commentText,
    };

    try {
        // POST /api/reviews 호출
        const response = await fetch(API_REVIEW_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(reviewData),
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(errorData.message || '리뷰 등록에 실패했습니다.');
        }

        alert('리뷰가 성공적으로 등록되었습니다.');
        // 폼 초기화 및 리뷰 목록 다시 불러오기
        document.getElementById('commentText').value = '';
        currentUserRating = 0;
        updateStarDisplay(0);
        document.getElementById('ratingText').textContent = '별점을 선택해주세요';
        loadReviews(postId);

    } catch (error) {
        console.error('리뷰 제출 실패:', error);
        alert(`오류: ${error.message}`);
    }
}