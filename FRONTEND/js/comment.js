// 탭 전환
const tabs = document.querySelectorAll('.tab-btn');
const panels = {
  mine: document.getElementById('tab-mine'),
  received: document.getElementById('tab-received'),
  activity: document.getElementById('tab-activity'),
};
tabs.forEach(btn=>{
  btn.addEventListener('click', ()=>{
    tabs.forEach(b=>b.classList.remove('is-active'));
    document.querySelectorAll('.panel').forEach(p=>p.classList.remove('is-active'));
    btn.classList.add('is-active');
    panels[btn.dataset.tab].classList.add('is-active');
  });
});

// 더 보기(스켈레톤 → 더미아이템 추가)
function addSkeleton(listEl, count=2){
  const tpl = document.getElementById('skeletonTpl');
  for(let i=0;i<count;i++) listEl.appendChild(tpl.content.cloneNode(true));
}
function replaceSkeletonWithData(listEl, items){
  // 스켈레톤 제거
  listEl.querySelectorAll('.skeleton').forEach(el=>el.remove());
  // 더미 데이터 추가
  items.forEach(it=>{
    const li = document.createElement('li');
    li.className = 'item';
    li.innerHTML = `
      <a class="thumb" href="${it.href}"></a>
      <div class="content">
        <a class="title" href="${it.href}">${it.title}</a>
        <p class="text">${it.text}</p>
        <div class="meta">${it.meta}</div>
      </div>
      <button class="more" aria-label="더보기">⋯</button>
    `;
    listEl.appendChild(li);
  });
}

document.getElementById('mineMore')?.addEventListener('click', e=>{
  const list = document.getElementById('mineList');
  addSkeleton(list);
  setTimeout(()=>{
    replaceSkeletonWithData(list, [
      { href: 'recipe.html?id=120', title:'들기름 막국수', text:'면 삶을 때 소금 살짝 넣으니 면합이 좋아요.', meta:'방금 전 · 좋아요 0' },
      { href: 'recipe.html?id=121', title:'버섯 크림리조또', text:'육수 조금 더 넣으면 덜 뻑뻑해요.', meta:'5분 전 · 좋아요 2' },
    ]);
  }, 600);
});

document.getElementById('recvMore')?.addEventListener('click', e=>{
  const list = document.getElementById('recvList');
  addSkeleton(list);
  setTimeout(()=>{
    replaceSkeletonWithData(list, [
      { href: 'recipe.html?id=130', title:'두부강정', text:'@ki*** 답글: 에어프라이어 180도 7분 좋았어요!', meta:'15분 전' },
    ]);
  }, 600);
});

document.getElementById('actMore')?.addEventListener('click', e=>{
  const list = document.getElementById('actList');
  addSkeleton(list);
  setTimeout(()=>{
    // 활동은 plain 형태
    list.querySelectorAll('.skeleton').forEach(el=>el.remove());
    const li = document.createElement('li');
    li.className = 'item plain';
    li.innerHTML = `
      <div class="content">
        <p class="text">레시피 <a class="link" href="recipe.html?id=140">바질 토마토 파스타</a> 를 저장했습니다.</p>
        <div class="meta">방금 전</div>
      </div>`;
    list.appendChild(li);
  }, 600);
});

// 비어있는 상태 예시(원하면 특정 리스트 초기화 후 빈상태 표시)
function showEmpty(panelId, message='레시피에 댓글을 남겨보세요.'){
  const panel = document.getElementById(panelId);
  const tpl = document.getElementById('emptyTpl');
  const box = tpl.content.cloneNode(true);
  box.querySelector('.empsub').textContent = message;
  panel.innerHTML = ''; panel.appendChild(box);
}
// 사용 예시: showEmpty('tab-mine');
