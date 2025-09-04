// 더미 레시피
const DUMMY = [
  {id: 1,  title: '새송이 버터간장볶음',  author: 'cook99',  rating: 4.9},
  {id: 2,  title: '참치마요 주먹밥',      author: 'anna',     rating: 4.6},
  {id: 3,  title: '두부부침 간장양념',     author: 'mint',     rating: 4.7},
  {id: 4,  title: '마늘볶음 파스타',       author: 'lee',      rating: 4.5},
  {id: 5,  title: '계란토스트',            author: 'jay',      rating: 4.3},
  {id: 6,  title: '버섯덮밥',              author: 'mori',     rating: 4.8},
  {id: 7,  title: '간장치킨',              author: 'hana',     rating: 4.2},
  {id: 8,  title: '김치볶음밥',            author: 'ryu',      rating: 4.4},
  {id: 9,  title: '봉골레 파스타',         author: 'cooky',    rating: 4.1},
  {id: 10, title: '돼지고기 제육볶음',     author: 'park',     rating: 4.7},
  {id: 11, title: '나물비빔밥',            author: 'kyle',     rating: 4.0},
  {id: 12, title: '닭가슴살 샐러드',       author: 'sue',      rating: 4.5}
];

const listEl = document.getElementById('cardList');
const countEl = document.getElementById('resultCount');
const loadMoreBtn = document.getElementById('loadMore');
const sortSel = document.getElementById('sortSel');
const form = document.getElementById('recipeSearchForm');
const qInput = document.getElementById('recipeQuery');

let pool = [...DUMMY];       // 현재 결과 풀
let page = 0;
const PAGE_SIZE = 5;

function shuffle(arr){
  const a = [...arr];
  for(let i=a.length-1;i>0;i--){
    const j = Math.floor(Math.random()*(i+1));
    [a[i],a[j]] = [a[j],a[i]];
  }
  return a;
}

function renderCount(){
  countEl.textContent = `총 ${pool.length}개 레시피`;
}

function makeCard(r){
  const a = document.createElement('a');
  a.className = 'card';
  a.href = `recipe_detail.html?id=${r.id}`;
  a.innerHTML = `
    <div class="thumb" aria-hidden="true"></div>
    <div class="meta">
      <div class="title">${r.title}</div>
      <div class="sub">by ${r.author} · ★ ${r.rating}</div>
    </div>
  `;
  return a;
}

function renderPage(reset=false){
  if (reset){ listEl.innerHTML = ''; page = 0; }
  const start = page * PAGE_SIZE;
  const slice = pool.slice(start, start + PAGE_SIZE);
  slice.forEach(r => listEl.appendChild(makeCard(r)));
  page++;
  loadMoreBtn.hidden = (page * PAGE_SIZE >= pool.length);
  renderCount();
}

function applySort(){
  const v = sortSel.value;
  if (v === 'popular'){
    pool.sort((a,b)=> b.rating - a.rating);
  } else if (v === 'latest'){
    pool.sort((a,b)=> b.id - a.id);   // 최신 가정
  } else {
    pool.sort((a,b)=> b.rating - a.rating);
  }
}

function searchByTitle(q){
  const n = q.trim().toLowerCase();
  pool = DUMMY.filter(r => r.title.toLowerCase().includes(n));
  applySort();
  renderPage(true);
}

// 초기: 랜덤 섞어서 노출
(function init(){
  pool = shuffle(DUMMY);
  applySort();
  renderPage(true);
})();

// 정렬 변경
sortSel.addEventListener('change', ()=>{
  applySort();
  renderPage(true);
});

// 검색 제출
form.addEventListener('submit', (e)=>{
  e.preventDefault();
  searchByTitle(qInput.value);
});

// 더 보기
loadMoreBtn.addEventListener('click', ()=> renderPage());
