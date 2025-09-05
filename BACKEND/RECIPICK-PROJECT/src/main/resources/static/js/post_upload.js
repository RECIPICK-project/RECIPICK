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

// S3 Presigned URL을 이용한 이미지 업로드 함수
async function uploadImageToS3(file, folder) {
    try {
        // 1. 백엔드에서 Presigned URL 요청
        console.log('Presigned URL 요청 중...', {
            fileName: file.name,
            fileType: file.type,
            folder: folder
        });
        
        const presignedResponse = await fetch('/api/s3/presigned-url', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                fileName: file.name,
                fileType: file.type,
                folder: folder
            })
        });

        console.log('Presigned URL 응답 상태:', presignedResponse.status);
        console.log('Presigned URL 응답 헤더:', presignedResponse.headers);

        if (!presignedResponse.ok) {
            const errorText = await presignedResponse.text();
            console.error('Presigned URL 에러 응답:', errorText);
            throw new Error(`Presigned URL 요청 실패: ${presignedResponse.status} - ${errorText}`);
        }

        const { uploadUrl, fileUrl } = await presignedResponse.json();

        // 2. Presigned URL을 사용하여 S3에 직접 업로드
        const uploadResponse = await fetch(uploadUrl, {
            method: 'PUT',
            headers: {
                'Content-Type': file.type
            },
            body: file
        });

        if (!uploadResponse.ok) {
            throw new Error(`S3 업로드 실패: ${uploadResponse.status}`);
        }

        console.log('S3 업로드 성공:', fileUrl);
        return fileUrl;

    } catch (error) {
        console.error('이미지 업로드 에러:', error);
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
            submitBtn.textContent = '레시피 저장 중...';
        }

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

        // 썸네일 이미지 업로드 (필수)
        const thumbFile = document.getElementById("thumbInput").files?.[0];
        if (!thumbFile) {
            alert('썸네일 이미지를 선택해주세요!');
            return;
        }
        
        console.log('썸네일 이미지 업로드 중...');
        const thumbnailUrl = await uploadImageToS3(thumbFile, 'recipe-thumbnails');
        console.log('썸네일 업로드 완료:', thumbnailUrl);

        // 단계별 이미지 업로드
        const stepImageUrls = [];
        const stepItems = document.querySelectorAll('.step-item');
        
        for (let i = 0; i < stepItems.length; i++) {
            const stepFile = stepItems[i].querySelector('[data-photo]')?.files?.[0];
            if (stepFile) {
                console.log(`${i+1}단계 이미지 업로드 중...`);
                const stepImageUrl = await uploadImageToS3(stepFile, 'recipe-steps-image');
                stepImageUrls.push(stepImageUrl);
                console.log(`${i+1}단계 업로드 완료:`, stepImageUrl);
            } else {
                stepImageUrls.push(''); // 이미지가 없는 단계는 빈 문자열
            }
        }

        // 단계별 설명 수집
        const stepDescriptions = [];
        stepItems.forEach((item) => {
            const desc = item.querySelector('[data-desc]')?.value.trim() || '';
            stepDescriptions.push(desc);
        });

        // 레시피 데이터 구성 (PostDto 형식에 맞게)
        const formData = new FormData();
        
        // 기본 정보
        formData.append('title', document.querySelector('[name="title"]')?.value || '제목 없음');
        formData.append('foodName', document.querySelector('[name="foodName"]')?.value || '음식명 없음');
        
        // 필수 enum 필드들 (기본값 설정)
        formData.append('ckgMth', 'OTHER');        // 조리방법
        formData.append('ckgCategory', 'OTHER');   // 카테고리  
        formData.append('ckgKnd', 'OTHER');        // 요리 종류
        
        // Integer 필드들 - 드롭다운에서 선택된 값 또는 기본값
        const inbunSelect = document.querySelector('select[name="CKG_INBUN"]');
        const levelSelect = document.querySelector('select[name="CKG_LEVEL"]');
        const timeSelect = document.querySelector('select[name="CKG_TIME"]');
        
        formData.append('ckgInbun', inbunSelect?.value || '1');   // 기본: 1인분
        formData.append('ckgLevel', levelSelect?.value || '1');   // 기본: 1 (★)
        formData.append('ckgTime', timeSelect?.value || '30');    // 기본: 30분이내
        
        // 재료 (List<String> 형태로)
        ingredients.forEach(ingredient => {
            formData.append('ckgMtrlCn', ingredient);
        });
        
        // 썸네일 이미지 URL (빈 문자열이면 기본 이미지로 대체)
        formData.append('rcpImgUrl', thumbnailUrl || 'https://via.placeholder.com/300x200?text=No+Image');
        
        // 조리 단계별 설명 (List<String> 형태로)
        stepDescriptions.forEach(step => {
            formData.append('rcpSteps', step);
        });
        
        // 단계별 이미지 URLs (List<String> 형태로)
        stepImageUrls.forEach(imageUrl => {
            formData.append('rcpStepsImg', imageUrl);
        });

        console.log('저장할 레시피 데이터:', Object.fromEntries(formData));

        // 백엔드 API로 레시피 저장 (올바른 엔드포인트 사용)
        const saveResponse = await fetch('/post/save', {
            method: 'POST',
            body: formData
        });

        if (!saveResponse.ok) {
            throw new Error(`레시피 저장 실패: ${saveResponse.status}`);
        }

        const result = await saveResponse.json();
        alert('레시피가 성공적으로 저장되었습니다!');
        console.log('저장된 레시피:', result);

        // 폼 초기화 또는 다른 페이지로 리다이렉트
        // window.location.href = '/recipes';

    } catch (error) {
        console.error('레시피 저장 에러:', error);
        alert('레시피 저장에 실패했습니다: ' + error.message);
    } finally {
        // 로딩 상태 해제
        const submitBtn = document.querySelector('button[type="submit"]');
        if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.textContent = '레시피 저장';
        }
    }
});
