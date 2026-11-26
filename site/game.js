/* «Поймай картриджи» — секретная мини-игра сайта KAPSULA (5 касаний маскота, страница «не найдено»).
   Сейвик ловит рюкзаком хорошее и уворачивается от плохого. 45 секунд, 3 жизни, рекорд — в браузере. */
(function () {
  "use strict";
  var doc = document;
  var GOOD = [["cart-red", 1], ["cart-blue", 1], ["cart-yellow", 1], ["cart-purple", 1], ["cart-grey", 1], ["card", 2], ["disc", 2], ["disc-case", 2], ["umd", 2], ["coin", 3], ["star", 5]];
  var BAD = ["bad-disc", "bad-bug", "bad-cable", "bad-cloud", "bad-bomb", "bad-apple"];
  var POWER = ["heart", "magnet", "hourglass", "lamp", "clock"];
  var ALL = GOOD.map(function (g) { return g[0]; }).concat(BAD, POWER, ["spark", "puff"]);
  var IMG = {}, POSE = {};
  ALL.forEach(function (n) { var i = new Image(); i.src = "/img/items/" + n + ".webp"; IMG[n] = i; });
  ["run", "catch", "dizzy", "jump", "idle"].forEach(function (n) { var i = new Image(); i.src = "/img/mascot/" + n + ".webp"; POSE[n] = i; });

  var W = 720, H = 460, el, cv, cx, ui, raf = 0, st;
  function best() { try { return +localStorage.getItem("kapsula-game-best") || 0; } catch (e) { return 0; } }
  function setBest(v) { try { localStorage.setItem("kapsula-game-best", v); } catch (e) {} }

  function build() {
    var host = doc.querySelector(".js-game-host"); el = doc.createElement("div"); el.className = "game" + (host ? " inline" : ""); el.setAttribute("role", "dialog"); el.setAttribute("aria-label", t("game.h1"));
    el.innerHTML = '<div class="game-box"><button class="x" type="button" aria-label="' + t("get.close") + '">×</button><canvas width="720" height="460"></canvas><div class="game-ui"></div></div>';
    (host || doc.body).appendChild(el);
    cv = el.querySelector("canvas"); cx = cv.getContext("2d"); ui = el.querySelector(".game-ui");
    // телефон вертикально — поле выше (во всю высоту экрана), чтобы и правила, и игра помещались
    if (innerHeight > innerWidth && innerWidth < 760) { W = 480; H = Math.round(Math.min(1000, Math.max(460, 480 * innerHeight * .78 / Math.min(innerWidth, 760)))); cv.setAttribute("width", W); }
    cv.setAttribute("height", H);
    var r = Math.min(window.devicePixelRatio || 1, 2); cv.width = W * r; cv.height = H * r; cx.setTransform(r, 0, 0, r, 0, 0);
    el.querySelector(".x").addEventListener("click", close);
    el.addEventListener("click", function (e) { if (e.target === el) close(); });
    var move = function (e) { if (!st) return; var b = cv.getBoundingClientRect(); st.tx = (e.clientX - b.left) / b.width * W; };
    cv.addEventListener("pointermove", move); cv.addEventListener("pointerdown", move);
    doc.addEventListener("keydown", key); doc.addEventListener("keyup", key);
  }
  function key(e) {
    if (!el || el.hidden || !st) return;
    var on = e.type === "keydown";
    if (e.key === "ArrowLeft" || e.key === "a") st.kl = on;
    else if (e.key === "ArrowRight" || e.key === "d") st.kr = on;
    else if (e.key === "Escape" && on) close();
    else return;
    e.preventDefault();
  }
  function menu(html, buttons) {
    ui.hidden = false; ui.innerHTML = "<div>" + html + '<div class="row"></div></div>';
    var row = ui.querySelector(".row");
    buttons.forEach(function (b) { var x = doc.createElement("button"); x.className = "btn " + (b[2] || ""); x.type = "button"; x.textContent = b[0]; x.addEventListener("click", b[1]); row.appendChild(x); });
    row.firstChild && row.firstChild.focus();
  }
  function intro() {
    var ic = function (n) { return '<img src="/img/items/' + n + '.webp" alt="">'; };
    menu("<b>" + t("game.h1") + "</b><p>" + t("gm.rules") + "</p>" +
      '<div class="legend">' + ic("cart-red") + ic("disc") + ic("coin") + ic("star") + " — " + t("gm.pts") + " · " + ic("bad-bomb") + ic("bad-bug") + " — " + t("gm.minus") + "</div>" +
      '<div class="legend">' + ic("magnet") + " " + t("gm.magnet") + " · " + ic("hourglass") + " " + t("gm.slow") + " · " + ic("lamp") + " ×2 · " + ic("clock") + " +5 " + t("u.s") + " · " + ic("heart") + " " + t("gm.life") + "</div>" +
      "<p>" + t("gm.best") + ": " + best() + "</p>", [[t("gm.play"), start]]);
  }
  function start() {
    ui.hidden = true;
    st = { x: W / 2, tx: W / 2, kl: false, kr: false, face: 1, items: [], fx: [], score: 0, lives: 3, time: 45, t0: 0, last: 0, spawn: 0, pose: "run", poseT: 0,
      magnet: 0, slow: 0, double: 0, hurt: 0 };
    var K = window.kstore; if (K && K.get("lives30", 0)) { K.set("lives30", 0); st.lives = 30; } st.god = K && Date.now() - K.get("god", 0) < 60000;
    if (window.kEgg) kEgg.unlock("gamer");
    cancelAnimationFrame(raf); raf = requestAnimationFrame(loop);
  }
  function spawn(dt) {
    var hard = 1 + (45 - st.time) / 30;
    st.spawn -= dt;
    if (st.spawn > 0) return;
    st.spawn = Math.max(.32, .85 - (45 - st.time) * .012);
    var r = Math.random(), kind, name, pts = 0;
    if (r < .2 + (45 - st.time) * .004) { kind = "bad"; name = BAD[Math.random() * BAD.length | 0]; }
    else if (r < .27) { kind = "power"; name = POWER[Math.random() * POWER.length | 0]; if (name === "heart" && st.lives >= 3) name = "clock"; }
    else { kind = "good"; var g = GOOD[Math.random() * GOOD.length | 0]; name = g[0]; pts = g[1]; }
    st.items.push({ x: 40 + Math.random() * (W - 80), y: -40, v: (120 + Math.random() * 90) * hard, rot: (Math.random() - .5) * 2, a: 0, kind: kind, name: name, pts: pts, s: kind === "bad" ? 50 : 46 });
  }
  function fx(name, x, y, text) { st.fx.push({ name: name, x: x, y: y, t: 0, text: text }); }
  function loop(now) {
    if (!st) return;
    if (!st.last) st.last = now;
    var dt = Math.min(.05, (now - st.last) / 1000); st.last = now;
    var slow = st.slow > 0 ? .5 : 1;
    st.time -= dt; st.magnet -= dt; st.slow -= dt; st.double -= dt; st.hurt -= dt; st.poseT -= dt;
    // движение Сейвика: за пальцем или стрелками
    if (st.kl) st.tx = st.x - 400 * dt; if (st.kr) st.tx = st.x + 400 * dt;
    st.tx = Math.max(50, Math.min(W - 50, st.tx));
    var dx = st.tx - st.x; if (Math.abs(dx) > 1) st.face = dx < 0 ? -1 : 1;
    st.x += dx * Math.min(1, dt * (st.hurt > 0 ? 4 : 12));
    spawn(dt);
    var py = H - 70;
    for (var i = st.items.length - 1; i >= 0; i--) {
      var it = st.items[i];
      if (st.magnet > 0 && it.kind !== "bad") it.x += (st.x - it.x) * Math.min(1, dt * 3);
      it.y += it.v * dt * slow; it.a += it.rot * dt;
      // поймал рюкзаком (верх головы Сейвика)
      if (Math.abs(it.x - st.x) < 46 && it.y > py - 90 && it.y < py - 20) {
        st.items.splice(i, 1);
        if (it.kind === "bad") {
          if (st.hurt <= 0 && !st.god) { st.lives--; st.hurt = .9; st.pose = "dizzy"; st.poseT = .9; fx("puff", it.x, it.y, "−♥"); if (navigator.vibrate) navigator.vibrate(60); }
        } else if (it.kind === "power") {
          if (it.name === "heart") { st.lives = Math.max(st.lives, Math.min(3, st.lives + 1)); fx("spark", it.x, it.y, "+♥"); }
          if (it.name === "magnet") { st.magnet = 6; fx("spark", it.x, it.y, t("gm.magnet") + "!"); }
          if (it.name === "hourglass") { st.slow = 5; fx("spark", it.x, it.y, t("gm.slow") + "…"); }
          if (it.name === "lamp") { st.double = 6; fx("spark", it.x, it.y, "×2"); }
          if (it.name === "clock") { st.time += 5; fx("spark", it.x, it.y, "+5 " + t("u.s")); }
          st.pose = "jump"; st.poseT = .4;
        } else {
          var p = it.pts * (st.double > 0 ? 2 : 1); st.score += p; fx("spark", it.x, it.y, "+" + p);
          if (st.hurt <= 0) { st.pose = "catch"; st.poseT = .25; }
        }
        continue;
      }
      if (it.y > H + 30) { st.items.splice(i, 1); if (it.kind === "good") fx("puff", it.x, H - 16); }
    }
    for (i = st.fx.length - 1; i >= 0; i--) { st.fx[i].t += dt; if (st.fx[i].t > .7) st.fx.splice(i, 1); }
    if (st.poseT <= 0) st.pose = "run";
    draw(now);
    if (st.lives <= 0 || st.time <= 0) return over();
    raf = requestAnimationFrame(loop);
  }
  function draw(now) {
    var g = cx.createLinearGradient(0, 0, 0, H); g.addColorStop(0, "#2a2338"); g.addColorStop(1, "#18141f");
    cx.fillStyle = g; cx.fillRect(0, 0, W, H);
    // лампа и пол
    var lg = cx.createRadialGradient(80, -20, 0, 80, -20, 420); lg.addColorStop(0, "rgba(255,184,102,.22)"); lg.addColorStop(1, "rgba(255,184,102,0)");
    cx.fillStyle = lg; cx.fillRect(0, 0, W, H);
    cx.fillStyle = "#211a2b"; cx.fillRect(0, H - 22, W, 22);
    cx.fillStyle = "rgba(242,166,90,.25)"; cx.fillRect(0, H - 22, W, 2);
    // предметы
    st.items.forEach(function (it) {
      var im = IMG[it.name]; if (!im.complete) return;
      cx.save(); cx.translate(it.x, it.y); cx.rotate(it.a);
      if (it.kind === "power") { cx.globalAlpha = .35 + .2 * Math.sin(now / 150); cx.fillStyle = "#ffd27a"; cx.beginPath(); cx.arc(0, 0, it.s * .62, 0, 6.28); cx.fill(); cx.globalAlpha = 1; }
      cx.drawImage(im, -it.s / 2, -it.s / 2, it.s, it.s); cx.restore();
    });
    // Сейвик
    var im = POSE[st.pose] || POSE.run, sw = 118, sh = sw * 311 / 321, bob = st.pose === "run" ? Math.abs(Math.sin(now / 90)) * 4 : 0;
    cx.save(); cx.translate(st.x, H - 20 - bob);
    if (st.face < 0) cx.scale(-1, 1);
    if (st.hurt > 0 && (now / 80 | 0) % 2) cx.globalAlpha = .5;
    if (st.magnet > 0) { cx.strokeStyle = "rgba(255,120,110,.5)"; cx.lineWidth = 3; cx.beginPath(); cx.arc(0, -sh * .6, 70 + Math.sin(now / 120) * 5, 0, 6.28); cx.stroke(); }
    if (im.complete) cx.drawImage(im, -sw / 2, -sh, sw, sh);
    cx.restore();
    // вспышки и подписи
    st.fx.forEach(function (f) {
      var k = f.t / .7, im2 = IMG[f.name];
      cx.globalAlpha = 1 - k;
      if (im2 && im2.complete) { var s = 30 + k * 30; cx.drawImage(im2, f.x - s / 2, f.y - s / 2 - k * 20, s, s); }
      if (f.text) { cx.font = "700 20px 'Balsamiq Sans', sans-serif"; cx.textAlign = "center"; cx.fillStyle = f.text[0] === "−" ? "#ff8f8f" : "#ffd27a"; cx.fillText(f.text, f.x, f.y - 26 - k * 30); }
      cx.globalAlpha = 1;
    });
    // табло
    cx.font = "700 22px 'Balsamiq Sans', sans-serif"; cx.textAlign = "left"; cx.fillStyle = "#f4eee4";
    cx.fillText(t("gm.score") + ": " + st.score, 18, 34);
    cx.textAlign = "center"; cx.fillStyle = st.time < 10 ? "#ffb070" : "#f4eee4"; cx.fillText(Math.max(0, Math.ceil(st.time)) + " " + t("u.s"), W / 2, 34);
    if (st.lives > 3) { cx.textAlign = "right"; cx.fillText("×" + st.lives, W - 100, 34); }
    for (var i = 0; i < 3; i++) { cx.globalAlpha = i < st.lives ? 1 : .2; if (IMG.heart.complete) cx.drawImage(IMG.heart, W - 92 - i * 32, 14, 28, 28); }
    cx.globalAlpha = 1;
    var tags = []; if (st.magnet > 0) tags.push(t("gm.magnet")); if (st.slow > 0) tags.push(t("gm.slow")); if (st.double > 0) tags.push("×2");
    if (tags.length) { cx.font = "700 15px 'Balsamiq Sans', sans-serif"; cx.textAlign = "left"; cx.fillStyle = "#f2a65a"; cx.fillText(tags.join(" · "), 18, 58); }
  }
  function over() {
    cancelAnimationFrame(raf);
    var b = best(), rec = st.score > b; if (rec) setBest(st.score);
    var s = st.score, died = st.lives <= 0; st = null;
    if (window.kEgg) { if (s >= 100) kEgg.unlock("legend"); var L5 = died ? kstore.get("lose", 0) + 1 : 0; kstore.set("lose", L5); if (L5 >= 5) { kstore.set("lose", 0); kEgg.unlock("loser5"); kEgg.toast(t("gm.hug"), "🫂", t("js.seivik")); } }
    menu("<b>" + (rec ? t("gm.rec") : t("gm.over")) + "</b><p>" + t("gm.score") + ": " + s + (rec ? "" : " · " + t("gm.best") + ": " + b) + "</p>" +
      "<p>" + (s >= 120 ? t("gm.v3") : s >= 60 ? t("gm.v2") : t("gm.v1")) + "</p>",
      [[t("qz.again"), start]].concat(doc.querySelector(".js-game-host") ? [] : [[t("get.close"), close, "btn-soft"]]));
  }
  function open() {
    if (!el) build();
    el.hidden = false; if (!el.classList.contains("inline")) doc.body.style.overflow = "hidden"; intro();
  }
  function close() { cancelAnimationFrame(raf); st = null; if (el) el.hidden = true; doc.body.style.overflow = ""; }
  window.KapsulaGame = { open: open, close: close };
  if (doc.querySelector(".js-game-host")) open();
})();
