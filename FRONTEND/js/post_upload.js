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

// 재료
const ingList = document.getElementById("ingList");
const addIng = document.getElementById("addIng");

function bindIngRow(row) {
    row.querySelector("[data-remove]")?.addEventListener("click", () => {
        if (ingList.querySelectorAll("[data-row]").length > 1) {
            row.remove();
        } else {
            // 3개 필드 모두 초기화
            row.querySelector("[data-name]").value = "";
            row.querySelector("[data-quantity]").value = "";
            row.querySelector("[data-unit]").value = "";
        }
    });
}

// 초기 재료 행 바인딩
bindIngRow(ingList.querySelector("[data-row]"));

addIng.addEventListener("click", () => {
    const base = ingList.querySelector("[data-row]");
    const clone = base.cloneNode(true);
    // 3개 필드 모두 초기화
    clone.querySelector("[data-name]").value = "";
    clone.querySelector("[data-quantity]").value = "";
    clone.querySelector("[data-unit]").value = "";
    bindIngRow(clone);
    ingList.appendChild(clone);
});

// 조리 순서
const stepList = document.getElementById("stepList");
const addStep = document.getElementById("addStep");

function makeStepItem(index) {
    const li = document.createElement("li");
    li.className = "step-item";
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

    // 삭제 버튼
    li.querySelector("[data-delstep]").addEventListener("click", () => {
        if (stepList.children.length > 1) {
            li.remove();
            renumberSteps();
        }
    });

    // 사진 미리보기
    const fileInput = li.querySelector("[data-photo]");
    const preview = li.querySelector("[data-preview]");

    fileInput.addEventListener("change", (e) => {
        const f = e.target.files?.[0];
        if (!f) return;
        const url = URL.createObjectURL(f);
        preview.querySelector("img")?.remove();
        const img = document.createElement("img");
        img.alt = `${index}단계 사진 미리보기`;
        img.src = url;
        preview.appendChild(img);
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

// 단계 추가
addStep.addEventListener("click", () => {
    const idx = stepList.children.length + 1;
    stepList.appendChild(makeStepItem(idx));
});

// 폼 제출 - 재료 데이터 처리 추가
document.getElementById("uploadForm").addEventListener("submit", (e) => {
    e.preventDefault();

    // 재료 데이터 수집
    const ingredients = [];
    const rows = document.querySelectorAll("[data-row]");

    rows.forEach((row) => {
        const name = row.querySelector("[data-name]").value.trim();
        const quantity = row.querySelector("[data-quantity]").value.trim();
        const unit = row.querySelector("[data-unit]").value.trim();

        if (name && quantity && unit) {
            ingredients.push(`${name} ${quantity}${unit}`);
        }
    });

    const ingredientsString = ingredients.join("|");
    console.log("저장할 재료 데이터:", ingredientsString);
    // 예시: "식빵 2장|튀김가루 100g|물 120ml"

    alert("레시피 저장 완료! (개발 중)");
});
