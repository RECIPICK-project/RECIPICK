const radios = document.querySelectorAll('input[name="theme"]');
radios.forEach(r=>{
  r.addEventListener('change', ()=>{
    if(r.checked){
      if(r.nextSibling.textContent.includes("다크")){
        document.documentElement.setAttribute('data-theme','dark');
      } else {
        document.documentElement.removeAttribute('data-theme');
      }
    }
  });
});
