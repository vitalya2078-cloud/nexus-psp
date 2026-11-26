/* Страница подарка: ?c=КОД → кнопка открывает KAPSULA (kapsula://gift/КОД). */
(function () {
  var c = (new URLSearchParams(location.search).get("c") || "").toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 32);
  var take = document.querySelector(".js-take");
  document.querySelector(".js-code").textContent = c || "—";
  if (!c) { take.removeAttribute("href"); take.style.opacity = ".5"; return; }
  take.href = "kapsula://gift/" + c;
  take.addEventListener("click", function () {
    document.querySelector(".js-gpose").src = "/img/mascot/gift-open.webp";
    setTimeout(function () { if (!document.hidden) document.querySelector(".lead").textContent = t("gift.retry"); }, 2000);
  });
})();
