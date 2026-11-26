/* «Что это за файл?» — те же признаки, что в приложении (Disc.kt, Bios.kt), но прямо в браузере: файл не загружается. */
(function () {
  "use strict";
  var D = window.KAPSULA_DATA || { platforms: [], ps1bios: {} };
  var P = {}; D.platforms.forEach(function (p) { P[p.id] = p; });
  var out = document.querySelector(".js-out"), drop = document.querySelector(".js-drop"), input = document.querySelector(".js-file");

  function mb(n) { return n >= 1073741824 ? (n / 1073741824).toFixed(1) + " ГБ" : n >= 1048576 ? (n / 1048576).toFixed(1) + " МБ" : n >= 1024 ? Math.round(n / 1024) + " КБ" : n + " байт"; }
  function esc(s) { return String(s).replace(/[&<>"]/g, function (c) { return { "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;" }[c]; }); }
  function ext(n) { var i = n.lastIndexOf("."); return i < 0 ? "" : n.slice(i + 1).toLowerCase(); }
  function head(f, n) { return f.slice(0, n).arrayBuffer().then(function (b) { return new Uint8Array(b); }); }
  function at(h, o, s) { if (h.length < o + s.length) return false; for (var i = 0; i < s.length; i++) if (h[o + i] !== s.charCodeAt(i)) return false; return true; }
  function str(h, o, n) { var s = ""; for (var i = o; i < Math.min(h.length, o + n); i++) s += String.fromCharCode(h[i]); return s; }
  function isText(h) {
    var n = Math.min(h.length, 4096), ok = 0; if (!n) return false;
    for (var i = 0; i < n; i++) { var b = h[i]; if (b === 0) return false; if (b === 9 || b === 10 || b === 13 || (b >= 32 && b <= 126) || b >= 0xC0) ok++; }
    return ok > n * 0.95;
  }

  // ---------- BIOS ----------
  function biosGuess(f) {
    var n = f.name.toLowerCase(), s = f.size;
    if (n === "neogeo.zip" || n === "aes.zip") return Promise.resolve(["ok", "neogeo", "BIOS Neo Geo", "Положи его в KAPSULA: Настройки → BIOS → Neo Geo."]);
    if (s === 512 && /lynx/.test(n)) return Promise.resolve(["ok", "lynx", "BIOS Atari Lynx", "Подходит по размеру. Настройки → BIOS → Lynx."]);
    if (s === 2097152 && /dc|boot|dreamcast/.test(n)) return Promise.resolve(["ok", "dc", "BIOS Dreamcast", "Подходит по размеру (2 МБ). Настройки → BIOS → Dreamcast."]);
    if (s === 4194304 && /scph|bios|ps2/.test(n)) return Promise.resolve(["ok", "ps2", "BIOS PS2", "Подходит по размеру (4 МБ). Настройки → BIOS → PS2."]);
    if (s === 524288) return f.arrayBuffer().then(function (b) {
      var sum = md5(new Uint8Array(b)), known = D.ps1bios[sum];
      if (known) return ["ok", "ps1", "BIOS PS1: " + known, "Известная версия — подходит. Настройки → BIOS → PS1."];
      if (/sat|sega/.test(n)) return ["ok", "saturn", "BIOS Sega Saturn", "Подходит по размеру (512 КБ). Настройки → BIOS → Saturn."];
      return ["warn", "ps1", "Похоже на BIOS (512 КБ), но версия неизвестна", "Если это PS1 — может не подойти: KAPSULA знает версии SCPH-1001, 5500, 5501, 5502, 7001, 101. Если Saturn — подойдёт. Отпечаток MD5: " + sum];
    });
    return null;
  }

  // ---------- игры ----------
  function cd(h) {
    return at(h, 0x8001, "CD001") || at(h, 16 * 2352 + 24 + 1, "CD001") || at(h, 16 * 2352 + 16 + 1, "CD001") ||
      at(h, 0, "SEGA SEGASATURN") || at(h, 16, "SEGA SEGASATURN") || at(h, 0, "SEGADISCSYSTEM") || at(h, 16, "SEGADISCSYSTEM") || at(h, 0, "SEGA SEGAKATANA") || at(h, 16, "SEGA SEGAKATANA");
  }
  function discKind(h, size) {
    var s0 = str(h, 0, 32) + str(h, 16, 32);
    if (/SEGA SEGASATURN/.test(s0)) return "saturn";
    if (/SEGA SEGAKATANA/.test(s0)) return "dc";
    if (/SEGADISCSYSTEM/.test(s0)) return "md";
    var pvd = [0x8000, 16 * 2352 + 24, 16 * 2352 + 16].filter(function (o) { return at(h, o + 1, "CD001"); })[0];
    if (pvd == null) return null;
    var sys = str(h, pvd + 8, 32).trim();
    if (/^PSP GAME/.test(sys)) return "psp";
    if (/^PLAYSTATION/.test(sys)) return size > 750 * 1048576 ? "ps2" : "ps1?";
    return null;
  }
  function gameGuess(f, h) {
    var e = ext(f.name), s = f.size;
    if (["zip", "7z", "rar", "gz"].indexOf(e) >= 0 && f.name.toLowerCase() !== "neogeo.zip")
      return ["bad", null, "Это архив (" + e.toUpperCase() + ")", "Сначала распакуй его — внутри будет образ игры (ISO, CUE + BIN, CHD…). KAPSULA архивы не открывает."];
    if (["txt", "md", "nfo", "html", "jpg", "png", "pdf", "exe"].indexOf(e) >= 0) return ["bad", null, "Это не игра", "Файл ." + e + " — описание, картинка или программа для компьютера."];
    if (e !== "cue" && e !== "m3u" && e !== "gdi" && isText(h)) return ["bad", null, "Это текстовый файл, а не игра", "Внутри текст, а у образов игр — двоичные данные."];
    if (s > 0 && s < 1024 && ["cue", "m3u", "gdi", "ccd"].indexOf(e) < 0) return ["bad", null, "Слишком маленький файл", "Всего " + mb(s) + " — так игры не весят. Возможно, скачался не до конца."];
    var id = null, note = "";
    switch (e) {
      case "iso": case "img": id = discKind(h, s); if (!id && at(h, 0x8008, "PSP GAME")) id = "psp"; break;
      case "bin": id = (s % 2352 === 0 && s >= 2352 * 17) ? discKind(h, s) || "ps1?" : at(h, 0x100, "SEGA") ? "md" : [0x7FF0, 0x3FF0, 0x1FF0].some(function (o) { return at(h, o, "TMR SEGA"); }) ? "sms" : s <= 65536 ? "a26" : "md";
        note = "Если это образ диска — рядом должен лежать CUE-файл с тем же названием."; break;
      case "cso": id = at(h, 0, "CISO") ? "psp" : null; break;
      case "zso": id = at(h, 0, "ZISO") ? "ps2" : null; break;
      case "pbp": id = at(h, 1, "PBP") ? "psp" : null; note = "PBP бывает и с играми PS1 — KAPSULA различит сама."; break;
      case "chd": id = at(h, 0, "MComprHD") ? "chd" : null; break;
      case "cue": var t = str(h, 0, 4096); id = /FILE/i.test(t) && /TRACK/i.test(t) ? "cue" : null; break;
      case "nes": id = at(h, 0, "NES\u001A") ? "nes" : null; break;
      case "jar": id = at(h, 0, "PK\u0003\u0004") ? "j2me" : null; break;
      case "neo": id = at(h, 0, "NEO") ? "neogeo" : null; break;
      case "lnx": id = at(h, 0, "LYNX") ? "lynx" : null; break;
      case "ngp": case "ngc": id = /SNK/.test(str(h, 0, 32)) ? "ngp" : null; break;
      default: D.platforms.some(function (p) { if (p.exts.indexOf(e) >= 0) { id = p.id; return true; } return false; });
    }
    if (id === "chd") return ["ok", null, "CHD — сжатый образ диска", "Подходит для PS1, PS2, PSP, Dreamcast и Saturn. KAPSULA сама определит приставку, а если ошибётся — её можно поменять в карточке игры."];
    if (id === "cue") return null; // CUE проверяется вместе с BIN ниже
    if (id === "ps1?") return ["ok", "ps1", "Образ PlayStation", (s > 700 * 1048576 ? "Скорее всего PS2. " : "Скорее всего PS1. ") + "KAPSULA точно определит по диску. " + note];
    if (!id) return ["warn", null, "Не узнаю этот файл", "Расширение ." + (e || "—") + " без признаков игры. Может, файл повреждён, недокачан или переименован."];
    var p = P[id];
    var bios = p && p.bios === 2 ? " Этой приставке нужен BIOS — проверь его здесь же." : "";
    var exp = p && p.exp ? " Приставка тестовая: нужен мощный телефон." : "";
    return ["ok", id, p ? p.full : id, "KAPSULA откроет эту игру." + bios + exp + (note ? " " + note : "")];
  }

  function card(name, size, v) {
    var p = v[1] && P[v[1]];
    var cls = v[0] === "ok" ? "ok" : v[0] === "warn" ? "warn" : "bad";
    var mark = v[0] === "ok" ? "<span class='tag-ok'>✓</span> " : v[0] === "warn" ? "<span class='tag-warn'>?</span> " : "<span class='tag-bad'>✕</span> ";
    return '<div class="verdict ' + cls + '">' + (p ? '<img class="con" src="' + p.img + '" alt="">' : "") +
      "<div><b>" + mark + esc(v[2]) + "</b><p>" + esc(v[3]) + '</p><p class="hint">' + esc(name) + " · " + mb(size) + "</p></div></div>";
  }

  function check(files) {
    files = Array.prototype.slice.call(files);
    if (!files.length) return;
    out.innerHTML = '<p class="hint">Смотрю…</p>';
    var names = files.map(function (f) { return f.name.toLowerCase(); });
    Promise.all(files.map(function (f) {
      var b = biosGuess(f);
      if (b) return b.then(function (v) { return card(f.name, f.size, v); });
      return head(f, 40 * 1024).then(function (h) {
        if (ext(f.name) === "cue") {
          var t = str(h, 0, h.length), need = [], re = /FILE\s+"([^"]+)"/gi, m;
          while ((m = re.exec(t))) need.push(m[1]);
          if (!need.length) return card(f.name, f.size, ["bad", null, "CUE без списка дорожек", "Внутри нет строк FILE — файл повреждён."]);
          var miss = need.filter(function (n) { return names.indexOf(n.toLowerCase()) < 0; });
          return card(f.name, f.size, miss.length === need.length && files.length === 1
            ? ["warn", "ps1", "CUE — описание диска", "Ему нужны файлы: " + need.join(", ") + ". Выбери их вместе с CUE, чтобы я проверил, что все на месте."]
            : miss.length ? ["bad", "ps1", "Не хватает файлов диска", "Нет рядом: " + miss.join(", ") + ". Без них игра не запустится."]
            : ["ok", "ps1", "CUE + BIN — всё на месте", "Все дорожки нашлись. Клади их вместе в одну папку."]);
        }
        var v = gameGuess(f, h);
        return v ? card(f.name, f.size, v) : "";
      });
    })).then(function (cards) { out.innerHTML = cards.join(""); });
  }

  input.addEventListener("change", function () { check(input.files); });
  ["dragenter", "dragover"].forEach(function (e) { drop.addEventListener(e, function (ev) { ev.preventDefault(); drop.classList.add("over"); }); });
  ["dragleave", "drop"].forEach(function (e) { drop.addEventListener(e, function (ev) { ev.preventDefault(); drop.classList.remove("over"); }); });
  drop.addEventListener("drop", function (ev) { check(ev.dataTransfer.files); });

  // ---------- MD5 (в браузере его нет среди встроенных) ----------
  function md5(bytes) {
    var K = [], S = [7, 12, 17, 22, 5, 9, 14, 20, 4, 11, 16, 23, 6, 10, 15, 21], i;
    for (i = 0; i < 64; i++) K[i] = Math.floor(Math.abs(Math.sin(i + 1)) * 4294967296) | 0;
    var len = bytes.length, n = ((len + 8) >> 6) + 1, w = new Int32Array(n * 16);
    for (i = 0; i < len; i++) w[i >> 2] |= bytes[i] << ((i % 4) * 8);
    w[len >> 2] |= 0x80 << ((len % 4) * 8);
    w[n * 16 - 2] = (len * 8) | 0; w[n * 16 - 1] = Math.floor(len / 536870912);
    var a0 = 0x67452301, b0 = 0xefcdab89 | 0, c0 = 0x98badcfe | 0, d0 = 0x10325476;
    for (var j = 0; j < n * 16; j += 16) {
      var a = a0, b = b0, c = c0, d = d0;
      for (i = 0; i < 64; i++) {
        var f, g;
        if (i < 16) { f = (b & c) | (~b & d); g = i; } else if (i < 32) { f = (d & b) | (~d & c); g = (5 * i + 1) % 16; }
        else if (i < 48) { f = b ^ c ^ d; g = (3 * i + 5) % 16; } else { f = c ^ (b | ~d); g = (7 * i) % 16; }
        var t = d; d = c; c = b;
        var x = (a + f + K[i] + w[j + g]) | 0, s = S[(i >> 4) * 4 + (i % 4)];
        b = (b + ((x << s) | (x >>> (32 - s)))) | 0; a = t;
      }
      a0 = (a0 + a) | 0; b0 = (b0 + b) | 0; c0 = (c0 + c) | 0; d0 = (d0 + d) | 0;
    }
    return [a0, b0, c0, d0].map(function (v) { var h = ""; for (var k = 0; k < 4; k++) h += ((v >>> (k * 8)) & 255).toString(16).padStart(2, "0"); return h; }).join("");
  }
  window.kapsulaMd5 = md5;
})();
