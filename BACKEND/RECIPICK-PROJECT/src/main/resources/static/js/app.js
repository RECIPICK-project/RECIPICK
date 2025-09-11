// js/app.js
(function () {
  const $ = (sel, root=document) => root.querySelector(sel);

  // 페이지별 초기화
  const path = location.pathname.split('/').pop(); // ex) admin.html
  if (path === 'admin.html') initAdmin();
  if (path === 'admin_reports.html') initReports();
  if (path === 'admin_users.html') initUsers();
  if (path === 'mypage.html') initMyPage();
  if (path === 'mypage_likes.html') initMyLikes();

  function initAdmin() {
    $('.title').textContent = '관리자 대시보드';
    const m = window.MOCK.metrics;
    const main = $('main');
    main.innerHTML = `
      <div class="grid cols-3">
        <div class="card"><div>전체 회원 수</div><h2>${m.totalUsers}</h2></div>
        <div class="card"><div>오늘 신규 가입</div><h2>${m.newToday}</h2></div>
        <div class="card"><div>활성/정지</div><h2>${m.active} / ${m.suspended}</h2></div>
      </div>
      <div class="card" style="margin-top:12px">
        <h3>신고 상위 레시피</h3>
        <table class="table">
          <thead><tr><th>ID</th><th>제목</th><th>신고수</th></tr></thead>
          <tbody>
            ${window.MOCK.topReported.map(r => `<tr><td>${r.id}</td><td>${r.title}</td><td>${r.count}</td></tr>`).join('')}
          </tbody>
        </table>
      </div>`;
  }

  function initReports() {
    $('.title').textContent = '신고 관리';
    const rows = window.MOCK.reports.map(r => `
      <tr>
        <td>${r.id}</td><td>${r.type}</td><td>${r.targetId}</td>
        <td>${r.reason}</td><td>${r.count}</td>
        <td class="actions">
          <button class="primary" data-id="${r.id}">조치</button>
          <button data-id="${r.id}">무시</button>
        </td>
      </tr>`).join('');
    $('main').innerHTML = `
      <div class="card">
        <table class="table">
          <thead><tr><th>ID</th><th>타입</th><th>대상ID</th><th>사유</th><th>신고수</th><th>액션</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>`;
  }

  function initUsers() {
    $('.title').textContent = '유저 관리';
    const rows = window.MOCK.users.map(u => `
      <tr>
        <td>${u.id}</td><td>${u.nickname}</td><td>${u.grade}</td><td>${u.status}</td><td>${u.latestAt}</td>
        <td class="actions">
          <button class="primary" data-id="${u.id}">정지</button>
          <button data-id="${u.id}">해제</button>
        </td>
      </tr>`).join('');
    $('main').innerHTML = `
      <div class="card">
        <table class="table">
          <thead><tr><th>유저ID</th><th>닉네임</th><th>등급</th><th>상태</th><th>최근 접속</th><th>액션</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>`;
  }

  function initMyPage() {
    $('.title').textContent = '마이페이지';
    const p = window.MOCK.mypage;
    $('main').innerHTML = `
      <div class="grid cols-2">
        <div class="card">
          <h3>프로필</h3>
          <p>닉네임: <b>${p.nickname}</b></p>
          <p>등급: <span class="badge">${p.grade}</span></p>
          <p>내 레시피: <b>${p.myPosts}</b></p>
          <p>받은 좋아요: <b>${p.receivedLikes}</b></p>
          <p>활동 수(댓글/별점): <b>${p.activities}</b></p>
          <a class="btn" href="#">프로필 편집</a>
        </div>
        <div class="card">
          <h3>바로가기</h3>
          <p><a href="mypage_likes.html">좋아요한 레시피</a></p>
        </div>
      </div>`;
  }

  function initMyLikes() {
    $('.title').textContent = '마이페이지 - 좋아요';
    const rows = window.MOCK.likes.map(it => `
      <tr><td>${it.postId}</td><td>${it.title}</td><td>${it.likeCount}</td></tr>
    `).join('');
    $('main').innerHTML = `
      <div class="card">
        <table class="table">
          <thead><tr><th>레시피ID</th><th>제목</th><th>좋아요</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      </div>`;
  }
})();
