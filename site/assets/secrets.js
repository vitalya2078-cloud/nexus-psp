/* Страница «Секреты»: достижения, книга читов, охота за картриджами, награда. Читает данные eggs.js (window.kEgg). */
(function () {
  "use strict";
  var E = window.kEgg; if (!E) return;
  var $ = function (s) { return document.querySelector(s); };
  var CODE = { konami: "↑↑↓↓←→←→ B A", iddqd: "IDDQD", idkfa: "IDKFA", hesoyam: "HESOYAM", hadoken: "↓ → A", abacabb: "ABACABB", levelselect: "↑↓←→ Enter", xyzzy: "XYZZY",
    pause: "PAUSE", oneup: "1UP", kapsula: "KAPSULA", sega: "SEGA", dendy: "DENDY", sleep: "SLEEP" };
  function esc(s) { return String(s).replace(/[&<>"]/g, function (c) { return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]; }); }
  function render() {
    var got = E.got(), codes = E.codes(), carts = E.carts();
    var open = E.ACH.filter(function (a) { return !a[2]; }), deep = E.ACH.filter(function (a) { return a[2]; });
    var n = E.ACH.filter(function (a) { return got[a[0]]; }).length, nd = deep.filter(function (a) { return got[a[0]]; }).length;
    var nc = E.CHEATS.filter(function (c) { return codes[c]; }).length, nk = E.CARTS.filter(function (c) { return carts[c]; }).length;
    var all = E.ACH.length + E.CHEATS.length + E.CARTS.length, have = n + nc + nk;
    $(".js-total").textContent = t("sec.total").replace("{n}", have).replace("{all}", all);
    $(".js-meter").style.width = Math.round(have / all * 100) + "%";
    // открытые: найденные — с названием и описанием; ненайденные — «??????» и намёк
    var list = open.concat(deep.filter(function (a) { return got[a[0]]; }));
    $(".js-ach").innerHTML = list.map(function (a) {
      var on = !!got[a[0]];
      return '<div class="ach' + (on ? "" : " off") + '"><i>' + (on ? esc(a[1]) : "?") + "</i><div><b>" + (on ? esc(t("egg." + a[0])) : "??????") + "</b><span>" + esc(t("egg." + a[0] + ".d")) + "</span></div></div>";
    }).join("");
    $(".js-deep").textContent = t("sec.deep").replace("{n}", nd).replace("{all}", deep.length);
    $(".js-book").innerHTML = E.CHEATS.map(function (c) {
      var on = !!codes[c];
      return '<div class="ach' + (on ? "" : " off") + '"><i>⌁</i><div><b>' + (on ? esc(t("cheat." + c)) : "??????") + '</b><span class="code-k">' + (on ? esc(CODE[c]) : "• • • • •") + "</span></div></div>";
    }).join("");
    $(".js-carts").innerHTML = E.CARTS.map(function (c, i) {
      return '<div class="cart-slot' + (carts[c] ? " on" : "") + '" title="' + (i + 1) + '"><img src="/img/items/' + ["cart-red", "cart-blue", "cart-yellow", "cart-purple", "cart-grey", "card", "umd"][i] + '.webp" alt=""></div>';
    }).join("") + '<b style="align-self:center;margin-left:8px">' + nk + "/7</b>";
    $(".js-empty").hidden = n > 1; // «Первый визит» не в счёт
    var rw = nk >= 7 && nc >= 7 && n >= 30; $(".js-reward").hidden = !rw; if (rw) $(".js-rcode").textContent = atob("U0VFS0VS");
  }
  render(); addEventListener("kegg", render);
  if (E.got() && Object.keys(E.got()).length <= 1) E.unlock("empty_secrets");
})();
