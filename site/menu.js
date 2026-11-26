/* Меню сайта: кнопка ☰ в шапке (на телефоне и на узком экране) → шторка со всеми разделами и «Полезным». Одно на все страницы. */
(function () {
  "use strict";
  var top = document.querySelector(".top"); if (!top || document.querySelector(".burger")) return;
  var here = location.pathname.split("/").pop() || "index.html";
  var home = here === "index.html" || here === "" ? "" : "./";
  var L = [
    ["Главное", [[home + "#inside", "Что внутри"], [home + "#consoles", "Приставки"], [home + "#news", "Что нового"], [home + "#faq", "Вопросы"]]],
    ["Полезное", [["check.html", "Что это за файл?"], ["pad.html", "Тестер геймпада"], ["phone.html", "Пойдёт ли у меня?"], ["quiz.html", "Ретро-викторина"], ["shelf.html", "Моя полка"]]],
    ["Ещё", [["https://vk.ru/glasty", "Написать нам"], ["privacy.html", "Конфиденциальность"], ["https://gitverse.ru/kapsula/KAPSULA", "Исходный код"]]],
  ];
  var b = document.createElement("button");
  b.className = "burger"; b.type = "button"; b.setAttribute("aria-label", "Меню"); b.setAttribute("aria-expanded", "false");
  b.innerHTML = "<i></i><i></i><i></i>";
  top.appendChild(b);
  var m = document.createElement("div"); m.className = "menu"; m.hidden = true;
  m.innerHTML = '<nav class="menu-sheet" aria-label="Меню">' + L.map(function (g) {
    return "<b>" + g[0] + "</b>" + g[1].map(function (l) {
      var on = l[0] === here ? ' class="on"' : "";
      return '<a href="' + l[0] + '"' + on + (l[0].indexOf("http") === 0 ? ' rel="noopener"' : "") + ">" + l[1] + "</a>";
    }).join("");
  }).join("") + '<a class="btn btn-main menu-get" href="' + home + '#get">Скачать KAPSULA</a></nav>';
  document.body.appendChild(m);
  function set(open) {
    m.hidden = !open; b.classList.toggle("x", open); b.setAttribute("aria-expanded", String(open));
    document.body.style.overflow = open ? "hidden" : "";
  }
  b.addEventListener("click", function () { set(m.hidden); });
  m.addEventListener("click", function (e) { if (e.target === m || e.target.tagName === "A") set(false); });
  addEventListener("keydown", function (e) { if (e.key === "Escape") set(false); });
})();
