/* «Моя полка»: карточка 1200×630 (как превью ссылок в ВК и Telegram) — приставки, ник, любимые игры, Сейвик. */
(function () {
  "use strict";
  var D = window.KAPSULA_DATA || { platforms: [] };
  var cv = document.querySelector(".js-cv"), g = cv.getContext("2d"), picks = document.querySelector(".js-picks");
  var chosen = ["psp", "ps1", "nes"], imgs = {}, mascot = new Image();
  mascot.src = "img/mascot/cartbox.webp"; mascot.onload = draw;
  function load(p) { if (!imgs[p.id]) { var i = new Image(); i.onload = draw; i.src = p.img; imgs[p.id] = i; } return imgs[p.id]; }
  D.platforms.forEach(function (p) {
    load(p);
    var b = document.createElement("button"); b.type = "button"; b.className = "pick" + (chosen.indexOf(p.id) >= 0 ? " on" : "");
    b.innerHTML = '<img src="' + p.img + '" alt=""><span></span>'; b.querySelector("span").textContent = p.short;
    b.addEventListener("click", function () {
      var k = chosen.indexOf(p.id);
      if (k >= 0) chosen.splice(k, 1); else if (chosen.length < 6) chosen.push(p.id); else return;
      b.classList.toggle("on", chosen.indexOf(p.id) >= 0); draw();
    });
    picks.appendChild(b);
  });
  ["js-nick", "js-g1", "js-g2", "js-g3"].forEach(function (c) { document.querySelector("." + c).addEventListener("input", draw); });
  function val(c) { return document.querySelector("." + c).value.trim(); }
  function round(x, y, w, h, r) { g.beginPath(); g.moveTo(x + r, y); g.arcTo(x + w, y, x + w, y + h, r); g.arcTo(x + w, y + h, x, y + h, r); g.arcTo(x, y + h, x, y, r); g.arcTo(x, y, x + w, y, r); g.closePath(); }
  function draw() {
    var bg = g.createLinearGradient(0, 0, 0, 630); bg.addColorStop(0, "#1d2240"); bg.addColorStop(1, "#0b0f1c"); g.fillStyle = bg; g.fillRect(0, 0, 1200, 630);
    var lamp = g.createRadialGradient(120, -40, 0, 120, -40, 720); lamp.addColorStop(0, "rgba(255,184,102,.33)"); lamp.addColorStop(1, "rgba(255,184,102,0)"); g.fillStyle = lamp; g.fillRect(0, 0, 1200, 630);
    for (var s = 0; s < 60; s++) { var x = (s * 197) % 1200, y = (s * 113) % 300; g.fillStyle = "rgba(255,244,220," + (0.15 + (s % 5) * 0.08) + ")"; g.fillRect(x, y, 2, 2); }
    var nick = val("js-nick") || "Игрок";
    g.fillStyle = "#f2a65a"; g.font = "700 30px 'Balsamiq Sans', sans-serif"; g.fillText("МОЯ ПОЛКА · KAPSULA", 70, 90);
    g.fillStyle = "#f4eee4"; g.font = "700 66px 'Balsamiq Sans', sans-serif"; g.fillText(nick.slice(0, 20), 70, 165);
    // полка с приставками
    var shelfY = 330;
    round(60, shelfY + 70, 720, 18, 9); g.fillStyle = "#3a2a1e"; g.fill();
    round(60, shelfY + 70, 720, 6, 3); g.fillStyle = "#6b4a30"; g.fill();
    chosen.slice(0, 6).forEach(function (id, k) {
      var p = D.platforms.filter(function (q) { return q.id === id; })[0], im = p && load(p);
      if (im && im.complete && im.naturalWidth) { var w = 112, h = w * im.naturalHeight / im.naturalWidth; g.drawImage(im, 70 + k * 118, shelfY + 70 - h, w, h); }
    });
    var games = [val("js-g1"), val("js-g2"), val("js-g3")].filter(Boolean);
    g.font = "700 30px 'Balsamiq Sans', sans-serif"; g.fillStyle = "#f4eee4";
    (games.length ? games : ["Любимые игры — ещё впереди"]).forEach(function (t, k) {
      g.fillStyle = "#f2a65a"; g.beginPath(); g.arc(82, 470 + k * 44 - 10, 6, 0, 6.28); g.fill();
      g.fillStyle = games.length ? "#f4eee4" : "#8891a8"; g.fillText(t.slice(0, 34), 102, 470 + k * 44);
    });
    if (mascot.complete && mascot.naturalWidth) g.drawImage(mascot, 830, 180, 330, 330 * mascot.naturalHeight / mascot.naturalWidth);
    g.fillStyle = "#aeb6cc"; g.font = "500 26px sans-serif"; g.fillText("kapsula.gitverse.site", 860, 590);
  }
  function blob(cb) { draw(); cv.toBlob(cb, "image/png"); }
  document.querySelector(".js-save").addEventListener("click", function () {
    blob(function (b) { var a = document.createElement("a"); a.href = URL.createObjectURL(b); a.download = "kapsula-polka.png"; a.click(); setTimeout(function () { URL.revokeObjectURL(a.href); }, 5000); });
  });
  document.querySelector(".js-share").addEventListener("click", function () {
    blob(function (b) {
      var f = new File([b], "kapsula-polka.png", { type: "image/png" });
      if (navigator.canShare && navigator.canShare({ files: [f] })) navigator.share({ files: [f], text: "Моя ретро-полка в KAPSULA https://kapsula.gitverse.site/shelf.html" }).catch(function () {});
      else { var a = document.createElement("a"); a.href = URL.createObjectURL(b); a.download = "kapsula-polka.png"; a.click(); }
    });
  });
  if (document.fonts && document.fonts.load) document.fonts.load("700 30px 'Balsamiq Sans'").then(draw);
  draw();
})();
