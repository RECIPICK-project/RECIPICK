(function () {
  'use strict';

  /* ---------------------------
   * 공통 유틸 (원본에 영향 없음)
   * --------------------------- */
  const USE_CREDENTIALS = true;
  function authHeader() {
    const t = localStorage.getItem('ACCESS_TOKEN');
    return t ? { Authorization: `Bearer ${t}` } : {};
  }
  function fx(path, opts = {}) {
    const headers = {
      Accept: 'application/json',
      ...authHeader(),
      ...(opts.headers || {}),
    };
    const cred = USE_CREDENTIALS ? { credentials: 'include' } : {};
    return fetch(path, { ...opts, headers, ...cred });
  }
  function $(sel, root = document) { return root.querySelector(sel); }
  function $all(sel, root = document) { return Array.from(root.querySelectorAll(sel)); }
  function toast(msg){ try{ console.log('[toast]', msg); }catch(_){} }

  // S3 Presigned 업로드
  async function uploadImageToS3(file, folder) {
    const presignedResponse = await fetch("/api/s3/presigned-url", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ fileName: file.name, fileType: file.type, folder })
    });
    if (!presignedResponse.ok) {
      const t = await presignedResponse.text();
      throw new Error(`Presigned URL 요청 실패: ${presignedResponse.status} - ${t}`);
    }
    const { uploadUrl, fileUrl } = await presignedResponse.json();
    const uploadResponse = await fetch(uploadUrl, {
      method: "PUT",
      headers: { "Content-Type": file.type },
      body: file
    });
    if (!uploadResponse.ok) throw new Error(`S3 업로드 실패: ${uploadResponse.status}`);
    return fileUrl;
  }

  /* ---------------------------
   * 타입 보정 헬퍼
   * --------------------------- */
  // string 또는 string[] -> '|' 연결 문자열
  function toPipeString(val) {
    if (val == null) return '';
    if (Array.isArray(val)) {
      return val.map(s => String(s ?? '').trim()).filter(Boolean).join('|');
    }
    return String(val);
  }
  // string 또는 string[] -> 배열
  function toArray(val) {
    if (val == null) return [];
    if (Array.isArray(val)) return val.map(s => String(s ?? '').trim()).filter(Boolean);
    return String(val).split('|').map(s => s.trim()).filter(Boolean);
  }

  /* ---------------------------
   * 재료/단계 UI 조립
   * --------------------------- */
  // "재료명 2큰술" -> {name:"재료명", quantity:"2", unit:"큰술"}
  function parseIngredientToken(token) {
    const s = String(token || '').trim();
    if (!s) return { name: '', quantity: '', unit: '' };

    const lastSpace = s.lastIndexOf(' ');
    if (lastSpace > 0) {
      const name = s.slice(0, lastSpace).trim();
      const tail = s.slice(lastSpace + 1).trim(); // "2큰술" "3g" "1.5컵" 등
      const m = tail.match(/^(\d+(?:\.\d+)?)(.*)$/);
      if (m) {
        return { name, quantity: m[1].trim(), unit: (m[2] || '').trim() };
      }
      return { name, quantity: '', unit: tail };
    }
    return { name: s, quantity: '', unit: '' };
  }

  function createIngRow({ name = '', quantity = '', unit = '' }) {
    const row = document.createElement('div');
    row.className = 'ing-row';
    row.setAttribute('data-row', '');
    row.innerHTML = `
      <input class="input" type="text" placeholder="재료명 (예: 식빵)" data-name />
      <input class="input" type="text" placeholder="수량 (예: 2)" data-quantity />
      <input class="input" type="text" placeholder="단위 (예: 장, g, 큰술)" data-unit />
      <button class="mini warn" type="button" data-remove>×</button>
    `;
    row.querySelector('[data-name]').value = name;
    row.querySelector('[data-quantity]').value = quantity;
    row.querySelector('[data-unit]').value = unit;

    row.querySelector('[data-remove]').addEventListener('click', () => {
      const ingList = document.getElementById('ingList');
      if (ingList.querySelectorAll('[data-row]').length > 1) {
        row.remove();
      } else {
        row.querySelector('[data-name]').value = '';
        row.querySelector('[data-quantity]').value = '';
        row.querySelector('[data-unit]').value = '';
      }
    });
    return row;
  }

  function createStepItem(index, { text = '', imgUrl = '' } = {}) {
    const li = document.createElement('li');
    li.className = 'step-item';
    li.innerHTML = `
      <div class="step-head">
        <span class="no">${index}단계</span>
        <button type="button" class="mini warn" data-delstep>×</button>
      </div>
      <div class="step-body">
        <textarea class="textarea" rows="3" placeholder="${index}단계 설명을 적어주세요" data-desc></textarea>
        <div class="step-photo">
          <label class="photo-btn">
            <input type="file" accept="image/*" hidden data-photo />
            📷 단계 사진 추가
          </label>
          <div class="photo-preview" data-preview></div>
        </div>
      </div>
    `;
    li.querySelector('[data-desc]').value = text || '';

    li.querySelector('[data-delstep]').addEventListener('click', () => {
      const stepList = document.getElementById('stepList');
      if (stepList.children.length > 1) {
        li.remove();
        [...stepList.children].forEach((x, i) => {
          x.querySelector('.no').textContent = `${i + 1}단계`;
          x.querySelector('[data-desc]').setAttribute('placeholder', `${i + 1}단계 설명을 적어주세요`);
        });
      }
    });

    const fileInput = li.querySelector('[data-photo]');
    const preview = li.querySelector('[data-preview]');
    fileInput.addEventListener('change', (e) => {
      const f = e.target.files?.[0];
      if (!f) return;
      const url = URL.createObjectURL(f);
      preview.querySelector('img')?.remove();
      const img = document.createElement('img');
      img.alt = `${index}단계 사진 미리보기`;
      img.src = url;
      preview.appendChild(img);
    });

    if (imgUrl) {
      const img = document.createElement('img');
      img.alt = `${index}단계 사진 미리보기`;
      img.src = imgUrl;
      li.querySelector('[data-preview]').appendChild(img);
    }

    return li;
  }

  /* ---------------------------
   * 수정 모드 감지
   * --------------------------- */
  const params  = new URLSearchParams(location.search);
  const EDIT_ID = params.get('edit');
  const IS_EDIT = !!(EDIT_ID && EDIT_ID.trim());
  const formEl  = document.getElementById('uploadForm');

  if (!formEl || !IS_EDIT) {
    // 폼이 없거나 수정 모드가 아니면 그대로 원본 흐름 유지
    return;
  }

  // 원본 submit 핸들러보다 먼저 받기 위해 캡처 단계 등록
  formEl.addEventListener('submit', onSubmitEditCapture, { capture: true });

  // 기존 값 저장소
  const loaded = {
    rcpImgUrl: '',
    rcpStepsImgArr: [], // ["url1","url2",...]
    ckgMtrlCn: '',      // "재료1 1개|재료2 2큰술"
    rcpSteps: ''        // "1단계|2단계"
  };

  // 초기 로딩
  document.addEventListener('DOMContentLoaded', () => {
    loadExisting(EDIT_ID).catch(err => {
      console.error('[edit-addon] loadExisting error', err);
      toast('기존 레시피를 불러오지 못했습니다.');
    });
  }, { once: true });

  /* ---------------------------
   * 기존 데이터 로드 → 폼 채우기
   * --------------------------- */
  async function loadExisting(id) {
    const r = await fx(`/me/posts/${encodeURIComponent(id)}`);
    if (r.status === 401) { location.href = '/pages/login.html'; return; }
    if (!r.ok) throw new Error(`GET /me/posts/${id} HTTP ${r.status}`);
    const d = await r.json();

    // 제목/음식명
    setVal('title', d.title);
    setVal('foodName', d.foodName);

    // 드롭다운(한글/영문 모두 허용됨; 백에서 파싱)
    selectSet('ckgMth', d.ckgMth);
    selectSet('ckgCategory', d.ckgCategory);
    selectSet('ckgKnd', d.ckgKnd);

    // 인분/난이도/시간 (HTML name이 대문자)
    numericSelectSet('CKG_INBUN', d.ckgInbun);
    numericSelectSet('CKG_LEVEL', d.ckgLevel);
    numericSelectSet('CKG_TIME', d.ckgTime);

    // 대표 이미지 프리뷰
    loaded.rcpImgUrl = d.rcpImgUrl || '';
    if (loaded.rcpImgUrl) {
      const box = document.getElementById('thumbBox');
      if (box) {
        box.querySelector('img')?.remove();
        const img = document.createElement('img');
        img.alt = '대표 이미지 미리보기';
        img.src = loaded.rcpImgUrl;
        box.appendChild(img);
        box.classList.add('has-img');
      }
    }

    // 재료/단계 문자열/배열 보정 저장
    loaded.ckgMtrlCn      = toPipeString(d.ckgMtrlCn);
    loaded.rcpSteps       = toPipeString(d.rcpSteps);
    loaded.rcpStepsImgArr = toArray(d.rcpStepsImg);

    // 헤더/버튼 문구
    const h = document.querySelector('header h1');
    if (h) h.textContent = '레시피 수정';
    const sb = document.querySelector('.savebar .save');
    if (sb) sb.textContent = '수정 저장';

    // ✅ 로딩 끝난 뒤 UI 렌더
    renderFromLoaded();
  }

  // 로딩된 값으로 재료/단계 렌더
  function renderFromLoaded() {
    // 재료
    {
      const ingList = document.getElementById('ingList');
      if (ingList) {
        const tokens = toArray(loaded.ckgMtrlCn); // ["식빵 2장", "버터 1큰술", ...]
        if (tokens.length) {
          ingList.innerHTML = '';
          tokens.forEach(tok => {
            const parsed = parseIngredientToken(tok);
            ingList.appendChild(createIngRow(parsed));
          });
        }
      }
    }
    // 단계
    {
      const stepList = document.getElementById('stepList');
      if (stepList) {
        const texts = toArray(loaded.rcpSteps);
        const imgs  = loaded.rcpStepsImgArr;
        if (texts.length || imgs.length) {
          stepList.innerHTML = '';
          const len = Math.max(texts.length, imgs.length, 1);
          for (let i = 0; i < len; i++) {
            const item = createStepItem(i + 1, { text: texts[i] || '', imgUrl: imgs[i] || '' });
            stepList.appendChild(item);
          }
        }
      }
    }
  }

  function setVal(name, v) {
    const el = formEl.querySelector(`[name="${name}"]`);
    if (el && v != null) el.value = v;
  }
  function selectSet(name, value) {
    const el = formEl.querySelector(`select[name="${name}"]`);
    if (!el || value == null) return;
    for (const opt of el.options) {
      if (opt.value === String(value) || opt.text === String(value)) {
        opt.selected = true;
        return;
      }
    }
  }
  function numericSelectSet(name, value) {
    const el = formEl.querySelector(`select[name="${name}"]`);
    if (!el || value == null) return;
    for (const opt of el.options) {
      if (Number(opt.value) === Number(value)) {
        opt.selected = true;
        return;
      }
    }
  }

  /* ---------------------------
   * 제출 가로채기 (수정 모드)
   * --------------------------- */
  async function onSubmitEditCapture(e) {
    // 기본 제출 및 이후 리스너 모두 차단 → 원본 /post/save 흐름 무효화
    e.preventDefault();
    e.stopImmediatePropagation();

    try {
      const submitBtn = document.querySelector('button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = '레시피 수정 중...';
      }

      // 1) 재료 문자열 구성 (사용자가 입력했으면 그 값, 아니면 기존값 유지)
      const ingRows = $all('[data-row]');
      const ingredientNames = [];
      const ingredientQuantities = [];
      const ingredientUnits = [];
      const ckgMtrlCnParts = [];

      for (const row of ingRows) {
        const name = row.querySelector('[data-name]')?.value?.trim() || '';
        const qty  = row.querySelector('[data-quantity]')?.value?.trim() || '';
        const unit = row.querySelector('[data-unit]')?.value?.trim() || '';
  
        if (name && qty && unit) {
          ingredientNames.push(name);
          ingredientQuantities.push(qty);
          ingredientUnits.push(unit);
          ckgMtrlCnParts.push(`${name} ${qty}${unit}`);
        }
      }

      // 2) 단계 설명/이미지 수집 + 필요 시 S3 업로드
      const stepItems = $all('.step-item');
      const stepTexts = [];
      const stepImgs  = [];

      for (let i = 0; i < stepItems.length; i++) {
        const item = stepItems[i];
        const txt = item.querySelector('[data-desc]')?.value?.trim() || '';
        stepTexts.push(txt);

        const file = item.querySelector('[data-photo]')?.files?.[0];
        if (file) {
          const url = await uploadImageToS3(file, 'recipe-steps-image');
          stepImgs.push(url);
        } else {
          // 새 파일 없으면 기존 i번째 URL 유지(없으면 빈칸)
          stepImgs.push(loaded.rcpStepsImgArr[i] || '');
        }
      }

      // 3) 대표 썸네일: 새 파일 있으면 업로드, 없으면 기존 URL 유지
      let thumbUrl = loaded.rcpImgUrl || '';
      const newThumb = document.getElementById('thumbInput')?.files?.[0];
      if (newThumb) {
        thumbUrl = await uploadImageToS3(newThumb, 'recipe-thumbnails');
      }

      // 4) PATCH 바디 구성 (PostUpdateRequest와 1:1)
      const val = (n) => formEl.querySelector(`[name="${n}"]`)?.value?.trim();
      const num = (n) => {
        const v = val(n);
        return v ? Number(v) : null;
      };

      const body = {
        title:       val('title') || undefined,
        foodName:    val('foodName') || undefined,
        ckgMth:      val('ckgMth') || undefined,
        ckgCategory: val('ckgCategory') || undefined,
        ckgKnd:      val('ckgKnd') || undefined,
        ckgMtrlCn:   ckgMtrlCnParts.length ? ckgMtrlCnParts : (loaded.ckgMtrlCn ? loaded.ckgMtrlCn.split('|') : undefined),
        ingredientNames:      ingredientNames.length ? ingredientNames : undefined,
        ingredientQuantities: ingredientQuantities.length ? ingredientQuantities : undefined,
        ingredientUnits:      ingredientUnits.length ? ingredientUnits : undefined,
        ckgInbun:    num('CKG_INBUN'),
        ckgLevel:    num('CKG_LEVEL'),
        ckgTime:     num('CKG_TIME'),
        rcpImgUrl:   thumbUrl || undefined,
        rcpSteps:    (stepTexts.length ? stepTexts : (loaded.rcpSteps ? loaded.rcpSteps.split('|') : undefined)),
        rcpStepsImg: (stepImgs.length ? stepImgs : (loaded.rcpStepsImgArr.length ? loaded.rcpStepsImgArr : undefined))
      };

      // 빈 값/NaN 제거 → 부분 수정만 전송
      Object.keys(body).forEach(k => {
        const v = body[k];
        if (v === '' || v === undefined || v === null || Number.isNaN(v)) delete body[k];
      });

      const res = await fx(`/me/posts/${encodeURIComponent(EDIT_ID)}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      });

      if (res.status === 401) { location.href = '/pages/login.html'; return; }
      if (res.status === 403) { toast('권한이 없습니다.'); return; }
      if (res.status === 409) { toast('정식 레시피는 수정할 수 없습니다.'); return; }
      if (!res.ok) throw new Error(`PATCH HTTP ${res.status}`);

      toast('수정되었습니다.');
      location.href = '/pages/profile.html#my';
    } catch (err) {
      console.error('[edit-addon] submit error', err);
      alert('수정 실패: ' + (err?.message || '알 수 없는 오류'));
    } finally {
      const submitBtn = document.querySelector('button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = '레시피 저장하기';
      }
    }
  }
})();
