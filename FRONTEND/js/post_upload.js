// 대표 썸네일 미리보기
const thumbInput = document.getElementById("thumbInput");
const thumbBox = document.getElementById("thumbBox");

thumbInput.addEventListener("change", (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    const url = URL.createObjectURL(f);
    // 기존 이미지 제거
    thumbBox.querySelector("img")?.remove();
    // 새 이미지 삽입
    const img = document.createElement("img");
    img.alt = "대표 이미지 미리보기";
    img.src = url;
    thumbBox.appendChild(img);
    thumbBox.classList.add("has-img");
});

// ---------- 재료 ----------
const ingList = document.getElementById("ingList");
const addIng = document.getElementById("addIng");

// 맨 처음 1줄은 템플릿 그대로 쓰지만, 제거가 눌리면 최소 1줄은 남겨줌
function bindIngRow(row) {
    row.querySelector("[data-remove]")?.addEventListener("click", () => {
        if (ingList.querySelectorAll("[data-row]").length > 1) row.remove();
        else {
            row.querySelector("[data-name]").value = "";
            row.querySelector("[data-qty]").value = "";
        }
    });
}
bindIngRow(ingList.querySelector("[data-row]"));

addIng.addEventListener("click", () => {
    const base = ingList.querySelector("[data-row]");
    const clone = base.cloneNode(true);
    clone.querySelector("[data-name]").value = "";
    clone.querySelector("[data-qty]").value = "";
    bindIngRow(clone);
    ingList.appendChild(clone);
});

// ---------- 조리 순서 ----------
const stepList = document.getElementById("stepList");
const addStep = document.getElementById("addStep");

function makeStepItem(index) {
    const li = document.createElement("li");
    li.className = "step-item";
    li.innerHTML = `
    <div class="step-head">
      <span class="no">${index}단계</span>
      <button type="button" class="mini warn" data-delstep>-</button>
    </div>
    <div class="step-body">
      <textarea class="input" rows="3" placeholder="${index}단계 설명을 적어주세요" data-desc></textarea>
      <div class="step-photo">
        <div class="photo-preview" data-preview></div>
        <label class="photo-btn" aria-label="단계 사진 추가">
          <input type="file" accept="image/*" hidden data-photo />
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#111827" stroke-width="2">
            <path d="M12 5v14M5 12h14"/>
          </svg>
        </label>
      </div>
    </div>
  `;

    // 삭제
    li.querySelector("[data-delstep]").addEventListener("click", () => {
        li.remove();
        renumberSteps();
    });

    // 사진 미리보기
    const file = li.querySelector("[data-photo]");
    const box = li.querySelector("[data-preview]");
    file.addEventListener("change", (e) => {
        const f = e.target.files?.[0];
        if (!f) return;
        const url = URL.createObjectURL(f);
        box.querySelector("img")?.remove();
        const img = document.createElement("img");
        img.alt = `${index}단계 사진 미리보기`;
        img.src = url;
        box.appendChild(img);
    });

    return li;
}

function renumberSteps() {
    [...stepList.children].forEach((li, i) => {
        li.querySelector(".no").textContent = `${i + 1}단계`;
        li.querySelector("[data-desc]").setAttribute(
            "placeholder",
            `${i + 1}단계 설명을 적어주세요`
        );
    });
}

// 초기 2단계 생성
stepList.appendChild(makeStepItem(1));
stepList.appendChild(makeStepItem(2));

addStep.addEventListener("click", () => {
    const idx = stepList.children.length + 1;
    stepList.appendChild(makeStepItem(idx));
});

// ---------- 저장(더미) ----------
document.getElementById("uploadForm").addEventListener("submit", (e) => {
    e.preventDefault();
    // TODO: 백엔드 연결 시 FormData 구성해서 전송
    // const fd = new FormData(e.currentTarget);
    alert("임시 저장 완료(더미). 백엔드 연결 시 FormData로 전송하면 됩니다.");
});
