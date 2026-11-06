/* NEXUS — прототип оболочки мультиплатформенного эмулятора (PSP, PS1, PS2).
   Демо-данные; обложки рисуются кодом. Вкладки: #games, #store. Панели поверх: #game-<id>, #search,
   #downloads, #alerts, #settings, #profile, #pad, #play-<id>. */
(function () {
'use strict';
var $ = function (s, r) { return (r || document).querySelector(s); };
try { if (/[?&]app=1/.test(location.search)) document.documentElement.classList.add('in-app'); } catch (e) {}

/* ---------- icons ---------- */
var P = {
  search: '<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
  pad: '<path d="M6.5 8h11A4.5 4.5 0 0 1 22 12.5v1a3.5 3.5 0 0 1-6.3 2.1L14.5 14h-5l-1.2 1.6A3.5 3.5 0 0 1 2 13.5v-1A4.5 4.5 0 0 1 6.5 8z"/><path d="M7.5 10.5v3M6 12h3"/>',
  bell: '<path d="M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"/><path d="M10 20.5a2 2 0 0 0 4 0"/>',
  gear: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-2.7-1.1l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1A1.6 1.6 0 0 0 3.6 15H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.1-2.7l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1A1.6 1.6 0 0 0 9.6 4.4V4a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 2.7 1.1l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0 1.1 2.7H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z"/>',
  dl: '<path d="M12 4v11"/><path d="m7 10 5 5 5-5"/><path d="M5 20h14"/>',
  play: '<path d="M7 4.8v14.4a.8.8 0 0 0 1.2.7l11.6-7.2a.8.8 0 0 0 0-1.4L8.2 4.1A.8.8 0 0 0 7 4.8z" fill="currentColor" stroke="none"/>',
  pause: '<rect x="6" y="5" width="4" height="14" rx="1" fill="currentColor" stroke="none"/><rect x="14" y="5" width="4" height="14" rx="1" fill="currentColor" stroke="none"/>',
  x: '<path d="M6 6l12 12M18 6 6 18"/>',
  heart: '<path d="M12 20s-7.5-4.7-7.5-10.2A4.3 4.3 0 0 1 12 7.1a4.3 4.3 0 0 1 7.5 2.7C19.5 15.3 12 20 12 20z"/>',
  heartF: '<path d="M12 20s-7.5-4.7-7.5-10.2A4.3 4.3 0 0 1 12 7.1a4.3 4.3 0 0 1 7.5 2.7C19.5 15.3 12 20 12 20z" fill="currentColor"/>',
  info: '<circle cx="12" cy="12" r="9"/><path d="M12 11v6M12 7.5v.1"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  save: '<path d="M5 4h11l3 3v12a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1z"/><path d="M8 4v5h7V4M8 20v-6h8v6"/>',
  load: '<path d="M3 12a9 9 0 1 0 3-6.7L3 8"/><path d="M3 3v5h5"/>',
  cam: '<path d="M4 8h3l2-3h6l2 3h3v11H4z"/><circle cx="12" cy="13" r="3.5"/>',
  exit: '<path d="M15 4h4v16h-4M10 8l-4 4 4 4M6 12h10"/>',
  sliders: '<path d="M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1"/><circle cx="15" cy="6" r="2"/><circle cx="9" cy="12" r="2"/><circle cx="17" cy="18" r="2"/>',
  trophy: '<path d="M8 4h8v5a4 4 0 0 1-8 0z"/><path d="M8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7M10 17h4"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
  check: '<path d="m5 12.5 4.5 4.5L19 7.5"/>',
  trash: '<path d="M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13"/>',
  cloud: '<path d="M7 18a4.5 4.5 0 0 1-.6-9 6 6 0 0 1 11.4 1.6A3.8 3.8 0 0 1 17.5 18z"/>',
  monitor: '<rect x="3" y="4.5" width="18" height="12" rx="2"/><path d="M8.5 20h7M12 16.5V20"/>',
  vol: '<path d="M4 9.5h3.5L12 6v12l-4.5-3.5H4z"/><path d="M15.5 9a4 4 0 0 1 0 6"/>',
  star: '<path d="m12 4 2.4 5 5.4.6-4 3.7 1.1 5.4L12 16l-4.9 2.7 1.1-5.4-4-3.7 5.4-.6z"/>',
  bolt: '<path d="M13 3 5 13.5h6L10 21l8-10.5h-6z"/>'
};
function I(n, s) { return '<svg class="i" viewBox="0 0 24 24"' + (s ? ' style="width:' + s + 'px;height:' + s + 'px"' : '') + ' aria-hidden="true">' + (P[n] || '') + '</svg>'; }
function esc(s) { return String(s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }

/* ---------- data ---------- */
var PL = {
  psp: { n: 'PSP', full: 'PlayStation Portable', c: '#4FC3FF' },
  ps1: { n: 'PS1', full: 'PlayStation', c: '#C4B5FF' },
  ps2: { n: 'PS2', full: 'PlayStation 2', c: '#6F8DFF' }
};
// демо-игры (выдуманные), m — мотив обложки
var GAMES = [
  ['drift', 'Неоновый Дрифт', 'psp', 'Гонки', 2009, 1240, 'Своя версия', ['#3B0D63', '#0A0418', '#FF4FD8'], 'sun', 'Ночные гонки по бесконечному шоссе. Дрифт заряжает ускорение, трасса меняется под музыку.', ['24 трассы и карьера', 'Мультиплеер до 4 игроков', 'Фоторежим']],
  ['orbit', 'Орбита-9', 'psp', 'Стратегия', 2008, 690, 'Своя версия', ['#06305A', '#020814', '#4FC3FF'], 'planet', 'Колонизируй систему из девяти планет и свяжи их в одну торговую сеть.', ['Генерация систем', 'Дипломатия', 'Партии по 30 минут']],
  ['rhythm', 'Ритм Улиц', 'psp', 'Музыка', 2009, 520, 'Демо', ['#25104F', '#080520', '#B48CFF'], 'waves', 'Попадай в бит на уличных площадках. Чем точнее серия, тем ярче район.', ['40 треков', 'Редактор карт', 'Дуэли']],
  ['pixel', 'Пиксельные Войны', 'psp', 'Стратегия', 2009, 310, 'Сборник', ['#24204F', '#07061A', '#FFD84D'], 'city', 'Тактика на клеточных картах: заводы, армия и защита столицы.', ['30 миссий', 'Редактор карт', 'Хот-сит']],
  ['ash', 'Хроники Пепла', 'ps1', 'RPG', 1999, 640, 'Сборник', ['#4A2A14', '#0E0806', '#FFB547'], 'hills', 'Королевство под вулканическим пеплом. Собери отряд и реши, кого спасать.', ['60+ часов', 'Три концовки', 'Русский перевод']],
  ['shards', 'Осколки', 'ps1', 'Головоломка', 1998, 180, 'Своя версия', ['#0B3A33', '#040C0A', '#45E3A0'], 'shards', 'Собери разбитый свет обратно, вращая и отражая лучи.', ['120 уровней', 'Без таймеров', 'Сессии по 5 минут']],
  ['forest', 'Тихий Лес', 'ps1', 'Хоррор', 2000, 560, 'Своя версия', ['#11301B', '#030705', '#9BE36A'], 'hills', 'Ночь в заповеднике, где пропал отряд лесников. Фонарь, рация и голос в эфире.', ['Динамический звук', 'Мало ресурсов', '6–8 часов']],
  ['rubezh', 'Последний Рубеж', 'ps2', 'Экшен', 2004, 1630, 'Демо', ['#4A1A0A', '#0B0606', '#FF8A3D'], 'city', 'Удерживай последние кварталы города вместе с напарником.', ['Кооператив', 'Укрытия', '12 глав']],
  ['deep', 'Глубина', 'ps2', 'Приключения', 2005, 960, 'Демо', ['#05304A', '#01080C', '#3FA9FF'], 'waves', 'Спустись в океанский разлом и найди источник сигнала со дна.', ['Нелинейная карта', 'Улучшения батискафа', 'Атмосферный звук']],
  ['fist', 'Стальной Кулак', 'ps2', 'Файтинг', 2003, 1420, 'Сборник', ['#4A0A16', '#0A0305', '#FF4D5E'], 'shards', 'Турнирный файтинг с 18 бойцами и глубокой системой комбо.', ['18 бойцов', 'Тренировка', 'Дуэли']],
  ['courier', 'Звёздный Курьер', 'ps2', 'Аркада', 2006, 820, 'Демо', ['#16124A', '#04030F', '#7FA6FF'], 'planet', 'Доставляй посылки между станциями и уворачивайся от метеоритов.', ['Свободный полёт', 'Улучшения корабля', 'Таблица рекордов']]
].map(function (a) { return { id: a[0], t: a[1], p: a[2], genre: a[3], year: a[4], size: a[5], kind: a[6], pal: a[7], m: a[8], desc: a[9], feat: a[10] }; });
var byId = {}; GAMES.forEach(function (g, i) { byId[g.id] = g; g.seed = 7 + i * 31; g.isNew = i % 3 === 0; });
var GENRES = GAMES.map(function (g) { return g.genre; }).filter(function (v, i, a) { return a.indexOf(v) === i; });

var lib = {
  drift: { s: 'ok', play: 762, last: 'Сегодня, 21:40', saves: 3 },
  ash: { s: 'ok', play: 1345, last: 'Вчера', saves: 5 },
  shards: { s: 'ok', play: 96, last: '3 дня назад', saves: 1 },
  rubezh: { s: 'ok', play: 210, last: 'На прошлой неделе', saves: 2 },
  orbit: { s: 'dl', pr: 46 },
  fist: { s: 'pause', pr: 12 }
};
var favs = { drift: 1, deep: 1 };
var alerts = [
  { t: '«Хроники Пепла» готова к запуску', d: 'Вчера', u: 1, g: 'ash' },
  { t: 'Новое в магазине: «Звёздный Курьер»', d: '2 дня назад', u: 1, g: 'courier' },
  { t: 'Обновление NEXUS 0.5.1', d: '5 дней назад', u: 0 }
];
var S = { con: 'all', wifi: true, auto: true, res: '2x', fps: false, vol: 80, vib: true, op: 55, sc: 100, hide: true, cloud: true, layout: 'Классика' };
try { var saved = JSON.parse(localStorage.getItem('nexus5') || '{}'); for (var k in saved) S[k] = saved[k]; } catch (e) {}
function save() { try { localStorage.setItem('nexus5', JSON.stringify(S)); } catch (e) {} }

/* ---------- helpers ---------- */
function size(mb) { return mb >= 1000 ? (mb / 1024).toFixed(1).replace('.', ',') + ' ГБ' : mb + ' МБ'; }
function hrs(min) { var h = Math.floor(min / 60), m = min % 60; return h ? h + ' ч' + (m ? ' ' + m + ' мин' : '') : m + ' мин'; }
function st(id) { return lib[id] ? lib[id].s : ''; }
function inCon(g) { return S.con === 'all' || g.p === S.con; }

/* ---------- procedural art ---------- */
var cache = {};
function rnd(seed) { var s = seed % 2147483647 || 1; return function () { s = s * 16807 % 2147483647; return (s - 1) / 2147483646; }; }
function art(g, w, h, k) {
  var key = g.id + w + h + (k || 0); if (cache[key]) return cache[key];
  var r = rnd(g.seed + (k || 0) * 101), a = g.pal[0], b = g.pal[1], c = g.pal[2], s = '', i;
  s += "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 " + w + ' ' + h + "' preserveAspectRatio='xMidYMid slice'><defs>";
  s += "<linearGradient id='a' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='" + a + "'/><stop offset='1' stop-color='" + b + "'/></linearGradient>";
  s += "<radialGradient id='g'><stop offset='0' stop-color='" + c + "' stop-opacity='.6'/><stop offset='1' stop-color='" + c + "' stop-opacity='0'/></radialGradient></defs>";
  s += "<rect width='" + w + "' height='" + h + "' fill='url(#a)'/>";
  for (i = 0; i < 50; i++) s += "<circle cx='" + (r() * w).toFixed(0) + "' cy='" + (r() * h * .65).toFixed(0) + "' r='" + (r() * 1.5 + .3).toFixed(1) + "' fill='#fff' opacity='" + (r() * .6 + .1).toFixed(2) + "'/>";
  var m = Math.min(w, h), cx = w * (.45 + r() * .3), hz = h * .62;
  if (g.m === 'sun') {
    s += "<circle cx='" + cx + "' cy='" + hz + "' r='" + m * .7 + "' fill='url(#g)'/><circle cx='" + cx + "' cy='" + hz + "' r='" + m * .28 + "' fill='" + c + "'/>";
    for (i = 0; i < 6; i++) s += "<rect x='0' y='" + (hz - m * .05 + i * m * .05).toFixed(0) + "' width='" + w + "' height='" + (2 + i * 1.5) + "' fill='" + a + "'/>";
    s += "<rect y='" + hz + "' width='" + w + "' height='" + (h - hz) + "' fill='" + b + "'/>";
    for (i = -14; i <= 14; i++) s += "<line x1='" + cx + "' y1='" + hz + "' x2='" + (cx + i * w * .1) + "' y2='" + h + "' stroke='" + c + "' stroke-opacity='.5'/>";
    for (i = 1; i < 9; i++) { var y = hz + Math.pow(i / 9, 2) * (h - hz); s += "<line x1='0' y1='" + y.toFixed(0) + "' x2='" + w + "' y2='" + y.toFixed(0) + "' stroke='" + c + "' stroke-opacity='.4'/>"; }
  } else if (g.m === 'planet') {
    var pr = m * .26, py = h * .42;
    s += "<circle cx='" + cx + "' cy='" + py + "' r='" + pr * 2.4 + "' fill='url(#g)'/><circle cx='" + cx + "' cy='" + py + "' r='" + pr + "' fill='" + c + "'/><circle cx='" + (cx - pr * .35) + "' cy='" + (py - pr * .3) + "' r='" + pr * .85 + "' fill='" + a + "' opacity='.4'/>";
    s += "<ellipse cx='" + cx + "' cy='" + py + "' rx='" + pr * 1.9 + "' ry='" + pr * .42 + "' fill='none' stroke='" + c + "' stroke-width='3' transform='rotate(-15 " + cx + ' ' + py + ")'/>";
  } else if (g.m === 'city') {
    s += "<circle cx='" + cx + "' cy='" + h * .3 + "' r='" + m * .55 + "' fill='url(#g)'/>";
    var x = 0; while (x < w) { var bw = w * (.05 + r() * .09), bh = h * (.2 + r() * .45); s += "<rect x='" + x.toFixed(0) + "' y='" + (h - bh).toFixed(0) + "' width='" + bw.toFixed(0) + "' height='" + bh.toFixed(0) + "' fill='" + b + "'/>"; for (var wy = h - bh + 8; wy < h - 6; wy += 11) for (var wx = x + 5; wx < x + bw - 5; wx += 8) if (r() > .65) s += "<rect x='" + wx.toFixed(0) + "' y='" + wy.toFixed(0) + "' width='3' height='4' fill='" + c + "'/>"; x += bw + 2; }
  } else if (g.m === 'hills') {
    s += "<circle cx='" + cx + "' cy='" + h * .3 + "' r='" + m * .12 + "' fill='" + c + "'/><circle cx='" + cx + "' cy='" + h * .3 + "' r='" + m * .45 + "' fill='url(#g)'/>";
    for (var L = 0; L < 3; L++) { var pts = '0,' + h, base = h * (.5 + L * .13); for (var px = 0; px <= w; px += w / 8) pts += ' ' + px.toFixed(0) + ',' + (base - r() * h * .18).toFixed(0); pts += ' ' + w + ',' + h; s += "<polygon points='" + pts + "' fill='" + (L === 2 ? b : c) + "' opacity='" + (L === 2 ? 1 : .2 + L * .2) + "'/>"; }
  } else if (g.m === 'waves') {
    s += "<circle cx='" + cx + "' cy='" + h * .45 + "' r='" + m * .55 + "' fill='url(#g)'/>";
    for (i = 0; i < 10; i++) { var y0 = h * (.3 + i * .07), am = h * (.03 + r() * .05), d = 'M0 ' + y0.toFixed(0); for (var xx = 0; xx <= w; xx += w / 24) d += ' L' + xx.toFixed(0) + ' ' + (y0 + Math.sin(xx / w * Math.PI * (2 + i * .4) + i) * am).toFixed(0); s += "<path d='" + d + "' fill='none' stroke='" + c + "' stroke-width='" + (1.5 + i * .3).toFixed(1) + "' opacity='" + (.2 + i * .07).toFixed(2) + "'/>"; }
  } else {
    s += "<circle cx='" + cx + "' cy='" + h * .45 + "' r='" + m * .55 + "' fill='url(#g)'/>";
    for (i = 0; i < 16; i++) { var tx = r() * w, ty = r() * h, z = m * (.08 + r() * .25); s += "<polygon points='" + tx.toFixed(0) + ',' + (ty - z).toFixed(0) + ' ' + (tx + z * .9).toFixed(0) + ',' + (ty + z * .6).toFixed(0) + ' ' + (tx - z * .7).toFixed(0) + ',' + (ty + z * .4).toFixed(0) + "' fill='" + (r() > .5 ? c : a) + "' opacity='" + (.15 + r() * .55).toFixed(2) + "'/>"; }
  }
  s += '</svg>';
  return cache[key] = "url('data:image/svg+xml," + encodeURIComponent(s).replace(/'/g, '%27').replace(/\(/g, '%28').replace(/\)/g, '%29') + "')";
}

/* ---------- background ---------- */
var bgI = $('#bg').children, bgN = 0, bgKey = '';
function setBg(g) { if (!g || bgKey === g.id) return; bgKey = g.id; var nx = bgI[1 - bgN]; nx.style.backgroundImage = art(g, 1600, 900, 3); nx.classList.add('on'); bgI[bgN].classList.remove('on'); bgN = 1 - bgN; }

/* ---------- shared bits ---------- */
function consSwitch() {
  var opts = [['all', 'Все', '#fff']].concat(Object.keys(PL).map(function (k) { return [k, PL[k].n, PL[k].c]; }));
  return '<div class="cons" role="tablist" aria-label="Консоль">' + opts.map(function (o) {
    var src = tab === 'games' ? Object.keys(lib).map(function (i) { return byId[i]; }) : GAMES, n = src.filter(function (g) { return o[0] === 'all' || g.p === o[0]; }).length;
    return '<button role="tab" aria-selected="' + (S.con === o[0]) + '" class="' + (S.con === o[0] ? 'on' : '') + '" data-a="con" data-v="' + o[0] + '" style="--c:' + o[2] + '"><i></i>' + o[1] + ' <span>' + n + '</span></button>';
  }).join('') + '</div>';
}
function card(g) {
  var s = st(g.id), badge = s === 'ok' ? 'Есть' : (s === 'dl' || s === 'pause') ? Math.floor(lib[g.id].pr) + '%' : '';
  return '<a class="card" href="#game-' + g.id + '"><span class="art" style="background-image:' + art(g, 300, 400) + '"><span class="pl">' + PL[g.p].n + '</span>' + (badge ? '<span class="st">' + badge + '</span>' : '') + '</span><b>' + g.t + '</b><small>' + g.genre + ' · ' + size(g.size) + '</small></a>';
}

/* ---------- GAMES tab ---------- */
var focusId = null;
function myGames() {
  var order = { ok: 0, dl: 1, pause: 1, q: 1 };
  return Object.keys(lib).map(function (i) { return byId[i]; }).filter(inCon).sort(function (a, b) { return order[lib[a.id].s] - order[lib[b.id].s]; });
}
function focusHtml(g) {
  var L = lib[g.id], p = PL[g.p], meta, acts;
  if (L.s === 'ok') {
    meta = '<span>Наиграно <b>' + hrs(L.play) + '</b></span><span>' + L.last + '</span><span>' + L.saves + ' сохранения</span>';
    acts = '<button class="b b-main" data-a="play" data-id="' + g.id + '">' + I('play') + 'Играть</button>';
  } else {
    meta = '<span>' + (L.s === 'pause' ? 'Загрузка на паузе' : 'Загружается') + ' · <b data-pr="' + g.id + '">' + Math.floor(L.pr) + '%</b></span><span>' + size(g.size) + '</span>';
    acts = '<div class="bar"><i data-bar="' + g.id + '" style="width:' + L.pr + '%"></i></div><button class="b b-glass b-sm" data-a="' + (L.s === 'pause' ? 'resume' : 'pause') + '" data-id="' + g.id + '">' + I(L.s === 'pause' ? 'play' : 'pause', 18) + (L.s === 'pause' ? 'Продолжить' : 'Пауза') + '</button>';
  }
  return '<span class="tag"><i></i>' + p.full + ' · ' + g.genre + '</span><h1>' + g.t + '</h1><div class="meta">' + meta + '</div><div class="acts">' + acts +
    '<a class="b b-glass" href="#game-' + g.id + '">' + I('info', 18) + 'Об игре</a><button class="b b-glass b-round' + (favs[g.id] ? ' fav' : '') + '" data-a="fav" data-id="' + g.id + '" aria-label="Избранное">' + I(favs[g.id] ? 'heartF' : 'heart', 20) + '</button></div>';
}
function vGames() {
  var list = myGames();
  if (!list.length) return '<div class="home"><div class="home-top">' + consSwitch() + '</div><div class="focus"><span class="tag">Пусто</span><h1>Здесь пока нет игр</h1><div class="meta">Скачай что-нибудь в магазине.</div><div class="acts"><a class="b b-main" href="#store">Открыть магазин</a></div></div></div>';
  if (!focusId || !list.some(function (g) { return g.id === focusId; })) focusId = list[0].id;
  var f = byId[focusId];
  return '<div class="home" style="--pc:' + PL[f.p].c + '"><div class="home-top">' + consSwitch() + '</div><div class="focus" id="focus">' + focusHtml(f) + '</div>' +
    '<div class="shelf" id="shelf">' + list.map(function (g) {
      var L = lib[g.id], dl = L.s !== 'ok';
      return '<button class="tile' + (g.id === focusId ? ' on' : '') + (dl ? ' dlg' : '') + '" data-a="tile" data-id="' + g.id + '" style="background-image:' + art(g, 300, 300) + '" aria-label="' + esc(g.t) + '"><span class="pl">' + PL[g.p].n + '</span>' + (dl ? '<span class="pg"><i data-bar="' + g.id + '" style="width:' + L.pr + '%"></i></span>' : '') + '</button>';
    }).join('') + '<a class="tile add" href="#store"><span>' + I('plus', 26) + 'Магазин</span></a></div></div>';
}
function setFocus(id) {
  if (id === focusId) return; focusId = id; var g = byId[id];
  document.querySelectorAll('.tile').forEach(function (t) { t.classList.toggle('on', t.getAttribute('data-id') === id); });
  var f = $('#focus'); if (!f) return; f.innerHTML = focusHtml(g); f.classList.remove('swap'); void f.offsetWidth; f.classList.add('swap');
  $('.home').style.setProperty('--pc', PL[g.p].c); setBg(g);
  if (S.vib && navigator.vibrate) try { navigator.vibrate(6); } catch (e) {}
}

/* ---------- STORE tab ---------- */
var featIdx = 0, featTimer = null;
function storeList() { return GAMES.filter(inCon); }
function featHtml() {
  var list = storeList().filter(function (g) { return g.isNew; }); if (!list.length) list = storeList();
  featIdx %= list.length; var g = list[featIdx], s = st(g.id);
  return '<div class="feat" id="feat" style="background-image:' + art(g, 1400, 420, 5) + '"><div class="feat-in"><small>Новое · ' + PL[g.p].full + '</small><h2>' + g.t + '</h2><p>' + g.desc + '</p><div class="acts">' +
    (s === 'ok' ? '<button class="b b-main b-sm" data-a="play" data-id="' + g.id + '">' + I('play', 18) + 'Играть</button>' : s ? '<a class="b b-glass b-sm" href="#downloads">Загружается</a>' : '<button class="b b-main b-sm" data-a="get" data-id="' + g.id + '">' + I('dl', 18) + 'Скачать · ' + size(g.size) + '</button>') +
    '<a class="b b-glass b-sm" href="#game-' + g.id + '">Подробнее</a></div></div><div class="feat-dots">' + list.map(function (x, i) { return '<button class="' + (i === featIdx ? 'on' : '') + '" data-a="feat" data-v="' + i + '" aria-label="Слайд ' + (i + 1) + '"></button>'; }).join('') + '</div></div>';
}
function row(title, list, sub) { if (!list.length) return ''; return '<section><div class="row-h"><h3>' + title + '</h3>' + (sub ? '<span>' + sub + '</span>' : '') + '</div><div class="row">' + list.map(card).join('') + '</div></section>'; }
function vStore() {
  var list = storeList();
  return '<div class="scroll"><div class="store-in">' + consSwitch() + featHtml() +
    '<section><div class="row-h"><h3>Жанры</h3></div><div class="genres">' + GENRES.filter(function (gn) { return list.some(function (g) { return g.genre === gn; }); }).map(function (gn) { var g = list.filter(function (x) { return x.genre === gn; })[0]; return '<a class="genre" href="#search" data-a="genre" data-v="' + gn + '" style="background-image:' + art(g, 400, 160, 9) + '">' + gn + '</a>'; }).join('') + '</div></section>' +
    row('Новое в магазине', list.filter(function (g) { return g.isNew; })) +
    row('Популярное', list.slice().sort(function (a, b) { return b.size - a.size; }).slice(0, 8), 'по загрузкам') +
    row('Небольшие игры', list.filter(function (g) { return g.size < 700; }), 'до 700 МБ') +
    row('Все игры', list, list.length + ' шт.') + '</div></div>';
}

/* ---------- overlays ---------- */
var gTab = 'about', setTab = 'general', query = '';
function oGame(id) {
  var g = byId[id]; if (!g) return ''; var s = st(id), L = lib[id], p = PL[g.p], btn;
  if (s === 'ok') btn = '<button class="b b-main" data-a="play" data-id="' + id + '">' + I('play') + 'Играть</button>';
  else if (s === 'dl') btn = '<button class="b b-glass" data-a="pause" data-id="' + id + '">' + I('pause', 18) + 'Пауза · <span data-pr="' + id + '">' + Math.floor(L.pr) + '%</span></button>';
  else if (s === 'pause') btn = '<button class="b b-main" data-a="resume" data-id="' + id + '">' + I('play') + 'Продолжить · ' + Math.floor(L.pr) + '%</button>';
  else btn = '<button class="b b-main" data-a="get" data-id="' + id + '">' + I('dl') + 'Скачать</button>';
  var body;
  if (gTab === 'about') body = '<p class="txt">' + g.desc + '</p><ul class="list">' + g.feat.map(function (f) { return '<li>' + f + '</li>'; }).join('') + '</ul>';
  else if (gTab === 'shots') body = '<div class="shots">' + [1, 2, 3, 4].map(function (n) { return '<div class="shot" style="background-image:' + art(g, 480, 272, n * 13) + '"></div>'; }).join('') + '</div>';
  else body = '<div class="kv">' + [['Платформа', p.full], ['Жанр', g.genre], ['Год', g.year], ['Размер', size(g.size)], ['Тип', g.kind], ['Языки', 'RU, EN'], ['Регион', 'EU'], ['Формат', g.p === 'ps1' ? 'CHD' : 'ISO']].map(function (r) { return '<div><small>' + r[0] + '</small><b>' + r[1] + '</b></div>'; }).join('') + '</div>';
  return '<div class="scrim" data-a="close"></div><div class="sheet" role="dialog" aria-label="' + esc(g.t) + '" style="--pc:' + p.c + '">' +
    '<div class="gs-hero" style="background-image:' + art(g, 1400, 300, 3) + '"><button class="b b-glass b-round gs-x" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div>' +
    '<div class="gs-body"><div class="gs-side"><div class="gs-cover" style="background-image:' + art(g, 300, 400) + '"></div>' + btn +
    '<button class="b b-glass' + (favs[id] ? ' fav' : '') + '" data-a="fav" data-id="' + id + '">' + I(favs[id] ? 'heartF' : 'heart', 18) + (favs[id] ? 'В избранном' : 'В избранное') + '</button>' +
    (s === 'ok' ? '<button class="b b-glass" data-a="remove" data-id="' + id + '">' + I('trash', 18) + 'Удалить</button>' : '') + '</div>' +
    '<div class="gs-main"><div class="pills"><span class="pill c">' + p.full + '</span><span class="pill">' + g.genre + '</span><span class="pill">' + g.year + '</span><span class="pill">' + g.kind + '</span><span class="pill ok">' + I('check', 14) + 'Работает отлично</span></div>' +
    '<h2>' + g.t + '</h2>' + (s === 'ok' ? '<div class="pills"><span class="pill">' + I('clock', 14) + hrs(L.play) + '</span><span class="pill">' + I('save', 14) + L.saves + ' сохр.</span></div>' : '') +
    '<div class="seg">' + [['about', 'Обзор'], ['shots', 'Скриншоты'], ['info', 'Детали']].map(function (t) { return '<button class="' + (gTab === t[0] ? 'on' : '') + '" data-a="gtab" data-v="' + t[0] + '">' + t[1] + '</button>'; }).join('') + '</div>' + body + '</div></div></div>';
}
function oSearch() {
  var q = query.trim().toLowerCase(), res = q ? GAMES.filter(function (g) { return (g.t + ' ' + g.genre + ' ' + PL[g.p].full + ' ' + PL[g.p].n).toLowerCase().indexOf(q) >= 0; }) : [];
  return '<div class="full" role="dialog" aria-label="Поиск"><div class="ov-h"><label class="sbox">' + I('search') + '<input id="q" type="search" placeholder="Игра, жанр или консоль" value="' + esc(query) + '" autocomplete="off"></label><button class="b b-glass b-round" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div><div class="ov-b" id="sres">' + searchBody(q, res) + '</div></div>';
}
function searchBody(q, res) {
  if (!q) return '<div class="sub">Жанры</div><div class="pills">' + GENRES.map(function (gn) { return '<button class="pill" data-a="genreq" data-v="' + gn + '">' + gn + '</button>'; }).join('') + '</div><div class="sub">Консоли</div><div class="pills">' + Object.keys(PL).map(function (k) { return '<button class="pill" data-a="genreq" data-v="' + PL[k].n + '">' + PL[k].full + '</button>'; }).join('') + '</div>';
  return res.length ? '<div class="sub">Найдено: ' + res.length + '</div><div class="sgrid">' + res.map(card).join('') + '</div>' : '<div class="sub">Ничего не нашлось по запросу «' + esc(q) + '»</div>';
}
function oDownloads() {
  var act = Object.keys(lib).filter(function (i) { return lib[i].s !== 'ok'; }), done = Object.keys(lib).filter(function (i) { return lib[i].s === 'ok'; });
  var used = done.reduce(function (a, i) { return a + byId[i].size; }, 0);
  return '<div class="scrim" data-a="close"></div><aside class="drawer" aria-label="Загрузки"><div class="ov-h"><h2>Загрузки</h2><button class="b b-glass b-round b-sm" data-a="close" aria-label="Закрыть">' + I('x', 18) + '</button></div><div class="ov-b">' +
    (act.length ? '<div class="sub">Сейчас</div>' + act.map(function (i) { var g = byId[i], L = lib[i]; return '<div class="it"><span class="th" style="background-image:' + art(g, 300, 300) + '"></span><span class="tx"><b>' + g.t + '</b><span>' + (L.s === 'pause' ? 'На паузе' : 'Загружается') + ' · <span data-pr="' + i + '">' + Math.floor(L.pr) + '%</span> из ' + size(g.size) + '</span><span class="pb"><i data-bar="' + i + '" style="width:' + L.pr + '%"></i></span></span><button class="b b-glass b-round b-sm" data-a="' + (L.s === 'pause' ? 'resume' : 'pause') + '" data-id="' + i + '" aria-label="' + (L.s === 'pause' ? 'Продолжить' : 'Пауза') + '">' + I(L.s === 'pause' ? 'play' : 'pause', 16) + '</button><button class="b b-glass b-round b-sm" data-a="cancel" data-id="' + i + '" aria-label="Отменить">' + I('x', 16) + '</button></div>'; }).join('') : '<div class="sub">Активных загрузок нет</div>') +
    '<div class="sub">Установлено</div>' + done.map(function (i) { var g = byId[i]; return '<div class="it"><span class="th" style="background-image:' + art(g, 300, 300) + '"></span><span class="tx"><b>' + g.t + '</b><span>' + PL[g.p].n + ' · ' + size(g.size) + '</span></span><button class="b b-main b-sm" data-a="play" data-id="' + i + '">Играть</button></div>'; }).join('') +
    '<div class="space"><b>Память</b><div class="bar"><i style="width:' + Math.min(100, (used + 14000) / 32768 * 100).toFixed(0) + '%"></i></div><span>Игры NEXUS ' + size(used) + ' · свободно ' + size(32768 - used - 14000) + ' (демо)</span></div></div></aside>';
}
function oAlerts() {
  return '<div class="scrim" data-a="close"></div><aside class="drawer" aria-label="Уведомления"><div class="ov-h"><h2>Уведомления</h2><button class="b b-glass b-sm" data-a="readall">Прочитать все</button><button class="b b-glass b-round b-sm" data-a="close" aria-label="Закрыть">' + I('x', 18) + '</button></div><div class="ov-b">' +
    alerts.map(function (a) { var g = a.g && byId[a.g]; return '<a class="it' + (a.u ? ' unread' : '') + '" href="' + (g ? '#game-' + g.id : '#settings') + '"><span class="th"' + (g ? ' style="background-image:' + art(g, 300, 300) + '"' : '') + '>' + (g ? '' : I('bolt', 20)) + '</span><span class="tx"><b>' + a.t + '</b><span>' + a.d + '</span></span></a>'; }).join('') + '</div></aside>';
}
function tg(k) { return '<button class="tg" role="switch" aria-checked="' + !!S[k] + '" data-a="tg" data-k="' + k + '" aria-label="Переключить"></button>'; }
function seg(k, opts) { return '<div class="seg">' + opts.map(function (o) { return '<button class="' + (S[k] === o ? 'on' : '') + '" data-a="set" data-k="' + k + '" data-v="' + o + '">' + o + '</button>'; }).join('') + '</div>'; }
function rngC(k, a, b) { return '<div class="rng"><input type="range" min="' + a + '" max="' + b + '" value="' + S[k] + '" data-k="' + k + '" aria-label="' + k + '"><output id="o-' + k + '">' + S[k] + '%</output></div>'; }
function opt(t, d, c) { return '<div class="opt"><span class="tx"><b>' + t + '</b>' + (d ? '<span>' + d + '</span>' : '') + '</span>' + c + '</div>'; }
function oSettings() {
  var nav = [['general', 'Общие', 'sliders'], ['dl', 'Загрузки', 'dl'], ['gfx', 'Графика', 'monitor'], ['snd', 'Звук', 'vol'], ['ctl', 'Управление', 'pad'], ['about', 'О приложении', 'info']], b = '';
  if (setTab === 'general') b = opt('Консоль при запуске', 'С какой консоли открывать вкладку «Игры»', seg('con', ['all', 'psp', 'ps1', 'ps2']).replace(/>all</, '>Все<').replace(/>psp</, '>PSP<').replace(/>ps1</, '>PS1<').replace(/>ps2</, '>PS2<')) + opt('Вибрация в меню', 'Короткий отклик при выборе игры', tg('vib')) + opt('Облачные сохранения', 'Сохранения копируются в облако (в планах)', tg('cloud'));
  if (setTab === 'dl') b = opt('Только по Wi-Fi', 'Не тратить мобильный трафик', tg('wifi')) + opt('Устанавливать автоматически', 'Сразу после загрузки', tg('auto')) + opt('Папка с играми', '/storage/emulated/0/NEXUS', '<button class="b b-glass b-sm" data-a="toast" data-v="Откроется выбор папки Android">Изменить</button>');
  if (setTab === 'gfx') b = opt('Разрешение', 'Для всех игр по умолчанию', seg('res', ['1x', '2x', '3x', '4x'])) + opt('Счётчик FPS', 'Частота кадров в углу экрана', tg('fps'));
  if (setTab === 'snd') b = opt('Громкость игр', '', rngC('vol', 0, 100));
  if (setTab === 'ctl') b = opt('Экранный геймпад', 'Раскладка, размер и прозрачность', '<a class="b b-main b-sm" href="#pad">Настроить</a>') + opt('Скрывать кнопки', 'Когда подключён настоящий геймпад', tg('hide'));
  if (setTab === 'about') b = opt('NEXUS', 'Версия 0.5.0 · прототип интерфейса', '') + opt('Эмуляторы', 'PPSSPP (PSP), DuckStation (PS1), PCSX2 (PS2) — будут подключены', '') + opt('Лицензии', 'Открытые компоненты и их лицензии', '<button class="b b-glass b-sm" data-a="toast" data-v="Список лицензий появится позже">Открыть</button>');
  return '<div class="full" role="dialog" aria-label="Настройки"><div class="ov-h"><h2>Настройки</h2><button class="b b-glass b-round" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div><div class="ov-b"><div class="set"><nav class="set-nav">' +
    nav.map(function (n) { return '<button class="' + (setTab === n[0] ? 'on' : '') + '" data-a="stab" data-v="' + n[0] + '">' + I(n[2], 20) + n[1] + '</button>'; }).join('') + '</nav><div>' + b + '</div></div></div></div>';
}
function oProfile() {
  var ok = Object.keys(lib).filter(function (i) { return lib[i].s === 'ok'; }), play = ok.reduce(function (a, i) { return a + lib[i].play; }, 0), sv = ok.reduce(function (a, i) { return a + lib[i].saves; }, 0);
  var xp = play * 2 + sv * 30, lvl = Math.floor(xp / 1000) + 1;
  var cons = {}; ok.forEach(function (i) { cons[byId[i].p] = 1; });
  var A = [['bolt', 'Первый запуск', ok.length > 0], ['clock', 'Марафон 10 ч', ok.some(function (i) { return lib[i].play >= 600; })], ['save', '10 сохранений', sv >= 10], ['star', 'Три консоли', Object.keys(cons).length >= 3], ['trophy', '100 часов', play >= 6000], ['heart', '5 в избранном', Object.keys(favs).length >= 5]];
  return '<div class="scrim" data-a="close"></div><aside class="drawer" aria-label="Профиль"><div class="ov-h"><h2>Профиль</h2><button class="b b-glass b-round b-sm" data-a="close" aria-label="Закрыть">' + I('x', 18) + '</button></div><div class="ov-b">' +
    '<div class="prof"><span class="av">N</span><span><h3>Игрок</h3><span>Уровень ' + lvl + ' · ' + (xp % 1000) + ' / 1000 XP</span><div class="lvl"><i style="width:' + (xp % 1000) / 10 + '%"></i></div></span></div>' +
    '<div class="nums"><div><b>' + hrs(play).split(' ').slice(0, 2).join(' ') + '</b><span>наиграно</span></div><div><b>' + ok.length + '</b><span>игр установлено</span></div><div><b>' + sv + '</b><span>сохранений</span></div><div><b>' + Object.keys(cons).length + '/3</b><span>консолей</span></div></div>' +
    '<div class="sub">Достижения</div><div class="ach">' + A.map(function (a) { return '<div class="' + (a[2] ? '' : 'off') + '">' + I(a[0]) + a[1] + '</div>'; }).join('') + '</div>' +
    '<div class="sub">Аккаунт</div>' + opt('Облачные сохранения', 'Продолжай на другом телефоне (в планах)', tg('cloud')) + '</div></aside>';
}
function ctl() {
  var k = function (cls, pos, t, l) { return '<span class="k ' + cls + '" style="' + pos + '" aria-label="' + l + '">' + t + '</span>'; };
  return '<div class="ctl" style="--op:' + S.op / 100 + ';--sc:' + S.sc / 100 + '">' +
    k('s', 'left:3%;top:6%;width:13%;height:11%', 'L', 'L') + k('s', 'right:3%;top:6%;width:13%;height:11%', 'R', 'R') +
    k('s', 'left:9%;top:44%;width:6%;aspect-ratio:1', '▲', 'Вверх') + k('s', 'left:9%;top:calc(44% + 12cqw);width:6%;aspect-ratio:1', '▼', 'Вниз') +
    k('s', 'left:3%;top:calc(44% + 6cqw);width:6%;aspect-ratio:1', '◀', 'Влево') + k('s', 'left:15%;top:calc(44% + 6cqw);width:6%;aspect-ratio:1', '▶', 'Вправо') +
    k('r', 'right:10%;top:40%;width:6.5%;aspect-ratio:1', '△', 'Треугольник') + k('r', 'right:3.5%;top:calc(40% + 6.5cqw);width:6.5%;aspect-ratio:1', '○', 'Круг') +
    k('r', 'right:10%;top:calc(40% + 13cqw);width:6.5%;aspect-ratio:1', '✕', 'Крест') + k('r', 'right:16.5%;top:calc(40% + 6.5cqw);width:6.5%;aspect-ratio:1', '□', 'Квадрат') +
    k('s', 'left:40%;bottom:5%;width:8%;height:7%;border-radius:20px;font-size:1.2cqw', 'SELECT', 'Select') + k('s', 'left:52%;bottom:5%;width:8%;height:7%;border-radius:20px;font-size:1.2cqw', 'START', 'Start') + '</div>';
}
function oPad() {
  return '<div class="full" role="dialog" aria-label="Экранный геймпад"><div class="ov-h"><h2>Экранный геймпад</h2><button class="b b-glass b-sm" data-a="padreset">Сбросить</button><button class="b b-glass b-round" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div><div class="ov-b"><div class="pad">' +
    '<div class="pad-v" id="padv" style="background-image:' + art(byId.drift, 1600, 900, 11) + '">' + ctl() + '</div><div>' +
    opt('Раскладка', '', seg('layout', ['Классика', 'Компакт', 'Своя'])) + opt('Прозрачность', '', rngC('op', 15, 100)) + opt('Размер кнопок', '', rngC('sc', 70, 130)) + opt('Вибрация', 'При нажатии кнопок', tg('vib')) + opt('Скрывать с геймпадом', 'Когда подключён контроллер', tg('hide')) + '</div></div></div></div>';
}
var paused = false, booted = {};
function oPlay(id) {
  var g = byId[id]; if (!g) return ''; var p = PL[g.p];
  return '<div class="play' + (booted[id] ? ' ready' : '') + (paused ? ' paused' : '') + '" id="play" style="--pc:' + p.c + '"><div class="play-g" style="background-image:' + art(g, 1600, 900, 11) + '"></div>' + ctl() +
    '<button class="play-pause" data-a="pausegame">' + I('pause', 14) + 'Меню</button>' +
    '<div class="qm"><div class="qm-in"><span class="pill c" style="align-self:flex-start">' + p.full + '</span><h2>' + g.t + '</h2><div class="qm-row">' +
    [['main', 'play', 'Продолжить', 'resumegame'], ['', 'save', 'Сохранить', 'qsave'], ['', 'load', 'Загрузить', 'qload'], ['', 'cam', 'Скриншот', 'qshot'], ['', 'sliders', 'Настройки', 'qset'], ['bad', 'exit', 'Выйти', 'exitgame']].map(function (b) { return '<button class="qb ' + b[0] + '" data-a="' + b[3] + '" data-id="' + id + '">' + I(b[1], 24) + b[2] + '</button>'; }).join('') + '</div></div></div>' +
    '<div class="boot"><div class="logo' + (g.p === 'psp' ? ' psp' : '') + '"></div><b>' + g.t + '</b><span>' + p.full.toUpperCase() + '</span></div></div>';
}

/* ---------- router ---------- */
var tab = 'games', layerKey = '';
function hash() { try { return (location.hash || '').slice(1); } catch (e) { return ''; } }
function render() {
  var h = hash() || 'games';
  if (h === 'games' || h === 'store') tab = h;
  document.body.classList.toggle('store', tab === 'store');
  document.querySelectorAll('.tabs a').forEach(function (a) { a.classList.toggle('on', a.getAttribute('data-tab') === tab); });
  $('#page').innerHTML = tab === 'games' ? vGames() : vStore();
  if (tab === 'games' && focusId) setBg(byId[focusId]);
  if (tab === 'store') { var l = storeList().filter(function (g) { return g.isNew; }); setBg(l[featIdx % (l.length || 1)] || storeList()[0]); }
  renderLayer(h);
  top();
}
function renderLayer(h) {
  var L = $('#layer'), html = '';
  if (h.indexOf('game-') === 0) html = oGame(h.slice(5));
  else if (h === 'search') html = oSearch();
  else if (h === 'downloads') html = oDownloads();
  else if (h === 'alerts') html = oAlerts();
  else if (h === 'settings') html = oSettings();
  else if (h === 'profile') html = oProfile();
  else if (h === 'pad') html = oPad();
  else if (h.indexOf('play-') === 0) html = oPlay(h.slice(5));
  var was = layerKey; layerKey = html ? h : '';
  L.innerHTML = html;
  if (html && was !== h) { L.classList.remove('on'); void L.offsetWidth; requestAnimationFrame(function () { L.classList.add('on'); }); } else L.classList.toggle('on', !!html);
  if (h === 'search') { var q = $('#q'); if (q && was !== h) setTimeout(function () { q.focus(); }, 60); }
  if (h.indexOf('play-') === 0) { var pid = h.slice(5); if (!booted[pid]) setTimeout(function () { booted[pid] = true; var el = $('#play'); if (el) el.classList.add('ready'); }, 1800); }
}
function close() { location.hash = tab; }
window.addEventListener('hashchange', function () { var h = hash(); if (h.indexOf('game-') === 0 && layerKey.indexOf('game-') !== 0) gTab = 'about'; render(); });

/* ---------- top bar state ---------- */
function top() {
  var act = Object.keys(lib).filter(function (i) { return lib[i].s !== 'ok'; }), dl = $('#dlBtn');
  var pr = act.length ? act.reduce(function (a, i) { return a + lib[i].pr; }, 0) / act.length : 0, C = 2 * Math.PI * 12;
  dl.innerHTML = I('dl', 20) + (act.length ? '<svg class="ring" viewBox="0 0 28 28"><circle cx="14" cy="14" r="12" stroke="rgba(255,255,255,.2)"/><circle cx="14" cy="14" r="12" stroke="#fff" stroke-dasharray="' + C.toFixed(1) + '" stroke-dashoffset="' + (C * (1 - pr / 100)).toFixed(1) + '"/></svg><b>' + act.length + '</b>' : '');
  $('#alertBtn').innerHTML = I('bell', 20) + (alerts.some(function (a) { return a.u; }) ? '<span class="dot"></span>' : '');
}
document.querySelectorAll('[data-i]').forEach(function (el) { el.innerHTML = I(el.getAttribute('data-i'), 20); });
function clock() { var d = new Date(); $('#clock').textContent = ('0' + d.getHours()).slice(-2) + ':' + ('0' + d.getMinutes()).slice(-2); }
clock(); setInterval(clock, 15000);

/* ---------- downloads engine ---------- */
function pump() { if (Object.keys(lib).some(function (i) { return lib[i].s === 'dl'; })) return; var q = Object.keys(lib).filter(function (i) { return lib[i].s === 'q'; })[0]; if (q) lib[q].s = 'dl'; }
setInterval(function () {
  var done = false;
  Object.keys(lib).forEach(function (i) {
    var L = lib[i]; if (L.s !== 'dl') return;
    L.pr = Math.min(100, L.pr + (8 + Math.random() * 6) / byId[i].size * 100 * 1.5);
    if (L.pr >= 100) { lib[i] = { s: 'ok', play: 0, last: 'Ещё не запускалась', saves: 0 }; done = true; alerts.unshift({ t: '«' + byId[i].t + '» установлена', d: 'Только что', u: 1, g: i }); toast(I('check', 18) + '«' + byId[i].t + '» установлена'); }
    else {
      document.querySelectorAll('[data-pr="' + i + '"]').forEach(function (n) { n.textContent = Math.floor(L.pr) + '%'; });
      document.querySelectorAll('[data-bar="' + i + '"]').forEach(function (n) { n.style.width = L.pr + '%'; });
    }
  });
  if (done) { pump(); render(); } else top();
}, 600);

/* ---------- toast ---------- */
var tT;
function toast(h) { var t = $('#toast'); t.innerHTML = h; t.classList.add('on'); clearTimeout(tT); tT = setTimeout(function () { t.classList.remove('on'); }, 2300); }

/* ---------- actions ---------- */
document.addEventListener('click', function (e) {
  var t = e.target.closest('[data-a]'); if (!t) return;
  var a = t.getAttribute('data-a'), id = t.getAttribute('data-id'), v = t.getAttribute('data-v'), g = byId[id];
  switch (a) {
    case 'con': S.con = v; save(); focusId = null; featIdx = 0; render(); break;
    case 'tile': e.preventDefault(); if (id === focusId) { if (st(id) === 'ok') location.hash = 'play-' + id; else location.hash = 'game-' + id; } else { setFocus(id); t.scrollIntoView({ behavior: 'smooth', inline: 'nearest', block: 'nearest' }); } break;
    case 'play': paused = false; location.hash = 'play-' + id; break;
    case 'get': lib[id] = { s: 'q', pr: 0 }; pump(); toast(I('dl', 18) + '«' + g.t + '» добавлена в загрузки'); render(); break;
    case 'pause': lib[id].s = 'pause'; pump(); render(); break;
    case 'resume': Object.keys(lib).forEach(function (i) { if (lib[i].s === 'dl') lib[i].s = 'q'; }); lib[id].s = 'dl'; render(); break;
    case 'cancel': delete lib[id]; pump(); toast('Загрузка «' + g.t + '» отменена'); render(); break;
    case 'remove': delete lib[id]; toast(I('trash', 18) + '«' + g.t + '» удалена'); close(); break;
    case 'fav': if (favs[id]) delete favs[id]; else favs[id] = 1; toast(favs[id] ? I('heartF', 18) + 'В избранном' : 'Убрано из избранного'); render(); break;
    case 'feat': featIdx = +v; render(); break;
    case 'gtab': gTab = v; renderLayer(hash()); break;
    case 'close': e.preventDefault(); close(); break;
    case 'genre': e.preventDefault(); query = v; location.hash = 'search'; break;
    case 'genreq': query = v; renderLayer('search'); break;
    case 'readall': alerts.forEach(function (x) { x.u = 0; }); render(); break;
    case 'stab': setTab = v; renderLayer('settings'); break;
    case 'tg': S[t.getAttribute('data-k')] = !S[t.getAttribute('data-k')]; save(); renderLayer(hash()); break;
    case 'set': S[t.getAttribute('data-k')] = v; save(); if (t.getAttribute('data-k') === 'con') { focusId = null; render(); } else renderLayer(hash()); break;
    case 'padreset': S.op = 55; S.sc = 100; S.layout = 'Классика'; save(); renderLayer('pad'); toast('Раскладка сброшена'); break;
    case 'toast': toast(v); break;
    case 'pausegame': paused = true; $('#play').classList.add('paused'); break;
    case 'resumegame': paused = false; $('#play').classList.remove('paused'); break;
    case 'qsave': toast(I('save', 18) + 'Сохранено в слот 4'); break;
    case 'qload': paused = false; $('#play').classList.remove('paused'); toast(I('load', 18) + 'Загружено последнее сохранение'); break;
    case 'qshot': toast(I('cam', 18) + 'Скриншот сохранён'); break;
    case 'qset': location.hash = 'pad'; break;
    case 'exitgame': paused = false; location.hash = 'games'; break;
  }
});
document.addEventListener('input', function (e) {
  var t = e.target;
  if (t.id === 'q') { query = t.value; $('#sres').innerHTML = searchBody(query.trim().toLowerCase(), GAMES.filter(function (g) { return (g.t + ' ' + g.genre + ' ' + PL[g.p].full + ' ' + PL[g.p].n).toLowerCase().indexOf(query.trim().toLowerCase()) >= 0; })); }
  if (t.type === 'range' && t.dataset.k) { S[t.dataset.k] = +t.value; save(); var o = $('#o-' + t.dataset.k); if (o) o.textContent = t.value + '%'; var c = document.querySelector('#padv .ctl'); if (c) { c.style.setProperty('--op', S.op / 100); c.style.setProperty('--sc', S.sc / 100); } }
});
document.addEventListener('keydown', function (e) {
  if (e.key === 'Escape' && layerKey) { if (layerKey.indexOf('play-') === 0) { paused = !paused; $('#play').classList.toggle('paused', paused); } else close(); }
  if (tab === 'games' && !layerKey && (e.key === 'ArrowRight' || e.key === 'ArrowLeft')) { var l = myGames(), i = l.findIndex(function (g) { return g.id === focusId; }); var n = l[Math.max(0, Math.min(l.length - 1, i + (e.key === 'ArrowRight' ? 1 : -1)))]; if (n) setFocus(n.id); }
});
$('#turnSkip').addEventListener('click', function () { $('#turn').classList.add('off'); });
setInterval(function () { if (tab === 'store' && !layerKey && !document.hidden) { featIdx++; var f = $('#feat'); if (f) { var tmp = document.createElement('div'); tmp.innerHTML = featHtml(); f.replaceWith(tmp.firstChild); var l = storeList().filter(function (g) { return g.isNew; }); setBg(l[featIdx % (l.length || 1)]); } } }, 7000);

render();
})();
