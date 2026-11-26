/* Ретро-викторина: 10 вопросов — силуэт приставки, год выхода, производитель. Результат — картинкой для соцсетей. */
(function () {
  "use strict";
  var D = window.KAPSULA_DATA || { platforms: [] };
  var P = D.platforms.filter(function (p) { return p.id !== "j2me" && p.id !== "a78"; });
  // год первого выхода (в Японии или США — где раньше) и производитель
  var FACT = { nes: [1983, "Nintendo"], sms: [1985, "Sega"], md: [1988, "Sega"], pce: [1987, "NEC"], gg: [1990, "Sega"], neogeo: [1990, "SNK"], saturn: [1994, "Sega"],
    ps1: [1994, "Sony"], dc: [1998, "Sega"], ngp: [1998, "SNK"], ws: [1999, "Bandai"], ps2: [2000, "Sony"], psp: [2004, "Sony"], a26: [1977, "Atari"], lynx: [1989, "Atari"] };
  // в вопросах про год и производителя — оригинал (Денди — клон Famicom, вышла у нас в 1992-м)
  var NAME = { nes: "Famicom" };
  var MAKERS = ["Sony", "Sega", "Nintendo", "SNK", "NEC", "Atari", "Bandai"];
  var box = document.querySelector(".js-quiz");
  var qs, i, score, marks, locked;
  function shuffle(a) { a = a.slice(); for (var k = a.length - 1; k > 0; k--) { var j = Math.random() * (k + 1) | 0, t = a[k]; a[k] = a[j]; a[j] = t; } return a; }
  function nameOf(p) { return p.full; }
  function origOf(p) { return NAME[p.id] || p.full; }
  function build() {
    var pool = shuffle(P.filter(function (p) { return FACT[p.id]; })), out = [];
    pool.slice(0, 6).forEach(function (p) {
      var opts = shuffle([p].concat(shuffle(P.filter(function (o) { return o.id !== p.id; })).slice(0, 3)));
      out.push({ img: p.img, q: "Что это за приставка?", opts: opts.map(nameOf), right: nameOf(p) });
    });
    pool.slice(6, 8).forEach(function (p) {
      var y = FACT[p.id][0], ys = shuffle([y, y - 3, y + 2, y + 5].map(String));
      out.push({ img: p.img, show: true, q: "В каком году вышла " + origOf(p) + "?", opts: ys, right: String(y) });
    });
    pool.slice(8, 10).forEach(function (p) {
      var m = FACT[p.id][1], ms = shuffle([m].concat(shuffle(MAKERS.filter(function (x) { return x !== m; })).slice(0, 3)));
      out.push({ img: p.img, show: true, q: "Кто выпустил " + origOf(p) + "?", opts: ms, right: m });
    });
    return shuffle(out);
  }
  function start() { qs = build(); i = 0; score = 0; marks = []; ask(); }
  function prog() { return '<div class="qprog">' + qs.map(function (_, k) { return '<i class="' + (marks[k] || (k === i ? "cur" : "")) + '"></i>'; }).join("") + "</div>"; }
  function ask() {
    var q = qs[i]; locked = false;
    box.innerHTML = prog() + '<div class="qpic' + (q.show ? " reveal" : "") + '"><img src="' + q.img + '" alt=""></div><p class="qq"></p><div class="qopts"></div>';
    box.querySelector(".qq").textContent = q.q;
    var opts = box.querySelector(".qopts");
    q.opts.forEach(function (o) {
      var b = document.createElement("button"); b.type = "button"; b.className = "qopt"; b.textContent = o;
      b.addEventListener("click", function () { answer(b, o); }); opts.appendChild(b);
    });
  }
  function answer(b, o) {
    if (locked) return; locked = true;
    var q = qs[i], ok = o === q.right;
    if (ok) score++;
    marks[i] = ok ? "ok" : "no";
    b.classList.add(ok ? "right" : "wrong");
    if (!ok) Array.prototype.forEach.call(box.querySelectorAll(".qopt"), function (x) { if (x.textContent === q.right) x.classList.add("right"); });
    box.querySelector(".qpic").classList.add("reveal");
    if (navigator.vibrate) navigator.vibrate(ok ? 20 : [30, 40, 30]);
    setTimeout(function () { i++; if (i < qs.length) ask(); else done(); }, ok ? 900 : 1600);
  }
  function verdict() { return score >= 9 ? ["trophy", "Ретро-легенда!"] : score >= 7 ? ["jump", "Настоящий знаток!"] : score >= 4 ? ["wink", "Неплохо! Ещё партию?"] : ["think", "Есть куда расти — зато интересно!"]; }
  function done() {
    var v = verdict();
    box.innerHTML = prog() + '<img class="qbuddy" src="img/mascot/' + v[0] + '.webp" alt=""><div class="qscore">' + score + " / 10</div><p class='qq'></p>" +
      '<div class="qrow"><button class="btn btn-main js-share" type="button">Поделиться результатом</button><button class="btn btn-soft js-again" type="button">Ещё раз</button></div>';
    box.querySelector(".qq").textContent = v[1];
    box.querySelector(".js-again").addEventListener("click", start);
    box.querySelector(".js-share").addEventListener("click", function () { share(v); });
  }
  function share(v) {
    var c = document.createElement("canvas"); c.width = 1200; c.height = 630; var g = c.getContext("2d");
    var bg = g.createLinearGradient(0, 0, 0, 630); bg.addColorStop(0, "#1d2240"); bg.addColorStop(1, "#0b0f1c"); g.fillStyle = bg; g.fillRect(0, 0, 1200, 630);
    var lamp = g.createRadialGradient(140, -40, 0, 140, -40, 700); lamp.addColorStop(0, "rgba(255,184,102,.35)"); lamp.addColorStop(1, "rgba(255,184,102,0)"); g.fillStyle = lamp; g.fillRect(0, 0, 1200, 630);
    var img = new Image(); img.onload = function () {
      g.drawImage(img, 760, 150, 380, 368);
      g.fillStyle = "#f2a65a"; g.font = "700 34px 'Balsamiq Sans', sans-serif"; g.fillText("Ретро-викторина KAPSULA", 80, 120);
      g.fillStyle = "#f4eee4"; g.font = "700 150px 'Balsamiq Sans', sans-serif"; g.fillText(score + " / 10", 80, 330);
      g.font = "700 46px 'Balsamiq Sans', sans-serif"; g.fillText(v[1], 80, 420);
      g.fillStyle = "#aeb6cc"; g.font = "500 30px sans-serif"; g.fillText("А ты сколько наберёшь? kapsula.gitverse.site", 80, 540);
      out(c, "kapsula-quiz.png", "Я набрал " + score + " из 10 в ретро-викторине KAPSULA! А ты?");
    };
    img.src = "img/mascot/" + v[0] + ".webp";
  }
  function out(c, file, text) {
    c.toBlob(function (b) {
      var f = new File([b], file, { type: "image/png" });
      if (navigator.canShare && navigator.canShare({ files: [f] })) navigator.share({ files: [f], text: text + " https://kapsula.gitverse.site/quiz.html" }).catch(function () {});
      else { var a = document.createElement("a"); a.href = URL.createObjectURL(b); a.download = file; a.click(); setTimeout(function () { URL.revokeObjectURL(a.href); }, 5000); }
    }, "image/png");
  }
  window.kapsulaShareCanvas = out;
  start();
})();
