// 대표 인네일 미리보기
const thumbInput = document.getElementById("thumbInput");
const thumbBox = document.getElementById("thumbBox");
const thumbControls = document.getElementById("thumbControls");
const changeThumbBtn = document.getElementById("changeThumb");
const deleteThumbBtn = document.getElementById("deleteThumb");

function showThumbImage(file) {
    const url = URL.createObjectURL(file);
    // 기존 이미지 제거
    thumbBox.querySelector("img")?.remove();
    // 새 이미지 삽입
    const img = document.createElement("img");
    img.alt = "대표 이미지 미리보기";
    img.src = url;
    thumbBox.appendChild(img);
    thumbBox.classList.add("has-img");
    // 컨트롤 버튼 표시
    thumbControls.style.display = "flex";
}

function hideThumbImage() {
    // 이미지 제거
    thumbBox.querySelector("img")?.remove();
    thumbBox.classList.remove("has-img");
    // 컨트롤 버튼 숨기기
    thumbControls.style.display = "none";
    // 파일 input 초기화
    thumbInput.value = "";
}

thumbInput.addEventListener("change", (e) => {
    const f = e.target.files?.[0];
    if (!f) return;
    showThumbImage(f);
});

// 인네일 교체 버튼
changeThumbBtn.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    thumbInput.click();
});

// 인네일 삭제 버튼
deleteThumbBtn.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (confirm("인네일 이미지를 삭제하시겠습니까?")) {
        hideThumbImage();
    }
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
                <label class="photo-btn" data-photo-btn>
                    <input type="file" accept="image/*" hidden data-photo />
                    📷 단계 사진 추가
                </label>
                <div class="photo-preview" data-preview></div>
                <div class="photo-controls" data-photo-controls style="display: none;">
                    <button type="button" class="photo-control-btn change" data-change-photo>📷 교체</button>
                    <button type="button" class="photo-control-btn delete" data-delete-photo>🗑️ 삭제</button>
                </div>
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

    // 사진 관련 요소들
    const fileInput = li.querySelector("[data-photo]");
    const preview = li.querySelector("[data-preview]");
    const photoBtn = li.querySelector("[data-photo-btn]");
    const photoControls = li.querySelector("[data-photo-controls]");
    const changePhotoBtn = li.querySelector("[data-change-photo]");
    const deletePhotoBtn = li.querySelector("[data-delete-photo]");

    function showStepImage(file) {
        const url = URL.createObjectURL(file);
        preview.querySelector("img")?.remove();
        const img = document.createElement("img");
        img.alt = `${index}단계 사진 미리보기`;
        img.src = url;
        preview.appendChild(img);
        // 컨트롤 버튼 표시, 추가 버튼 숨기기
        photoControls.style.display = "flex";
        photoBtn.style.display = "none";
    }

    function hideStepImage() {
        // 이미지 제거
        preview.querySelector("img")?.remove();
        // 컨트롤 버튼 숨기기, 추가 버튼 표시
        photoControls.style.display = "none";
        photoBtn.style.display = "flex";
        // 파일 input 초기화
        fileInput.value = "";
    }

    // 사진 미리보기
    fileInput.addEventListener("change", (e) => {
        const f = e.target.files?.[0];
        if (!f) return;
        showStepImage(f);
    });

    // 사진 교체 버튼
    changePhotoBtn.addEventListener("click", (e) => {
        e.preventDefault();
        e.stopPropagation();
        fileInput.click();
    });

    // 사진 삭제 버튼
    deletePhotoBtn.addEventListener("click", (e) => {
        e.preventDefault();
        e.stopPropagation();
        if (confirm("단계 사진을 삭제하시겠습니까?")) {
            hideStepImage();
        }
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

// S3 Presigned URL을 이용한 이미지 업로드 함수
async function uploadImageToS3(file, folder) {
    try {
        // 1. 백엔드에서 Presigned URL 요청
        console.log("Presigned URL 요청 중...", {
            fileName: file.name,
            fileType: file.type,
            folder: folder,
        });

        const presignedResponse = await fetch("/api/s3/presigned-url", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                fileName: file.name,
                fileType: file.type,
                folder: folder,
            }),
        });

        console.log("Presigned URL 응답 상태:", presignedResponse.status);
        console.log("Presigned URL 응답 헤더:", presignedResponse.headers);

        if (!presignedResponse.ok) {
            const errorText = await presignedResponse.text();
            console.error("Presigned URL 에러 응답:", errorText);
            throw new Error(`Presigned URL 요청 실패: ${presignedResponse.status} - ${errorText}`);
        }

        const { uploadUrl, fileUrl } = await presignedResponse.json();

        // 2. Presigned URL을 사용하여 S3에 직접 업로드
        const uploadResponse = await fetch(uploadUrl, {
            method: "PUT",
            headers: {
                "Content-Type": file.type,
            },
            body: file,
        });

        if (!uploadResponse.ok) {
            throw new Error(`S3 업로드 실패: ${uploadResponse.status}`);
        }

        console.log("S3 업로드 성공:", fileUrl);
        return fileUrl;
    } catch (error) {
        console.error("이미지 업로드 에러:", error);
        throw error;
    }
}

// 수정된 폼 제출 - Presigned URL 방식으로 이미지 업로드
document.getElementById("uploadForm").addEventListener("submit", async (e) => {
    e.preventDefault();

    try {
        // 로딩 상태 표시
        const submitBtn = document.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.textContent = "레시피 저장 중...";
        }

        // 재료 데이터 수집 - 3개 배열로 분리
        const ingredients = [];           
        const ingredientNames = [];       
        const ingredientQuantities = [];  
        const ingredientUnits = [];       

        const rows = document.querySelectorAll("[data-row]");
        rows.forEach((row) => {
            const name = row.querySelector("[data-name]").value.trim();
            const quantity = row.querySelector("[data-quantity]").value.trim();
            const unit = row.querySelector("[data-unit]").value.trim();

            if (name) {
                ingredients.push(`${name} ${quantity}${unit}`);
                ingredientNames.push(name);
                ingredientQuantities.push(quantity);
                ingredientUnits.push(unit);
            }
        });

        // 인네일 이미지 업로드 (필수)
        const thumbFile = document.getElementById("thumbInput").files?.[0];
        if (!thumbFile) {
            alert("인네일 이미지를 선택해주세요!");
            return;
        }

        console.log("인네일 이미지 업로드 중...");
        const thumbnailUrl = await uploadImageToS3(thumbFile, "recipe-thumbnails");
        console.log("인네일 업로드 완료:", thumbnailUrl);

        // 단계별 이미지 업로드
        const stepImageUrls = [];
        const stepItems = document.querySelectorAll(".step-item");

        for (let i = 0; i < stepItems.length; i++) {
            const stepFile = stepItems[i].querySelector("[data-photo]")?.files?.[0];
            if (stepFile) {
                console.log(`${i + 1}단계 이미지 업로드 중...`);
                const stepImageUrl = await uploadImageToS3(stepFile, "recipe-steps-image");
                stepImageUrls.push(stepImageUrl);
                console.log(`${i + 1}단계 업로드 완료:`, stepImageUrl);
            } else {
                stepImageUrls.push("");
            }
        }

        // 단계별 설명 수집
        const stepDescriptions = [];
        stepItems.forEach((item) => {
            const desc = item.querySelector("[data-desc]")?.value.trim() || "";
            stepDescriptions.push(desc);
        });

        // 레시피 데이터 구성 (PostDto 형식에 맞게)
        const methodSelect = document.querySelector('select[name="ckgMth"]');
        const categorySelect = document.querySelector('select[name="ckgCategory"]');
        const kindSelect = document.querySelector('select[name="ckgKnd"]');
        const inbunSelect = document.querySelector('select[name="CKG_INBUN"]');
        const levelSelect = document.querySelector('select[name="CKG_LEVEL"]');
        const timeSelect = document.querySelector('select[name="CKG_TIME"]');

        const requestBody = {
            title: document.querySelector('[name="title"]')?.value || "제목 없음",
            foodName: document.querySelector('[name="foodName"]')?.value || "음식명 없음",
            ckgMth: methodSelect?.value || "OTHER",
            ckgCategory: categorySelect?.value || "OTHER",
            ckgKnd: kindSelect?.value || "OTHER",
            ckgInbun: parseInt(inbunSelect?.value) || 1,
            ckgLevel: parseInt(levelSelect?.value) || 1,
            ckgTime: parseInt(timeSelect?.value) || 30,
            ckgMtrlCn: ingredients,
            ingredientNames: ingredientNames,
            ingredientQuantities: ingredientQuantities,
            ingredientUnits: ingredientUnits,
            rcpImgUrl: thumbnailUrl,
            rcpSteps: stepDescriptions,
            rcpStepsImg: stepImageUrls
        };

        console.log("수집된 재료 데이터:");
        console.log("- Post용 ingredients:", ingredients);
        console.log("- 재료명 배열:", ingredientNames);
        console.log("- 수량 배열:", ingredientQuantities);
        console.log("- 단위 배열:", ingredientUnits);

        // 백엔드 API로 레시피 저장
        const saveResponse = await fetch("/post/save", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(requestBody)
        });

        if (!saveResponse.ok) {
            const errorText = await saveResponse.text();
            console.error("서버 응답:", errorText);
            throw new Error(`레시피 저장 실패: ${saveResponse.status}`);
        }

        const result = await saveResponse.json();

        let postId;
        if (result && result.data && result.data.postId) {
            postId = result.data.postId;
        } else if (result && result.data && result.data.id) {
            postId = result.data.id;
        } else {
            console.error("응답에서 postId를 찾을 수 없습니다:", result);
            postId = null;
        }

        console.log("추출된 postId:", postId);
        console.log("전체 응답 데이터:", result);

        alert("레시피가 성공적으로 저장되었습니다!");

        // 리다이렉트 처리
        if (postId) {
            window.location.href = `/pages/post_detail.html?postId=${postId}`;
        } else {
            console.warn("postId를 찾을 수 없어 메인 페이지로 이동합니다.");
            window.location.href = '/pages/main.html';
        }

    } catch (error) {
        console.error("레시피 저장 에러:", error);
        alert("레시피 저장에 실패했습니다: " + error.message);
    } finally {
        // 로딩 상태 해제
        const submitBtn = document.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = "레시피 저장";
        }
    }
});