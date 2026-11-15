/* KAPSULA — прототип оболочки мультиплатформенного эмулятора (PSP, PS1, PS2).
   Демо-данные; обложки рисуются кодом. Вкладки: #games, #store.
   Панели поверх: #game-<id>, #gset-<id>, #library, #search, #downloads, #alerts, #quick, #settings, #profile, #pad, #play-<id>. */
(function () {
'use strict';
var $ = function (s, r) { return (r || document).querySelector(s); };
var $$ = function (s, r) { return Array.prototype.slice.call((r || document).querySelectorAll(s)); };
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
P.left = '<path d="M15 5l-7 7 7 7"/>';

P.grid = '<rect x="4" y="4" width="7" height="7" rx="2"/><rect x="13" y="4" width="7" height="7" rx="2"/><rect x="4" y="13" width="7" height="7" rx="2"/><rect x="13" y="13" width="7" height="7" rx="2"/>';
P.sun = '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2M12 19.5v2M2.5 12h2M19.5 12h2M5.3 5.3l1.4 1.4M17.3 17.3l1.4 1.4M5.3 18.7l1.4-1.4M17.3 6.7l1.4-1.4"/>';
P.wifi = '<path d="M3 9a13 13 0 0 1 18 0M6 12.5a8.5 8.5 0 0 1 12 0M9 16a4 4 0 0 1 6 0"/><circle cx="12" cy="19" r=".9" fill="currentColor"/>';
P.battery = '<rect x="3" y="7" width="16" height="10" rx="2"/><path d="M21 10.5v3M6 10v4"/>';
P.moon = '<path d="M20 14.5A8 8 0 1 1 9.5 4 6.5 6.5 0 0 0 20 14.5z"/>';
P.folder = '<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>';
P.user = '<circle cx="12" cy="8.5" r="4"/><path d="M4.5 20a7.5 7.5 0 0 1 15 0"/>';
P.hand = '<path d="M8 13V5.5a1.5 1.5 0 0 1 3 0V12M11 11V4.5a1.5 1.5 0 0 1 3 0V12M14 11.5V6a1.5 1.5 0 0 1 3 0v7c0 4-2.5 7-6 7-2.5 0-4-1.2-5.5-3.5L4 13.5a1.5 1.5 0 0 1 2.5-1.7L8 14"/>';
P.alert = '<path d="M12 3 2 20h20z"/><path d="M12 10v4M12 17v.1"/>';
P.chip = '<rect x="6" y="6" width="12" height="12" rx="2"/><path d="M9 2v4M15 2v4M9 18v4M15 18v4M2 9h4M2 15h4M18 9h4M18 15h4"/>';
P.list = '<path d="M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01"/>';
P.move = '<path d="M12 3v18M3 12h18M12 3l-3 3M12 3l3 3M12 21l-3-3M12 21l3-3M3 12l3-3M3 12l3 3M21 12l-3-3M21 12l-3 3"/>';
P.text = '<path d="M4 7V5h16v2M9 19h6M12 5v14"/>';
P.contrast = '<circle cx="12" cy="12" r="9"/><path d="M12 3v18a9 9 0 0 0 0-18z" fill="currentColor"/>';
P.music = '<path d="M9 18V5l11-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="17" cy="16" r="3"/>';
P.globe = '<circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/>';
P.right = '<path d="m9 5 7 7-7 7"/>';
P.book = '<path d="M4 5a2 2 0 0 1 2-2h13v16H6a2 2 0 0 0-2 2z"/><path d="M4 19V5"/>';
function I(n, s) { return '<svg class="i" viewBox="0 0 24 24"' + (s ? ' style="width:' + s + 'px;height:' + s + 'px"' : '') + ' aria-hidden="true">' + (P[n] || '') + '</svg>'; }
function esc(s) { return String(s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }

/* ---------- data ---------- */
var PL = {
  psp: { n: 'PSP', full: 'PlayStation Portable', c: '#9DB8D6', bios: false },
  ps1: { n: 'PS1', full: 'PlayStation', c: '#B8B4AE', bios: true },
  ps2: { n: 'PS2', full: 'PlayStation 2', c: '#8E96C4', bios: true }
};
var BIOS = { ps1: true, ps2: false }; // демо: найден ли BIOS
// демо-игры (выдуманные), m — мотив обложки
var GAMES = [
  ['drift', 'Ночной Дрифт', 'psp', 'Гонки', 2009, 1240, 'Своя версия', ['#2A3140', '#0D1016', '#D9C7A3'], 'sun', 'Ночные гонки по бесконечному шоссе. Дрифт заряжает ускорение, трасса меняется под музыку.', ['24 трассы и карьера', 'Мультиплеер до 4 игроков', 'Фоторежим']],
  ['orbit', 'Орбита-9', 'psp', 'Стратегия', 2008, 690, 'Своя версия', ['#1F2C3A', '#0B1016', '#A9BCCF'], 'planet', 'Колонизируй систему из девяти планет и свяжи их в одну торговую сеть.', ['Генерация систем', 'Дипломатия', 'Партии по 30 минут']],
  ['rhythm', 'Ритм Улиц', 'psp', 'Музыка', 2009, 520, 'Демо', ['#2B2A33', '#0E0E12', '#BDB6C9'], 'waves', 'Попадай в бит на уличных площадках. Чем точнее серия, тем ярче район.', ['40 треков', 'Редактор карт', 'Дуэли']],
  ['pixel', 'Пиксельные Войны', 'psp', 'Стратегия', 2009, 310, 'Сборник', ['#2E2B26', '#100F0D', '#D8C9A6'], 'city', 'Тактика на клеточных картах: заводы, армия и защита столицы.', ['30 миссий', 'Редактор карт', 'Хот-сит']],
  ['ash', 'Хроники Пепла', 'ps1', 'RPG', 1999, 640, 'Сборник', ['#3A2E25', '#110D0A', '#D2B48C'], 'hills', 'Королевство под вулканическим пеплом. Собери отряд и реши, кого спасать.', ['60+ часов', 'Три концовки', 'Русский перевод']],
  ['shards', 'Осколки', 'ps1', 'Головоломка', 1998, 180, 'Своя версия', ['#1F302C', '#0A100E', '#A8C2B6'], 'shards', 'Собери разбитый свет обратно, вращая и отражая лучи.', ['120 уровней', 'Без таймеров', 'Сессии по 5 минут']],
  ['forest', 'Тихий Лес', 'ps1', 'Хоррор', 2000, 560, 'Своя версия', ['#1C2A21', '#090E0B', '#9DB3A1'], 'hills', 'Ночь в заповеднике, где пропал отряд лесников. Фонарь, рация и голос в эфире.', ['Динамический звук', 'Мало ресурсов', '6–8 часов']],
  ['rubezh', 'Последний Рубеж', 'ps2', 'Экшен', 2004, 1630, 'Демо', ['#3A2523', '#110B0A', '#C9A396'], 'city', 'Удерживай последние кварталы города вместе с напарником.', ['Кооператив', 'Укрытия', '12 глав']],
  ['deep', 'Глубина', 'ps2', 'Приключения', 2005, 960, 'Демо', ['#1B2836', '#080C11', '#9FB3C8'], 'waves', 'Спустись в океанский разлом и найди источник сигнала со дна.', ['Нелинейная карта', 'Улучшения батискафа', 'Атмосферный звук']],
  ['fist', 'Стальной Кулак', 'ps2', 'Файтинг', 2003, 1420, 'Сборник', ['#36202A', '#100A0D', '#C4A1AE'], 'shards', 'Турнирный файтинг с 18 бойцами и глубокой системой комбо.', ['18 бойцов', 'Тренировка', 'Дуэли']],
  ['courier', 'Звёздный Курьер', 'ps2', 'Аркада', 2006, 820, 'Демо', ['#22253A', '#0B0C13', '#B3B8D4'], 'planet', 'Доставляй посылки между станциями и уворачивайся от метеоритов.', ['Свободный полёт', 'Улучшения корабля', 'Таблица рекордов']]
].map(function (a) { return { id: a[0], t: a[1], p: a[2], genre: a[3], year: a[4], size: a[5], kind: a[6], pal: a[7], m: a[8], desc: a[9], feat: a[10] }; });

var byId = {}; GAMES.forEach(function (g, i) { byId[g.id] = g; g.seed = 7 + i * 31; g.isNew = i % 3 === 0; g.rev = 40 + (i * 37) % 160; g.compat = g.p === 'ps2' && i % 2 ? 2 : 3; });
var GENRES = GAMES.map(function (g) { return g.genre; }).filter(function (v, i, a) { return a.indexOf(v) === i; });
var CURATED = [
  { t: 'Выбор KAPSULA', d: 'лучшее из каталога', ids: ['ash', 'drift', 'deep', 'shards', 'orbit'] },
  { t: 'Короткие игры на вечер', d: 'партии до 30 минут', ids: ['shards', 'rhythm', 'pixel', 'courier', 'orbit'] },
  { t: 'Для двоих', d: 'ad-hoc и один экран', ids: ['drift', 'rubezh', 'fist', 'rhythm', 'pixel'] },
  { t: 'Атмосферные приключения', d: 'истории и миры', ids: ['deep', 'forest', 'ash', 'orbit'] }
];
var lib = {
  drift: { s: 'ok', play: 762, last: 'Сегодня, 21:40', saves: 3, t: 9 },
  ash: { s: 'ok', play: 1345, last: 'Вчера', saves: 5, t: 8 },
  shards: { s: 'ok', play: 96, last: '3 дня назад', saves: 1, t: 7 },
  rubezh: { s: 'ok', play: 210, last: 'На прошлой неделе', saves: 2, t: 6 },
  orbit: { s: 'dl', pr: 46, t: 5 },
  fist: { s: 'pause', pr: 12, t: 4 },
  deep: { s: 'own', t: 3 },
  courier: { s: 'own', t: 2 }
};
var cols = { 'Пройти': ['deep', 'courier'], 'Любимые': ['drift', 'ash'], 'С другом': ['rubezh'] };
var favs = { drift: 1, deep: 1 };
var alerts = [
  { t: '«Хроники Пепла» готова к игре', d: 'Вчера', u: 1, g: 'ash' },
  { t: 'Новое в магазине: «Звёздный Курьер»', d: '2 дня назад', u: 1, g: 'courier' },
  { t: 'Обновление KAPSULA 0.7.0', d: '5 дней назад', u: 0 }
];
var D = { con: 'all', theme: 'dark', big: false, contrast: false, calm: false, mirror: false, saver: false, wifi: true, auto: true, res: '2x', fps: false, vol: 80, bright: 100, vib: true, sound: false, op: 55, sc: 100, hide: true, cloud: true, layout: 'Классика', lang: 'RU', fit: false, sort: 'Недавние', lf: 'Все', lc: '', onboarded: false, launched: false, recent: [] };
var S = JSON.parse(JSON.stringify(D));
try { var sv = JSON.parse(localStorage.getItem('kapsula') || '{}'); for (var k in sv) S[k] = sv[k]; } catch (e) {}
function save() { try { localStorage.setItem('kapsula', JSON.stringify(S)); } catch (e) {} }
var TOTAL = 32768, OTHER = 14000;

/* ---------- helpers ---------- */
function size(mb) { return mb >= 1000 ? (mb / 1024).toFixed(1).replace('.', ',') + ' ГБ' : Math.round(mb) + ' МБ'; }
function hrs(min) { var h = Math.floor(min / 60), m = min % 60; return h ? h + ' ч' + (m ? ' ' + m + ' мин' : '') : m + ' мин'; }
function inCon(g) { return S.con === 'all' || g.p === S.con; }
function used() { return Object.keys(lib).filter(function (i) { return lib[i].s === 'ok' || lib[i].s === 'dl' || lib[i].s === 'pause' || lib[i].s === 'q'; }).reduce(function (a, i) { return a + byId[i].size; }, 0); }
function free() { return TOTAL - OTHER - used(); }
function hexA(h, a) { var n = parseInt(h.slice(1), 16); return 'rgba(' + (n >> 16 & 255) + ',' + (n >> 8 & 255) + ',' + (n & 255) + ',' + a + ')'; }

/* ---------- единые статусы (п. 13) ---------- */
function stat(id) {
  var L = lib[id];
  if (!L) return { k: 'none', lbl: '' };
  if (L.s === 'ok') return { k: 'ok', lbl: 'Установлена' };
  if (L.s === 'own') return { k: 'own', lbl: 'В библиотеке' };
  if (L.s === 'q') return { k: 'q', lbl: 'В очереди' };
  if (L.s === 'dl') return { k: 'dl', lbl: Math.floor(L.pr) + '%' };
  return { k: 'pause', lbl: 'Пауза · ' + Math.floor(L.pr) + '%' };
}
function badge(id) { var s = stat(id); if (s.k === 'none') return ''; var cls = { ok: 'ok', own: 'own', q: 'pause', dl: 'dl', pause: 'pause' }[s.k]; return '<span class="st ' + cls + '"' + (s.k === 'dl' ? ' data-pr="' + id + '"' : '') + '>' + (s.k === 'ok' ? I('check', 12) : '') + s.lbl + '</span>'; }
function act(g, sz) {
  var s = stat(g.id).k, c = sz ? ' ' + sz : '', id = g.id;
  if (s === 'ok') return '<button class="b b-main' + c + '" data-a="play" data-id="' + id + '">' + I('play', 16) + 'Играть</button>';
  if (s === 'own') return '<button class="b b-soft' + c + '" data-a="install" data-id="' + id + '">' + I('dl', 16) + 'Установить · ' + size(g.size) + '</button>';
  if (s === 'q') return '<button class="b b-soft' + c + '" data-a="cancel" data-id="' + id + '">' + I('clock', 16) + 'В очереди</button>';
  if (s === 'dl') return '<button class="b b-soft' + c + '" data-a="pause" data-id="' + id + '">' + I('pause', 16) + '<span data-pr="' + id + '">' + Math.floor(lib[id].pr) + '%</span></button>';
  if (s === 'pause') return '<button class="b b-soft' + c + '" data-a="resume" data-id="' + id + '">' + I('play', 16) + 'Продолжить · ' + Math.floor(lib[id].pr) + '%</button>';
  return '<button class="b b-soft' + c + '" data-a="add" data-id="' + id + '">' + I('plus', 16) + 'Добавить</button>';
}

/* ---------- procedural art ---------- */
var cache = {};
function rnd(seed) { var s = seed % 2147483647 || 1; return function () { s = s * 16807 % 2147483647; return (s - 1) / 2147483646; }; }
function art(g, w, h, k) {
  var key = g.id + w + h + (k || 0); if (cache[key]) return cache[key];
  var r = rnd(g.seed + (k || 0) * 101), a = g.pal[0], b = g.pal[1], c = g.pal[2], s = '', i;
  s += "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 " + w + ' ' + h + "' preserveAspectRatio='xMidYMid slice'><defs>";
  s += "<linearGradient id='a' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='" + a + "'/><stop offset='1' stop-color='" + b + "'/></linearGradient>";
  s += "<radialGradient id='g'><stop offset='0' stop-color='" + c + "' stop-opacity='.22'/><stop offset='1' stop-color='" + c + "' stop-opacity='0'/></radialGradient></defs>";
  s += "<rect width='" + w + "' height='" + h + "' fill='url(#a)'/>";
  for (i = 0; i < 18; i++) s += "<circle cx='" + (r() * w).toFixed(0) + "' cy='" + (r() * h * .65).toFixed(0) + "' r='" + (r() * 1.5 + .3).toFixed(1) + "' fill='#fff' opacity='" + (r() * .25 + .05).toFixed(2) + "'/>";
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


/* ---------- ambient tint (п. 25) ---------- */
var bgI = $('#bg').children, bgN = 0, bgKey = '';
function tint(g) { if (!g || bgKey === g.id) return; bgKey = g.id; var nx = bgI[1 - bgN]; nx.style.setProperty('--c', hexA(g.pal[2], .22)); nx.classList.add('on'); bgI[bgN].classList.remove('on'); bgN = 1 - bgN; }

/* ---------- sound & haptics (п. 32) ---------- */
var actx = null;
function click(f) { if (!S.sound) return; try { actx = actx || new (window.AudioContext || window.webkitAudioContext)(); var o = actx.createOscillator(), g = actx.createGain(); o.frequency.value = f || 880; g.gain.value = .04; o.connect(g); g.connect(actx.destination); o.start(); g.gain.exponentialRampToValueAtTime(.0001, actx.currentTime + .06); o.stop(actx.currentTime + .07); } catch (e) {} }
function buzz(ms) { if (S.vib && navigator.vibrate) try { navigator.vibrate(ms || 8); } catch (e) {} }

/* ---------- shared ---------- */
function conChips(src) {
  var o = [['all', 'Все', '']].concat(Object.keys(PL).map(function (k) { return [k, PL[k].n, PL[k].c]; }));
  return '<div class="chips" role="tablist" aria-label="Консоль">' + o.map(function (x) { var n = src.filter(function (g) { return x[0] === 'all' || g.p === x[0]; }).length; return '<button class="chip' + (S.con === x[0] ? ' on' : '') + '" role="tab" aria-selected="' + (S.con === x[0]) + '" data-a="con" data-v="' + x[0] + '"' + (x[2] ? ' style="--c:' + x[2] + '"' : '') + '>' + (x[2] ? '<i></i>' : '') + x[1] + ' <small>' + n + '</small></button>'; }).join('') + '</div>';
}
function card(g, withAct) {
  return '<div class="card" style="--pc:' + PL[g.p].c + '"><a class="art" href="#game-' + g.id + '" data-vt="' + g.id + '" style="background-image:' + art(g, 300, 400) + '"><span class="pl">' + PL[g.p].n + '</span>' + badge(g.id) + '</a><b>' + g.t + '</b><small>' + g.genre + ' · ' + size(g.size) + '</small>' + (withAct ? act(g, 'b-xs') : '') + '</div>';
}
function myList() {
  var ord = { ok: 0, dl: 1, pause: 1, q: 1, own: 2 };
  return Object.keys(lib).map(function (i) { return byId[i]; }).filter(inCon).sort(function (a, b) { return ord[lib[a.id].s] - ord[lib[b.id].s] || lib[b.id].t - lib[a.id].t; });
}

/* ---------- GAMES: carousel (п. 1) ---------- */
var focusId = null;
function infoHtml(g) {
  var L = lib[g.id], s = stat(g.id).k, meta;
  if (s === 'ok') meta = '<span>Наиграно <b class="num">' + hrs(L.play) + '</b></span><span>' + L.last + '</span><span>' + L.saves + ' сохр.</span>';
  else if (s === 'own') meta = '<span>В библиотеке · не установлена</span><span>' + size(g.size) + '</span>';
  else meta = '<span>' + stat(g.id).lbl.replace(/^(\d+%)$/, 'Загружается · $1') + '</span><span>' + size(g.size) + '</span>';
  return '<div class="g-tx"><h1>' + g.t + '</h1><div class="meta"><span class="pill c" style="--pc:' + PL[g.p].c + '">' + PL[g.p].full + '</span>' + meta + '</div></div><div class="g-acts">' + act(g) +
    '<a class="b b-soft b-rd" href="#game-' + g.id + '" aria-label="Об игре">' + I('info', 20) + '</a></div>';
}
function vGames() {
  var list = myList();
  var h = '<div class="games"><div class="g-head">' + conChips(Object.keys(lib).map(function (i) { return byId[i]; })) + '<a class="b b-soft b-sm" href="#library">' + I('grid', 18) + 'Вся библиотека</a></div>';
  if (!list.length) return h + '<div class="g-info" style="flex:1;align-items:center"><div class="g-tx"><h1>Библиотека пуста</h1><div class="meta"><span>Добавь игры в магазине — они появятся здесь.</span></div></div><a class="b b-main" href="#store">Открыть магазин</a></div></div>';
  if (!focusId || !lib[focusId] || !list.some(function (g) { return g.id === focusId; })) focusId = list[0].id;
  h += '<div class="car" id="car">' + list.map(function (g) {
    var L = lib[g.id], dl = L.s === 'dl' || L.s === 'pause' || L.s === 'q';
    return '<button class="cc' + (g.id === focusId ? ' on' : '') + (L.s === 'own' ? ' own' : '') + '" data-a="cc" data-id="' + g.id + '" style="--pc:' + PL[g.p].c + ';background-image:' + art(g, 300, 400) + '" aria-label="' + esc(g.t) + '"><span class="pl">' + PL[g.p].n + '</span>' + (L.s === 'ok' ? '' : badge(g.id)) + (dl ? '<span class="fill"><i data-bar="' + g.id + '" style="width:' + (L.pr || 0) + '%"></i></span>' : '') + '</button>';
  }).join('') + '<a class="cc more" href="#library"><span>' + I('grid', 26) + 'Вся<br>библиотека</span></a></div>';
  return h + '<div class="g-info" id="ginfo">' + infoHtml(byId[focusId]) + '</div></div>';
}
function initCar() {
  var car = $('#car'); if (!car) return;
  var cards = $$('.cc[data-id]', car), raf = 0;
  function pick() {
    raf = 0; var c = car.scrollLeft + car.clientWidth / 2, best = null, bd = 1e9;
    cards.forEach(function (el) { var d = Math.abs(el.offsetLeft + el.offsetWidth / 2 - c); if (d < bd) { bd = d; best = el; } });
    if (!best || best.getAttribute('data-id') === focusId) return;
    focusId = best.getAttribute('data-id');
    cards.forEach(function (el) { el.classList.toggle('on', el === best); });
    var gi = $('#ginfo'); gi.innerHTML = infoHtml(byId[focusId]); gi.classList.remove('swap'); void gi.offsetWidth; gi.classList.add('swap');
    tint(byId[focusId]); buzz(6); click(1200);
  }
  car.addEventListener('scroll', function () { if (!raf) raf = requestAnimationFrame(pick); }, { passive: true });
  var cur = cards.filter(function (el) { return el.getAttribute('data-id') === focusId; })[0];
  if (cur) car.scrollLeft = cur.offsetLeft + cur.offsetWidth / 2 - car.clientWidth / 2;
}

/* ---------- STORE (п. 5, 17) ---------- */
var featIdx = 0, storeSeen = false;
function sList() { return GAMES.filter(inCon); }
function featHtml() {
  var l = sList().filter(function (g) { return g.isNew; }); if (!l.length) l = sList(); if (!l.length) return '';
  featIdx %= l.length; var g = l[featIdx];
  return '<div class="feat" id="feat" style="background-image:' + art(g, 1400, 420, 5) + '"><div class="feat-in"><small>Новое · ' + PL[g.p].full + '</small><h2>' + g.t + '</h2><p>' + g.desc + '</p><div class="row">' + act(g, 'b-sm') + '<a class="b b-soft b-sm" href="#game-' + g.id + '">Подробнее</a></div></div></div>';
}
function rowC(t, d, list) { if (!list.length) return ''; return '<section><div class="hrow"><h3>' + t + '</h3>' + (d ? '<span class="hint">' + d + '</span>' : '') + '</div><div class="row-c" style="margin-top:10px">' + list.map(function (g) { return card(g, true); }).join('') + '</div></section>'; }
function vStore() {
  if (!storeSeen) { storeSeen = true; setTimeout(function () { if (tab === 'store') renderPage(); }, 450); return '<div class="scroll"><div class="wrap">' + '<div class="sk" style="height:34px;width:300px"></div><div class="sk" style="height:200px"></div><div class="row-c">' + '<div class="sk" style="aspect-ratio:3/4"></div>'.repeat(7) + '</div></div></div>'; }
  var l = sList(), all = S.fit ? l.filter(function (g) { return lib[g.id] && lib[g.id].s === 'ok' || g.size <= free(); }) : l;
  return '<div class="scroll"><div class="wrap">' + conChips(GAMES) + (navigator.onLine === false ? '<div class="banner">' + I('wifi', 18) + 'Нет интернета — магазин недоступен, установленные игры работают.</div>' : '') + featHtml() +
    CURATED.map(function (c) { return rowC(c.t, c.d, c.ids.map(function (i) { return byId[i]; }).filter(inCon)); }).join('') +
    '<section><div class="hrow"><h3>Все игры</h3><button class="chip' + (S.fit ? ' on' : '') + '" data-a="fit">' + I('chip', 15) + 'Только то, что поместится</button><span class="hint">свободно ' + size(free()) + '</span></div><div class="lgrid" style="margin-top:12px">' + all.map(function (g) { return card(g, true); }).join('') + '</div></section></div></div>';
}

/* ---------- overlays ---------- */
var gTab = 'about', setTab = 'general', query = '', ptab = 'menu';
function shade(h) { return '<div class="scrim" data-a="close"></div>' + h; }
function opt(t, d, c) { return '<div class="opt"><span class="tx"><b>' + t + '</b>' + (d ? '<span>' + d + '</span>' : '') + '</span>' + c + '</div>'; }
function tg(k) { return '<button class="tg" role="switch" aria-checked="' + !!S[k] + '" data-a="tg" data-k="' + k + '" aria-label="Переключить"></button>'; }
function seg(k, o, lbl) { return '<div class="seg">' + o.map(function (v, i) { return '<button class="' + (String(S[k]) === String(v) ? 'on' : '') + '" data-a="set" data-k="' + k + '" data-v="' + v + '">' + (lbl ? lbl[i] : v) + '</button>'; }).join('') + '</div>'; }
function rng(k, a, b, u) { return '<div class="rng"><input type="range" min="' + a + '" max="' + b + '" value="' + S[k] + '" data-k="' + k + '" aria-label="' + k + '"><output class="num" id="o-' + k + '">' + S[k] + (u || '%') + '</output></div>'; }
function slot(g, i, label, when, cls) { return '<button class="' + (cls || 'slot') + '" data-a="loadslot" data-id="' + g.id + '"><span class="shot" style="background-image:' + art(g, 480, 272, 30 + i) + '"></span><b>' + label + '</b><span>' + when + '</span></button>'; }
function saves(g) { var n = (lib[g.id] && lib[g.id].saves) || 0, W = ['5 минут назад', 'вчера', '3 дня назад', 'неделю назад', 'месяц назад']; var o = []; for (var i = 0; i < Math.min(n, 4); i++) o.push(slot(g, i, i === 0 ? 'Быстрое сохранение' : 'Слот ' + (i + 1), W[i])); return o.join(''); }

/* game sheet (п. 3, 18, 9) */
function oGame(id) {
  var g = byId[id]; if (!g) return ''; var p = PL[g.p], s = stat(id).k, L = lib[id];
  var tabs = [['about', 'Обзор'], ['shots', 'Скриншоты'], ['info', 'Детали']].concat(s === 'ok' ? [['saves', 'Сохранения']] : []), body = '';
  if (gTab === 'saves' && s !== 'ok') gTab = 'about';
  if (gTab === 'about') {
    body = '<p class="txt">' + g.desc + '</p><ul class="list">' + g.feat.map(function (f) { return '<li>' + f + '</li>'; }).join('') + '</ul>' +
      '<div class="sub">Что нужно для игры</div><div class="kv"><div><small>Размер</small><b>' + size(g.size) + '</b></div><div><small>BIOS</small><b>' + (p.bios ? (BIOS[g.p] ? 'Найден' : 'Нужен BIOS ' + p.n) : 'Не нужен') + '</b></div><div><small>Управление</small><b>Экран или геймпад</b></div><div><small>Игроки</small><b>' + (/двоих|Дуэли|Кооператив|Мультиплеер|Хот-сит/.test(g.feat.join(' ')) ? '1–2' : '1') + '</b></div></div>' +
      (p.bios && !BIOS[g.p] ? '<div class="note warn" style="margin-top:8px">' + I('alert', 20) + '<span><b>Нужен BIOS ' + p.n + '.</b> Без него игра не запустится. Добавь свой файл BIOS в настройках — там есть инструкция.</span></div>' : '') +
      '<div class="sub">Совместимость</div><div class="rev"><span class="bars">' + [1, 2, 3].map(function (n) { return '<i class="' + (n <= g.compat ? 'on' : '') + '"></i>'; }).join('') + '</span>' + (g.compat === 3 ? 'Работает отлично' : 'Играбельно, бывают просадки') + ' · <span class="num">' + g.rev + '</span> отзывов</div>' +
      '<div class="sub">Похожие игры</div><div class="row-c">' + GAMES.filter(function (x) { return x.id !== id && (x.genre === g.genre || x.p === g.p); }).slice(0, 6).map(function (x) { return card(x, false); }).join('') + '</div>';
  } else if (gTab === 'shots') body = '<div class="shots">' + [1, 2, 3, 4].map(function (n) { return '<div class="shot" style="background-image:' + art(g, 480, 272, n * 13) + '"></div>'; }).join('') + '</div>';
  else if (gTab === 'saves') body = '<div class="slots">' + saves(g) + '<button class="slot empty" data-a="toast" data-v="Новый слот создаётся в меню паузы"><span class="shot">' + I('plus', 22) + '</span><b>Пустой слот</b><span>Сохрани из игры</span></button></div>';
  else body = '<div class="kv">' + [['Платформа', p.full], ['Жанр', g.genre], ['Год', g.year], ['Размер', size(g.size)], ['Тип', g.kind], ['Языки', 'RU, EN'], ['Регион', 'EU'], ['Формат', g.p === 'ps1' ? 'CHD' : 'ISO']].map(function (r) { return '<div><small>' + r[0] + '</small><b>' + r[1] + '</b></div>'; }).join('') + '</div>';
  var colChips = Object.keys(cols).map(function (c) { var on = cols[c].indexOf(id) >= 0; return '<button class="chip' + (on ? ' on' : '') + '" data-a="togcol" data-id="' + id + '" data-v="' + esc(c) + '">' + (on ? I('check', 14) : I('plus', 14)) + esc(c) + '</button>'; }).join('');
  var second = s === 'none' ? '<button class="b b-soft" data-a="addinstall" data-id="' + id + '">' + I('dl', 18) + 'Добавить и установить</button>' : s === 'ok' ? '<a class="b b-soft b-rd" href="#gset-' + id + '" aria-label="Настройки игры">' + I('sliders', 20) + '</a><button class="b b-soft b-rd" data-a="uninstall" data-id="' + id + '" aria-label="Удалить с телефона">' + I('trash', 20) + '</button>' : '';
  return shade('<div class="sheet" role="dialog" aria-label="' + esc(g.t) + '" style="--pc:' + p.c + '"><div class="gs-top" style="background-image:' + art(g, 1400, 300, 3) + '"><button class="b b-soft b-rd gs-x" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div>' +
    '<div class="gs-body"><div><div class="gs-cover" style="background-image:' + art(g, 300, 400) + '"></div></div><div class="gs-main"><div class="pills"><span class="pill c">' + p.full + '</span><span class="pill">' + g.genre + '</span><span class="pill">' + g.year + '</span><span class="pill">' + g.kind + '</span>' + (s !== 'none' ? '<span class="pill">' + stat(id).lbl + '</span>' : '') + '</div>' +
    '<h2>' + g.t + '</h2>' + (s === 'ok' ? '<div class="pills"><span class="pill">' + I('clock', 14) + hrs(L.play) + '</span><span class="pill">' + I('save', 14) + L.saves + ' сохр.</span></div>' : '') +
    (s !== 'none' ? '<div class="chips">' + colChips + '<button class="chip' + (favs[id] ? ' on' : '') + '" data-a="fav" data-id="' + id + '">' + I(favs[id] ? 'heartF' : 'heart', 14) + 'Избранное</button></div>' : '') +
    '<div class="seg">' + tabs.map(function (t) { return '<button class="' + (gTab === t[0] ? 'on' : '') + '" data-a="gtab" data-v="' + t[0] + '">' + t[1] + '</button>'; }).join('') + '</div>' + body + '</div></div>' +
    '<div class="gs-bar"><span class="tx"><b>' + g.t + '</b><span>' + p.n + ' · ' + size(g.size) + (s !== 'none' ? ' · ' + stat(id).lbl : '') + '</span></span>' + second + act(g) + '</div></div>');
}
/* per-game settings (п. 9) */
function oGSet(id) {
  var g = byId[id]; if (!g) return '';
  return '<div class="full" role="dialog" aria-label="Настройки игры"><div class="ov-h"><button class="b b-soft b-rd" data-a="back" data-v="game-' + id + '" aria-label="Назад">' + I('left', 20) + '</button><h2>' + g.t + ' — настройки</h2><button class="b b-soft b-rd" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div><div class="ov-b" style="max-width:820px">' +
    '<div class="note">' + I('star', 20) + '<span><b>Применён пресет автора каталога.</b> Проверенные настройки для этой игры. Можно изменить — общие настройки не затронутся.</span></div>' +
    opt('Разрешение', 'Только для этой игры', seg('res', ['1x', '2x', '3x', '4x'])) + opt('Раскладка кнопок', '', seg('layout', ['Классика', 'Компакт', 'Своя'])) + opt('Счётчик FPS', '', tg('fps')) + opt('Читы', 'Появятся позже', '<span class="hint">скоро</span>') + '</div></div>';
}
/* full library (п. 2, 19) */
var newCol = '';
function oLibrary() {
  var q = (S.lq || '').toLowerCase(), list = Object.keys(lib).map(function (i) { return byId[i]; }).filter(inCon).filter(function (g) {
    var s = lib[g.id].s;
    if (S.lf === 'Установленные' && s !== 'ok') return false;
    if (S.lf === 'Не установленные' && s !== 'own') return false;
    if (S.lf === 'Загружаются' && ['dl', 'pause', 'q'].indexOf(s) < 0) return false;
    if (S.lf === 'Избранное' && !favs[g.id]) return false;
    if (S.lc && (!cols[S.lc] || cols[S.lc].indexOf(g.id) < 0)) return false;
    return !q || g.t.toLowerCase().indexOf(q) >= 0;
  });
  var srt = { 'Недавние': function (a, b) { return lib[b.id].t - lib[a.id].t; }, 'По названию': function (a, b) { return a.t.localeCompare(b.t, 'ru'); }, 'По времени игры': function (a, b) { return (lib[b.id].play || 0) - (lib[a.id].play || 0); } };
  list.sort(srt[S.sort] || srt['Недавние']);
  return '<div class="full" role="dialog" aria-label="Вся библиотека"><div class="ov-h"><h2>Вся библиотека</h2><span class="hint num">' + list.length + ' игр</span><button class="b b-soft b-rd" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div><div class="ov-b"><div class="wrap" style="gap:12px">' +
    '<div class="lib-tools"><label class="lsearch">' + I('search', 18) + '<input id="lq" type="search" placeholder="Поиск в библиотеке" value="' + esc(S.lq || '') + '"></label>' + seg('sort', ['Недавние', 'По названию', 'По времени игры']) + '</div>' +
    conChips(Object.keys(lib).map(function (i) { return byId[i]; })) +
    '<div class="chips">' + ['Все', 'Установленные', 'Не установленные', 'Загружаются', 'Избранное'].map(function (f) { return '<button class="chip' + (S.lf === f ? ' on' : '') + '" data-a="set" data-k="lf" data-v="' + f + '">' + f + '</button>'; }).join('') + '</div>' +
    '<div class="chips">' + I('folder', 18) + Object.keys(cols).map(function (c) { return '<button class="chip' + (S.lc === c ? ' on' : '') + '" data-a="lcol" data-v="' + esc(c) + '">' + esc(c) + ' <small>' + cols[c].length + '</small></button>'; }).join('') +
    '<label class="lsearch" style="max-width:220px;height:34px"><input id="newcol" placeholder="Новая коллекция" value="' + esc(newCol) + '"></label><button class="b b-soft b-xs" data-a="mkcol">' + I('plus', 14) + 'Создать</button></div>' +
    (list.length ? '<div class="lgrid">' + list.map(function (g) { return card(g, true); }).join('') + '</div>' : '<div class="banner">' + I('search', 18) + 'Ничего не нашлось. Сбрось фильтры или добавь игры в магазине.</div>') + '</div></div></div>';
}
/* search (п. 8) */
function sMatch(g, q) { return (g.t + ' ' + g.genre + ' ' + PL[g.p].full + ' ' + PL[g.p].n).toLowerCase().indexOf(q) >= 0; }
function sBody() {
  var q = query.trim().toLowerCase();
  if (!q) return (S.recent.length ? '<div class="sub">Недавние запросы</div><div class="chips">' + S.recent.map(function (r) { return '<button class="chip" data-a="sq" data-v="' + esc(r) + '">' + I('clock', 14) + esc(r) + '</button>'; }).join('') + '<button class="chip" data-a="clrrecent">Очистить</button></div>' : '') +
    '<div class="sub">Популярное</div><div class="row-c">' + GAMES.filter(function (g) { return g.isNew; }).map(function (g) { return card(g, false); }).join('') + '</div>' +
    '<div class="sub">Жанры и консоли</div><div class="chips">' + GENRES.map(function (gn) { return '<button class="chip" data-a="sq" data-v="' + gn + '">' + gn + '</button>'; }).join('') + Object.keys(PL).map(function (k) { return '<button class="chip" style="--c:' + PL[k].c + '" data-a="sq" data-v="' + PL[k].n + '"><i></i>' + PL[k].full + '</button>'; }).join('') + '</div>';
  var mine = GAMES.filter(function (g) { return lib[g.id] && sMatch(g, q); }), shop = GAMES.filter(function (g) { return !lib[g.id] && sMatch(g, q); });
  if (!mine.length && !shop.length) return '<div class="banner">' + I('search', 18) + 'По запросу «' + esc(query) + '» ничего нет. Проверь написание или выбери жанр.</div>';
  return (mine.length ? '<div class="sub">В библиотеке · ' + mine.length + '</div><div class="lgrid">' + mine.map(function (g) { return card(g, true); }).join('') + '</div>' : '') + (shop.length ? '<div class="sub">В магазине · ' + shop.length + '</div><div class="lgrid">' + shop.map(function (g) { return card(g, true); }).join('') + '</div>' : '');
}
function oSearch() { return '<div class="full" role="dialog" aria-label="Поиск"><div class="ov-h"><label class="sbox">' + I('search') + '<input id="q" type="search" placeholder="Игра, жанр или консоль" value="' + esc(query) + '" autocomplete="off" enterkeyhint="search"></label><button class="b b-soft b-rd" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div><div class="ov-b" id="sres">' + sBody() + '</div></div>'; }
/* downloads */
function oDownloads() {
  var actv = Object.keys(lib).filter(function (i) { return ['dl', 'pause', 'q'].indexOf(lib[i].s) >= 0; }), done = Object.keys(lib).filter(function (i) { return lib[i].s === 'ok'; });
  return shade('<aside class="drawer" aria-label="Загрузки"><div class="ov-h"><h2>Загрузки</h2><button class="b b-soft b-rd b-sm" data-a="close" aria-label="Закрыть">' + I('x', 18) + '</button></div><div class="ov-b">' +
    '<div class="sub">Сейчас</div>' + (actv.length ? actv.map(function (i) { var g = byId[i], L = lib[i]; return '<div class="it"><span class="th" style="background-image:' + art(g, 300, 300) + '"></span><span class="tx"><b>' + g.t + '</b><span>' + stat(i).lbl + ' · ' + size(g.size) + '</span><span class="pb"><i data-bar="' + i + '" style="width:' + (L.pr || 0) + '%"></i></span></span><button class="b b-soft b-rd b-sm" data-a="' + (L.s === 'dl' ? 'pause' : 'resume') + '" data-id="' + i + '" aria-label="' + (L.s === 'dl' ? 'Пауза' : 'Продолжить') + '">' + I(L.s === 'dl' ? 'pause' : 'play', 16) + '</button><button class="b b-soft b-rd b-sm" data-a="cancel" data-id="' + i + '" aria-label="Отменить">' + I('x', 16) + '</button></div>'; }).join('') : '<div class="hint">Активных загрузок нет</div>') +
    '<div class="sub">Установлены</div>' + done.map(function (i) { var g = byId[i]; return '<div class="it"><span class="th" style="background-image:' + art(g, 300, 300) + '"></span><span class="tx"><b>' + g.t + '</b><span>' + PL[g.p].n + ' · ' + size(g.size) + '</span></span><button class="b b-main b-sm" data-a="play" data-id="' + i + '">Играть</button></div>'; }).join('') +
    '<div class="sub">Память</div><div class="pb" style="height:8px"><i style="width:' + ((TOTAL - free()) / TOTAL * 100).toFixed(0) + '%"></i></div><p class="hint" style="margin:6px 0 0">Игры KAPSULA ' + size(used()) + ' · свободно ' + size(free()) + ' (демо)</p>' + (free() < 3000 ? '<div class="note warn" style="margin-top:10px">' + I('alert', 20) + '<span><b>Мало памяти.</b> Удали с телефона игры, в которые не играешь, — они останутся в библиотеке.</span></div>' : '') + '</div></aside>');
}
function oAlerts() {
  return shade('<aside class="drawer" aria-label="Уведомления"><div class="ov-h"><h2>Уведомления</h2><button class="b b-soft b-sm" data-a="readall">Прочитать все</button><button class="b b-soft b-rd b-sm" data-a="close" aria-label="Закрыть">' + I('x', 18) + '</button></div><div class="ov-b">' +
    alerts.map(function (a) { var g = a.g && byId[a.g]; return '<a class="it' + (a.u ? ' unread' : '') + '" href="' + (g ? '#game-' + g.id : '#settings') + '"><span class="th"' + (g ? ' style="background-image:' + art(g, 300, 300) + '"' : '') + '>' + (g ? '' : I('bolt', 20)) + '</span><span class="tx"><b>' + a.t + '</b><span>' + a.d + '</span></span></a>'; }).join('') + '</div></aside>');
}
/* quick panel (п. 16) */
function padState() { try { var p = navigator.getGamepads && Array.prototype.filter.call(navigator.getGamepads(), Boolean)[0]; return p ? p.id.split('(')[0].trim().slice(0, 22) || 'Подключён' : 'Не подключён'; } catch (e) { return 'Не подключён'; } }
function oQuick() {
  var tiles = [['pad', 'Геймпад', padState(), 'go', 'pad', false], ['wifi', 'Только Wi-Fi', S.wifi ? 'Вкл' : 'Выкл', 'tg', 'wifi', S.wifi], ['battery', 'Экономия', S.saver ? 'Вкл' : 'Выкл', 'tg', 'saver', S.saver], ['moon', 'Тема', { dark: 'Тёмная', oled: 'OLED', light: 'Светлая' }[S.theme], 'theme', '', false],
    ['hand', 'Вибрация', S.vib ? 'Вкл' : 'Выкл', 'tg', 'vib', S.vib], ['music', 'Звуки', S.sound ? 'Вкл' : 'Выкл', 'tg', 'sound', S.sound], ['bell', 'Уведомления', alerts.filter(function (a) { return a.u; }).length + ' новых', 'go', 'alerts', false], ['gear', 'Настройки', 'Все параметры', 'go', 'settings', false]];
  return shade('<div class="shade" role="dialog" aria-label="Быстрая панель"><div class="ov-h"><h2>Быстрая панель</h2><button class="b b-soft b-rd b-sm" data-a="close" aria-label="Закрыть">' + I('x', 18) + '</button></div><div class="ov-b"><div class="qp">' +
    tiles.map(function (t) { return '<button class="qt' + (t[5] ? ' on' : '') + '" data-a="q' + t[3] + '" data-k="' + t[4] + '">' + I(t[0], 22) + '<div>' + t[1] + '<br><span>' + t[2] + '</span></div></button>'; }).join('') + '</div>' +
    '<div class="qsl"><label>' + I('vol', 20) + '<input type="range" min="0" max="100" value="' + S.vol + '" data-k="vol" aria-label="Громкость"></label><label>' + I('sun', 20) + '<input type="range" min="40" max="100" value="' + S.bright + '" data-k="bright" aria-label="Яркость"></label></div></div></div>');
}
/* settings (п. 21, 22, 27, 43, 47, 51, 53) */
function oSettings() {
  var nav = [['general', 'Общие', 'sliders'], ['dl', 'Загрузки и память', 'dl'], ['gfx', 'Графика', 'monitor'], ['ctl', 'Управление', 'pad'], ['bios', 'BIOS', 'chip'], ['a11y', 'Доступность', 'hand'], ['about', 'О приложении', 'info']], b = '';
  if (setTab === 'general') b = opt('Тема', 'OLED — чистый чёрный для AMOLED-экранов', seg('theme', ['dark', 'oled', 'light'], ['Тёмная', 'OLED', 'Светлая'])) + opt('Язык', '', seg('lang', ['RU', 'EN'])) + opt('Вибрация', 'Отклик при выборе и важных действиях', tg('vib')) + opt('Звуки интерфейса', 'Тихий щелчок в карусели', tg('sound')) + opt('Облачные сохранения', 'Продолжай на другом телефоне (в планах)', tg('cloud'));
  if (setTab === 'dl') b = opt('Только по Wi-Fi', 'Не тратить мобильный трафик', tg('wifi')) + opt('Устанавливать сразу', 'После добавления в библиотеку', tg('auto')) + opt('Папка с играми', '/storage/emulated/0/KAPSULA', '<button class="b b-soft b-sm" data-a="toast" data-v="Откроется выбор папки Android">Изменить</button>') + opt('Сохранения', 'Резервная копия в файл', '<button class="b b-soft b-sm" data-a="toast" data-v="Сохранения экспортированы (демо)">Экспорт</button> <button class="b b-soft b-sm" data-a="toast" data-v="Выбор файла для импорта (демо)">Импорт</button>');
  if (setTab === 'gfx') b = opt('Разрешение по умолчанию', 'Подобрано под этот телефон', seg('res', ['1x', '2x', '3x', '4x'])) + opt('Счётчик FPS', '', tg('fps')) + opt('Экономия батареи', 'Ниже разрешение и FPS, без анимаций', tg('saver'));
  if (setTab === 'ctl') b = opt('Экранный геймпад', 'Раскладка, размер, прозрачность', '<a class="b b-main b-sm" href="#pad">Настроить</a>') + opt('Скрывать кнопки', 'Когда подключён геймпад', tg('hide')) + opt('Подключённый геймпад', padState(), '<span class="hint">профиль подбирается автоматически</span>');
  if (setTab === 'bios') b = Object.keys(PL).map(function (k) { var p = PL[k]; return opt('BIOS ' + p.full, !p.bios ? 'Не требуется' : BIOS[k] ? 'Найден и проверен' : 'Не найден — нужен свой файл BIOS', p.bios ? (BIOS[k] ? '<span class="pill ok">' + I('check', 14) + 'OK</span>' : '<button class="b b-main b-sm" data-a="toast" data-v="Выбор файла BIOS (демо)">Добавить файл</button>') : '<span class="pill">—</span>'); }).join('') + '<div class="note" style="margin-top:10px">' + I('book', 20) + '<span>BIOS — файл прошивки консоли. Его нужно снять со своей приставки; KAPSULA проверит его по контрольной сумме.</span></div>';
  if (setTab === 'a11y') b = opt('Крупный текст', '', tg('big')) + opt('Высокий контраст', '', tg('contrast')) + opt('Уменьшить движение', 'Без анимаций', tg('calm')) + opt('Раскладка для левшей', 'Зеркальные экранные кнопки', tg('mirror'));
  if (setTab === 'about') b = opt('KAPSULA', 'Версия 0.7.0 · прототип интерфейса', '<button class="b b-soft b-sm" data-a="replay">Показать заставку</button>') + opt('Эмуляторы', 'PPSSPP (PSP), DuckStation (PS1), PCSX2 (PS2) — будут подключены', '') + opt('Лицензии', 'Открытые компоненты (GPL)', '<button class="b b-soft b-sm" data-a="toast" data-v="Список лицензий появится с подключением ядер">Открыть</button>') + opt('Конфиденциальность', 'Какие данные хранятся и где', '<button class="b b-soft b-sm" data-a="toast" data-v="Политика будет опубликована до релиза">Открыть</button>') + opt('Знакомство', 'Пройти первый запуск заново', '<button class="b b-soft b-sm" data-a="onb">Запустить</button>');
  return '<div class="full" role="dialog" aria-label="Настройки"><div class="ov-h"><h2>Настройки</h2><button class="b b-soft b-rd" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div><div class="ov-b"><div class="set"><nav class="set-nav">' + nav.map(function (n) { return '<button class="' + (setTab === n[0] ? 'on' : '') + '" data-a="stab" data-v="' + n[0] + '">' + I(n[2], 20) + n[1] + '</button>'; }).join('') + '</nav><div>' + b + '</div></div></div></div>';
}
/* profile (п. 10) */
function oProfile() {
  var ok = Object.keys(lib).filter(function (i) { return lib[i].s === 'ok'; }), play = ok.reduce(function (a, i) { return a + lib[i].play; }, 0), svs = ok.reduce(function (a, i) { return a + lib[i].saves; }, 0);
  var xp = play * 2 + svs * 30, days = [['Сегодня', 95], ['Вчера', 140], ['Ср', 40], ['Вт', 0], ['Пн', 75]], mx = 140;
  return shade('<aside class="drawer" aria-label="Профиль"><div class="ov-h"><h2>Профиль</h2><button class="b b-soft b-rd b-sm" data-a="close" aria-label="Закрыть">' + I('x', 18) + '</button></div><div class="ov-b">' +
    '<div class="prof"><span class="av">N</span><span><h3>Игрок</h3><span>Уровень ' + (Math.floor(xp / 1000) + 1) + ' · ' + (xp % 1000) + ' / 1000 XP</span></span></div>' +
    '<div class="nums"><div><b class="num">' + Math.floor(play / 60) + ' ч</b><span>наиграно</span></div><div><b class="num">' + Object.keys(lib).length + '</b><span>в библиотеке</span></div><div><b class="num">' + svs + '</b><span>сохранений</span></div><div><b class="num">' + Object.keys(favs).length + '</b><span>в избранном</span></div></div>' +
    '<div class="sub">Облако</div>' + opt('Продолжить на другом устройстве', 'Сохранения и библиотека в аккаунте (в планах)', tg('cloud')) +
    '<div class="sub">История по дням</div><div class="days">' + days.map(function (d) { return '<div class="day"><b>' + d[0] + '</b><i style="width:' + Math.max(4, d[1] / mx * 180) + 'px"></i><span class="hint num">' + (d[1] ? hrs(d[1]) : '—') + '</span></div>'; }).join('') + '</div>' +
    '<div class="sub">Избранное</div>' + Object.keys(favs).map(function (i) { var g = byId[i]; return '<a class="it" href="#game-' + i + '"><span class="th" style="background-image:' + art(g, 300, 300) + '"></span><span class="tx"><b>' + g.t + '</b><span>' + PL[g.p].n + ' · ' + stat(i).lbl + '</span></span>' + I('right', 18) + '</a>'; }).join('') + '</div></aside>');
}
/* controls */
var keyPos = {};
function ctl(edit) {
  var K = [['L', 's', 'left:3%;top:6%;width:13%;height:11%', 'L'], ['R', 's', 'right:3%;top:6%;width:13%;height:11%', 'R'], ['up', 's', 'left:9%;top:44%;width:6%;aspect-ratio:1', '▲'], ['dn', 's', 'left:9%;top:calc(44% + 12cqw);width:6%;aspect-ratio:1', '▼'], ['lt', 's', 'left:3%;top:calc(44% + 6cqw);width:6%;aspect-ratio:1', '◀'], ['rt', 's', 'left:15%;top:calc(44% + 6cqw);width:6%;aspect-ratio:1', '▶'],
    ['tri', 'r', 'right:10%;top:40%;width:6.5%;aspect-ratio:1', '△'], ['cir', 'r', 'right:3.5%;top:calc(40% + 6.5cqw);width:6.5%;aspect-ratio:1', '○'], ['crs', 'r', 'right:10%;top:calc(40% + 13cqw);width:6.5%;aspect-ratio:1', '✕'], ['sqr', 'r', 'right:16.5%;top:calc(40% + 6.5cqw);width:6.5%;aspect-ratio:1', '□'],
    ['sel', 's', 'left:40%;bottom:5%;width:8%;height:7%;border-radius:20px;font-size:1.2cqw', 'SELECT'], ['sta', 's', 'left:52%;bottom:5%;width:8%;height:7%;border-radius:20px;font-size:1.2cqw', 'START']];
  return '<div class="ctl' + (S.mirror ? ' mirror' : '') + (edit ? ' edit' : '') + '" style="--op:' + S.op / 100 + ';--sc:' + S.sc / 100 + '">' + K.map(function (k) { var p = keyPos[k[0]]; return '<span class="k ' + k[1] + '" data-key="' + k[0] + '" style="' + k[2] + (p ? ';translate:' + p[0] + 'px ' + p[1] + 'px' : '') + '">' + k[3] + '</span>'; }).join('') + '</div>';
}
function oPad() {
  return '<div class="full" role="dialog" aria-label="Экранный геймпад"><div class="ov-h"><h2>Экранный геймпад</h2><button class="b b-soft b-sm" data-a="padreset">Сбросить</button><button class="b b-soft b-rd" data-a="close" aria-label="Закрыть">' + I('x') + '</button></div><div class="ov-b"><div class="pad">' +
    '<div class="pad-v" id="padv" style="background-image:' + art(byId.drift, 1600, 900, 11) + '">' + ctl(true) + '</div><div>' + opt('Раскладка', '', seg('layout', ['Классика', 'Компакт', 'Своя'])) + opt('Прозрачность', '', rng('op', 15, 100)) + opt('Размер кнопок', '', rng('sc', 70, 130)) + opt('Для левшей', 'Зеркальная раскладка', tg('mirror')) + opt('Скрывать с геймпадом', '', tg('hide')) + '<p class="hint">Перетаскивай кнопки на превью. В игре — кнопка «Кнопки» сверху.</p></div></div></div></div>';
}
/* play (п. 4, 20, 31) */
var paused = false, booted = {}, editKeys = false;
function oPlay(id) {
  var g = byId[id]; if (!g) return ''; var p = PL[g.p];
  var boot = g.p === 'psp' ? '<svg class="boot-psp" viewBox="0 0 150 62"><rect x="2" y="2" width="146" height="58" rx="29"/><rect x="48" y="12" width="54" height="38" rx="3"/><circle cx="24" cy="31" r="6"/><circle cx="126" cy="31" r="6"/></svg>' : g.p === 'ps1' ? '<div class="boot-ps1"></div>' : '<div class="boot-ps2"></div>';
  var W = ['5 минут назад', 'вчера', '3 дня назад', 'неделю назад'];
  return '<div class="play' + (booted[id] ? ' ready' : '') + (paused ? ' paused' : '') + '" id="play" style="--pc:' + p.c + '"><div class="play-g" style="background-image:' + art(g, 1600, 900, 11) + '"></div>' + ctl(editKeys) +
    '<div class="play-tb"><button data-a="pausegame">' + I('pause', 14) + 'Меню</button><button class="' + (editKeys ? 'on' : '') + '" data-a="editkeys">' + I('move', 14) + (editKeys ? 'Готово' : 'Кнопки') + '</button></div>' +
    '<div class="qm"><div class="qm-in"><span class="pill c" style="align-self:flex-start;--pc:' + p.c + '">' + p.full + '</span><h2>' + g.t + '</h2><div class="qm-row">' +
    [['main', 'play', 'Продолжить', 'resumegame'], ['', 'save', 'Сохранить', 'qsave'], ['', 'cam', 'Скриншот', 'qshot'], ['', 'sliders', 'Настройки игры', 'qset'], ['bad', 'exit', 'Выйти', 'exitgame']].map(function (b) { return '<button class="qb ' + b[0] + '" data-a="' + b[3] + '" data-id="' + id + '">' + I(b[1], 22) + b[2] + '</button>'; }).join('') +
    '</div><div class="qm-row">' + [0, 1, 2, 3].map(function (i) { return slot(g, i, i === 0 ? 'Быстрое' : 'Слот ' + (i + 1), W[i], 'qslot'); }).join('') + '</div></div></div>' +
    '<div class="boot">' + boot + '<b>' + g.t + '</b><span>' + p.full.toUpperCase() + '</span></div></div>';
}

/* ---------- onboarding (п. 6) ---------- */
var onb = { step: 0, cons: { psp: 1, ps1: 1, ps2: 1 }, where: 'int', how: 'touch' };
function onbHtml() {
  var st = onb.step, body, h, p;
  if (st === 0) { h = 'Во что играешь?'; p = 'Выбери консоли — KAPSULA покажет подходящие игры.'; body = '<div class="onb-opts">' + Object.keys(PL).map(function (k) { return '<button class="onb-opt' + (onb.cons[k] ? ' on' : '') + '" data-a="onbc" data-v="' + k + '" style="--pc:' + PL[k].c + '"><span class="pill c">' + PL[k].n + '</span><b>' + PL[k].full + '</b><span>' + GAMES.filter(function (g) { return g.p === k; }).length + ' игр в каталоге</span></button>'; }).join('') + '</div>'; }
  if (st === 1) { h = 'Где хранить игры?'; p = 'Игры весят 0,2–2 ГБ. Свободно ' + size(free()) + '.'; body = '<div class="onb-opts">' + [['int', 'Память телефона', '/storage/emulated/0/KAPSULA'], ['sd', 'Карта памяти', 'Если вставлена SD-карта']].map(function (o) { return '<button class="onb-opt' + (onb.where === o[0] ? ' on' : '') + '" data-a="onbw" data-v="' + o[0] + '">' + I('folder', 22) + '<b>' + o[1] + '</b><span>' + o[2] + '</span></button>'; }).join('') + '</div>'; }
  if (st === 2) { h = 'Как будешь играть?'; p = 'Можно поменять в любой момент.'; body = '<div class="onb-opts">' + [['touch', 'Сенсорный экран', 'Кнопки поверх игры'], ['pad', 'Геймпад', 'Bluetooth или USB — экранные кнопки скроются']].map(function (o) { return '<button class="onb-opt' + (onb.how === o[0] ? ' on' : '') + '" data-a="onbh" data-v="' + o[0] + '">' + I(o[0] === 'pad' ? 'pad' : 'hand', 22) + '<b>' + o[1] + '</b><span>' + o[2] + '</span></button>'; }).join('') + '</div>'; }
  return '<div class="onb-steps">' + [0, 1, 2].map(function (i) { return '<i class="' + (i <= st ? 'on' : '') + '"></i>'; }).join('') + '</div><h2>' + h + '</h2><p>' + p + '</p><div class="onb-body">' + body + '</div><div class="onb-foot"><button class="b b-soft" data-a="onbskip">Пропустить</button><div style="display:flex;gap:8px">' + (st ? '<button class="b b-soft" data-a="onbback">Назад</button>' : '') + '<button class="b b-main" data-a="onbnext">' + (st === 2 ? 'Готово' : 'Далее') + I('right', 18) + '</button></div></div>';
}
function showOnb() { var o = $('#onb'); if (!o) { o = document.createElement('div'); o.id = 'onb'; o.className = 'onb'; o.setAttribute('role', 'dialog'); o.setAttribute('aria-label', 'Знакомство'); document.body.appendChild(o); } o.innerHTML = onbHtml(); }
function endOnb() { var ks = Object.keys(onb.cons).filter(function (k) { return onb.cons[k]; }); S.con = ks.length === 1 ? ks[0] : 'all'; S.hide = onb.how === 'pad'; S.onboarded = true; save(); var o = $('#onb'); if (o) o.remove(); location.hash = 'store'; render(); }

/* ---------- router ---------- */
var tab = 'games', layerKey = '';
function hash() { try { return (location.hash || '').slice(1); } catch (e) { return ''; } }
function renderPage() {
  document.body.classList.toggle('store', tab === 'store');
  $$('.tabs a').forEach(function (a) { a.classList.toggle('on', a.getAttribute('data-tab') === tab); });
  $('#page').innerHTML = tab === 'games' ? vGames() : vStore();
  if (tab === 'games') { initCar(); if (focusId) tint(byId[focusId]); }
  else { var l = sList().filter(function (g) { return g.isNew; }); tint(l[featIdx % (l.length || 1)] || sList()[0]); }
}
function render() {
  var h = hash() || 'games';
  if (h === 'games' || h === 'store') tab = h;
  renderPage(); renderLayer(h); topState(); mini();
}
function renderLayer(h) {
  var L = $('#layer'), html = '';
  if (h.indexOf('game-') === 0) html = oGame(h.slice(5));
  else if (h.indexOf('gset-') === 0) html = oGSet(h.slice(5));
  else if (h.indexOf('play-') === 0) html = oPlay(h.slice(5));
  else html = ({ library: oLibrary, search: oSearch, downloads: oDownloads, alerts: oAlerts, quick: oQuick, settings: oSettings, profile: oProfile, pad: oPad }[h] || function () { return ''; })();
  var was = layerKey; layerKey = html ? h : '';
  L.innerHTML = html;
  if (html && was !== h) { L.classList.remove('on'); void L.offsetWidth; requestAnimationFrame(function () { L.classList.add('on'); }); } else L.classList.toggle('on', !!html);
  if (h === 'search' && was !== h) setTimeout(function () { var q = $('#q'); if (q) q.focus(); }, 80);
  if (h.indexOf('play-') === 0) { var pid = h.slice(5); if (!booted[pid]) setTimeout(function () { booted[pid] = true; var el = $('#play'); if (el) el.classList.add('ready'); }, 1900); }
  bindDrag();
}
function close() { location.hash = tab; }
function go(h) { var run = function () { location.hash = h; }; if (document.startViewTransition && !S.calm) document.startViewTransition(run); else run(); }
window.addEventListener('hashchange', function () { var h = hash(); if (h.indexOf('game-') === 0 && layerKey.indexOf('game-') !== 0) gTab = 'about'; if (h.indexOf('play-') !== 0) { paused = false; editKeys = false; } render(); });

/* ---------- top bar, mini bar (п. 7, 15) ---------- */
function topState() {
  var a = Object.keys(lib).filter(function (i) { return ['dl', 'pause', 'q'].indexOf(lib[i].s) >= 0; }), pr = a.length ? a.reduce(function (s, i) { return s + (lib[i].pr || 0); }, 0) / a.length : 0, C = 2 * Math.PI * 13;
  $('#dlBtn').innerHTML = I('dl', 20) + (a.length ? '<svg class="ring" viewBox="0 0 30 30"><circle cx="15" cy="15" r="13" stroke="var(--line2)"/><circle cx="15" cy="15" r="13" stroke="var(--acc)" stroke-dasharray="' + C.toFixed(1) + '" stroke-dashoffset="' + (C * (1 - pr / 100)).toFixed(1) + '"/></svg>' : '');
  $('#quickBtn').innerHTML = I('grid', 20) + (alerts.some(function (x) { return x.u; }) ? '<span class="dot"></span>' : '');
}
function mini() {
  var m = $('#mini'), id = Object.keys(lib).filter(function (i) { return lib[i].s === 'dl'; })[0] || Object.keys(lib).filter(function (i) { return lib[i].s === 'pause'; })[0];
  var hide = !id || layerKey === 'downloads' || layerKey.indexOf('play-') === 0 || layerKey === 'library' || layerKey === 'settings';
  m.classList.toggle('on', !hide); if (!id) return;
  var g = byId[id], L = lib[id], left = L.s === 'dl' ? Math.max(1, Math.round(g.size * (1 - L.pr / 100) / 11 / 60)) : 0;
  m.innerHTML = '<span class="th" style="background-image:' + art(g, 300, 300) + '"></span><a class="tx" href="#downloads"><b>' + g.t + '</b><span><span data-pr="' + id + '">' + Math.floor(L.pr) + '%</span>' + (L.s === 'dl' ? ' · ещё ~' + left + ' мин' : ' · пауза') + '</span><span class="pb"><i data-bar="' + id + '" style="width:' + L.pr + '%"></i></span></a><button class="b b-soft b-rd b-sm" data-a="' + (L.s === 'dl' ? 'pause' : 'resume') + '" data-id="' + id + '" aria-label="' + (L.s === 'dl' ? 'Пауза' : 'Продолжить') + '">' + I(L.s === 'dl' ? 'pause' : 'play', 16) + '</button>';
}

/* ---------- downloads engine ---------- */
function pump() { if (Object.keys(lib).some(function (i) { return lib[i].s === 'dl'; })) return; var q = Object.keys(lib).filter(function (i) { return lib[i].s === 'q'; })[0]; if (q) lib[q].s = 'dl'; }
function install(id) {
  var g = byId[id];
  if (navigator.onLine === false) { toast(I('wifi', 18) + 'Нет интернета — установка начнётся, когда он появится'); }
  if (g.size > free()) { toast(I('alert', 18) + 'Мало памяти: нужно ' + size(g.size) + ', свободно ' + size(free())); go('downloads'); return; }
  lib[id] = lib[id] || { t: Date.now() }; lib[id].s = 'q'; lib[id].pr = 0; pump(); toast(I('dl', 18) + '«' + g.t + '» — установка');
}
setInterval(function () {
  var done = false;
  Object.keys(lib).forEach(function (i) {
    var L = lib[i]; if (L.s !== 'dl') return;
    L.pr = Math.min(100, L.pr + (9 + Math.random() * 5) / byId[i].size * 100 * 1.5);
    if (L.pr >= 100) { lib[i] = { s: 'ok', play: 0, last: 'Ещё не запускалась', saves: 0, t: Date.now() }; done = true; alerts.unshift({ t: '«' + byId[i].t + '» установлена', d: 'Только что', u: 1, g: i }); toast(I('check', 18) + '«' + byId[i].t + '» установлена'); buzz(20); }
    else { $$('[data-pr="' + i + '"]').forEach(function (n) { n.textContent = Math.floor(L.pr) + '%'; }); $$('[data-bar="' + i + '"]').forEach(function (n) { n.style.width = L.pr + '%'; }); }
  });
  if (done) { pump(); var keep = layerKey; renderPage(); if (keep) renderLayer(keep); }
  topState(); if (done) mini();
}, 600);

/* ---------- toast ---------- */
var tT;
function toast(h) { var t = $('#toast'); t.innerHTML = h; t.classList.add('on'); clearTimeout(tT); tT = setTimeout(function () { t.classList.remove('on'); }, 2400); }

/* ---------- apply prefs ---------- */
function applyPrefs() {
  var r = document.documentElement;
  r.setAttribute('data-theme', S.theme); r.setAttribute('data-big', S.big ? '1' : '0'); r.setAttribute('data-contrast', S.contrast ? 'high' : ''); r.setAttribute('data-calm', S.calm || S.saver ? '1' : '0');
  r.style.filter = S.bright < 100 ? 'brightness(' + S.bright / 100 + ')' : '';
  var m = $('meta[name=theme-color]'); if (m) m.setAttribute('content', getComputedStyle(r).getPropertyValue('--bg').trim() || '#0B0C0F');
}
function refresh() { applyPrefs(); var k = layerKey; renderPage(); if (k) renderLayer(k); topState(); mini(); }

/* ---------- actions ---------- */
document.addEventListener('click', function (e) {
  var vt = e.target.closest('a[data-vt]');
  if (vt && !e.defaultPrevented) { e.preventDefault(); vt.style.viewTransitionName = 'cover'; go('game-' + vt.getAttribute('data-vt')); setTimeout(function () { vt.style.viewTransitionName = ''; }, 400); return; }
  var t = e.target.closest('[data-a]'); if (!t) return;
  var a = t.getAttribute('data-a'), id = t.getAttribute('data-id'), v = t.getAttribute('data-v'), k = t.getAttribute('data-k'), g = byId[id];
  switch (a) {
    case 'con': S.con = v; save(); focusId = null; refresh(); break;
    case 'cc': e.preventDefault(); if (id === focusId) { if (lib[id].s === 'ok') go('play-' + id); else go('game-' + id); } else { var car = $('#car'); car.scrollTo({ left: t.offsetLeft + t.offsetWidth / 2 - car.clientWidth / 2, behavior: S.calm ? 'auto' : 'smooth' }); } break;
    case 'play': paused = false; buzz(12); click(660); go('play-' + id); break;
    case 'add': lib[id] = { s: 'own', t: Date.now() }; toast(I('plus', 18) + '«' + g.t + '» в библиотеке'); if (S.auto) install(id); refresh(); break;
    case 'addinstall': lib[id] = { s: 'own', t: Date.now() }; install(id); refresh(); break;
    case 'install': install(id); refresh(); break;
    case 'pause': lib[id].s = 'pause'; pump(); refresh(); break;
    case 'resume': Object.keys(lib).forEach(function (i) { if (lib[i].s === 'dl') lib[i].s = 'q'; }); lib[id].s = 'dl'; refresh(); break;
    case 'cancel': lib[id].s = 'own'; delete lib[id].pr; pump(); toast('Установка «' + g.t + '» отменена — игра осталась в библиотеке'); refresh(); break;
    case 'uninstall': lib[id] = { s: 'own', t: lib[id].t, saves: lib[id].saves }; toast(I('trash', 18) + '«' + g.t + '» удалена с телефона, осталась в библиотеке'); refresh(); break;
    case 'fav': if (favs[id]) delete favs[id]; else favs[id] = 1; refresh(); break;
    case 'togcol': var c = cols[v], ix = c.indexOf(id); if (ix >= 0) c.splice(ix, 1); else c.push(id); refresh(); break;
    case 'lcol': S.lc = S.lc === v ? '' : v; save(); refresh(); break;
    case 'mkcol': var nm = ($('#newcol') || {}).value || ''; nm = nm.trim(); if (!nm) { toast('Введи название коллекции'); break; } if (!cols[nm]) cols[nm] = []; newCol = ''; S.lc = nm; toast('Коллекция «' + esc(nm) + '» создана'); refresh(); break;
    case 'fit': S.fit = !S.fit; save(); refresh(); break;
    case 'gtab': gTab = v; renderLayer(hash()); break;
    case 'close': e.preventDefault(); close(); break;
    case 'back': go(v); break;
    case 'sq': query = v; addRecent(v); $('#q').value = v; $('#sres').innerHTML = sBody(); break;
    case 'clrrecent': S.recent = []; save(); $('#sres').innerHTML = sBody(); break;
    case 'readall': alerts.forEach(function (x) { x.u = 0; }); refresh(); break;
    case 'stab': setTab = v; renderLayer('settings'); break;
    case 'tg': S[k] = !S[k]; save(); if (k === 'saver') toast(S.saver ? I('battery', 18) + 'Экономия: ниже разрешение и FPS, без анимаций' : 'Экономия выключена'); refresh(); break;
    case 'set': S[k] = v; save(); if (k === 'con') focusId = null; refresh(); break;
    case 'qtg': S[k] = !S[k]; save(); refresh(); break;
    case 'qtheme': S.theme = { dark: 'oled', oled: 'light', light: 'dark' }[S.theme]; save(); refresh(); break;
    case 'qgo': go(k); break;
    case 'padreset': S.op = 55; S.sc = 100; S.layout = 'Классика'; keyPos = {}; save(); renderLayer('pad'); toast('Раскладка сброшена'); break;
    case 'toast': toast(v); break;
    case 'pausegame': paused = true; editKeys = false; $('#play').classList.add('paused'); break;
    case 'resumegame': paused = false; $('#play').classList.remove('paused'); break;
    case 'editkeys': editKeys = !editKeys; renderLayer(hash()); if (editKeys) toast(I('move', 18) + 'Перетаскивай кнопки пальцем'); break;
    case 'qsave': lib[id].saves = (lib[id].saves || 0) + 1; toast(I('save', 18) + 'Сохранено · слот ' + lib[id].saves); buzz(10); break;
    case 'loadslot': if (layerKey.indexOf('play-') === 0) { paused = false; $('#play').classList.remove('paused'); toast(I('load', 18) + 'Сохранение загружено'); } else { paused = false; go('play-' + id); toast(I('load', 18) + 'Запуск с сохранения'); } break;
    case 'qshot': toast(I('cam', 18) + 'Скриншот сохранён в галерею'); break;
    case 'qset': go('gset-' + id); break;
    case 'exitgame': lib[id].last = 'Только что'; go('games'); break;
    case 'replay': splash(true); break;
    case 'onb': onb.step = 0; close(); showOnb(); break;
    case 'onbc': if (onb.cons[v]) delete onb.cons[v]; else onb.cons[v] = 1; showOnb(); break;
    case 'onbw': onb.where = v; showOnb(); break;
    case 'onbh': onb.how = v; showOnb(); break;
    case 'onbback': onb.step--; showOnb(); break;
    case 'onbnext': if (onb.step < 2) { onb.step++; showOnb(); } else endOnb(); break;
    case 'onbskip': endOnb(); break;
  }
});
function addRecent(v) { v = v.trim(); if (!v) return; S.recent = [v].concat(S.recent.filter(function (x) { return x !== v; })).slice(0, 5); save(); }
var qT;
document.addEventListener('input', function (e) {
  var t = e.target;
  if (t.id === 'q') { query = t.value; $('#sres').innerHTML = sBody(); clearTimeout(qT); qT = setTimeout(function () { addRecent(query); }, 1200); }
  if (t.id === 'lq') { S.lq = t.value; var pos = t.selectionStart; renderLayer('library'); var n = $('#lq'); n.focus(); try { n.setSelectionRange(pos, pos); } catch (er) {} }
  if (t.id === 'newcol') newCol = t.value;
  if (t.type === 'range' && t.dataset.k) { S[t.dataset.k] = +t.value; save(); var o = $('#o-' + t.dataset.k); if (o) o.textContent = t.value + '%'; $$('.ctl').forEach(function (c) { c.style.setProperty('--op', S.op / 100); c.style.setProperty('--sc', S.sc / 100); }); if (t.dataset.k === 'bright') applyPrefs(); }
});

/* drag keys (п. 20) */
function bindDrag() {
  $$('.edit .k').forEach(function (k) {
    k.onpointerdown = function (ev) {
      ev.preventDefault(); var id = k.getAttribute('data-key'), p = keyPos[id] || [0, 0], sx = ev.clientX, sy = ev.clientY; k.setPointerCapture(ev.pointerId);
      k.onpointermove = function (m) { var nx = p[0] + (m.clientX - sx) * (S.mirror ? -1 : 1), ny = p[1] + m.clientY - sy; keyPos[id] = [nx, ny]; k.style.translate = nx + 'px ' + ny + 'px'; };
      k.onpointerup = function () { k.onpointermove = null; S.layout = 'Своя'; buzz(6); };
    };
  });
}

/* ---------- keyboard + gamepad navigation (п. 11) ---------- */
function focusables() { var root = $('#onb') || (layerKey ? $('#layer') : document.body); return $$('a[href],button:not([disabled]),input', root).filter(function (el) { var r = el.getBoundingClientRect(); return r.width > 0 && r.height > 0 && r.bottom > 0 && r.top < innerHeight && !el.closest('.layer:not(.on)') && (layerKey || !el.closest('#layer')); }); }
function moveFocus(dir) {
  var list = focusables(), cur = document.activeElement; if (!list.length) return;
  if (!cur || list.indexOf(cur) < 0) { list[0].focus(); mark(list[0]); return; }
  var a = cur.getBoundingClientRect(), ax = a.left + a.width / 2, ay = a.top + a.height / 2, best = null, bd = 1e9;
  list.forEach(function (el) { if (el === cur) return; var b = el.getBoundingClientRect(), bx = b.left + b.width / 2, by = b.top + b.height / 2, dx = bx - ax, dy = by - ay;
    var ok = dir === 'r' ? dx > 4 : dir === 'l' ? dx < -4 : dir === 'd' ? dy > 4 : dy < -4; if (!ok) return;
    var d = (dir === 'r' || dir === 'l') ? Math.abs(dx) + Math.abs(dy) * 2.5 : Math.abs(dy) + Math.abs(dx) * 2.5; if (d < bd) { bd = d; best = el; } });
  if (best) { best.focus({ preventScroll: false }); best.scrollIntoView({ block: 'nearest', inline: 'center', behavior: S.calm ? 'auto' : 'smooth' }); mark(best); click(1000); }
}
function mark(el) { $$('.gp-focus').forEach(function (x) { x.classList.remove('gp-focus'); }); el.classList.add('gp-focus'); }
function back() { if ($('#onb')) return; if (layerKey.indexOf('play-') === 0) { paused = !paused; $('#play').classList.toggle('paused', paused); } else if (layerKey) close(); }
function switchTab(d) { go(d > 0 ? 'store' : 'games'); }
document.addEventListener('keydown', function (e) {
  var typing = /INPUT|TEXTAREA/.test((document.activeElement || {}).tagName || '');
  if (e.key === 'Escape') { back(); return; }
  if (typing) return;
  var m = { ArrowRight: 'r', ArrowLeft: 'l', ArrowDown: 'd', ArrowUp: 'u' }[e.key];
  if (m) { e.preventDefault(); moveFocus(m); }
  if (e.key === '/' ) { e.preventDefault(); go('search'); }
});
var gpPrev = {};
function gpLoop() {
  var pads = navigator.getGamepads ? Array.prototype.filter.call(navigator.getGamepads(), Boolean) : [];
  if (pads.length) {
    var p = pads[0], b = function (i) { return p.buttons[i] && p.buttons[i].pressed; }, ax = p.axes[0] || 0, ay = p.axes[1] || 0;
    var now = { r: b(15) || ax > .6, l: b(14) || ax < -.6, d: b(13) || ay > .6, u: b(12) || ay < -.6, a: b(0), b: b(1), lb: b(4), rb: b(5), st: b(9) };
    Object.keys(now).forEach(function (k) {
      if (now[k] && !gpPrev[k]) {
        if ('rldu'.indexOf(k) >= 0 && k.length === 1) moveFocus(k);
        else if (k === 'a') { var el = document.activeElement; if (el && el !== document.body) el.click(); }
        else if (k === 'b') back();
        else if (k === 'lb' && !layerKey) switchTab(-1);
        else if (k === 'rb' && !layerKey) switchTab(1);
        else if (k === 'st') go(layerKey === 'quick' ? tab : 'quick');
      }
    });
    gpPrev = now;
  }
  requestAnimationFrame(gpLoop);
}
window.addEventListener('gamepadconnected', function (e) { toast(I('pad', 18) + 'Геймпад подключён: ' + (e.gamepad.id.split('(')[0].trim() || 'контроллер')); if (S.hide) document.documentElement.classList.add('has-pad'); });
window.addEventListener('gamepaddisconnected', function () { toast('Геймпад отключён'); document.documentElement.classList.remove('has-pad'); });
requestAnimationFrame(gpLoop);
window.addEventListener('online', function () { toast(I('wifi', 18) + 'Интернет снова есть'); refresh(); });
window.addEventListener('offline', function () { toast(I('wifi', 18) + 'Нет интернета — играть можно в установленные'); refresh(); });
$('#turnSkip').addEventListener('click', function () { $('#turn').classList.add('off'); });
setInterval(function () { if (tab === 'store' && !layerKey && !document.hidden && $('#feat')) { featIdx++; var f = $('#feat'), tmp = document.createElement('div'); tmp.innerHTML = featHtml(); f.replaceWith(tmp.firstChild); } }, 7000);

/* ---------- splash (п. 34) ---------- */
function splash(forceFull) {
  var full = forceFull || !S.launched, sp = document.createElement('div');
  sp.className = 'splash ' + (full ? 'full' : 'short'); sp.setAttribute('aria-hidden', 'true');
  sp.innerHTML = '<div class="sp"><div class="sp-cap"><span class="sp-h l"></span><span class="sp-h r"></span><span class="sp-play"></span><span class="sp-lines"></span></div><span class="sp-wd">KAPSULA</span></div>';
  document.body.appendChild(sp);
  var calm = S.calm || (window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches), hold = calm ? 300 : full ? 1350 : 550;
  if (full) setTimeout(function () { buzz(15); }, 400);
  setTimeout(function () {
    var mk = $('.brand-mk'), s = $('.sp', sp);
    if (mk && s && !calm) { var a = mk.getBoundingClientRect(), b = s.getBoundingClientRect(), k = a.height / 52; s.style.transformOrigin = '0 0'; s.style.transform = 'translate(' + (a.left - b.left) + 'px,' + (a.top - b.top) + 'px) scale(' + k + ')'; }
    setTimeout(function () { sp.classList.add('out'); setTimeout(function () { sp.remove(); }, 450); }, calm ? 0 : 450);
  }, hold);
  if (!S.launched) { S.launched = true; save(); }
  return full;
}

/* ---------- start ---------- */
$$('[data-i]').forEach(function (el) { el.innerHTML = I(el.getAttribute('data-i'), 20); });
applyPrefs();
var firstEver = !S.onboarded;
splash(false);
render();
if (firstEver) setTimeout(showOnb, 1900);
})();
