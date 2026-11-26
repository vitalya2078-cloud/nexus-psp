/* «Пойдёт ли у меня?»: класс процессора (1 — простой … 5 — флагман) → оценка по приставкам. Примерно, по опыту эмуляторов. */
(function () {
  "use strict";
  var D = window.KAPSULA_DATA || { platforms: [] };
  var CHIPS = [
    [5, "Snapdragon 8 Elite"], [5, "Snapdragon 8 Gen 3"], [5, "Snapdragon 8 Gen 2"], [5, "Dimensity 9400"], [5, "Dimensity 9300"], [5, "Dimensity 9200"],
    [4, "Snapdragon 8s Gen 3"], [4, "Snapdragon 8+ Gen 1"], [4, "Snapdragon 8 Gen 1"], [4, "Snapdragon 888"], [4, "Snapdragon 870"], [4, "Snapdragon 865"], [4, "Snapdragon 7+ Gen 2"],
    [4, "Dimensity 9000"], [4, "Dimensity 8300"], [4, "Dimensity 8200"], [4, "Dimensity 8100"], [4, "Exynos 2400"], [4, "Exynos 2200"], [4, "Exynos 2100"], [4, "Tensor G4"], [4, "Tensor G3"],
    [3, "Snapdragon 860"], [3, "Snapdragon 855"], [3, "Snapdragon 845"], [3, "Snapdragon 7 Gen 3"], [3, "Snapdragon 7 Gen 1"], [3, "Snapdragon 7s Gen 2"], [3, "Snapdragon 782G"], [3, "Snapdragon 780G"], [3, "Snapdragon 778G"],
    [3, "Snapdragon 6 Gen 1"], [3, "Dimensity 1200"], [3, "Dimensity 1100"], [3, "Dimensity 1080"], [3, "Dimensity 7200"], [3, "Dimensity 7050"], [3, "Exynos 1480"], [3, "Exynos 1380"], [3, "Exynos 990"], [3, "Tensor G2"], [3, "Tensor"],
    [2, "Snapdragon 765G"], [2, "Snapdragon 750G"], [2, "Snapdragon 732G"], [2, "Snapdragon 730G"], [2, "Snapdragon 720G"], [2, "Snapdragon 695"], [2, "Snapdragon 690"], [2, "Snapdragon 685"], [2, "Snapdragon 680"], [2, "Snapdragon 675"], [2, "Snapdragon 665"], [2, "Snapdragon 662"], [2, "Snapdragon 660"],
    [2, "Snapdragon 4 Gen 2"], [2, "Dimensity 7020"], [2, "Dimensity 6100+"], [2, "Dimensity 6080"], [2, "Dimensity 6020"], [2, "Dimensity 920"], [2, "Dimensity 900"], [2, "Dimensity 810"], [2, "Dimensity 800U"], [2, "Dimensity 720"], [2, "Dimensity 700"],
    [2, "Helio G99"], [2, "Helio G96"], [2, "Helio G95"], [2, "Helio G90T"], [2, "Helio G88"], [2, "Exynos 1330"], [2, "Exynos 1280"], [2, "Exynos 9611"], [2, "Exynos 9810"], [2, "Unisoc T820"],
    [1, "Snapdragon 4 Gen 1"], [1, "Snapdragon 480"], [1, "Snapdragon 460"], [1, "Snapdragon 450"], [1, "Snapdragon 439"], [1, "Snapdragon 429"], [1, "Helio G85"], [1, "Helio G80"], [1, "Helio G70"], [1, "Helio G37"], [1, "Helio G35"], [1, "Helio G36"],
    [1, "Helio P60"], [1, "Helio P35"], [1, "Helio A22"], [1, "Exynos 850"], [1, "Exynos 7904"], [1, "Exynos 7884"], [1, "Unisoc T616"], [1, "Unisoc T610"], [1, "Unisoc T606"], [1, "Unisoc T618"], [1, "Unisoc SC9863A"],
  ];
  // приставка → [класс «хорошо», класс «отлично»]
  var NEED = { a26: [1, 1], a78: [1, 1], nes: [1, 1], sms: [1, 1], gg: [1, 1], pce: [1, 1], ngp: [1, 1], ws: [1, 1], lynx: [1, 1], j2me: [1, 1],
    md: [1, 2], neogeo: [1, 2], ps1: [1, 2], psp: [2, 3], dc: [3, 4], saturn: [3, 5], ps2: [4, 5] };
  var TIER = ["", "простой — для 8- и 16-битных приставок", "средний", "хороший", "мощный", "флагман"];
  var q = document.querySelector(".js-q"), list = document.querySelector(".js-list"), res = document.querySelector(".js-res");
  function grade(tier, id) {
    var n = NEED[id] || [2, 3];
    if (tier >= n[1]) return ["Отлично", "tag-ok"];
    if (tier >= n[0]) return ["Хорошо", "tag-ok"];
    if (tier >= n[0] - 1) return ["Только лёгкие игры", "tag-warn"];
    return ["Не потянет", "tag-bad"];
  }
  function show(c) {
    res.hidden = false;
    document.querySelector(".js-chip").textContent = c[1];
    document.querySelector(".js-tier").textContent = "Класс: " + TIER[c[0]];
    var g = document.querySelector(".js-grades"); g.innerHTML = "";
    D.platforms.forEach(function (p, i) {
      var r = grade(c[0], p.id), el = document.createElement("div");
      el.className = "grade"; el.style.animationDelay = (i * 30) + "ms";
      el.innerHTML = '<img src="' + p.img + '" alt=""><div><b></b><span class="' + r[1] + '"></span></div>';
      el.querySelector("b").textContent = p.short; el.querySelector("span").textContent = r[0];
      g.appendChild(el);
    });
    res.scrollIntoView({ behavior: "smooth", block: "start" });
  }
  function render() {
    var s = q.value.toLowerCase().replace(/\s+/g, " ").trim();
    list.innerHTML = "";
    CHIPS.filter(function (c) { return !s || c[1].toLowerCase().indexOf(s) >= 0 || c[1].toLowerCase().replace(/\s/g, "").indexOf(s.replace(/\s/g, "")) >= 0; }).slice(0, 40).forEach(function (c) {
      var b = document.createElement("button"); b.type = "button"; b.className = "row-pick";
      b.innerHTML = "<span></span><small></small>"; b.firstChild.textContent = c[1]; b.lastChild.textContent = ["", "простой", "средний", "хороший", "мощный", "флагман"][c[0]];
      b.addEventListener("click", function () { show(c); }); list.appendChild(b);
    });
    if (!list.children.length) list.innerHTML = '<p class="hint">Не нашёл такой процессор. Попробуй короче: «778», «G99», «8100».</p>';
  }
  q.addEventListener("input", render); render();
  // модель телефона (Chrome на Android сообщает её сам) — подсказка, где искать процессор
  if (navigator.userAgentData && navigator.userAgentData.getHighEntropyValues) navigator.userAgentData.getHighEntropyValues(["model"]).then(function (v) {
    if (v.model) document.querySelector(".js-model").textContent = "Твой телефон: " + v.model + ". Процессор написан в Настройки → О телефоне (или найди модель в поиске).";
  }).catch(function () {});
})();
