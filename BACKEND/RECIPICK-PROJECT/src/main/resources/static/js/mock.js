// js/mock.js
window.MOCK = {
  metrics: { totalUsers: 1200, newToday: 18, active: 860, suspended: 14, dau: 320, wau: 1280 },
  topReported: [
    { id: 101, title: "매운 제육볶음", count: 6 },
    { id: 77,  title: "김치볶음밥",   count: 4 }
  ],
  reports: [
    { id: 1, type: "POST",   targetId: 101, reason: "부적절", count: 6 },
    { id: 2, type: "REVIEW", targetId: 55,  reason: "스팸",   count: 3 }
  ],
  users: [
    { id: 1, nickname: "하원", grade: "USER", status: "ACTIVE",    latestAt: "2025-08-24 21:10" },
    { id: 2, nickname: "민지", grade: "USER", status: "SUSPENDED", latestAt: "2025-08-20 10:03" }
  ],
  mypage: { nickname:"하원", grade:"USER", myPosts:12, receivedLikes:48, activities:9 },
  likes: [
    { postId: 101, title: "매운 제육볶음", likeCount: 99 },
    { postId: 77,  title: "김치볶음밥",   likeCount: 120 }
  ]
};
