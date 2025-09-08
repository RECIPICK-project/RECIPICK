// /js/theme.js
(function () {
    const KEY = "theme"; // 'light' | 'dark'
    const root = document.documentElement;

    function applyTheme(mode) {
        if (mode === "dark") {
            root.setAttribute("data-theme", "dark");
            localStorage.setItem(KEY, "dark");
        } else {
            root.removeAttribute("data-theme");
            localStorage.setItem(KEY, "light");
        }
    }

    // 초기 적용: 저장값 → 없으면 시스템 선호
    // const saved = localStorage.getItem(KEY);
    // if (saved === "dark" || saved === "light") {
    //     applyTheme(saved);
    // } else {
    //     const prefersDark =
    //         window.matchMedia && window.matchMedia("(prefers-color-scheme: dark)").matches;
    //     applyTheme(prefersDark ? "dark" : "light");
    // }

    // 설정 페이지 라디오가 있으면 연결(value="light" | "dark" 권장)
    function wireRadios() {
        const radios = document.querySelectorAll('input[name="theme"]');
        if (!radios.length) return;

        // 현재 상태 반영
        const isDark = root.getAttribute("data-theme") === "dark";
        radios.forEach((r) => {
            // value가 없으면 라벨 텍스트로 판별(예방)
            const val = (r.value || r.nextElementSibling?.textContent || "").trim();
            r.checked =
                (isDark && val.includes("다크")) ||
                (!isDark && val.includes("라이트")) ||
                val === (isDark ? "dark" : "light");
            r.addEventListener("change", () => {
                if (!r.checked) return;
                const v = (r.value || r.nextElementSibling?.textContent || "").toLowerCase();
                if (v.includes("dark") || v.includes("다크")) applyTheme("dark");
                else applyTheme("light");
            });
        });
    }

    // 시스템 테마 바뀌면(저장 없을 때만) 따라가기
    window.matchMedia?.("(prefers-color-scheme: dark)").addEventListener?.("change", (e) => {
        const savedNow = localStorage.getItem(KEY);
        if (!savedNow) {
            applyTheme(e.matches ? "dark" : "light");
            wireRadios();
        }
    });

    document.addEventListener("DOMContentLoaded", wireRadios);
})();
