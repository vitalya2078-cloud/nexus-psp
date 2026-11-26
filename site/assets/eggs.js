/* KAPSULA — секреты сайта: достижения, «Книга читов», охота за картриджами, пасхалки.
   Всё в браузере (localStorage), ничего не отправляется. Правило: секреты только радуют — не мешают скачать и не шумят без спроса. */
(function () {
  "use strict";
  var W = window, doc = document, root = doc.documentElement, body = doc.body;
  var t = W.t || function (k, d) { return d || k; }, store = W.kstore, L = W.L || "/";
  var $ = function (s, r) { return (r || doc).querySelector(s); }, $$ = function (s, r) { return Array.prototype.slice.call((r || doc).querySelectorAll(s)); };
  var still = W.kStill, B = function () { return W.kBuddy; };
  var page = (location.pathname.split("/").pop() || "index.html").replace(".html", "") || "index";

  // ---------- список: id, иконка (буква/символ), скрыт ли до открытия ----------
  var ACH = [
    ["first", "★"], ["curious", "👀", 0], ["explorer", "🗺"], ["night_owl", "🌙"], ["early_bird", "☀", 1], ["polyglot", "🌍"], ["lang_hopper", "🔀", 1],
    ["streak3", "3"], ["streak7", "7"], ["midnight", "🕛", 1], ["leet", "1337", 1], ["tickle", "😆"], ["hider", "🎒", 1], ["thrower", "🤾", 1], ["spinner", "🌀", 1],
    ["shaker", "📳"], ["upside", "🙃", 1], ["sleepy", "💤"], ["offline", "🔌", 1], ["squeeze", "↔", 1], ["reader", "📖", 1], ["copycat", "📋", 1], ["miss_you", "🥺", 1],
    ["hunter", "🧩"], ["proto", "❓", 1], ["chiptune", "♪", 1], ["quiz_master", "🏆"], ["quiz_twice", "🏅", 1], ["gamer", "🕹"], ["legend", "👑"], ["loser5", "🫂", 1],
    ["pad_maniac", "🎮"], ["detective", "🔍"], ["weakling", "🐢", 1], ["flagship", "🚀", 1], ["shelf_star", "📸"], ["seivik_card", "💛", 1], ["why", "❔"], ["lost3", "🧭", 1],
    ["downloader", "⬇"], ["patient", "⏳", 1], ["blower", "💨", 1], ["printer", "🖨", 1], ["hacker", "</>", 1], ["noisy", "⌨", 1], ["long_stay", "⏰", 1], ["hundred", "💯", 1],
    ["seasonal", "🎄", 1], ["friday", "🛌", 1], ["monday", "☕", 1], ["april", "🤡", 1], ["version5", "🔢", 1], ["title_jump", "🦘", 1], ["flag_hover", "🏳", 1],
    ["footer_logo", "👋", 1], ["hash_seivik", "#", 1], ["theme_flip", "🌗", 1], ["debug", "🧰", 1], ["scroll_faq", "↕", 1], ["empty_secrets", "🫙", 1],
    ["cheat_console", ">_", 1], ["all_cheats", "📕"], ["completionist", "💎"],
  ];
  var CHEATS = ["konami", "iddqd", "idkfa", "hesoyam", "hadoken", "abacabb", "levelselect", "xyzzy", "pause", "oneup", "kapsula", "sega", "dendy", "sleep"];
  var CARTS = ["c1", "c2", "c3", "c4", "c5", "c6", "c7"];
  var REWARD = "SEEKER";

  var got = store.get("ach", {}), codes = store.get("codes", {}), carts = store.get("carts", {});
  function has(id) { return !!got[id]; }
  function unlock(id) {
    if (got[id] || !ACH.some(function (a) { return a[0] === id; })) return false;
    got[id] = Date.now(); store.set("ach", got);
    toast(t("egg." + id, id), ACH.filter(function (a) { return a[0] === id; })[0][1]);
    if (id !== "completionist" && ACH.every(function (a) { return a[0] === "completionist" || got[a[0]]; })) setTimeout(function () { unlock("completionist"); }, 1500);
    W.dispatchEvent(new Event("kegg"));
    return true;
  }
  function code(id) {
    if (!codes[id]) { codes[id] = Date.now(); store.set("codes", codes); toast(t("cheat." + id, id), "⌁", t("js.cheat", "Чит-код")); W.dispatchEvent(new Event("kegg")); }
    if (CHEATS.every(function (c) { return codes[c]; })) unlock("all_cheats");
  }
  var tq = [], tbusy = false;
  function toast(title, icon, label) {
    tq.push([title, icon, label]); if (tbusy) return;
    (function next() {
      var x = tq.shift(); if (!x) { tbusy = false; return; } tbusy = true;
      var el = doc.createElement("div"); el.className = "toast";
      el.innerHTML = '<img src="/img/mascot/trophy.webp" alt=""><div><small></small><b></b></div>';
      el.querySelector("small").textContent = (x[2] || t("js.ach", "Секрет найден")) + " · " + x[1];
      el.querySelector("b").textContent = x[0];
      body.appendChild(el); if (navigator.vibrate && navigator.userActivation && navigator.userActivation.hasBeenActive) navigator.vibrate(25);
      setTimeout(function () { el.remove(); next(); }, 4300);
    })();
  }
  W.kEgg = { unlock: unlock, code: code, has: has, ACH: ACH, CHEATS: CHEATS, CARTS: CARTS, REWARD: REWARD, got: function () { return got; }, codes: function () { return codes; }, carts: function () { return carts; },
    lang: function (l) { var seen = store.get("langs", {}); seen[l] = 1; store.set("langs", seen); if (Object.keys(seen).length >= (W.KLANGS || []).length) unlock("polyglot");
      var hop = store.get("langhop", []).filter(function (x) { return Date.now() - x < 10000; }); hop.push(Date.now()); store.set("langhop", hop); if (hop.length >= 3) unlock("lang_hopper"); },
    download: function () { unlock("downloader"); }, toast: toast };

  // ---------- эффекты ----------
  function fx(html, css, ms) { var e = doc.createElement("div"); e.className = "fx"; e.innerHTML = html || ""; e.style.cssText = css; body.appendChild(e); setTimeout(function () { e.remove(); }, ms || 3000); return e; }
  function rain(items, n, dur) {
    if (still) return;
    for (var i = 0; i < n; i++) (function (i) {
      setTimeout(function () {
        var x = Math.random() * innerWidth, s = 26 + Math.random() * 22, e = fx('<img src="/img/items/' + items[i % items.length] + '.webp" style="width:' + s + 'px">', "left:" + x + "px;top:-50px;transition:transform " + (dur / 1000) + "s linear, opacity .4s", dur + 200);
        requestAnimationFrame(function () { e.style.transform = "translate(" + ((Math.random() - .5) * 120) + "px," + (innerHeight + 100) + "px) rotate(" + (Math.random() * 720 - 360) + "deg)"; });
      }, i * 60);
    })(i);
  }
  function confetti() { rain(["star", "heart", "coin", "spark", "cart-red", "cart-blue", "cart-yellow", "disc"], 40, 2600); }
  function say(s, pose, ms) { var b = B(); if (b) { b.act(pose || "laugh", ms || 1600, s); } else toast(s, "💬", t("js.seivik")); }
  function overlay(html, css, ms) { return fx(html, "inset:0;display:grid;place-items:center;z-index:97;" + css, ms); }

  // ---------- чит-коды: клавиатура ----------
  var buf = "", keys = [];
  var CODES = { UUDDLRLRBA: "konami", IDDQD: "iddqd", IDKFA: "idkfa", HESOYAM: "hesoyam", DRA: "hadoken", ABACABB: "abacabb", UDLRE: "levelselect", XYZZY: "xyzzy", PAUSE: "pause", "1UP": "oneup",
    KAPSULA: "kapsula", SEGA: "sega", DENDY: "dendy", SLEEP: "sleep" };
  function run(id) {
    code(id); var b = B();
    switch (id) {
      case "konami": root.classList.add("crt"); store.set("lives30", 1); say(t("js.c.konami"), "dance1", 2500);
        if (b && !still) { var n = 0, iv = setInterval(function () { b.set(["dance1", "dance2", "dance3", "dance2"][n++ % 4]); b.hop(); }, 380); setTimeout(function () { clearInterval(iv); b.set(b.base); }, 6000); }
        setTimeout(function () { root.classList.remove("crt"); }, 6000); break;
      case "iddqd": store.set("god", Date.now()); fx("", "inset:0;z-index:96;box-shadow:inset 0 0 120px 30px rgba(255,210,90,.55);animation:pulse 1s ease-in-out infinite alternate", 30000); say(t("js.c.iddqd"), "jump"); break;
      case "idkfa": rain(["cart-red", "cart-blue", "cart-yellow", "cart-purple", "disc", "umd", "card", "coin", "star", "heart", "magnet", "hourglass", "lamp", "clock"], 60, 2400); say(t("js.c.idkfa"), "dance3"); break;
      case "hesoyam": rain(["coin"], 50, 2200); say(t("js.c.hesoyam"), "catch", 2200); break;
      case "hadoken": hadoken(); break;
      case "abacabb": var o = overlay('<b style="font:700 64px var(--head);color:#e33">FATALITY?</b>', "background:rgba(0,0,0,.85)", 2600);
        setTimeout(function () { o.innerHTML = '<div style="text-align:center"><img src="/img/mascot/heart.webp" style="width:180px;margin:auto"><b style="font:700 54px var(--head);color:#f6c3a1">FRIENDSHIP!</b></div>'; }, 1300); break;
      case "levelselect": levelSelect(); break;
      case "xyzzy": say(t("js.c.xyzzy"), "wink", 2200); spawnCart("c7"); break;
      case "pause": var p = overlay('<span>PAUSE</span>', "", 999999); p.className = "fx pausebox"; root.classList.add("paused");
        var un = function () { p.remove(); root.classList.remove("paused"); removeEventListener("keydown", un); removeEventListener("pointerdown", un); };
        setTimeout(function () { addEventListener("keydown", un); addEventListener("pointerdown", un); }, 300); break;
      case "oneup": var r = b ? b.el.getBoundingClientRect() : { left: innerWidth / 2, top: innerHeight / 2, width: 0 };
        var u = fx('<b style="font:700 34px var(--head);color:#9fd6b4">+1 UP</b>', "left:" + (r.left + r.width / 2 - 40) + "px;top:" + r.top + "px;transition:transform 1.4s ease-out,opacity 1.4s", 1500);
        requestAnimationFrame(function () { u.style.transform = "translateY(-80px)"; u.style.opacity = 0; }); beep([988, 1319], .09); break;
      case "kapsula": confetti(); say(t("js.c.kapsula"), "jump"); break;
      case "sega": var s = overlay('<b style="font:700 90px/1 var(--head);letter-spacing:6px;color:#fff">SEGA</b>', "background:#1546b8", 1500); beep([523, 784], .12); break;
      case "dendy": if (b) { b.el.style.imageRendering = "pixelated"; var im = b.el.querySelector("img"); im.style.cssText = "width:16%;transform:scale(6.25);transform-origin:50% 100%;image-rendering:pixelated"; setTimeout(function () { im.style.cssText = ""; }, 6000); } say(t("js.c.dendy"), "pad"); break;
      case "sleep": root.style.transition = "filter 1s"; root.style.filter = "brightness(.55)"; if (b) { b.base = "sleep"; b.set("sleep"); }
        setTimeout(function () { root.style.filter = ""; if (b) { b.base = "idle"; b.set("idle"); } }, 7000); break;
    }
  }
  function hadoken() {
    var b = B(), r = b ? b.el.getBoundingClientRect() : { left: 40, top: innerHeight / 2, width: 0, height: 0 };
    if (b) b.act("point-right", 1400, t("js.c.hadoken"));
    var x = r.left + r.width * .8, y = r.top + r.height * .45, vx = 9, vy = 0, n = 0;
    var e = fx('<div style="width:54px;height:54px;border-radius:50%;background:radial-gradient(circle,#fff,#7fd3ff 35%,#2f6bff 65%,transparent 70%);box-shadow:0 0 30px #7fd3ff"></div>', "left:0;top:0", 4000);
    (function fly() { n++; x += vx; y += vy; if (x > innerWidth - 54 || x < 0) vx = -vx * .9; e.style.transform = "translate(" + x + "px," + y + "px)"; if (n < 200) requestAnimationFrame(fly); })();
  }
  function levelSelect() {
    var pages = [["index", "js.p.home"], ["consoles", "js.p.consoles"], ["tools", "js.p.tools"], ["game", "js.p.game"], ["faq", "js.p.faq"], ["news", "js.p.news"], ["secrets", "js.p.secrets"]];
    var o = overlay('<div style="background:#120e18;border:3px solid #f4b26b;padding:22px 30px;border-radius:12px;font:700 18px/2 ui-monospace,monospace;color:#9fd6b4;min-width:260px"><div style="color:#f4b26b;margin-bottom:6px">LEVEL SELECT</div>' +
      pages.map(function (p, i) { return '<a href="' + L + (p[0] === "index" ? "" : p[0] + ".html") + '" style="display:block;color:inherit;text-decoration:none">' + (i + 1) + ". " + t(p[1], p[0]).toUpperCase() + "</a>"; }).join("") + "</div>", "background:rgba(0,0,0,.6);pointer-events:auto", 15000);
    o.addEventListener("click", function (e) { if (e.target === o) o.remove(); });
  }
  var actx; function beep(fr, d) { try { actx = actx || new (W.AudioContext || W.webkitAudioContext)(); fr.forEach(function (f, i) { var o = actx.createOscillator(), g = actx.createGain(); o.type = "square"; o.frequency.value = f; g.gain.value = .05; o.connect(g); g.connect(actx.destination); o.start(actx.currentTime + i * d); o.stop(actx.currentTime + (i + 1) * d); }); } catch (e) {} }
  W.kBeep = beep;
  var keyTimes = [];
  addEventListener("keydown", function (e) {
    if (e.target && /INPUT|TEXTAREA/.test(e.target.tagName)) return;
    keyTimes.push(Date.now()); keyTimes = keyTimes.filter(function (x) { return Date.now() - x < 2000; }); if (keyTimes.length >= 20) unlock("noisy");
    var k = e.key, c = k === "ArrowUp" ? "U" : k === "ArrowDown" ? "D" : k === "ArrowLeft" ? "L" : k === "ArrowRight" ? "R" : k === "Enter" ? "E" : k.length === 1 ? k.toUpperCase() : "";
    if (k === "Pause") { run("pause"); return; }
    if (!c) return;
    buf = (buf + c).slice(-12);
    for (var s in CODES) if (buf.slice(-s.length) === s) { buf = ""; run(CODES[s]); break; }
  });

  // ---------- телефон: чит-консоль (7 нажатий на логотип) и свайпы ----------
  var lt = [];
  $$(".top .brand").forEach(function (a) {
    a.addEventListener("click", function (e) {
      lt.push(Date.now()); lt = lt.filter(function (x) { return Date.now() - x < 3000; });
      if (lt.length >= 3) e.preventDefault();
      if (lt.length >= 7) { lt = []; console7(); }
    });
  });
  function console7() {
    unlock("cheat_console");
    var o = fx('<div>&gt; ' + t("js.cheat.enter", "ВВЕДИ КОД:") + '</div><input autocapitalize="characters" autocomplete="off" spellcheck="false" maxlength="12">', "", 600000);
    o.className = "cheat"; o.style.pointerEvents = "auto"; var i = o.querySelector("input"); i.focus();
    i.addEventListener("keydown", function (e) {
      if (e.key === "Escape") o.remove();
      if (e.key !== "Enter") return;
      var v = i.value.toUpperCase().replace(/\s/g, "").replace("↑↑↓↓←→←→BA", "UUDDLRLRBA");
      o.remove(); var id = CODES[v] || (v === "KONAMI" ? "konami" : v === "HADOKEN" ? "hadoken" : null);
      if (id) run(id); else toast(t("js.cheat.no", "Нет такого кода"), "✕", ">_");
    });
    setTimeout(function () { doc.addEventListener("pointerdown", function off(e) { if (!o.contains(e.target)) { o.remove(); doc.removeEventListener("pointerdown", off); } }); }, 50);
  }
  var sw = "", sx = 0, sy = 0, st = 0;
  addEventListener("touchstart", function (e) { var p = e.touches[0]; sx = p.clientX; sy = p.clientY; st = Date.now(); }, { passive: true });
  addEventListener("touchend", function (e) {
    var p = e.changedTouches[0], dx = p.clientX - sx, dy = p.clientY - sy; if (Date.now() - st > 700) return;
    var d = Math.max(Math.abs(dx), Math.abs(dy)) < 30 ? "A" : Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "R" : "L") : (dy < 0 ? "U" : "D");
    sw = (sw + d).slice(-10);
    if (/UUDDLRLR$/.test(sw)) { sw = ""; run("konami"); } else if (/UDLRA$/.test(sw)) { sw = ""; run("levelselect"); } else if (/DRA$/.test(sw) && B()) { sw = ""; run("hadoken"); }
  }, { passive: true });

  // ---------- консоль разработчика ----------
  try {
    console.log("%c  ▄▄▄▄\n ▐ ◕ ◕ ▌   Сейвик: «Ищешь секреты? Набери kapsula.secret()»\n  ▀▄▄▀", "color:#f4b26b;font:700 14px monospace");
  } catch (e) {}
  W.kapsula = {
    secret: function () { unlock("hacker"); return t("js.hacker", "Ты в деле. Подсказка: XYZZY, IDDQD и стрелочки, о которых знают все."); },
    dance: function () { run("konami"); return "♪"; }, sleep: function () { run("sleep"); return "zZz"; }, party: function () { confetti(); return "🎉"; },
  };

  // ---------- посещения, серии, время, сезоны ----------
  var visits = store.get("visits", 0) + 1; store.set("visits", visits);
  var pages = store.get("pages", {}); pages[page] = 1; store.set("pages", pages);
  unlock("first");
  if (Object.keys(pages).length >= 5) unlock("curious");
  if (["index", "consoles", "tools", "faq", "news", "secrets", "game", "check", "pad", "phone", "quiz", "shelf"].every(function (p) { return pages[p]; })) unlock("explorer");
  if (visits >= 100) unlock("hundred");
  var now = new Date(), day = now.toISOString().slice(0, 10), last = store.get("day", null), streak = store.get("streak", 0);
  if (last !== day) { var y = new Date(now - 864e5).toISOString().slice(0, 10); streak = last === y ? streak + 1 : 1; store.set("streak", streak); store.set("day", day); }
  if (streak >= 3) unlock("streak3"); if (streak >= 7) unlock("streak7");
  var hh = now.getHours(), mm = now.getMinutes(), dow = now.getDay(), md = (now.getMonth() + 1) * 100 + now.getDate();
  if (hh < 5) unlock("night_owl"); if (hh >= 5 && hh < 7) unlock("early_bird");
  function clock() { var d = new Date(); if (d.getHours() === 0 && d.getMinutes() === 0) { unlock("midnight"); confetti(); } if (d.getHours() === 13 && d.getMinutes() === 37) unlock("leet"); }
  clock(); setInterval(clock, 30000);
  setTimeout(function () {
    var b = B();
    if (md >= 1220 || md <= 110) { unlock("seasonal"); snow(); }
    if (md >= 1025 && md <= 1101) { unlock("seasonal"); rain(["bad-apple"], 6, 4000); }
    if (md === 1114) { unlock("seasonal"); confetti(); b && b.say(t("js.bday")); }
    if (md === 401) { unlock("april"); if (b) { b.el.style.transform = "scaleY(-1)"; b.say(t("js.april")); } }
    if (dow === 5 && hh >= 18) { unlock("friday"); b && b.act("mug", 2500, t("js.friday")); }
    if (dow === 1 && hh >= 6 && hh < 12) { unlock("monday"); b && b.act("mug", 2500, t("js.monday")); }
  }, 6500);
  function snow() { if (still) return; for (var i = 0; i < 40; i++) (function (i) { var s = 4 + Math.random() * 6; var e = fx("", "left:" + Math.random() * innerWidth + "px;top:-10px;width:" + s + "px;height:" + s + "px;border-radius:50%;background:#fff;opacity:.8;transition:transform " + (6 + Math.random() * 6) + "s linear", 13000); setTimeout(function () { e.style.transform = "translate(" + (Math.random() * 80 - 40) + "px," + (innerHeight + 20) + "px)"; }, i * 200); })(i); }

  // ---------- поведение страницы ----------
  var title = doc.title;
  doc.addEventListener("visibilitychange", function () {
    if (doc.hidden) doc.title = t("js.miss", "Сейвик скучает…");
    else { doc.title = t("js.back", "Ура, ты вернулся!"); unlock("miss_you"); setTimeout(function () { doc.title = title; }, 2500); }
  });
  addEventListener("offline", function () { unlock("offline"); var b = B(); if (b) { b.busy = true; b.set("sad"); b.say(t("js.offline"), 600000); } else toast(t("js.offline"), "🔌", t("js.seivik")); });
  addEventListener("online", function () { var b = B(); if (b) { b.busy = false; b.act("jump", 1200, t("js.online")); } });
  addEventListener("resize", function () { if (innerWidth < 340) { unlock("squeeze"); var b = B(); if (b) { b.el.style.transform = "scaleX(.7)"; b.say(t("js.squeeze")); setTimeout(function () { b.el.style.transform = ""; }, 2500); } } });
  doc.addEventListener("selectionchange", function () {
    var s = String(W.getSelection && W.getSelection() || "").trim(); if (s.length < 4 || s.length > 140) return;
    clearTimeout(W.__sel); W.__sel = setTimeout(function () { unlock("reader"); var b = B(); b && b.say("«" + s.slice(0, 60) + "»… " + t("js.reader"), 3500); }, 700);
  });
  doc.addEventListener("copy", function () { unlock("copycat"); toast(t("js.copy"), "📋", t("js.seivik")); });
  addEventListener("beforeprint", function () { unlock("printer"); });
  try { matchMedia("(prefers-color-scheme: dark)").addEventListener("change", function () { unlock("theme_flip"); var b = B(); b && b.act("blink", 1500, t("js.theme")); }); } catch (e) {}
  var idle; function wake() { clearTimeout(idle); idle = setTimeout(function () { unlock("sleepy"); var b = B(); if (b) { b.dozed = true; b.base = "sleep"; b.set("sleep"); b.say("Zzz…", 6000); } }, 120000); var b = B(); if (b && b.dozed && !b.busy) { b.dozed = false; b.base = "idle"; b.act("stretch", 1300, t("js.woke")); } }
  ["pointermove", "keydown", "scroll", "touchstart"].forEach(function (ev) { addEventListener(ev, wake, { passive: true }); }); wake();
  var stay = setTimeout(function () { unlock("long_stay"); var b = B(); b ? b.act("pad", 2500, t("js.stay")) : toast(t("js.stay"), "⏰", t("js.seivik")); }, 30 * 60000);
  if (/[?&]debug/.test(location.search)) { unlock("debug"); var o = overlay('<pre style="font:700 15px/1.6 ui-monospace,monospace;color:#c8c8c8;white-space:pre-wrap">KAPSULA BIOS v0.50  (C) Seivik Megatrends\n\nCPU: Retro Core @ 60 FPS ......... OK\nMemory test: 640 KB ............... OK (хватит всем)\nCartridge slot .................... пыльно, подуйте\nMascot ............................ Сейвик (дышит)\n\nPress DEL to enter SETUP</pre>', "background:#0000a8;place-items:start;padding:40px;pointer-events:auto", 6000); o.addEventListener("click", function () { o.remove(); }); }
  if (location.hash.toLowerCase() === "#seivik") { unlock("hash_seivik"); walk(); }
  function walk() { if (still) return; var e = fx('<img src="/img/mascot/run.webp" style="width:110px">', "left:-120px;bottom:20px;transition:transform 6s linear", 6500); requestAnimationFrame(function () { e.style.transform = "translateX(" + (innerWidth + 240) + "px)"; }); }

  // ---------- маскот главной: щекотка, прятки, бросок, круг, тряска, переворот ----------
  var b0 = B();
  if (b0) {
    var el = b0.el, taps = [];
    el.addEventListener("click", function () {
      if (el.__dragged) { el.__dragged = false; return; }
      taps.push(Date.now()); taps = taps.filter(function (x) { return Date.now() - x < 5000; });
      if (taps.length === 10) { unlock("tickle"); b0.act("laugh", 1600, t("js.tickle")); return; }
      if (taps.length >= 20) { taps = []; unlock("hider"); b0.busy = true; b0.set("hide"); b0.say(t("js.hide"), 9000); setTimeout(function () { b0.busy = false; b0.act("wave", 1200, t("js.peek")); }, 10000); return; }
      if (!b0.busy) b0.act("laugh", 900);
    });
    // схватить и бросить
    var drag = null;
    el.addEventListener("pointerdown", function (e) { drag = { x: e.clientX, y: e.clientY, t: Date.now(), vx: 0, vy: 0, dx: 0, dy: 0, moved: false }; el.setPointerCapture(e.pointerId); });
    el.addEventListener("pointermove", function (e) {
      if (!drag) return; var nx = e.clientX - drag.x, ny = e.clientY - drag.y, dt = Math.max(1, Date.now() - drag.t);
      drag.vx = (nx - drag.dx) / dt * 16; drag.vy = (ny - drag.dy) / dt * 16; drag.dx = nx; drag.dy = ny; drag.t = Date.now();
      if (Math.abs(nx) + Math.abs(ny) > 14) { drag.moved = true; el.style.transform = "translate(" + nx + "px," + ny + "px)"; if (!b0.busy) b0.set("wow"); }
    });
    el.addEventListener("pointerup", function () {
      if (!drag) return; var d = drag; drag = null; if (!d.moved) return; el.__dragged = true;
      var x = d.dx, y = d.dy, vx = d.vx, vy = d.vy, hits = 0, r0 = el.getBoundingClientRect();
      (function fly() {
        vy += .9; x += vx; y += vy; vx *= .99;
        var r = { l: r0.left - d.dx + x, t: r0.top - d.dy + y }, w = r0.width, h = r0.height;
        if (r.l < 0 || r.l + w > innerWidth) { vx = -vx * .7; x += r.l < 0 ? -r.l : innerWidth - (r.l + w); hits++; }
        if (r.t + h > innerHeight) { vy = -vy * .6; y -= r.t + h - innerHeight; hits++; if (Math.abs(vy) < 2) vy = 0; }
        if (r.t < 0) { vy = Math.abs(vy) * .6; y -= r.t; }
        el.style.transform = "translate(" + x + "px," + y + "px)";
        if ((Math.abs(vx) + Math.abs(vy) > 1.2 || r.t + h < innerHeight - 2) && hits < 14) requestAnimationFrame(fly);
        else { unlock("thrower"); b0.act(hits > 3 ? "dizzy" : "jump", 1600, hits > 3 ? t("js.dizzy") : t("js.again")); el.style.transition = "transform .6s cubic-bezier(.3,1.4,.5,1)"; el.style.transform = ""; setTimeout(function () { el.style.transition = ""; }, 700); }
      })();
    });
    // обвести кругом (мышь/палец вокруг маскота, не хватая)
    var ang = 0, la = null, lt0 = 0;
    addEventListener("pointermove", function (e) {
      if (drag) return; var r = el.getBoundingClientRect(), cx = r.left + r.width / 2, cy = r.top + r.height / 2, dx = e.clientX - cx, dy = e.clientY - cy, dist = Math.hypot(dx, dy);
      if (dist < r.width * .4 || dist > r.width * 1.3) { ang = 0; la = null; return; }
      var a = Math.atan2(dy, dx); if (la != null) { var d = a - la; if (d > Math.PI) d -= 2 * Math.PI; if (d < -Math.PI) d += 2 * Math.PI; ang += d; }
      la = a; if (Date.now() - lt0 > 2500) { ang = 0; lt0 = Date.now(); }
      if (Math.abs(ang) > 5.8) { ang = 0; unlock("spinner"); el.animate([{ transform: "rotate(0)" }, { transform: "rotate(" + (ang > 0 ? 360 : -360) + "deg)" }], { duration: 800, easing: "ease-in-out" }); b0.say(t("js.spin"), 1500); }
    }, { passive: true });
    // потрясти / перевернуть телефон
    var shakes = 0, sht = 0;
    addEventListener("devicemotion", function (e) {
      var a = e.accelerationIncludingGravity; if (!a) return; var m = Math.abs(a.x) + Math.abs(a.y) + Math.abs(a.z);
      if (m > 38) { if (Date.now() - sht > 2000) shakes = 0; sht = Date.now(); if (++shakes > 5) { shakes = 0; unlock("shaker"); b0.act("dizzy", 2000, t("js.shake")); rain(["cart-red", "cart-blue", "disc", "coin"], 10, 1800); } }
    });
    addEventListener("deviceorientation", function (e) { if (e.beta != null && Math.abs(e.beta) > 150) { unlock("upside"); b0.el.style.transform = "scaleY(-1)"; b0.say(t("js.upside"), 2000); setTimeout(function () { b0.el.style.transform = ""; }, 2500); } });
  }

  // ---------- главная: лента приставок — прототип «???» и 8-бит мелодии ----------
  var ribbon = $(".cons");
  if (ribbon) {
    var proto = doc.createElement("a"); proto.href = "#"; proto.innerHTML = '<b style="font:700 20px var(--head);color:var(--ink3)">???</b>'; ribbon.appendChild(proto);
    proto.addEventListener("click", function (e) { e.preventDefault(); unlock("proto"); var b = B(); if (b) { b.act("hide", 2500, t("js.proto")); } });
    var cl = {}; $$("a[data-id]", ribbon).forEach(function (a, i) {
      a.addEventListener("click", function (e) {
        var id = a.getAttribute("data-id"); cl[id] = (cl[id] || []).concat(Date.now()).filter(function (x) { return Date.now() - x < 3000; });
        if (cl[id].length >= 2) e.preventDefault();
        if (cl[id].length >= 5) { cl[id] = []; unlock("chiptune"); var base = 262 * Math.pow(2, (i % 7) / 12); beep([0, 4, 7, 12, 7, 4, 9, 12].map(function (s) { return base * Math.pow(2, s / 12); }), .11); B() && B().act("dance2", 1200); }
      });
    });
  }
  // двойное нажатие на заголовок — буквы прыгают
  var h1 = $("h1");
  if (h1) h1.addEventListener("dblclick", function () {
    unlock("title_jump"); if (still) return; var txt = h1.innerHTML;
    var spans = h1.textContent.split("").map(function (c, i) { return '<span style="display:inline-block;animation:hop .5s ' + (i * 0.03) + 's">' + (c === " " ? "&nbsp;" : c) + "</span>"; }).join("");
    h1.innerHTML = spans; setTimeout(function () { h1.innerHTML = txt; }, 1500);
  });
  // версия: 5 нажатий — шуточный список изменений
  var vt = []; $$(".js-ver").forEach(function (v) { v.style.cursor = "default"; v.addEventListener("click", function () { vt.push(Date.now()); vt = vt.filter(function (x) { return Date.now() - x < 3000; }); if (vt.length >= 5) { vt = []; unlock("version5"); toast(t("js.ver5"), "🔢", "Changelog"); } }); });
  // флаги: навести на все
  var fl = {}; $$(".langs a").forEach(function (a) { a.addEventListener("pointerenter", function () { fl[a.getAttribute("data-l")] = 1; if (Object.keys(fl).length >= $$(".langs a").length) unlock("flag_hover"); }); });
  // «Скачать»: подержать 3 секунды
  $$(".js-apk").forEach(function (a) { var ht; a.addEventListener("pointerdown", function () { ht = setTimeout(function () { unlock("patient"); toast(t("js.patient"), "⏳", t("js.seivik")); }, 3000); }); ["pointerup", "pointerleave"].forEach(function (ev) { a.addEventListener(ev, function () { clearTimeout(ht); }); }); });
  // «Подуть в картридж» в окне загрузки
  var blow = $(".js-blow"); if (blow) blow.addEventListener("click", function () { unlock("blower"); blow.textContent = "💨 ffffff…"; setTimeout(function () { blow.textContent = t("js.blown"); }, 1200); });
  // подвал: логотип 3 раза — маскот выглядывает
  var ft = []; $$(".foot .brand").forEach(function (a) { a.addEventListener("click", function (e) { e.preventDefault(); ft.push(Date.now()); ft = ft.filter(function (x) { return Date.now() - x < 2500; }); if (ft.length >= 3) { ft = []; unlock("footer_logo"); peek(); } }); });
  function peek() { var e = fx('<img src="/img/mascot/wave.webp" style="width:140px">', "right:30px;bottom:-160px;transition:transform .6s cubic-bezier(.3,1.4,.5,1)", 4000); requestAnimationFrame(function () { e.style.transform = "translateY(-150px)"; }); setTimeout(function () { e.style.transform = ""; }, 3000); }
  // вопросы: открыть все; прокрутить вниз-вверх 3 раза
  var dets = $$("details"); if (page === "faq" && dets.length) {
    dets.forEach(function (d) { d.addEventListener("toggle", function () { if (dets.every(function (x) { return x.open || x.__was; }) || dets.every(function (x) { return x.open; })) unlock("why"); if (d.open) d.__was = true; }); });
    var trips = 0, atBottom = false; addEventListener("scroll", function () { var bot = innerHeight + scrollY >= doc.documentElement.scrollHeight - 4; if (bot && !atBottom) { atBottom = true; } if (scrollY < 4 && atBottom) { atBottom = false; if (++trips >= 3) unlock("scroll_faq"); } }, { passive: true });
  }
  // 404 три раза
  if (page === "404" || doc.body.classList.contains("is-404")) { var n4 = store.get("lost", 0) + 1; store.set("lost", n4); if (n4 >= 3) unlock("lost3"); }

  // ---------- охота за картриджами ----------
  function spawnCart(id, host) {
    if (carts[id]) return;
    var e = doc.createElement("button"); e.type = "button"; e.className = "egg-cart"; e.setAttribute("aria-label", "?");
    if (host) { host.style.position = host.style.position || "relative"; host.appendChild(e); } else { e.style.position = "fixed"; e.style.left = "50%"; e.style.top = "40%"; body.appendChild(e); }
    e.addEventListener("click", function (ev) {
      ev.preventDefault(); ev.stopPropagation(); carts[id] = Date.now(); store.set("carts", carts); e.remove();
      var n = CARTS.filter(function (c) { return carts[c]; }).length; toast(t("js.cart", "Картридж найден") + " " + n + "/7", "🧩", t("js.hunt", "Охота")); beep([660, 880], .08);
      if (n >= 7) unlock("hunter"); W.dispatchEvent(new Event("kegg"));
    });
  }
  $$("[data-egg]").forEach(function (h) { spawnCart(h.getAttribute("data-egg"), h); });
  W.kSpawnCart = spawnCart;
  // печать — раскраска с маскотом
  if (!$(".print-page")) { var pp = doc.createElement("div"); pp.className = "print-page"; pp.innerHTML = '<h1 style="font-family:sans-serif">KAPSULA — ' + t("js.color", "раскраска") + '</h1><img src="/img/mascot/wave.webp" alt=""><p>kapsula.gitverse.site</p>'; body.appendChild(pp); }
})();
