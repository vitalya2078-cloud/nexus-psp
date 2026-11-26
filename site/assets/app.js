/* KAPSULA сайт v3 — общее для всех страниц: язык, маскот на главной, загрузка APK (с GitHub), окно установки. */
(function () {
  "use strict";
  var doc = document, root = doc.documentElement, W = window;
  var T = W.T || {}, L = W.L || "/", LANG = W.LANG || "ru";
  W.t = function (k, d) { return T[k] != null ? T[k] : (d != null ? d : k); };
  W.tf = function (k) { var s = W.t(k); for (var i = 1; i < arguments.length; i++) s = s.split("{" + (i - 1) + "}").join(arguments[i]); return s; };
  var $ = function (s, r) { return (r || doc).querySelector(s); }, $$ = function (s, r) { return Array.prototype.slice.call((r || doc).querySelectorAll(s)); };
  W.$k = $; W.$$k = $$;
  var store = W.kstore = {
    get: function (k, d) { try { var v = localStorage.getItem("k." + k); return v == null ? d : JSON.parse(v); } catch (e) { return d; } },
    set: function (k, v) { try { localStorage.setItem("k." + k, JSON.stringify(v)); } catch (e) {} }
  };
  var still = W.matchMedia && matchMedia("(prefers-reduced-motion: reduce)").matches;
  W.kStill = still;
  var ua = navigator.userAgent || "";
  if (/iPhone|iPad|iPod/.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1)) root.classList.add("is-ios");

  // ---------- язык: первый заход — по браузеру; дальше — выбор ----------
  var LANGS = W.KLANGS || [];
  var saved = store.get("lang", null);
  if (!saved && !/[?&]stay/.test(location.search)) {
    var want = (navigator.languages || [navigator.language || "ru"]).map(function (x) { return x.slice(0, 2).toLowerCase(); })
      .filter(function (c) { return LANGS.some(function (l) { return l[0] === c; }); })[0];
    store.set("lang", want || LANG);
    if (want && want !== LANG) { var tgt = (want === "ru" ? "/" : "/" + want + "/") + (location.pathname.replace(/^\/(en|es|pt|de|fr|it|pl|tr|uk|ja|zh|ko)\//, "/").slice(1)); location.replace(tgt + location.hash); return; }
  }
  var lb = $(".lang-b"), lm = $(".langs");
  if (lb && lm) {
    lb.addEventListener("click", function (e) { e.stopPropagation(); lm.hidden = !lm.hidden; });
    doc.addEventListener("click", function (e) { if (!lm.hidden && !lm.contains(e.target)) lm.hidden = true; });
    $$("a", lm).forEach(function (a) { a.addEventListener("click", function () { store.set("lang", a.getAttribute("data-l")); W.kEgg && W.kEgg.lang(a.getAttribute("data-l")); }); });
  }

  // ---------- версия и ссылка на APK ----------
  var GH = "https://github.com/vitalya2078-cloud/nexus-psp/releases/download/latest/kapsula.apk";
  var apk = { url: GH, ver: null, mb: 0 };
  fetch("/latest.json?t=" + Date.now(), { cache: "no-store" }).then(function (r) { return r.ok ? r.json() : null; }).then(function (d) {
    if (!d || !d.url) return; apk = d;
    $$(".js-ver").forEach(function (v) { v.textContent = t("js.version", "Версия") + " " + d.ver + (d.mb ? " · " + d.mb + " " + t("js.mb", "МБ") : "") + " · Android 8+"; });
  }).catch(function () {});

  // ---------- «Скачать»: окно с подсказками установки + обычная загрузка браузером (GitHub докачивает после обрыва) ----------
  var modal = $(".js-get");
  function closeGet() { if (modal) { modal.hidden = true; doc.body.style.overflow = ""; } }
  if (modal) {
    $(".x", modal).addEventListener("click", closeGet);
    modal.addEventListener("click", function (e) { if (e.target === modal) closeGet(); });
    addEventListener("keydown", function (e) { if (e.key === "Escape") closeGet(); });
  }
  $$(".js-apk").forEach(function (a) {
    a.href = apk.url;
    a.addEventListener("click", function (e) {
      if (root.classList.contains("is-ios")) return;
      e.preventDefault();
      W.kEgg && W.kEgg.download();
      if (modal) { modal.hidden = false; doc.body.style.overflow = "hidden"; }
      setTimeout(function () { location.href = apk.url; }, 350);
    });
  });

  // ---------- маскот главной: приветствие по времени суток, моргает, смотрит, на касание смеётся ----------
  var img = $(".js-pose"), btn = $(".js-buddy"), bubble = $(".js-bubble");
  W.kBuddy = null;
  if (img && btn) {
    var P = function (n) { return "/img/mascot/" + n + ".webp"; };
    ["idle", "blink", "lookl", "lookr", "laugh", "sleep", "stretch", "dance1", "dance2", "dance3", "hide", "dizzy", "jump", "wow"].forEach(function (n) { new Image().src = P(n); });
    var h = new Date().getHours();
    var greet = h >= 6 && h <= 10 ? ["stretch", t("js.hi.morning")] : h >= 11 && h <= 17 ? ["wave", t("js.hi.day")] : h >= 18 && h <= 22 ? ["mug", t("js.hi.evening")] : ["sleep", t("js.hi.night")];
    var B = W.kBuddy = { base: greet[0], busy: false, look: null, el: btn,
      set: function (n) { img.src = P(n); },
      hop: function () { btn.classList.remove("hop"); void btn.offsetWidth; btn.classList.add("hop"); },
      say: function (s, ms) { if (!bubble) return; bubble.textContent = s; bubble.classList.remove("hide"); clearTimeout(B.st); B.st = setTimeout(function () { bubble.classList.add("hide"); }, ms || 3200); },
      act: function (pose, ms, text) { // короткая сценка: поза на время, потом обратно
        if (B.busy && !text) return; B.busy = true; B.set(pose); B.hop(); if (text) B.say(text, ms + 600);
        clearTimeout(B.at); B.at = setTimeout(function () { B.busy = false; B.set(B.look || B.base); }, ms || 1100);
      } };
    B.set(B.base); if (bubble) bubble.textContent = greet[1];
    setTimeout(function () { bubble && bubble.classList.add("hide"); }, 5500);
    setTimeout(function () { if (B.base !== "sleep") { B.base = "idle"; B.hop(); B.set("idle"); } }, 3000);
    if (!still) (function life() {
      setTimeout(function () {
        if (!B.busy && B.base === "idle" && !B.look) { B.set("blink"); setTimeout(function () { if (!B.busy) B.set(B.look || B.base); }, 130); }
        life();
      }, 3000 + Math.random() * 3000);
    })();
    if (W.matchMedia && matchMedia("(pointer: fine)").matches) addEventListener("pointermove", function (e) {
      if (B.busy || B.base !== "idle") return;
      var r = btn.getBoundingClientRect(), dx = e.clientX - (r.left + r.width / 2);
      var want = dx < -r.width * .7 ? "lookl" : dx > r.width * .7 ? "lookr" : null;
      if (want !== B.look) { B.look = want; B.set(want || B.base); }
    }, { passive: true });
  }
})();
