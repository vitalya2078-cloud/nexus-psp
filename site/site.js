/* KAPSULA — живой маскот, актуальная версия APK, кнопка «Скачать» на телефоне. Без сторонних библиотек и счётчиков. */
(function () {
  var ua = navigator.userAgent || "";
  if (/iPhone|iPad|iPod/.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1)) document.documentElement.classList.add("is-ios");

  // версия и прямая ссылка на APK: latest.json кладёт сборка сайта (с GitVerse, без VPN)
  fetch("latest.json", { cache: "no-store" }).then(function (r) { return r.ok ? r.json() : null; }).then(function (d) {
    if (!d || !d.url) return;
    document.querySelectorAll(".js-apk").forEach(function (a) { a.href = d.url; });
    var v = document.querySelector(".js-ver");
    if (v && d.ver) v.textContent = "Версия " + d.ver + (d.mb ? " · " + d.mb + " МБ" : "");
  }).catch(function () {});

  // телефон: «Скачать APK» появляется внизу, когда главная кнопка ушла за экран
  var main = document.querySelector(".cta .js-apk"), sticky = document.querySelector(".sticky");
  if (main && sticky && "IntersectionObserver" in window) {
    new IntersectionObserver(function (e) { sticky.classList.toggle("show", !e[0].isIntersecting && e[0].boundingClientRect.top < 0); }).observe(main);
  }

  // маскот: приветствие по времени суток, моргает, смотрит по сторонам, на касание — смеётся и подпрыгивает
  var img = document.querySelector(".js-pose"), btn = document.querySelector(".js-buddy"), bubble = document.querySelector(".js-bubble");
  if (!img || !btn) return;
  var still = window.matchMedia && matchMedia("(prefers-reduced-motion: reduce)").matches;
  var P = function (n) { return "img/mascot/" + n + ".webp"; };
  ["idle", "blink", "lookl", "lookr", "laugh"].forEach(function (n) { new Image().src = P(n); });
  var h = new Date().getHours();
  var greet = h >= 6 && h <= 10 ? ["stretch", "Доброе утро! Кофе и одна игра?"]
    : h <= 17 && h > 10 ? ["wave", "Привет! Во что сыграем?"]
    : h >= 18 && h <= 22 ? ["mug", "Добрый вечер. Самое время для игры"]
    : ["sleep", "Не спится? Мне тоже…"];
  var base = greet[0], busy = false;
  img.src = P(base); if (bubble) bubble.textContent = greet[1];
  function set(n) { img.src = P(n); }
  function hop() { btn.classList.remove("hop"); void btn.offsetWidth; btn.classList.add("hop"); }
  setTimeout(function () { if (bubble) bubble.classList.add("hide"); }, 6000);
  setTimeout(function () { if (base !== "sleep") { base = "idle"; hop(); set(base); } }, 3200);
  if (!still) (function life() {
    setTimeout(function () {
      if (!busy && base === "idle") {
        if (Math.random() < .25) { var l = Math.random() < .5 ? "lookl" : "lookr"; set(l); setTimeout(function () { if (!busy) set(base); }, 1300); }
        else { set("blink"); setTimeout(function () { if (!busy) set(base); }, 130); }
      }
      life();
    }, 3000 + Math.random() * 3000);
  })();
  btn.addEventListener("click", function () {
    if (busy) return;
    busy = true; if (base === "sleep") base = "idle";
    set("laugh"); hop();
    setTimeout(function () { set(base); busy = false; }, 1100);
  });
})();
