/* KAPSULA — небо с параллаксом, живой маскот, витрина, загрузка с прогрессом, установка, пасхалки.
   Без библиотек и счётчиков. Анимации останавливаются, когда вкладка скрыта; «уменьшить движение» — спокойный режим. */
(function () {
  "use strict";
  var doc = document, root = doc.documentElement, win = window;
  var still = win.matchMedia && matchMedia("(prefers-reduced-motion: reduce)").matches;
  var finePointer = win.matchMedia && matchMedia("(pointer: fine)").matches;
  var small = innerWidth < 720;
  var $ = function (s, r) { return (r || doc).querySelector(s); };
  var $$ = function (s, r) { return Array.prototype.slice.call((r || doc).querySelectorAll(s)); };
  var ua = navigator.userAgent || "";
  if (/iPhone|iPad|iPod/.test(ua) || (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1)) root.classList.add("is-ios");
  var P = function (n) { return "img/mascot/" + n + ".webp"; };

  // ---------- версия и ссылка на APK (latest.json кладёт сборка сайта) ----------
  var apk = { url: null, ver: null, mb: 0 };
  // описание версии; кнопка «Скачать» ждёт его (раньше нажатие до загрузки уводило на страницу выпусков GitVerse)
  var apkReady = fetch("latest.json?t=" + Date.now(), { cache: "no-store" }).then(function (r) { return r.ok ? r.json() : null; }).then(function (d) {
    if (!d || !d.url) return;
    apk = d;
    $$(".js-apk, .js-direct").forEach(function (a) { a.href = d.url; });
    var v = $(".js-ver"); if (v && d.ver) v.textContent = "Версия " + d.ver + (d.mb ? " · " + d.mb + " МБ" : "");
  }).catch(function () {});

  // ---------- курсор / наклон телефона → общий сдвиг (лампа, маскот) ----------
  var mx = 0, my = 0, tx = 0, ty = 0;
  if (!still) {
    addEventListener("pointermove", function (e) { if (e.pointerType === "mouse") { tx = e.clientX / innerWidth * 2 - 1; ty = e.clientY / innerHeight * 2 - 1; } }, { passive: true });
    addEventListener("deviceorientation", function (e) { if (e.gamma == null) return; tx = Math.max(-1, Math.min(1, e.gamma / 30)); ty = Math.max(-1, Math.min(1, (e.beta - 40) / 30)); }, { passive: true });
  }

  // ---------- небо: звёзды (два слоя), огоньки, пылинки в свете лампы, падающие звёзды ----------
  var sky = $(".sky"), cx = sky && sky.getContext("2d");
  var W = 0, H = 0, dpr = 1, stars = [], bokeh = [], dust = [], shoot = null, nextShoot = 0, night = 0, scrollY0 = 0, dotSprite;
  function makeSprite(r, rgb) {
    var c = doc.createElement("canvas"); c.width = c.height = r * 2; var g = c.getContext("2d");
    var gr = g.createRadialGradient(r, r, 0, r, r, r); gr.addColorStop(0, "rgba(" + rgb + ",1)"); gr.addColorStop(.35, "rgba(" + rgb + ",.35)"); gr.addColorStop(1, "rgba(" + rgb + ",0)");
    g.fillStyle = gr; g.fillRect(0, 0, r * 2, r * 2); return c;
  }
  var bokehWarm = makeSprite(64, "242,166,90"), bokehCold = makeSprite(64, "120,150,220");
  dotSprite = makeSprite(8, "255,226,170");
  function seed() {
    dpr = Math.min(win.devicePixelRatio || 1, small ? 1.5 : 2);
    W = innerWidth; H = innerHeight;
    sky.width = W * dpr; sky.height = H * dpr; cx.setTransform(dpr, 0, 0, dpr, 0, 0);
    var k = small ? .5 : 1, i;
    stars = []; for (i = 0; i < 170 * k; i++) stars.push({ x: Math.random() * W, y: Math.random() * H * 1.6, r: Math.random() < .12 ? 1.6 : .5 + Math.random() * .8,
      d: Math.random() < .35 ? .12 : .05, t: Math.random() * .9, p: Math.random() * 6.28, s: .6 + Math.random() * 1.6 });
    bokeh = []; for (i = 0; i < 12 * k + 2; i++) bokeh.push({ x: Math.random() * W, y: Math.random() * H * 1.4, r: 30 + Math.random() * 70, a: .05 + Math.random() * .08,
      warm: Math.random() < .55, vx: (Math.random() - .5) * .08, vy: -.03 - Math.random() * .05 });
    dust = []; for (i = 0; i < 46 * k; i++) dust.push(newDust(true));
  }
  function newDust(any) {
    // пылинки живут в конусе света лампы (сверху слева)
    return { x: Math.random() * W * .7, y: any ? Math.random() * H * .8 : H * .85, r: .6 + Math.random() * 1.6, vx: .05 + Math.random() * .12, vy: -.05 - Math.random() * .15,
      p: Math.random() * 6.28, life: 0, max: 600 + Math.random() * 900 };
  }
  function wrap(v, m) { return ((v % m) + m) % m; }
  function frame(t) {
    if (!cx) return;
    mx += (tx - mx) * .05; my += (ty - my) * .05;
    root.style.setProperty("--mx", mx.toFixed(3)); root.style.setProperty("--my", my.toFixed(3));
    var sy = scrollY0;
    cx.clearRect(0, 0, W, H);
    // небо темнеет с прокруткой
    var g = cx.createLinearGradient(0, 0, 0, H);
    g.addColorStop(0, night < .5 ? mix("#1d2240", "#10152a", night * 2) : mix("#10152a", "#080b17", night * 2 - 1));
    g.addColorStop(1, mix("#141a30", "#06080f", night));
    cx.fillStyle = g; cx.fillRect(0, 0, W, H);
    // огоньки не в фокусе
    for (var i = 0; i < bokeh.length; i++) {
      var b = bokeh[i]; b.x += b.vx; b.y += b.vy;
      var bx = wrap(b.x + mx * 14, W + 200) - 100, by = wrap(b.y - sy * .25 + my * 10, H * 1.4) - H * .2;
      cx.globalAlpha = b.a; cx.drawImage(b.warm ? bokehWarm : bokehCold, bx - b.r, by - b.r, b.r * 2, b.r * 2);
    }
    // звёзды: появляются по мере наступления ночи, мерцают, дальние едва сдвигаются
    for (i = 0; i < stars.length; i++) {
      var s = stars[i], vis = Math.max(0, Math.min(1, (night + .35 - s.t) * 3));
      if (vis <= 0) continue;
      var a = vis * (.55 + .45 * Math.sin(t / 1000 * s.s + s.p));
      var x = wrap(s.x + mx * s.d * 60, W), y = wrap(s.y - sy * s.d + my * s.d * 40, H * 1.6);
      if (y > H) continue;
      cx.globalAlpha = a; cx.fillStyle = "#fff4dc"; cx.beginPath(); cx.arc(x, y, s.r, 0, 6.283); cx.fill();
      if (s.r > 1.4) { cx.globalAlpha = a * .35; cx.fillRect(x - 4, y - .5, 8, 1); cx.fillRect(x - .5, y - 4, 1, 8); }
    }
    // падающая звезда
    if (!shoot && t > nextShoot && night > .25) { shoot = { x: W * (.3 + Math.random() * .6), y: H * Math.random() * .35, v: 9 + Math.random() * 5, l: 0 }; nextShoot = t + 6000 + Math.random() * 9000; }
    if (shoot) {
      shoot.l += 1; var sx = shoot.x - shoot.l * shoot.v, sy2 = shoot.y + shoot.l * shoot.v * .45, fade = 1 - shoot.l / 50;
      var lg = cx.createLinearGradient(sx, sy2, sx + 90, sy2 - 40); lg.addColorStop(0, "rgba(255,240,210," + fade + ")"); lg.addColorStop(1, "rgba(255,240,210,0)");
      cx.globalAlpha = 1; cx.strokeStyle = lg; cx.lineWidth = 1.6; cx.beginPath(); cx.moveTo(sx, sy2); cx.lineTo(sx + 90, sy2 - 40); cx.stroke();
      if (shoot.l > 50) shoot = null;
    }
    // пылинки в свете лампы: ближний слой, двигается быстрее всех
    for (i = 0; i < dust.length; i++) {
      var d = dust[i]; d.life++; d.x += d.vx + Math.sin(t / 1400 + d.p) * .12; d.y += d.vy;
      if (d.life > d.max || d.y < -10 || d.x > W) dust[i] = d = newDust(false);
      var lx = d.x + mx * 24, ly = wrap(d.y - sy * .5 + my * 18, H);
      var near = Math.max(0, 1 - Math.hypot(lx - W * .12, ly) / (Math.max(W, H) * .8));
      var al = Math.min(1, d.life / 120, (d.max - d.life) / 120) * (.25 + .75 * near) * (1 - night * .6);
      cx.globalAlpha = al; cx.drawImage(dotSprite, lx - d.r * 3, ly - d.r * 3, d.r * 6, d.r * 6);
    }
    cx.globalAlpha = 1;
    if (!still) raf = requestAnimationFrame(frame);
  }
  function mix(a, b, k) {
    k = Math.max(0, Math.min(1, k));
    var pa = parseInt(a.slice(1), 16), pb = parseInt(b.slice(1), 16);
    var r = (pa >> 16) + (((pb >> 16) - (pa >> 16)) * k), g = ((pa >> 8) & 255) + ((((pb >> 8) & 255) - ((pa >> 8) & 255)) * k), bl = (pa & 255) + (((pb & 255) - (pa & 255)) * k);
    return "rgb(" + (r | 0) + "," + (g | 0) + "," + (bl | 0) + ")";
  }
  var raf = 0;
  function onScroll() {
    scrollY0 = scrollY;
    var max = Math.max(1, doc.documentElement.scrollHeight - innerHeight);
    night = Math.min(1, scrollY / max * 1.15);
    root.style.setProperty("--night", night.toFixed(3));
    drawCity();
    if (still && cx) frame(0);
  }
  if (sky && cx) {
    seed(); onScroll();
    addEventListener("resize", function () { small = innerWidth < 720; seed(); sizeCity(); onScroll(); });
    addEventListener("scroll", onScroll, { passive: true });
    doc.addEventListener("visibilitychange", function () { cancelAnimationFrame(raf); if (!doc.hidden && !still) raf = requestAnimationFrame(frame); });
    if (still) frame(0); else raf = requestAnimationFrame(frame);
  }

  // ---------- спящий город в подвале: окна загораются по мере прокрутки ----------
  var city = $(".city"), cc = city && city.getContext("2d"), blocks = [], lastLit = -1;
  function sizeCity() {
    if (!cc) return;
    var w = city.clientWidth, h = city.clientHeight, r = Math.min(win.devicePixelRatio || 1, 2);
    city.width = w * r; city.height = h * r; cc.setTransform(r, 0, 0, r, 0, 0);
    blocks = []; var x = -10, seedv = 7;
    var rnd = function () { seedv = (seedv * 16807) % 2147483647; return (seedv - 1) / 2147483646; };
    while (x < w + 10) {
      var bw = 34 + rnd() * 70, bh = 40 + rnd() * (h - 60), wins = [];
      for (var wy = h - bh + 12; wy < h - 14; wy += 16) for (var wx = x + 8; wx < x + bw - 10; wx += 14) if (rnd() < .55) wins.push([wx, wy, rnd()]);
      blocks.push({ x: x, w: bw, h: bh, roof: rnd(), wins: wins }); x += bw + 2 + rnd() * 6;
    }
    lastLit = -1; drawCity();
  }
  function drawCity() {
    if (!cc || !blocks.length) return;
    var max = Math.max(1, doc.documentElement.scrollHeight - innerHeight), p = Math.min(1, Math.max(0, (scrollY / max - .55) / .45));
    var lit = Math.round(p * 100); if (lit === lastLit) return; lastLit = lit;
    var w = city.clientWidth, h = city.clientHeight;
    cc.clearRect(0, 0, w, h);
    // дальние холмы
    cc.fillStyle = "#121730"; cc.beginPath(); cc.moveTo(0, h);
    for (var x = 0; x <= w; x += 20) cc.lineTo(x, h - 46 - Math.sin(x / 140) * 16 - Math.sin(x / 53) * 6); cc.lineTo(w, h); cc.fill();
    blocks.forEach(function (b) {
      cc.fillStyle = "#0a0d18"; cc.fillRect(b.x, h - b.h, b.w, b.h);
      if (b.roof > .7) { cc.fillRect(b.x + b.w * .6, h - b.h - 14, 4, 14); }
      b.wins.forEach(function (o) {
        var on = o[2] < p * 1.05;
        cc.fillStyle = on ? (o[2] < .15 ? "#ffd78a" : "#f2a65a") : "#151b2e";
        cc.globalAlpha = on ? .55 + o[2] * .45 : 1; cc.fillRect(o[0], o[1], 6, 8);
      });
      cc.globalAlpha = 1;
    });
  }
  sizeCity();

  // ---------- появление при прокрутке, реплики маскота у разделов ----------
  $$("[data-buddy]").forEach(function (h) {
    var v = h.getAttribute("data-buddy").split("|");
    var t = doc.createElement("span"); t.className = "tip"; t.innerHTML = '<img alt="" width="58" height="56"><span></span>';
    t.querySelector("img").src = P(v[0]); t.querySelector("span").textContent = v[1]; h.appendChild(t);
  });
  $$(".cards, .consoles, .news").forEach(function (g) { $$(".reveal, .con, .rel", g).forEach(function (el, i) { el.classList.add("reveal"); el.style.setProperty("--i", i % 8); }); });
  if ("IntersectionObserver" in win && !still) {
    var io = new IntersectionObserver(function (es) { es.forEach(function (e) { if (e.isIntersecting) { e.target.classList.add("in"); io.unobserve(e.target); } }); }, { rootMargin: "0px 0px -8% 0px" });
    $$(".reveal").forEach(function (el) { io.observe(el); });
  } else $$(".reveal").forEach(function (el) { el.classList.add("in"); });

  // подсветка карточек за курсором
  if (finePointer) $$(".card").forEach(function (c) {
    c.addEventListener("pointermove", function (e) { var r = c.getBoundingClientRect(); c.style.setProperty("--cx", (e.clientX - r.left) + "px"); c.style.setProperty("--cy", (e.clientY - r.top) + "px"); });
  });

  // ---------- витрина: скриншоты сами сменяются; наклон и блик за курсором ----------
  var phone = $(".js-phone");
  if (phone) {
    var shots = $$(".phone-screen img", phone), dots = $(".js-dots"), cap = $(".js-cap"), cur = 0, timer;
    var caps = ["Главная — как меню старой приставки", "Карточка игры: сейвы, скриншоты, файл", "Свои кнопки и подсказки по управлению", "Профиль, часы в играх, друзья", "Аватары, рамки, фоны — на свой вкус", "16 приставок и Java-игры"];
    shots.forEach(function (_, i) {
      var b = doc.createElement("button"); b.type = "button"; b.setAttribute("aria-label", "Скриншот " + (i + 1));
      b.addEventListener("click", function () { go(i); }); dots.appendChild(b);
    });
    var go = function (i) {
      shots[cur].classList.remove("on"); dots.children[cur].classList.remove("on");
      cur = (i + shots.length) % shots.length;
      shots[cur].classList.add("on"); dots.children[cur].classList.add("on"); cap.textContent = caps[cur] || "";
      clearInterval(timer); if (!still) timer = setInterval(function () { go(cur + 1); }, 4200);
    };
    go(0);
    var x0 = null;
    phone.addEventListener("touchstart", function (e) { x0 = e.touches[0].clientX; }, { passive: true });
    phone.addEventListener("touchend", function (e) { if (x0 == null) return; var dx = e.changedTouches[0].clientX - x0; if (Math.abs(dx) > 40) go(cur + (dx < 0 ? 1 : -1)); x0 = null; });
    if (finePointer && !still) {
      phone.addEventListener("pointermove", function (e) {
        var r = phone.getBoundingClientRect(), px = (e.clientX - r.left) / r.width, py = (e.clientY - r.top) / r.height;
        phone.style.setProperty("--ry", ((px - .5) * 8).toFixed(2) + "deg"); phone.style.setProperty("--rx", ((.5 - py) * 6).toFixed(2) + "deg");
        phone.style.setProperty("--gx", (px * 100) + "%"); phone.style.setProperty("--gy", (py * 100) + "%");
      });
      phone.addEventListener("pointerleave", function () { phone.style.setProperty("--rx", "0deg"); phone.style.setProperty("--ry", "0deg"); });
    }
  }

  // ---------- «Скачать»: притягивается к курсору, искорки, загрузка с прогрессом и подсказки установки ----------
  if (finePointer && !still) $$(".js-magnet").forEach(function (b) {
    b.addEventListener("pointermove", function (e) { var r = b.getBoundingClientRect(); b.style.transform = "translate(" + ((e.clientX - r.left - r.width / 2) * .18) + "px," + ((e.clientY - r.top - r.height / 2) * .25) + "px)"; });
    b.addEventListener("pointerleave", function () { b.style.transform = ""; });
  });
  function burst(x, y) {
    if (still) return;
    for (var i = 0; i < 14; i++) {
      var s = doc.createElement("i"), a = Math.random() * 6.28, d = 40 + Math.random() * 60;
      s.className = "burst"; s.style.left = x + "px"; s.style.top = y + "px"; s.style.setProperty("--dx", Math.cos(a) * d + "px"); s.style.setProperty("--dy", Math.sin(a) * d + "px");
      doc.body.appendChild(s); setTimeout(s.remove.bind(s), 750);
    }
  }
  var modal = $(".js-get"), bar = $(".js-bar"), title = $(".js-get-title"), sub = $(".js-get-sub"), pose = $(".js-get-pose"), head = $(".get-head"), steps = $$(".js-step");
  var busy = false, ctrl = null, lastVer = null;
  function step(n) { steps.forEach(function (s, i) { s.classList.toggle("on", i === n); }); }
  function openGet() { modal.hidden = false; doc.body.style.overflow = "hidden"; }
  function closeGet() { modal.hidden = true; doc.body.style.overflow = ""; }
  $(".js-close").addEventListener("click", closeGet);
  modal.addEventListener("click", function (e) { if (e.target === modal) closeGet(); });
  addEventListener("keydown", function (e) { if (e.key === "Escape" && !modal.hidden) closeGet(); });
  function fallback(url) {
    // скрипт не смог скачать (сеть, ограничения сайта) — обычная загрузка браузером, подсказки остаются
    if (ctrl) ctrl.abort(); busy = false;
    bar.parentNode.classList.remove("wait"); bar.style.width = "100%";
    title.textContent = "Загрузка началась"; sub.textContent = "Браузер качает файл сам — следи за ним в шторке уведомлений. Если связь оборвётся, браузер докачает с того же места.";
    pose.src = P("phone"); step(0);
    location.href = url;
  }
  function done(blob) {
    busy = false; parts = []; partGot = [];
    var u = URL.createObjectURL(new Blob([blob], { type: "application/vnd.android.package-archive" }));
    var a = doc.createElement("a"); a.href = u; a.download = "kapsula.apk"; doc.body.appendChild(a); a.click(); a.remove();
    setTimeout(function () { URL.revokeObjectURL(u); }, 60000);
    title.textContent = "Скачано!"; sub.textContent = "Осталось открыть файл и разрешить установку.";
    pose.src = P("jump"); head.classList.add("done"); step(0);
    setTimeout(function () { step(1); }, 2500);
  }
  $$(".js-apk").forEach(function (a) {
    a.addEventListener("click", function (e) {
      var r = a.getBoundingClientRect(); burst(r.left + r.width / 2, r.top + r.height / 2);
      e.preventDefault();
      openGet(); if (busy) return;
      if (!apk.url) {
        // описание версии ещё не пришло — подождать его; не пришло совсем — качать целый файл с сайта (не страницу выпусков)
        title.textContent = "Узнаю последнюю версию…"; bar.parentNode.classList.add("wait");
        apkReady.then(function () { if (!apk.url) apk = { url: "https://github.com/vitalya2078-cloud/nexus-psp/releases/download/latest/kapsula.apk", ver: null, mb: 0 }; a.click(); });
        return;
      }
      head.classList.remove("done"); pose.src = P("run"); step(-1);
      if (apk.ver !== lastVer) { parts = []; partGot = []; lastVer = apk.ver; }
      title.textContent = "Качаю KAPSULA…"; sub.textContent = "Не закрывай страницу, пока идёт загрузка.";
      bar.style.width = "0"; bar.parentNode.classList.add("wait");
      if (!win.fetch || !win.AbortController) { fallback(apk.url); return; }
      busy = true; ctrl = new AbortController();
      if (!apk.parts) { fallback(apk.url); return; }
      bar.parentNode.classList.remove("wait");
      getParts();
    });
  });
  // загрузка кусками: сервер сайта не умеет докачивать, поэтому при обрыве перекачиваем только упавший кусок (до 6 попыток)
  var parts = [], partGot = [];
  function getParts() {
    var n = apk.parts, total = apk.size || apk.mb * 1048576, base = apk.url.replace(/\?.*$/, "");
    function shown() {
      var got = partGot.reduce(function (a, b) { return a + (b || 0); }, 0);
      bar.style.width = (Math.min(1, got / total) * 100).toFixed(1) + "%";
      sub.textContent = (got / 1048576).toFixed(1) + " из " + (total / 1048576).toFixed(0) + " МБ · не закрывай страницу";
    }
    function one(k, tries) {
      if (parts[k]) return Promise.resolve();
      var url = base + "." + (k < 10 ? "0" : "") + k + "?v=" + encodeURIComponent(apk.ver || "");
      return fetch(url, { signal: ctrl.signal, cache: "no-store" }).then(function (res) {
        if (!res.ok || !res.body) throw new Error("http " + res.status);
        var reader = res.body.getReader(), chunks = []; partGot[k] = 0;
        function pump() {
          return reader.read().then(function (c) {
            if (c.done) { parts[k] = new Blob(chunks); return; }
            chunks.push(c.value); partGot[k] += c.value.length; shown(); return pump();
          });
        }
        return pump();
      }).catch(function (e) {
        if (ctrl.signal.aborted) throw e;
        partGot[k] = 0; shown();
        if (tries >= 6) throw e;
        sub.textContent = "Связь прервалась — докачиваю кусок " + (k + 1) + " из " + n + "…";
        return new Promise(function (r) { setTimeout(r, 1500 * tries); }).then(function () { return one(k, tries + 1); });
      });
    }
    // по два куска одновременно — быстрее, но не перегружая мобильную сеть
    var next = 0;
    function worker() { if (next >= n) return Promise.resolve(); var k = next++; return one(k, 1).then(worker); }
    Promise.all([worker(), worker()]).then(function () {
      busy = false; done(new Blob(parts));
    }).catch(function () {
      busy = false;
      title.textContent = "Связь с сервером рвётся";
      sub.textContent = "Скачанное не пропало — нажми «Скачать APK» ещё раз, докачаю остальное. Или скачай напрямую ссылкой ниже.";
      pose.src = P("sad");
    });
  }

  // телефон: «Скачать APK» появляется внизу, когда главная кнопка ушла за экран
  var mainBtn = $(".cta .js-apk"), sticky = $(".sticky");
  if (mainBtn && sticky && "IntersectionObserver" in win) {
    new IntersectionObserver(function (e) { sticky.classList.toggle("show", !e[0].isIntersecting && e[0].boundingClientRect.top < 0); }).observe(mainBtn);
  }

  // ---------- маскот: приветствие, моргает, смотрит на курсор, засыпает без дела, касание — смех ----------
  var img = $(".js-pose"), btn = $(".js-buddy"), bubble = $(".js-bubble");
  if (!img || !btn) return;
  ["idle", "blink", "lookl", "lookr", "laugh", "sleep", "stretch", "dance1", "dance2", "dance3", "run", "jump", "phone"].forEach(function (n) { new Image().src = P(n); });
  var h = new Date().getHours();
  var greet = h >= 6 && h <= 10 ? ["stretch", "Доброе утро! Кофе и одна игра?"]
    : h >= 11 && h <= 17 ? ["wave", "Привет! Во что сыграем?"]
    : h >= 18 && h <= 22 ? ["mug", "Добрый вечер. Самое время для игры"]
    : ["sleep", "Не спится? Мне тоже…"];
  var base = greet[0], busyB = false, lastAct = Date.now(), look = null;
  img.src = P(base); bubble.textContent = greet[1];
  function set(n) { img.src = P(n); }
  function hop(el) { el = el || btn; el.classList.remove("hop"); void el.offsetWidth; el.classList.add("hop"); }
  function say(t, ms) { bubble.textContent = t; bubble.classList.remove("hide"); clearTimeout(say.t); say.t = setTimeout(function () { bubble.classList.add("hide"); }, ms || 3500); }
  setTimeout(function () { bubble.classList.add("hide"); }, 6000);
  setTimeout(function () { if (base !== "sleep") { base = "idle"; hop(); set(base); } }, 3200);
  function wake() {
    lastAct = Date.now();
    if (base === "sleep" && !busyB) { base = "idle"; set("stretch"); hop(); busyB = true; setTimeout(function () { busyB = false; set(base); }, 1200); }
  }
  ["pointerdown", "keydown", "scroll"].forEach(function (ev) { addEventListener(ev, wake, { passive: true }); });
  if (finePointer) addEventListener("pointermove", function (e) {
    lastAct = Date.now(); if (busyB || base !== "idle") return;
    var r = btn.getBoundingClientRect(), dx = e.clientX - (r.left + r.width / 2);
    var want = dx < -r.width * .7 ? "lookl" : dx > r.width * .7 ? "lookr" : null;
    if (want !== look) { look = want; set(want || base); }
  }, { passive: true });
  if (!still) (function life() {
    setTimeout(function () {
      if (!busyB && base === "idle" && Date.now() - lastAct > 40000) { base = "sleep"; set("yawn"); setTimeout(function () { if (base === "sleep") set("sleep"); }, 1400); }
      else if (!busyB && base === "idle" && !look) {
        if (!finePointer && Math.random() < .25) { var l = Math.random() < .5 ? "lookl" : "lookr"; set(l); setTimeout(function () { if (!busyB && !look) set(base); }, 1300); }
        else { set("blink"); setTimeout(function () { if (!busyB) set(look || base); }, 130); }
      }
      life();
    }, 3000 + Math.random() * 3000);
  })();
  // касание — смех; пять касаний подряд — секретная мини-игра
  var taps = [];
  btn.addEventListener("click", function () {
    var now = Date.now(); taps = taps.filter(function (t) { return now - t < 2500; }); taps.push(now);
    if (taps.length >= 5) { taps = []; say("Поймаешь картриджи?", 2000); setTimeout(startGame, 600); return; }
    if (busyB) return;
    busyB = true; if (base === "sleep") base = "idle";
    set(taps.length >= 3 ? "dance" + (taps.length - 2) : "laugh"); hop();
    if (taps.length === 3) say("Ещё чуть-чуть…", 1500);
    setTimeout(function () { set(look || base); busyB = false; }, 1100);
  });

  // ---------- подвал: маскот с фонариком машет на прощание ----------
  var fb = $(".js-foot-buddy");
  if (fb && "IntersectionObserver" in win) new IntersectionObserver(function (es) {
    if (es[0].isIntersecting) { fb.classList.add("hello"); hop(fb); setTimeout(function () { fb.classList.remove("hello"); }, 4000); }
  }, { threshold: .6 }).observe(fb);

  // ---------- пасхалка: код Konami → старый телевизор и танец ----------
  // компьютер — клавишами ↑↑↓↓←→←→BA; телефон — те же стрелки свайпами (↑↑↓↓←→←→) или подержать Сейвика полторы секунды
  var retroOn = false;
  function retro() {
    if (retroOn) return; retroOn = true;
    root.classList.add("crt"); busyB = true; say("Ретро-режим!", 5000);
    if (navigator.vibrate) navigator.vibrate([40, 60, 40]);
    var n = 0, iv = setInterval(function () { set(["dance1", "dance2", "dance3", "dance2"][n++ % 4]); if (n % 2) hop(); }, 380);
    setTimeout(function () { clearInterval(iv); root.classList.remove("crt"); busyB = false; retroOn = false; set(base); }, 6500);
  }
  var code = ["ArrowUp", "ArrowUp", "ArrowDown", "ArrowDown", "ArrowLeft", "ArrowRight", "ArrowLeft", "ArrowRight", "b", "a"], pos = 0;
  addEventListener("keydown", function (e) {
    var k = e.key.length === 1 ? e.key.toLowerCase() : e.key;
    pos = k === code[pos] ? pos + 1 : (k === code[0] ? 1 : 0);
    if (pos === code.length) { pos = 0; retro(); }
  });
  var swipe = ["u", "u", "d", "d", "l", "r", "l", "r"], sp = 0, sx = 0, sy = 0, st = 0;
  addEventListener("touchstart", function (e) { var t = e.touches[0]; sx = t.clientX; sy = t.clientY; st = Date.now(); }, { passive: true });
  addEventListener("touchend", function (e) {
    var t = e.changedTouches[0], dx = t.clientX - sx, dy = t.clientY - sy;
    if (Date.now() - st > 700 || Math.max(Math.abs(dx), Math.abs(dy)) < 40) return;
    // палец вверх — страница едет вниз: считаем направление жеста, как стрелку на клавиатуре
    var d = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "r" : "l") : (dy < 0 ? "u" : "d");
    sp = d === swipe[sp] ? sp + 1 : (d === swipe[0] ? 1 : 0);
    if (sp === swipe.length) { sp = 0; retro(); }
  }, { passive: true });
  var hold = 0;
  btn.addEventListener("pointerdown", function (e) { if (e.pointerType !== "mouse") hold = setTimeout(function () { hold = 0; taps = []; retro(); }, 1500); });
  ["pointerup", "pointercancel", "pointerleave"].forEach(function (ev) { btn.addEventListener(ev, function () { clearTimeout(hold); }); });
  btn.addEventListener("contextmenu", function (e) { e.preventDefault(); });
  var secret = $(".js-secret");
  if (secret) secret.addEventListener("click", function (e) {
    e.preventDefault(); scrollTo({ top: 0, behavior: still ? "auto" : "smooth" });
    setTimeout(function () { say(finePointer ? "Попробуй ↑↑↓↓←→←→ B A на клавиатуре" : "Подержи меня пальцем… или смахни ↑↑↓↓←→←→", 6000); hop(); }, 700);
  });

  // ---------- мини-игра: грузится только по требованию ----------
  function startGame() {
    if (win.KapsulaGame) { win.KapsulaGame.open(); return; }
    var s = doc.createElement("script"); s.src = "game.js"; s.onload = function () { win.KapsulaGame && win.KapsulaGame.open(); }; doc.body.appendChild(s);
  }
  win.kapsulaStartGame = startGame;
})();
