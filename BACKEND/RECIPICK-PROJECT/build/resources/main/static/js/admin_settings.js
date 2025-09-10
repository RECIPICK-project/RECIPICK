(() => {
  // 키 정의
  const KEY_PROMO = 'promotionRule';
  const KEY_REPORT = 'reportPolicy';
  const KEY_UI     = 'adminUi';

  // 헬퍼
  const $ = (s, c=document) => c.querySelector(s);
  const get = k => { try { return JSON.parse(localStorage.getItem(k)) } catch(e){ return null } };
  const set = (k,v) => localStorage.setItem(k, JSON.stringify(v));

  // ===== 승격 규칙 =====
  const promoDefault = {
    mode:'auto', windowDays:7, cooldown:24,
    minLikes:50, minSaves:20, minRating:4.3, minReviews:10, minViews:1000
  };
  function renderPromo(p){
    $('#mode').value = p.mode;
    $('#windowDays').value = p.windowDays;
    $('#cooldown').value = p.cooldown;
    $('#minLikes').value = p.minLikes;
    $('#minSaves').value = p.minSaves;
    $('#minRating').value = p.minRating;
    $('#minReviews').value = p.minReviews;
    $('#minViews').value = p.minViews;
  }
  renderPromo(get(KEY_PROMO) || promoDefault);

  $('#savePromotion').addEventListener('click', ()=>{
    const p = {
      mode: $('#mode').value,
      windowDays:+$('#windowDays').value,
      cooldown:+$('#cooldown').value,
      minLikes:+$('#minLikes').value,
      minSaves:+$('#minSaves').value,
      minRating:+$('#minRating').value,
      minReviews:+$('#minReviews').value,
      minViews:+$('#minViews').value
    };
    set(KEY_PROMO,p);
    $('#savedP').style.display='inline-block';
    setTimeout(()=> $('#savedP').style.display='none',1200);
  });
  $('#resetPromotion').addEventListener('click', ()=>{
    set(KEY_PROMO, promoDefault);
    renderPromo(promoDefault);
  });

  // ===== 신고 정책 =====
  const reportDefault = {
    defaultAction:'keep',
    banDays:{ light:0, mid:3, hard:7 }
  };
  function renderReport(r){
    $('#reportDefaultAction').value = r.defaultAction;
    $('#banLight').value = r.banDays.light;
    $('#banMid').value = r.banDays.mid;
    $('#banHard').value = r.banDays.hard;
  }
  renderReport(get(KEY_REPORT) || reportDefault);

  $('#saveReport').addEventListener('click', ()=>{
    const r = {
      defaultAction: $('#reportDefaultAction').value,
      banDays: {
        light:+$('#banLight').value,
        mid:+$('#banMid').value,
        hard:+$('#banHard').value
      }
    };
    set(KEY_REPORT, r);
    $('#savedR').style.display='inline-block';
    setTimeout(()=> $('#savedR').style.display='none',1200);
  });
  $('#resetReport').addEventListener('click', ()=>{
    set(KEY_REPORT, reportDefault);
    renderReport(reportDefault);
  });

  // ===== UI 옵션 =====
  const uiDefault = { charts:'on', density:'normal' };
  function renderUi(u){
    $('#optCharts').value = u.charts;
    $('#optDensity').value = u.density;
  }
  renderUi(get(KEY_UI) || uiDefault);

  $('#saveUi').addEventListener('click', ()=>{
    const u = { charts: $('#optCharts').value, density: $('#optDensity').value };
    set(KEY_UI, u);
    $('#savedU').style.display='inline-block';
    setTimeout(()=> $('#savedU').style.display='none',1200);
  });

  // ===== 내보내기/가져오기 =====
  $('#exportBtn').addEventListener('click', ()=>{
    const blob = new Blob([JSON.stringify({
      promotionRule: get(KEY_PROMO) || promoDefault,
      reportPolicy : get(KEY_REPORT) || reportDefault,
      adminUi      : get(KEY_UI) || uiDefault
    }, null, 2)], {type:'application/json'});
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = 'recipick-admin-settings.json'; a.click();
    URL.revokeObjectURL(url);
  });

  $('#importFile').addEventListener('change', (e)=>{
    const file = e.target.files?.[0];
    if(!file) return;
    const reader = new FileReader();
    reader.onload = () => {
      try{
        const data = JSON.parse(reader.result);
        if(data.promotionRule) set(KEY_PROMO, data.promotionRule);
        if(data.reportPolicy)  set(KEY_REPORT, data.reportPolicy);
        if(data.adminUi)       set(KEY_UI, data.adminUi);
        // 다시 렌더
        renderPromo(get(KEY_PROMO) || promoDefault);
        renderReport(get(KEY_REPORT) || reportDefault);
        renderUi(get(KEY_UI) || uiDefault);
        alert('설정을 불러왔습니다.');
      }catch(err){
        alert('JSON 형식이 올바르지 않습니다.');
      }
    };
    reader.readAsText(file);
    e.target.value = '';
  });
})();
