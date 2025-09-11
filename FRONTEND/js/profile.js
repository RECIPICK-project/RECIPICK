// 안전하게: 파싱 에러 방지 + DOM 로드 후 실행
(function(){
  /* ===== 탭 전환 ===== */
  document.querySelectorAll(".tab-btn").forEach(btn=>{
    btn.addEventListener("click", ()=>{
      document.querySelectorAll(".tab-btn").forEach(b=>b.classList.remove("active"));
      btn.classList.add("active");
      document.querySelectorAll(".tab-panel").forEach(p=>p.classList.remove("active"));
      document.getElementById("tab-"+btn.dataset.target).classList.add("active");
    });
  });
  (function(){
    const VALID = ["my","saved","activity"];

    function selectTab(target){
      // 버튼 상태 동기화
      document.querySelectorAll(".tab-btn").forEach(b=>{
        const on = b.dataset.target === target;
        b.classList.toggle("active", on);
        b.setAttribute("aria-selected", on ? "true" : "false");
      });
      // 패널 상태 동기화
      document.querySelectorAll(".tab-panel").forEach(p=>{
        const on = p.id === `tab-${target}`;
        p.classList.toggle("active", on);
        p.hidden = !on;
      });
      // (선택) 같은 탭 재클릭 시 UX 피드백 주고 싶으면 여기에 스크롤/하이라이트 로직 가능
      // ex) document.getElementById(`tab-${target}`).scrollTo({ top: 0, behavior: 'smooth' });
    }

    // 해시 반영 + 렌더 (해시가 같아도 무조건 selectTab 실행)
    function navigateTo(target, {push=false}={}){
      if(!VALID.includes(target)) target = "my";
      const current = (location.hash || "").replace("#","");
      // 히스토리 조작
      if (current !== target){
        const url = `#${target}`;
        push ? history.pushState(null, "", url) : history.replaceState(null, "", url);
      }
      // 해시가 같아도 강제로 탭 갱신
      selectTab(target);
    }

    // 탭 클릭: 기본 앵커 동작 막고 SPA 방식으로 이동
    document.querySelectorAll(".tab-btn").forEach(a=>{
      a.addEventListener("click", (e)=>{
        e.preventDefault();               // 해시 점프/스크롤 방지
        const target = a.dataset.target;
        // 이미 같은 해시여도 navigateTo가 다시 렌더한다
        navigateTo(target, { push:false });
      });
    });

    // 뒤로/앞으로 가기 대응
    window.addEventListener("hashchange", ()=>{
      const t = (location.hash || "").replace("#","");
      navigateTo(VALID.includes(t) ? t : "my", { push:false });
    });

    // 최초 진입: 해시 기준 선택(#saved로 들어오면 저장 탭부터)
    (function init(){
      const t = (location.hash || "").replace("#","");
      navigateTo(VALID.includes(t) ? t : "my", { push:false });
    })();

    // ===== 바텀 탭바 하트가 이 페이지에서 다시 눌릴 때도 반응하도록 보강 (선택) =====
    // 프로필 화면에서 하트를 다시 눌러도 저장 탭을 재렌더/스크롤시키고 싶으면:
    const heart = document.querySelector('.tabbar .heart');
    if (heart) {
      heart.addEventListener('click', (e)=>{
        // 같은 페이지에서 눌렀다면 페이지 리로딩 말고 탭만 갱신
        if (location.pathname.endsWith('profile.html')) {
          e.preventDefault();
          navigateTo("saved", { push:false });
        }
      });
    }
  })();


  /* ===== 메뉴 시트 ===== */
  const sheet = document.getElementById("menuSheet");
  const openMenu = ()=> sheet.setAttribute("aria-hidden","false");
  const closeMenu = ()=> sheet.setAttribute("aria-hidden","true");
  document.getElementById("openMenu").addEventListener("click", openMenu);
  sheet.addEventListener("click", (e)=>{ if(e.target===sheet) closeMenu(); });
  document.getElementById("logoutBtn").addEventListener("click", ()=>{ /* TODO: POST /logout */ alert("로그아웃 되었습니다."); });

  /* ===== 프로필 편집 모달 ===== */
  const modal = (function(){
    const wrap = document.createElement("div");
    wrap.className = "modal";
    wrap.setAttribute("aria-hidden","true");
    wrap.innerHTML = `
      <div class="overlay" data-close></div>
      <div class="dialog" role="dialog" aria-modal="true" aria-labelledby="editTitle">
        <div class="dialog-head">
          <h3 id="editTitle">프로필 편집</h3>
          <button class="btn-ghost" data-close aria-label="닫기">닫기</button>
        </div>
        <div class="dialog-body">
          <label class="row"><span>닉네임</span><input class="input" id="editName" maxlength="20" placeholder="닉네임" /></label>
          <label class="row"><span>프로필 사진</span><input class="input" id="editAvatar" type="file" accept="image/*" /></label>
        </div>
        <div class="dialog-foot">
          <button class="btn-ghost" data-close>취소</button>
          <button class="btn success" id="saveProfile">저장</button>
        </div>
      </div>`;
    document.body.appendChild(wrap);
    wrap.addEventListener("click", e=>{ if(e.target.dataset.close!==undefined) wrap.setAttribute("aria-hidden","true"); });
    return {
      open(){ document.getElementById("editName").value = document.getElementById("userName").textContent.trim(); wrap.setAttribute("aria-hidden","false"); },
      root: wrap
    };
  })();
  document.getElementById("openEdit").addEventListener("click", ()=> modal.open());

  // 아바타 프리뷰
  document.getElementById("avatarInput")?.addEventListener("change", e=>{
    const f = e.target.files?.[0]; if(!f) return;
    document.getElementById("avatarImg").src = URL.createObjectURL(f);
  });

  // 저장 액션(연동 지점)
  document.addEventListener("click", (e)=>{
    if(e.target && e.target.id==="saveProfile"){
      const nickname = document.getElementById("editName").value.trim();
      if(!nickname) return alert("닉네임을 입력해 주세요.");
      // TODO: 아바타 업로드 후 PATCH /api/profile
      document.getElementById("userName").textContent = nickname;
      document.querySelector(".modal").setAttribute("aria-hidden","true");
    }
  });

  /* ===== 등급 표시 유틸 ===== */
  window.setTier = function(grade){ // 'BRONZE' | 'SILVER' | 'GOLD' | 'PLATINUM' | 'DIAMOND'
    const el = document.getElementById("userTier");
    const key = String(grade||"").toUpperCase();
    const shown = { BRONZE:"Bronze", SILVER:"Silver", GOLD:"Gold", PLATINUM:"Platinum", DIAMOND:"Diamond" }[key];
    if(!shown) return; // INVALID_GRADE 방지
    el.dataset.tier = key;
    el.textContent = shown;
  };

  /* ✅ 여기를 새로 추가: 프로필 초기화 함수 */
  window.initProfile = function({ nickname, avatarUrl, grade } = {}){
    if (nickname)  document.getElementById("userName").textContent = nickname;
    if (avatarUrl) document.getElementById("avatarImg").src = avatarUrl;
    if (grade)     window.setTier(grade);
  };

  /* ===== 목록 렌더(연동 지점) ===== */
  window.renderMine = function(items){
    const ul = document.getElementById("listMine"); ul.innerHTML = "";
    document.querySelector("[data-empty-mine]").hidden = items.length>0;
    items.forEach(it=>{
      const li = document.createElement("li");
      li.className = "card";
      li.innerHTML = `
        <div class="thumb" style="background-image:url('${it.thumb||""}')"></div>
        <div class="meta">
          <div class="title">${it.title||"레시피 제목"}</div>
          <div class="sub">${it.date||""}</div>
        </div>
        <div class="rating">${it.rating ? "⭐ "+it.rating : ""}</div>
        <div class="actions">
          <button class="icon" data-edit="${it.id}">✏️</button>
          <button class="icon" data-del="${it.id}">🗑️</button>
        </div>`;
      ul.appendChild(li);
    });
    ul.onclick = (e)=>{
      const btn = e.target.closest("button[data-edit],button[data-del]");
      if(!btn) return;
      const id = btn.dataset.edit || btn.dataset.del;
      if(btn.dataset.edit) location.href = `post_upload.html?edit=${id}`;
      else if(confirm("삭제하시겠어요?")) {/* TODO: DELETE /post/{id} */}
    };
  };

  window.renderLinkList = function(listId, items){
    const ul = document.getElementById(listId); ul.innerHTML = "";
    const emptySel = listId==="listSaved" ? "[data-empty-saved]" : "[data-empty-activity]";
    document.querySelector(emptySel).hidden = items.length>0;
    items.forEach(it=>{
      const li = document.createElement("li");
      li.className = "card";
      li.innerHTML = `
        <a class="link" href="recipe_detail.html?id=${it.recipeId}">
          <div class="thumb" style="background-image:url('${it.thumb||""}')"></div>
          <div class="meta">
            <div class="title">${it.title||""}</div>
            <div class="sub">${it.meta||""}</div>
          </div>
          <div class="rating">${it.rating ? "⭐ "+it.rating : ""}</div>
        </a>`;
      ul.appendChild(li);
    });
  };

// ===== 프로필 더미 =====
initProfile({
  nickname: "Chef",
  avatarUrl: "https://picsum.photos/seed/user1/160/160",
  grade: "GOLD" // BRONZE | SILVER | GOLD | PLATINUM | DIAMOND
});

// ===== 나의 레시피 (임시만) 더미 =====
const draftSamples = [
  {
    id: 101, status: "DRAFT",
    title: "아보카도 토스트 테스트",
    date: "2025-09-10 14:05",
    rating: null,
    thumb: "https://picsum.photos/seed/draft1/120/120"
  },
  {
    id: 102, status: "DRAFT",
    title: "간단 계란말이 초안",
    date: "2025-09-09 22:41",
    rating: 4.3,
    thumb: "https://picsum.photos/seed/draft2/120/120"
  },
  {
    id: 103, status: "DRAFT",
    title: "치킨 샐러드(작성중)",
    date: "2025-09-08 09:12",
    rating: 4.9,
    thumb: "https://picsum.photos/seed/draft3/120/120"
  }
];
// 화면 렌더
renderMine(draftSamples);

// ===== 저장한 레시피 더미 =====
const savedSamples = [
  {
    recipeId: 501,
    title: "버터갈릭 새우",
    meta: "별점 4.8 · 32분",
    rating: 4.8,
    thumb: "https://picsum.photos/seed/saved1/120/120"
  },
  {
    recipeId: 502,
    title: "토마토 바질 파스타",
    meta: "별점 4.6 · 25분",
    rating: 4.6,
    thumb: "https://picsum.photos/seed/saved2/120/120"
  }
];
renderLinkList("listSaved", savedSamples);

// ===== 댓글/활동 더미 =====
const activitySamples = [
  {
    recipeId: 701,
    title: "수비드 닭가슴살",
    meta: "내 댓글: '염지 시간 궁금해요!' (1일 전)",
    rating: null,
    thumb: "https://picsum.photos/seed/act1/120/120"
  },
  {
    recipeId: 702,
    title: "프렌치 토스트",
    meta: "내 별점: ★★★★★ (2일 전)",
    rating: 5.0,
    thumb: "https://picsum.photos/seed/act2/120/120"
  }
];
renderLinkList("listActivity", activitySamples);
})();
