const input = document.getElementById('photoInput');
const preview = document.getElementById('preview');
const placeholder = document.getElementById('placeholder');
const clearBtn = document.getElementById('clearBtn');
const saveBtn = document.getElementById('saveBtn');

function resetPreview(){
  preview.src = '';
  preview.hidden = true;
  placeholder.hidden = false;
  clearBtn.hidden = true;
  saveBtn.disabled = true;
}

input?.addEventListener('change', () => {
  const file = input.files?.[0];
  if (!file) { resetPreview(); return; }

  const url = URL.createObjectURL(file);
  preview.src = url;
  preview.hidden = false;
  placeholder.hidden = true;
  clearBtn.hidden = false;
  saveBtn.disabled = false;
});

clearBtn?.addEventListener('click', (e) => {
  e.stopPropagation();
  input.value = '';
  resetPreview();
});

// 실제 저장(백엔드 연동 시 FormData 활용)
saveBtn?.addEventListener('click', async () => {
  const file = input.files?.[0];
  if (!file) return;

  // 예시: 추후 백엔드 연결
  // const fd = new FormData(); fd.append('photo', file);
  // await fetch('/api/upload', { method:'POST', body: fd });

  alert('저장(더미) 완료!');
});
