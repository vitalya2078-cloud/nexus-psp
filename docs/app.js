(function(){
'use strict';
/* ---------- icons ---------- */
var IC={
home:'<path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/>',
lib:'<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
store:'<path d="M4 9h16l-1.2 11H5.2z"/><path d="M8.5 9V7a3.5 3.5 0 0 1 7 0v2"/>',
heart:'<path d="M12 20s-7.5-4.7-7.5-10.2A4.3 4.3 0 0 1 12 7.1a4.3 4.3 0 0 1 7.5 2.7C19.5 15.3 12 20 12 20z"/>',
heartf:'<path fill="currentColor" d="M12 20s-7.5-4.7-7.5-10.2A4.3 4.3 0 0 1 12 7.1a4.3 4.3 0 0 1 7.5 2.7C19.5 15.3 12 20 12 20z"/>',
download:'<path d="M12 4v11"/><path d="m7 10 5 5 5-5"/><path d="M5 20h14"/>',
gear:'<path d="M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1"/><circle cx="15" cy="6" r="2"/><circle cx="9" cy="12" r="2"/><circle cx="17" cy="18" r="2"/>',
search:'<circle cx="11" cy="11" r="7"/><path d="m20 20-4-4"/>',
pad:'<path d="M6.5 8h11A4.5 4.5 0 0 1 22 12.5v1a3.5 3.5 0 0 1-6.3 2.1L14.5 14h-5l-1.2 1.6A3.5 3.5 0 0 1 2 13.5v-1A4.5 4.5 0 0 1 6.5 8z"/><path d="M7.5 10.5v3M6 12h3"/><circle cx="16" cy="11" r=".7" fill="currentColor"/><circle cx="18" cy="13" r=".7" fill="currentColor"/>',
bell:'<path d="M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"/><path d="M10 20.5a2 2 0 0 0 4 0"/>',
left:'<path d="M15 5l-7 7 7 7"/>', right:'<path d="M9 5l7 7-7 7"/>',
play:'<path fill="currentColor" stroke="none" d="M8 5.5v13a1 1 0 0 0 1.5.9l10.2-6.5a1 1 0 0 0 0-1.8L9.5 4.6A1 1 0 0 0 8 5.5z"/>',
pause:'<rect x="6.5" y="5" width="3.5" height="14" rx="1" fill="currentColor" stroke="none"/><rect x="14" y="5" width="3.5" height="14" rx="1" fill="currentColor" stroke="none"/>',
x:'<path d="M6 6l12 12M18 6 6 18"/>', check:'<path d="m5 12.5 4.5 4.5L19 7.5"/>',
cloud:'<path d="M7 18a4.5 4.5 0 0 1-.6-9 6 6 0 0 1 11.4 1.6A3.8 3.8 0 0 1 17.5 18z"/>',
trophy:'<path d="M8 4h8v5a4 4 0 0 1-8 0z"/><path d="M8 6H5a3 3 0 0 0 3 4M16 6h3a3 3 0 0 1-3 4M12 13v4M8.5 20h7M10 17h4"/>',
clock:'<circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2"/>',
save:'<path d="M5 4h11l3 3v12a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1z"/><path d="M8 4v5h7V4M8 20v-6h8v6"/>',
trash:'<path d="M4 7h16M9 7V4.5h6V7M6.5 7l1 13h9l1-13"/>',
plus:'<path d="M12 5v14M5 12h14"/>',
folder:'<path d="M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>',
wifi:'<path d="M3 9a13 13 0 0 1 18 0M6 12.5a8.5 8.5 0 0 1 12 0M9 16a4 4 0 0 1 6 0"/><circle cx="12" cy="19" r=".9" fill="currentColor"/>',
info:'<circle cx="12" cy="12" r="8.5"/><path d="M12 11v5.5M12 7.8v.1"/>',
monitor:'<rect x="3" y="4.5" width="18" height="12" rx="2"/><path d="M8.5 20h7M12 16.5V20"/>',
volume:'<path d="M4 9.5h3.5L12 6v12l-4.5-3.5H4z"/><path d="M15.5 9a4 4 0 0 1 0 6M18 6.5a7.5 7.5 0 0 1 0 11"/>',
sparkle:'<path d="M12 3.5 13.8 10 20.5 12l-6.7 2L12 20.5 10.2 14 3.5 12l6.7-2z"/>',
bolt:'<path d="M13 3 5 13.5h6L10 21l8-10.5h-6z"/>',
user:'<circle cx="12" cy="8.5" r="4"/><path d="M4.5 20a7.5 7.5 0 0 1 15 0"/>',
phone:'<rect x="3" y="7" width="18" height="10" rx="2.5"/><path d="M6.5 12h1.5"/>',
star:'<path d="m12 4 2.4 5 5.4.6-4 3.7 1.1 5.4L12 16l-4.9 2.7 1.1-5.4-4-3.7 5.4-.6z"/>',
bell2:'<path d="M6 16v-5a6 6 0 0 1 12 0v5l1.5 2h-15z"/>',
expand:'<path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>',
grid4:'<rect x="4" y="4" width="6" height="6" rx="1"/><rect x="14" y="4" width="6" height="6" rx="1"/><rect x="4" y="14" width="6" height="6" rx="1"/><rect x="14" y="14" width="6" height="6" rx="1"/>'
};
function ic(n,s,extra){s=s||20;return '<svg aria-hidden="true" width="'+s+'" height="'+s+'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"'+(extra||'')+'>'+(IC[n]||'')+'</svg>';}
function esc(s){return String(s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];});}

/* ---------- procedural cover art ---------- */
function rng(seed){var s=seed%2147483647;if(s<=0)s+=2147483646;return function(){s=s*16807%2147483647;return (s-1)/2147483646;};}
var artCache={};
function art(g,w,h,seed){
  var key=g.id+w+'x'+h+(seed||0); if(artCache[key])return artCache[key];
  var r=rng((g.seed+(seed||0)*977)), a=g.pal[0], b=g.pal[1], c=g.pal[2], i, s='';
  s+="<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 "+w+" "+h+"' preserveAspectRatio='xMidYMid slice'>";
  s+="<defs><linearGradient id='g' x1='0' y1='0' x2='0' y2='1'><stop offset='0' stop-color='"+a+"'/><stop offset='1' stop-color='"+b+"'/></linearGradient>";
  s+="<radialGradient id='glow' cx='.5' cy='.5' r='.5'><stop offset='0' stop-color='"+c+"' stop-opacity='.55'/><stop offset='1' stop-color='"+c+"' stop-opacity='0'/></radialGradient></defs>";
  s+="<rect width='"+w+"' height='"+h+"' fill='url(#g)'/>";
  for(i=0;i<40;i++){s+="<circle cx='"+(r()*w).toFixed(1)+"' cy='"+(r()*h*.6).toFixed(1)+"' r='"+(r()*1.6+.3).toFixed(2)+"' fill='#fff' opacity='"+(r()*.6+.1).toFixed(2)+"'/>";}
  var m=g.motif, hz=h*.6, cx=w*(.35+r()*.3);
  if(m==='synth'){
    var R=Math.min(w,h)*.27;
    s+="<circle cx='"+w/2+"' cy='"+hz+"' r='"+R*2.2+"' fill='url(#glow)'/><circle cx='"+w/2+"' cy='"+hz+"' r='"+R+"' fill='"+c+"'/>";
    for(i=0;i<6;i++){var yy=hz-R*.1+i*R*.17;s+="<rect x='0' y='"+yy.toFixed(1)+"' width='"+w+"' height='"+(3+i*1.6).toFixed(1)+"' fill='"+a+"'/>";}
    s+="<rect x='0' y='"+hz+"' width='"+w+"' height='"+(h-hz)+"' fill='"+b+"'/>";
    for(i=-12;i<=12;i++){s+="<line x1='"+w/2+"' y1='"+hz+"' x2='"+(w/2+i*w*.12)+"' y2='"+h+"' stroke='"+c+"' stroke-opacity='.55' stroke-width='1.4'/>";}
    for(i=1;i<9;i++){var ly=hz+Math.pow(i/9,2)*(h-hz);s+="<line x1='0' y1='"+ly.toFixed(1)+"' x2='"+w+"' y2='"+ly.toFixed(1)+"' stroke='"+c+"' stroke-opacity='.45' stroke-width='1.2'/>";}
  } else if(m==='city'){
    s+="<circle cx='"+cx+"' cy='"+h*.32+"' r='"+Math.min(w,h)*.5+"' fill='url(#glow)'/>";
    var x=0; while(x<w){var bw=w*(.06+r()*.1), bh=h*(.2+r()*.45); s+="<rect x='"+x.toFixed(1)+"' y='"+(h-bh).toFixed(1)+"' width='"+bw.toFixed(1)+"' height='"+bh.toFixed(1)+"' fill='"+b+"' stroke='"+c+"' stroke-opacity='.35'/>";
      for(var wy=h-bh+8;wy<h-6;wy+=12){for(var wx=x+5;wx<x+bw-6;wx+=9){if(r()>.62)s+="<rect x='"+wx.toFixed(1)+"' y='"+wy.toFixed(1)+"' width='4' height='5' fill='"+c+"' opacity='"+(r()*.7+.3).toFixed(2)+"'/>";}}
      x+=bw+2;}
  } else if(m==='orbit'){
    var pr=Math.min(w,h)*(.2+r()*.1), py=h*(.4+r()*.15);
    s+="<circle cx='"+cx+"' cy='"+py+"' r='"+pr*2.4+"' fill='url(#glow)'/><circle cx='"+cx+"' cy='"+py+"' r='"+pr+"' fill='"+c+"'/><circle cx='"+(cx-pr*.3)+"' cy='"+(py-pr*.3)+"' r='"+pr*.85+"' fill='"+a+"' opacity='.35'/>";
    s+="<ellipse cx='"+cx+"' cy='"+py+"' rx='"+pr*1.9+"' ry='"+pr*.45+"' fill='none' stroke='"+c+"' stroke-width='3' opacity='.8' transform='rotate(-16 "+cx+" "+py+")'/>";
    s+="<circle cx='"+(w*.82)+"' cy='"+(h*.2)+"' r='"+pr*.22+"' fill='"+c+"' opacity='.8'/>";
  } else if(m==='mountains'){
    s+="<circle cx='"+cx+"' cy='"+h*.3+"' r='"+Math.min(w,h)*.12+"' fill='"+c+"' opacity='.9'/><circle cx='"+cx+"' cy='"+h*.3+"' r='"+Math.min(w,h)*.4+"' fill='url(#glow)'/>";
    for(var L=0;L<3;L++){var pts='0,'+h, base=h*(.5+L*.12);for(var px=0;px<=w;px+=w/8){pts+=' '+px.toFixed(1)+','+(base-r()*h*.18).toFixed(1);}pts+=' '+w+','+h;
      s+="<polygon points='"+pts+"' fill='"+(L===2?b:c)+"' opacity='"+(L===2?1:(.18+L*.2))+"'/>";}
  } else if(m==='waves'){
    s+="<circle cx='"+cx+"' cy='"+h*.45+"' r='"+Math.min(w,h)*.5+"' fill='url(#glow)'/>";
    for(i=0;i<9;i++){var y0=h*(.3+i*.075), amp=h*(.03+r()*.05), d='M0 '+y0.toFixed(1);for(var xx=0;xx<=w;xx+=w/24){d+=' L'+xx.toFixed(1)+' '+(y0+Math.sin(xx/w*Math.PI*(2+i*.4)+i)*amp).toFixed(1);}
      s+="<path d='"+d+"' fill='none' stroke='"+c+"' stroke-width='"+(1.5+i*.35).toFixed(1)+"' opacity='"+(.2+i*.08).toFixed(2)+"'/>";}
  } else {
    s+="<circle cx='"+cx+"' cy='"+h*.45+"' r='"+Math.min(w,h)*.5+"' fill='url(#glow)'/>";
    for(i=0;i<16;i++){var tx=r()*w,ty=r()*h,sz=Math.min(w,h)*(.08+r()*.25);s+="<polygon points='"+tx.toFixed(1)+","+(ty-sz).toFixed(1)+" "+(tx+sz*.9).toFixed(1)+","+(ty+sz*.6).toFixed(1)+" "+(tx-sz*.7).toFixed(1)+","+(ty+sz*.4).toFixed(1)+"' fill='"+(r()>.5?c:a)+"' opacity='"+(.15+r()*.55).toFixed(2)+"' stroke='"+c+"' stroke-opacity='.6'/>";}
  }
  s+="</svg>";
  return artCache[key]="url('data:image/svg+xml,"+encodeURIComponent(s).replace(/'/g,'%27').replace(/\(/g,'%28').replace(/\)/g,'%29')+"')";
}

/* ---------- demo data ---------- */
var G=[
 {id:'drift',title:'Неоновый Дрифт',genre:'Гонки',tags:['Аркада','Ночной город','Саундтрек синтвейв'],size:1240,ver:'1.2',region:'EU',lang:'RU, EN',players:'1–4 (ad-hoc)',year:2009,dev:'Студия «Полночь»',compat:3,fmt:'ISO',discId:'ULES-00000',added:'2 дня назад',isNew:true,
  pal:['#2A0B4A','#0A0618','#FF4FD8'],motif:'synth',seed:11,desc:'Аркадные ночные гонки по бесконечному неоновому шоссе. Дрифт заряжает ускорение, а ритм трассы подстраивается под музыку.',feat:['24 трассы и режим карьеры','Мультиплеер до 4 игроков по ad-hoc','Фоторежим и повторы заездов']},
 {id:'rubezh',title:'Последний Рубеж',genre:'Экшен',tags:['Шутер от 3-го лица','Кооператив'],size:1630,ver:'1.0',region:'US',lang:'EN',players:'1–2',year:2010,dev:'Северный Код',compat:2,fmt:'ISO',discId:'ULUS-00000',added:'5 дней назад',isNew:true,
  pal:['#3A1408','#0B0606','#FF8A3D'],motif:'city',seed:23,desc:'Город пал за одну ночь. Удерживай последние кварталы вместе с напарником, строй баррикады и прорывайся к эвакуации.',feat:['Кооператив на двоих','Система укрытий','12 сюжетных глав']},
 {id:'orbit',title:'Орбита-9',genre:'Стратегия',tags:['Космос','Пошаговая'],size:690,ver:'2.1',region:'EU',lang:'RU, EN',players:'1',year:2008,dev:'Квант Геймс',compat:3,fmt:'CSO',discId:'ULES-00000',added:'неделю назад',isNew:true,
  pal:['#06223A','#03070F','#35D7FF'],motif:'orbit',seed:37,desc:'Пошаговая стратегия о колонизации системы из девяти планет. Каждая орбита — отдельная экономика, которую нужно связать в одну сеть.',feat:['Генерация звёздных систем','Дипломатия и торговля','Партия на 20–40 минут']},
 {id:'ash',title:'Хроники Пепла',genre:'RPG',tags:['Фэнтези','Открытый мир','Крафт'],size:1810,ver:'1.4',region:'EU',lang:'RU',players:'1',year:2011,dev:'Тёплый Ветер',compat:3,fmt:'ISO',discId:'ULES-00000',added:'2 недели назад',
  pal:['#3B2416','#0E0907','#FFB547'],motif:'mountains',seed:41,desc:'Ролевая игра о королевстве, засыпанном вулканическим пеплом. Исследуй долины, собирай отряд и решай, кого спасать.',feat:['60+ часов прохождения','Три концовки','Полная русская локализация']},
 {id:'rhythm',title:'Ритм Улиц',genre:'Музыка',tags:['Ритм-игра','Танцы'],size:520,ver:'1.1',region:'JP',lang:'EN, JP',players:'1–2',year:2009,dev:'Бит-Лаб',compat:2,fmt:'CSO',discId:'ULJS-00000',added:'3 недели назад',
  pal:['#1A0A3D','#08061A','#B48CFF'],motif:'waves',seed:53,desc:'Попадай в бит на уличных площадках. Чем точнее серия, тем ярче светится район.',feat:['40 треков','Редактор собственных карт','Дуэли на двоих']},
 {id:'shards',title:'Осколки',genre:'Головоломка',tags:['Физика','Минимализм'],size:180,ver:'1.0',region:'EU',lang:'RU, EN',players:'1',year:2010,dev:'Тихая Комната',compat:3,fmt:'ISO',discId:'ULES-00000',added:'месяц назад',
  pal:['#0B2E2A','#050C0B','#45E3A0'],motif:'shards',seed:67,desc:'Собери разбитый свет обратно. Каждый уровень — кристалл, который нужно сложить, вращая и отражая лучи.',feat:['120 уровней','Без таймеров и спешки','Короткие сессии по 5 минут']},
 {id:'deep',title:'Глубина',genre:'Приключения',tags:['Подводный мир','Исследование'],size:960,ver:'1.3',region:'US',lang:'EN',players:'1',year:2011,dev:'Синий Маяк',compat:2,fmt:'ISO',discId:'ULUS-00000',added:'месяц назад',
  pal:['#032433','#01080C','#3FA9FF'],motif:'waves',seed:79,desc:'Спустись на батискафе в океанский разлом и найди источник сигнала, который идёт со дна уже сорок лет.',feat:['Нелинейная карта глубин','Улучшения батискафа','Атмосферный звук']},
 {id:'fist',title:'Стальной Кулак',genre:'Файтинг',tags:['Турнир','Комбо'],size:1420,ver:'1.0',region:'EU',lang:'RU, EN',players:'1–2 (ad-hoc)',year:2010,dev:'Арена-7',compat:1,fmt:'ISO',discId:'ULES-00000',added:'2 месяца назад',
  pal:['#3A0812','#0A0305','#FF4D5E'],motif:'shards',seed:83,desc:'Турнирный файтинг с 18 бойцами и глубокой системой комбо. Тренируйся в додзё и выходи на подпольную арену.',feat:['18 бойцов','Режим тренировки с подсказками','Дуэли по ad-hoc']},
 {id:'forest',title:'Тихий Лес',genre:'Хоррор',tags:['Выживание','Атмосфера'],size:870,ver:'1.0',region:'EU',lang:'RU',players:'1',year:2012,dev:'Полночь',compat:2,fmt:'CSO',discId:'ULES-00000',added:'2 месяца назад',
  pal:['#0E2214','#030705','#9BE36A'],motif:'mountains',seed:97,desc:'Ночь в заповеднике, где пропал отряд лесников. Фонарь, рация и то, что отвечает тебе в эфире.',feat:['Динамический звук','Ограниченные ресурсы','Прохождение за 6–8 часов']},
 {id:'pixel',title:'Пиксельные Войны',genre:'Стратегия',tags:['Тактика','Пиксель-арт'],size:310,ver:'1.2',region:'US',lang:'EN',players:'1–4',year:2009,dev:'Восемь Бит',compat:3,fmt:'ISO',discId:'ULUS-00000',added:'3 месяца назад',
  pal:['#1D1A40','#07061A','#FFD84D'],motif:'city',seed:101,desc:'Тактика на клеточных картах: захватывай заводы, строй армию и защищай столицу. Матч длится одну поездку в метро.',feat:['Кампания на 30 миссий','Редактор карт','Хот-сит на одном устройстве']}
];
var SOON=[
 {id:'courier',title:'Звёздный Курьер',genre:'Аркада',pal:['#140F3A','#05040F','#7FA6FF'],motif:'orbit',seed:131,when:'Декабрь',desc:'Доставляй посылки между станциями, уворачиваясь от метеоритов.'},
 {id:'code',title:'Код Тьмы',genre:'Стелс',pal:['#1F0B24','#07030A','#E070FF'],motif:'shards',seed:151,when:'Январь',desc:'Проникни в сеть корпорации и выйди незамеченным.'}
];
var byId={};G.concat(SOON).forEach(function(g){byId[g.id]=g;g.glow=g.pal[2];});
var GENRES=[];G.forEach(function(g){if(GENRES.indexOf(g.genre)<0)GENRES.push(g.genre);});

var lib={
 drift:{status:'installed',progress:100,play:762,last:'Сегодня, 21:40',saves:3},
 ash:{status:'installed',progress:100,play:1345,last:'Вчера',saves:5},
 orbit:{status:'downloading',progress:46,speed:11.4},
 rhythm:{status:'queued',progress:0},
 shards:{status:'installed',progress:100,play:96,last:'3 дня назад',saves:1},
 fist:{status:'paused',progress:12}
};
var favs={drift:true,ash:true,deep:true};
var soonWatch={};
var notifs=[
 {ic:'download',color:'var(--cyan)',text:'«Хроники Пепла» установлена и готова к запуску',time:'Вчера',unread:true},
 {ic:'sparkle',color:'var(--neon)',text:'Новая игра в каталоге: «Неоновый Дрифт»',time:'2 дня назад',unread:true},
 {ic:'info',color:'var(--soft)',text:'Доступно обновление NEXUS 0.3.1: улучшена совместимость',time:'5 дней назад',unread:false}
];
var defaults={accent:'ember',lang:'ru',start:'home',wifiOnly:true,parallel:'1',autoInstall:true,res:'2',backend:'vulkan',fps:false,frameskip:false,vol:80,haptic:true,nDl:true,nNew:true,nUpd:true,
  padOp:60,padSc:100,padLayout:'classic',padHaptic:true,padHide:true,padEdit:false,cloud:true};
var S={};
try{S=JSON.parse(localStorage.getItem('nexus-proto-settings')||'{}')||{};}catch(e){S={};}
Object.keys(defaults).forEach(function(k){if(S[k]===undefined)S[k]=defaults[k];});
if(S.accent==='violet'||S.accent==='cyan'||S.accent==='pink'||S.accent==='lime')S.accent='ember';
function saveS(){try{localStorage.setItem('nexus-proto-settings',JSON.stringify(S));}catch(e){}}
var ACCENTS={ember:{n:'Огонь',neon:'#FF4A1C',btn:'#FF4A1C',ink:'#140400',soft:'rgba(255,74,28,.14)'},ice:{n:'Лёд',neon:'#7FB2FF',btn:'#7FB2FF',ink:'#06102A',soft:'rgba(127,178,255,.14)'},gold:{n:'Золото',neon:'#FFC23D',btn:'#FFC23D',ink:'#1A1200',soft:'rgba(255,194,61,.14)'},toxic:{n:'Токсик',neon:'#B6F03C',btn:'#B6F03C',ink:'#0F1404',soft:'rgba(182,240,60,.13)'}};
function applyAccent(){var a=ACCENTS[S.accent]||ACCENTS.ember,st=document.documentElement.style;st.setProperty('--neon',a.neon);st.setProperty('--btn',a.btn);st.setProperty('--btn-ink',a.ink);st.setProperty('--neon-soft',a.soft);}

/* ---------- helpers ---------- */
function fmtSize(mb){return mb>=1000?(mb/1024).toFixed(1).replace('.',',')+' ГБ':Math.round(mb)+' МБ';}
function fmtTime(min){var h=Math.floor(min/60),m=min%60;return h?(h+' ч '+(m?m+' мин':'')):m+' мин';}
function compatLabel(c){return c===3?'Отлично':c===2?'Хорошо':'Играбельно';}
function compatHtml(c){return '<span class="compat c'+c+'">'+ic(c===3?'check':c===2?'star':'info',14)+'Совместимость: '+compatLabel(c)+'</span>';}
function st(id){return lib[id]&&lib[id].status;}
function ring(p,size){var r=38,C=2*Math.PI*r;return '<div class="ring"><svg viewBox="0 0 86 86" width="86" height="86"><circle cx="43" cy="43" r="'+r+'" fill="rgba(5,5,10,.6)" stroke="rgba(255,255,255,.14)" stroke-width="5"/><circle data-ring="" cx="43" cy="43" r="'+r+'" fill="none" stroke="var(--cyan)" stroke-width="5" stroke-linecap="round" stroke-dasharray="'+C.toFixed(1)+'" stroke-dashoffset="'+(C*(1-p/100)).toFixed(1)+'"/></svg><b data-pct="">'+Math.floor(p)+'%</b></div>';}

function actionBtn(g,size){
  var cls=size==='sm'?' btn-sm':'', s=st(g.id), L=lib[g.id];
  if(g.when){return '<button class="btn btn-ghost'+cls+(soonWatch[g.id]?' fav-on':'')+'" data-act="watch" data-id="'+g.id+'">'+ic(soonWatch[g.id]?'check':'bell2',16)+(soonWatch[g.id]?'Напомним':'Уведомить')+'</button>';}
  if(s==='installed')return '<button class="btn btn-primary'+cls+'" data-act="play" data-id="'+g.id+'">'+ic('play',16)+'Играть</button>';
  if(s==='downloading')return '<button class="btn btn-progress'+cls+'" data-act="pause" data-id="'+g.id+'" data-live="'+g.id+'" aria-label="Пауза загрузки"><i class="bar" data-bar="" style="width:'+L.progress+'%"></i><span>'+ic('pause',14)+' <b data-pct="">'+Math.floor(L.progress)+'%</b></span></button>';
  if(s==='paused')return '<button class="btn btn-ghost'+cls+'" data-act="resume" data-id="'+g.id+'">'+ic('play',14)+'Продолжить · '+Math.floor(L.progress)+'%</button>';
  if(s==='queued')return '<button class="btn btn-ghost'+cls+'" data-act="cancel" data-id="'+g.id+'">'+ic('clock',16)+'В очереди</button>';
  return '<button class="btn btn-ghost'+cls+'" data-act="get" data-id="'+g.id+'">'+ic('download',16)+'Скачать · '+fmtSize(g.size)+'</button>';
}
function storeCard(g){
  return '<article class="card" style="--glow:'+g.glow+'"><a class="cover" href="#game-'+g.id+'" style="background-image:'+art(g,300,420)+'" aria-label="'+esc(g.title)+'">'+
    '<span class="fmt">'+(g.fmt||'СКОРО')+'</span>'+(favs[g.id]?'<span class="chip-state" style="color:var(--hot)">'+ic('heartf',12)+'</span>':'')+
    '<span class="cover-title">'+g.title+'</span></a>'+
    '<div><a class="card-title" href="#game-'+g.id+'">'+g.title+'</a></div><div class="card-meta">'+g.genre+(g.size?' · '+fmtSize(g.size):' · '+g.when)+'</div>'+actionBtn(g,'sm')+'</article>';
}

/* ---------- views ---------- */
var heroIdx=0, heroTimer=null, catFilter='Все', catSort='new', catQuery='', libTab='all', setTab='general';
var FEATURED=['drift','orbit','rubezh'];

function vHome(){
  var g=byId[FEATURED[heroIdx]];
  var html='<section class="hero" style="--glow:'+g.glow+'"><div class="hero-art" style="background-image:'+art(g,1600,700,3)+'"></div>'+
   '<div class="hero-body"><span class="hero-tag">НОВОЕ В КАТАЛОГЕ</span><h2>'+g.title+'</h2><p>'+g.desc+'</p>'+
   '<div class="hero-meta"><span>'+g.genre+'</span><span>'+fmtSize(g.size)+'</span><span>'+g.players+' игр.</span><span>'+compatLabel(g.compat)+' работает</span></div>'+
   '<div class="hero-actions">'+actionBtn(g)+'<a class="btn btn-ghost" href="#game-'+g.id+'">Подробнее</a></div></div>'+
   '<div class="hero-dots">'+FEATURED.map(function(id,i){return '<button data-act="hero" data-i="'+i+'" class="'+(i===heroIdx?'on':'')+'" aria-label="Слайд '+(i+1)+'"></button>';}).join('')+'</div></section>';
  html+='<section><div class="chips">'+GENRES.map(function(x){return '<button class="chip" data-act="genre" data-g="'+x+'">'+x+'</button>';}).join('')+'</div></section>';
  html+='<section><div class="sec-head"><h2>Новые поступления</h2><a href="#catalog">Весь каталог →</a></div><div class="row-scroll">'+G.filter(function(x){return x.isNew;}).concat(G.slice(3,6)).map(storeCard).join('')+'</div></section>';
  html+='<section><div class="sec-head"><h2>Выбор NEXUS</h2><span class="card-meta" style="margin:0">Лучшая совместимость с эмулятором</span></div><div class="row-scroll">'+G.filter(function(x){return x.compat===3;}).map(storeCard).join('')+'</div></section>';
  html+='<section><div class="sec-head"><h2>Скоро в каталоге</h2></div><div class="soon-grid">'+SOON.map(function(s){
    return '<div class="soon-card" style="--glow:'+s.glow+'"><span class="cover" style="background-image:'+art(s,300,420)+'"><span class="cover-title">'+s.title+'</span></span><div style="display:flex;flex-direction:column;gap:6px;min-width:0;flex:1"><span class="eyebrow">'+s.when+' · '+s.genre+'</span><b style="font-size:16px">'+s.title+'</b><span class="card-meta" style="margin:0">'+s.desc+'</span><div>'+actionBtn(s,'sm')+'</div></div></div>';}).join('')+'</div></section>';
  html+='<section><div class="sec-head"><h2>Все игры</h2><a href="#catalog">Фильтры и сортировка →</a></div><div class="grid">'+G.map(storeCard).join('')+'</div></section>';
  return html;
}

function vCatalog(){
  var list=G.filter(function(g){return (catFilter==='Все'||g.genre===catFilter)&&(!catQuery||(g.title+' '+g.genre+' '+g.tags.join(' ')).toLowerCase().indexOf(catQuery.toLowerCase())>=0);});
  if(catSort==='name')list.sort(function(a,b){return a.title.localeCompare(b.title,'ru');});
  if(catSort==='size')list.sort(function(a,b){return a.size-b.size;});
  var h='<div class="page-head"><div><div class="eyebrow">'+list.length+' из '+G.length+' игр</div><h1 class="page">Каталог</h1></div>'+
   '<label style="display:flex;align-items:center;gap:8px;color:var(--muted);font-size:13px">Сортировка <select class="sel" id="catSort"><option value="new"'+(catSort==='new'?' selected':'')+'>Сначала новые</option><option value="name"'+(catSort==='name'?' selected':'')+'>По названию</option><option value="size"'+(catSort==='size'?' selected':'')+'>По размеру</option></select></label></div>';
  h+='<div class="chips">'+['Все'].concat(GENRES).map(function(x){return '<button class="chip'+(x===catFilter?' on':'')+'" data-act="cat" data-g="'+x+'">'+x+'</button>';}).join('')+
   (catQuery?'<button class="chip on" data-act="clearq">'+ic('search',14)+'«'+esc(catQuery)+'» '+ic('x',14)+'</button>':'')+'</div>';
  h+=list.length?'<div class="grid">'+list.map(storeCard).join('')+'</div>':'<div class="empty">'+ic('search',32)+'<h3>Ничего не нашлось</h3><span>Попробуй другой жанр или сбрось поиск.</span></div>';
  return h;
}

function libCard(g){
  var L=lib[g.id], s=L.status, p=L.progress, top='', mid='', sub='', actions='';
  if(s==='installed'){top='<span class="chip-state st-ok">'+ic('check',12)+'Установлена</span>';mid='<span class="play-over">'+ic('play',26)+'</span>';
    sub='<div class="lcard-sub"><span>'+ic('clock',12)+' '+fmtTime(L.play)+'</span><span>'+L.saves+' сохр.</span></div>';
    actions='<div class="lcard-row"><button class="btn btn-primary btn-sm" data-act="play" data-id="'+g.id+'">'+ic('play',14)+'Играть</button><button class="btn btn-ghost btn-sm btn-icon'+(favs[g.id]?' fav-on':'')+'" data-act="fav" data-id="'+g.id+'" aria-label="Избранное">'+ic(favs[g.id]?'heartf':'heart',16)+'</button></div>';}
  else if(s==='downloading'){top='<span class="chip-state st-dl">'+ic('download',12)+'Загрузка</span>';mid=ring(p);
    sub='<div class="lcard-sub"><span data-speed="">'+L.speed.toFixed(1).replace('.',',')+' МБ/с</span><span data-left="">'+fmtSize(g.size*(1-p/100))+' осталось</span></div>';
    actions='<div class="lcard-row"><button class="btn btn-ghost btn-sm" data-act="pause" data-id="'+g.id+'">'+ic('pause',14)+'Пауза</button><button class="btn btn-ghost btn-sm btn-icon" data-act="cancel" data-id="'+g.id+'" aria-label="Отменить загрузку">'+ic('x',16)+'</button></div>';}
  else if(s==='paused'){top='<span class="chip-state st-pause">'+ic('pause',11)+'Пауза</span>';mid=ring(p);
    sub='<div class="lcard-sub"><span>Приостановлено</span><span>'+fmtSize(g.size*(1-p/100))+' осталось</span></div>';
    actions='<div class="lcard-row"><button class="btn btn-primary btn-sm" data-act="resume" data-id="'+g.id+'">'+ic('play',14)+'Продолжить</button><button class="btn btn-ghost btn-sm btn-icon" data-act="cancel" data-id="'+g.id+'" aria-label="Отменить загрузку">'+ic('x',16)+'</button></div>';}
  else {top='<span class="chip-state st-q">'+ic('clock',12)+'В очереди</span>';mid=ring(0);
    sub='<div class="lcard-sub"><span>Начнётся следующей</span><span>'+fmtSize(g.size)+'</span></div>';
    actions='<div class="lcard-row"><button class="btn btn-ghost btn-sm" data-act="startnow" data-id="'+g.id+'">'+ic('bolt',14)+'Начать сейчас</button><button class="btn btn-ghost btn-sm btn-icon" data-act="cancel" data-id="'+g.id+'" aria-label="Убрать из очереди">'+ic('x',16)+'</button></div>';}
  var dl=s!=='installed';
  return '<article class="lcard" data-live="'+g.id+'" style="--glow:'+g.glow+'"><a class="lcover" href="#game-'+g.id+'" aria-label="'+esc(g.title)+'">'+
    (dl?'<span class="art base" style="background-image:'+art(g,300,420)+'"></span><span class="art" data-clip="" style="background-image:'+art(g,300,420)+';clip-path:inset('+(100-p)+'% 0 0 0)"></span><span class="scan" data-scan="" style="top:calc('+(100-p)+'% - 1px)"></span>'
       :'<span class="art" style="background-image:'+art(g,300,420)+'"></span>')+
    '<span class="shade"></span><span class="fmt">'+g.fmt+'</span>'+top+mid+'<span class="cover-title">'+g.title+'</span>'+
    '<span class="lmeta">'+(s==='installed'?ic('clock',12)+L.last:g.genre+' · '+fmtSize(g.size))+'</span></a>'+sub+actions+'</article>';
}
function vLibrary(){
  var ids=Object.keys(lib), inst=ids.filter(function(i){return lib[i].status==='installed';}), dls=ids.filter(function(i){return lib[i].status!=='installed';});
  var used=inst.reduce(function(a,i){return a+byId[i].size;},0), play=inst.reduce(function(a,i){return a+lib[i].play;},0);
  var list=libTab==='installed'?inst:libTab==='dl'?dls:libTab==='fav'?ids.filter(function(i){return favs[i];}):ids;
  var order={downloading:0,paused:1,queued:2,installed:3};
  list=list.slice().sort(function(a,b){return order[lib[a].status]-order[lib[b].status];});
  var last=byId['drift'];
  var h='<div class="page-head"><div><div class="eyebrow">Твоя коллекция</div><h1 class="page">Библиотека</h1></div>'+
    '<div class="stats"><div class="stat"><b>'+ids.length+'</b><span>игр в библиотеке</span></div><div class="stat"><b>'+fmtTime(play).split(' ').slice(0,2).join(' ')+'</b><span>наиграно всего</span></div><div class="stat"><b>'+fmtSize(used)+'</b><span>занято на устройстве</span></div></div></div>';
  if(st('drift')==='installed'&&libTab==='all'){
    h+='<section class="continue" style="--glow:'+last.glow+'"><div class="continue-art" style="background-image:'+art(last,1000,560,5)+'"></div><div class="continue-body"><span class="eyebrow">Продолжить с места остановки</span><h3>'+last.title+'</h3><p>Быстрое сохранение · слот 2 · '+lib.drift.last+'</p><p>Наиграно '+fmtTime(lib.drift.play)+'</p><div class="hero-actions"><button class="btn btn-primary" data-act="play" data-id="drift">'+ic('play',16)+'Продолжить</button><a class="btn btn-ghost" href="#game-drift">'+ic('save',16)+'Сохранения</a></div></div></section>';
  }
  var tabs=[['all','Все',ids.length],['installed','Установлены',inst.length],['dl','Загружаются',dls.length],['fav','Избранное',ids.filter(function(i){return favs[i];}).length]];
  h+='<div class="chips" role="tablist">'+tabs.map(function(t){return '<button role="tab" aria-selected="'+(libTab===t[0])+'" class="chip'+(libTab===t[0]?' on':'')+'" data-act="libtab" data-t="'+t[0]+'">'+t[1]+' <span class="mono" style="color:var(--muted)">'+t[2]+'</span></button>';}).join('')+'</div>';
  h+=list.length?'<div class="lgrid">'+list.map(function(i){return libCard(byId[i]);}).join('')+'</div>':'<div class="empty">'+ic('lib',32)+'<h3>Здесь пока пусто</h3><span>Скачай игру из каталога, и она появится в библиотеке.</span><a class="btn btn-primary" href="#home">Открыть каталог</a></div>';
  return h;
}

function vFavorites(){
  var list=Object.keys(favs).filter(function(i){return favs[i];}).map(function(i){return byId[i];});
  return '<div class="page-head"><div><div class="eyebrow">'+list.length+' игр</div><h1 class="page">Избранное</h1></div></div>'+
    (list.length?'<div class="grid">'+list.map(storeCard).join('')+'</div>':'<div class="empty">'+ic('heart',32)+'<h3>Пока ничего</h3><span>Нажми на сердечко на карточке игры, чтобы сохранить её здесь.</span></div>');
}

function dlRow(g){
  var L=lib[g.id], s=L.status, paused=s==='paused';
  var info=s==='downloading'?'<span data-pct2="">'+Math.floor(L.progress)+'%</span> · <span data-speed="">'+L.speed.toFixed(1).replace('.',',')+' МБ/с</span> · <span data-left="">'+fmtSize(g.size*(1-L.progress/100))+' осталось</span>':s==='paused'?Math.floor(L.progress)+'% · на паузе':'в очереди · '+fmtSize(g.size);
  var act=s==='downloading'?'<button class="btn btn-ghost btn-sm btn-icon" data-act="pause" data-id="'+g.id+'" aria-label="Пауза">'+ic('pause',16)+'</button>':
    s==='paused'?'<button class="btn btn-primary btn-sm btn-icon" data-act="resume" data-id="'+g.id+'" aria-label="Продолжить">'+ic('play',16)+'</button>':
    '<button class="btn btn-ghost btn-sm" data-act="startnow" data-id="'+g.id+'">Начать</button>';
  return '<div class="dl-row" data-live="'+g.id+'"><a class="cover" href="#game-'+g.id+'" style="background-image:'+art(g,300,420)+'" aria-label="'+esc(g.title)+'"></a><div style="min-width:0"><div class="dl-title">'+g.title+'<span>'+info+'</span></div><div class="track'+(paused||s==='queued'?' paused':'')+'"><i data-bar="" style="width:'+L.progress+'%"></i></div></div><div class="dl-actions">'+act+'<button class="btn btn-ghost btn-sm btn-icon" data-act="cancel" data-id="'+g.id+'" aria-label="Отменить">'+ic('x',16)+'</button></div></div>';
}
function vDownloads(){
  var ids=Object.keys(lib), act=ids.filter(function(i){return lib[i].status!=='installed';}), done=ids.filter(function(i){return lib[i].status==='installed';});
  var order={downloading:0,paused:1,queued:2};act.sort(function(a,b){return order[lib[a].status]-order[lib[b].status];});
  var used=done.reduce(function(a,i){return a+byId[i].size;},0), total=32768, other=14200;
  var h='<div class="page-head"><div><div class="eyebrow">'+(S.wifiOnly?'Только по Wi-Fi':'Wi-Fi и мобильная сеть')+' · по '+S.parallel+' одновременно</div><h1 class="page">Загрузки</h1></div><a class="btn btn-ghost btn-sm" href="#options" data-act="settab" data-t="downloads">'+ic('gear',16)+'Настройки загрузок</a></div>';
  h+='<section><div class="sec-head"><h2>Активные</h2><span class="card-meta" style="margin:0">'+act.length+'</span></div>'+(act.length?'<div class="dl-list">'+act.map(function(i){return dlRow(byId[i]);}).join('')+'</div>':'<div class="empty"><h3>Нет активных загрузок</h3><span>Новые загрузки появятся здесь.</span></div>')+'</section>';
  h+='<section><div class="sec-head"><h2>Завершённые</h2></div><div class="dl-list">'+done.map(function(i){var g=byId[i];return '<div class="dl-row"><a class="cover" href="#game-'+g.id+'" style="background-image:'+art(g,300,420)+'" aria-label="'+esc(g.title)+'"></a><div style="min-width:0"><div class="dl-title" style="margin:0">'+g.title+'<span class="st-ok">'+ic('check',12)+' Установлена · '+fmtSize(g.size)+'</span></div></div><div class="dl-actions"><button class="btn btn-primary btn-sm" data-act="play" data-id="'+g.id+'">'+ic('play',14)+'Играть</button></div></div>';}).join('')+'</div></section>';
  h+='<section class="storage"><div class="sec-head" style="margin:0"><h2>Память устройства</h2><span class="card-meta" style="margin:0">демо-значения</span></div><div class="track"><i style="width:'+((used+other)/total*100).toFixed(1)+'%"></i></div><div class="storage-legend"><span>Игры NEXUS: '+fmtSize(used)+'</span><span>Другие файлы: '+fmtSize(other)+'</span><span>Свободно: '+fmtSize(total-used-other)+'</span></div></section>';
  return h;
}

function vGame(id){
  var g=byId[id]; if(!g)return '<div class="empty"><h3>Игра не найдена</h3><a class="btn btn-ghost" href="#home">На главную</a></div>';
  if(g.when)return '<div class="g-hero" style="--glow:'+g.glow+'"><div class="g-hero-art" style="background-image:'+art(g,1600,600,2)+'"></div><div class="g-top"><a class="btn btn-ghost btn-sm btn-icon" href="#home" aria-label="Назад">'+ic('left',18)+'</a></div><div class="g-main"><span class="cover" style="background-image:'+art(g,300,420)+'"></span><div class="g-info"><span class="eyebrow">Скоро · '+g.when+'</span><h1>'+g.title+'</h1><p class="g-dev">'+g.desc+'</p><div class="g-actions">'+actionBtn(g)+'</div></div></div></div>';
  var L=lib[id], s=st(id), inst=s==='installed';
  var h='<div class="g-hero" style="--glow:'+g.glow+'"><div class="g-hero-art" style="background-image:'+art(g,1600,600,2)+'"></div>'+
   '<div class="g-top"><button class="btn btn-ghost btn-sm" data-act="back" aria-label="Назад">'+ic('left',16)+'Назад</button><button class="btn btn-ghost btn-sm btn-icon'+(favs[id]?' fav-on':'')+'" data-act="fav" data-id="'+id+'" aria-label="Избранное">'+ic(favs[id]?'heartf':'heart',18)+'</button></div>'+
   '<div class="g-main"><span class="cover" style="background-image:'+art(g,300,420)+'"><span class="fmt">'+g.fmt+'</span></span><div class="g-info"><span class="eyebrow">'+g.genre+' · '+g.year+'</span><h1>'+g.title+'</h1><span class="g-dev">'+g.dev+'</span>'+
   '<div class="tags">'+compatHtml(g.compat)+g.tags.map(function(t){return '<span class="tag">'+t+'</span>';}).join('')+'</div>'+
   '<div class="g-actions" data-live="'+id+'">'+actionBtn(g).replace('class="btn ','class="btn btn-main ')+(inst?'<button class="btn btn-ghost" data-act="remove" data-id="'+id+'">'+ic('trash',16)+'Удалить</button>':'')+(s&&!inst?'<button class="btn btn-ghost btn-icon" data-act="cancel" data-id="'+id+'" aria-label="Отменить загрузку">'+ic('x',18)+'</button>':'')+'</div></div></div></div>';
  h+='<div class="g-cols"><div style="display:flex;flex-direction:column;gap:22px;min-width:0">';
  h+='<section class="panel"><h2>Об игре</h2><p>'+g.desc+'</p><ul class="feat">'+g.feat.map(function(f){return '<li>'+ic('sparkle',16)+'<span>'+f+'</span></li>';}).join('')+'</ul></section>';
  h+='<section class="panel"><h2>Скриншоты</h2><div class="shots">'+[1,2,3,4].map(function(n){return '<div class="shot" style="background-image:'+art(g,480,272,n*7)+'"><span>480×272 · кадр '+n+'</span></div>';}).join('')+'</div></section>';
  if(inst){h+='<section class="panel"><h2>Твои сохранения</h2><div class="saves">'+
    [['Быстрое · слот 2',L.last],['Слот 1','3 дня назад'],['Автосохранение','неделю назад']].slice(0,Math.min(3,L.saves)).map(function(x,i){return '<div class="save"><span class="shot" style="background-image:'+art(g,480,272,30+i)+'"></span><div style="flex:1;min-width:0"><b>'+x[0]+'</b><span>'+x[1]+'</span></div><button class="btn btn-ghost btn-sm" data-act="play" data-id="'+id+'">Загрузить</button></div>';}).join('')+'</div></section>';}
  h+='<section class="panel"><h2>Что нового в версии '+g.ver+'</h2><ul class="changelog"><li><span class="mono">v'+g.ver+'</span><span>Исправлены вылеты при сохранении в некоторых главах, обновлена обложка.</span></li><li><span class="mono">v1.0</span><span>Игра добавлена в каталог NEXUS.</span></li></ul></section>';
  h+='</div><div style="display:flex;flex-direction:column;gap:22px;min-width:0">';
  h+='<section class="panel"><h2>Работа в NEXUS</h2>'+compatHtml(g.compat)+'<div class="meter">'+[1,2,3].map(function(n){return '<i class="'+(n<=g.compat?'on':'')+'"></i>';}).join('')+'</div><p style="font-size:13px">Рекомендуем: рендер ×'+S.res+', '+(S.backend==='vulkan'?'Vulkan':'OpenGL')+'. '+(g.compat<3?'Возможны редкие просадки кадров в насыщенных сценах.':'Игра проходится полностью без проблем.')+'</p><div style="margin-top:14px"><a class="btn btn-ghost btn-sm" href="#options" data-act="settab" data-t="graphics">'+ic('gear',14)+'Настройки для этой игры</a></div></section>';
  h+='<section class="panel"><h2>Детали</h2><dl class="specs">'+
   [['Размер',fmtSize(g.size)],['Формат',g.fmt],['Версия',g.ver],['Регион',g.region],['Языки',g.lang],['Игроки',g.players],['Жанр',g.genre],['Год',g.year],['ID диска',g.discId],['В каталоге',g.added]].map(function(r){return '<dt>'+r[0]+'</dt><dd>'+r[1]+'</dd>';}).join('')+'</dl></section>';
  h+='</div></div>';
  var sim=G.filter(function(x){return x.genre===g.genre&&x.id!==id;}).concat(G.filter(function(x){return x.compat===g.compat&&x.id!==id&&x.genre!==g.genre;})).slice(0,6);
  h+='<section><div class="sec-head"><h2>Похожие игры</h2></div><div class="row-scroll">'+sim.map(storeCard).join('')+'</div></section>';
  return h;
}

function sw(key){return '<button class="switch" role="switch" aria-checked="'+(!!S[key])+'" data-act="toggle" data-k="'+key+'" aria-label="Переключить"></button>';}
function seg(key,opts){return '<div class="seg">'+opts.map(function(o){return '<button class="'+(String(S[key])===String(o[0])?'on':'')+'" data-act="set" data-k="'+key+'" data-v="'+o[0]+'">'+o[1]+'</button>';}).join('')+'</div>';}
function row(t,sub,ctl){return '<div class="set-row"><div class="txt"><b>'+t+'</b>'+(sub?'<span>'+sub+'</span>':'')+'</div>'+ctl+'</div>';}
function rng_(key,min,max,unit){return '<div class="range"><input type="range" id="r-'+key+'" data-k="'+key+'" min="'+min+'" max="'+max+'" value="'+S[key]+'"><output id="o-'+key+'">'+S[key]+unit+'</output></div>';}
function vSettings(){
  var tabs=[['general','Общие','gear'],['downloads','Загрузки и память','download'],['graphics','Графика','monitor'],['audio','Звук','volume'],['controls','Управление','pad'],['notify','Уведомления','bell'],['about','О приложении','info']];
  var body='';
  if(setTab==='general'){body=row('Акцентный цвет','Цвет активных элементов и главных кнопок','<div class="swatches">'+Object.keys(ACCENTS).map(function(k){var a=ACCENTS[k];return '<button class="swatch'+(S.accent===k?' on':'')+'" style="background:'+a.neon+';--sw:'+a.neon+'" data-act="set" data-k="accent" data-v="'+k+'" aria-label="'+a.n+'"></button>';}).join('')+'</div>')+
    row('Язык интерфейса','','<select class="sel" id="sel-lang" data-k="lang"><option value="ru"'+(S.lang==='ru'?' selected':'')+'>Русский</option><option value="en"'+(S.lang==='en'?' selected':'')+'>English</option></select>')+
    row('Стартовый экран','Что открывать при запуске приложения',seg('start',[['home','Главная'],['library','Библиотека'],['last','Последняя игра']]));}
  if(setTab==='downloads'){body=row('Только по Wi-Fi','Не тратить мобильный трафик на игры',sw('wifiOnly'))+row('Одновременных загрузок','Больше — быстрее в сумме, но каждая медленнее',seg('parallel',[['1','1'],['2','2'],['3','3']]))+
    row('Устанавливать автоматически','Распаковывать и добавлять в библиотеку сразу после загрузки',sw('autoInstall'))+row('Папка с играми','Сюда сохраняются скачанные и твои ISO/CSO','<div style="display:flex;gap:8px;align-items:center;flex-wrap:wrap"><span class="path">/storage/emulated/0/NEXUS/games</span><button class="btn btn-ghost btn-sm" data-act="toast" data-msg="Здесь откроется выбор папки Android">'+ic('folder',16)+'Изменить</button></div>')+
    row('Очистить кэш обложек','Освободит место, обложки загрузятся заново','<button class="btn btn-ghost btn-sm" data-act="toast" data-msg="Кэш обложек очищен">Очистить · 46 МБ</button>');}
  if(setTab==='graphics'){body=row('Разрешение рендера','Значение по умолчанию для всех игр. ×1 = 480×272',seg('res',[['1','×1'],['2','×2'],['3','×3'],['4','×4']]))+row('Графический движок','Vulkan быстрее на большинстве новых телефонов',seg('backend',[['vulkan','Vulkan'],['gl','OpenGL']]))+
    row('Счётчик FPS','Показывать частоту кадров в углу во время игры',sw('fps'))+row('Пропуск кадров','Помогает на слабых устройствах, но картинка менее плавная',sw('frameskip'));}
  if(setTab==='audio'){body=row('Громкость игр','',rng_('vol',0,100,'%'))+row('Звуки интерфейса','Щелчки при навигации по меню',sw('haptic'));}
  if(setTab==='controls'){body=row('Экранный геймпад','Расположение, размер и прозрачность кнопок','<a class="btn btn-primary btn-sm" href="#gamepad">'+ic('pad',16)+'Настроить</a>')+row('Вибрация при нажатии','',sw('padHaptic'))+row('Скрывать экранные кнопки','Когда подключён физический геймпад',sw('padHide'));}
  if(setTab==='notify'){body=row('Загрузка завершена','',sw('nDl'))+row('Новые игры в каталоге','',sw('nNew'))+row('Обновления NEXUS','',sw('nUpd'));}
  if(setTab==='about'){body=row('Версия','NEXUS PSP 0.3.0 · сборка отладочная','<button class="btn btn-ghost btn-sm" data-act="toast" data-msg="У тебя последняя версия">Проверить обновления</button>')+row('Эмулятор','Ядро PPSSPP (GPL-2.0-or-later) — будет подключено','<span class="soon-label">ПЛАНИРУЕТСЯ</span>')+row('Лицензии и исходный код','Открытые компоненты и их лицензии','<button class="btn btn-ghost btn-sm" data-act="toast" data-msg="Здесь будет список лицензий">Открыть</button>');}
  return '<div class="page-head"><div><div class="eyebrow">Приложение NEXUS</div><h1 class="page">Настройки</h1></div></div><div class="set-wrap"><nav class="set-nav" aria-label="Разделы настроек">'+tabs.map(function(t){return '<button class="'+(setTab===t[0]?'on':'')+'" data-act="settab2" data-t="'+t[0]+'">'+ic(t[2],18)+t[1]+'</button>';}).join('')+'</nav><div class="set-group">'+body+'</div></div>';
}

function pb(st_,inner,label){return '<span class="pb" style="'+st_+'" aria-label="'+label+'">'+inner+'</span>';}
function vGamepad(){
  var g=byId.drift, compact=S.padLayout==='compact';
  var y=compact?'58%':'48%';
  var layer='<div class="pad-layer" id="padLayer" style="--pad-op:'+(S.padOp/100)+';--pad-sc:'+(S.padSc/100)+'">'+
   pb('left:3%;top:5%;width:15%;height:12%;border-radius:10px 26px 10px 10px;font-size:2.4cqw','L','L')+
   pb('right:3%;top:5%;width:15%;height:12%;border-radius:26px 10px 10px 10px;font-size:2.4cqw','R','R')+
   pb('left:'+(compact?'4%':'6%')+';top:'+y+';width:6.5%;aspect-ratio:1;border-radius:8px;margin-left:6.5%','','Вверх')+
   pb('left:'+(compact?'4%':'6%')+';top:calc('+y+' + 13cqw);width:6.5%;aspect-ratio:1;border-radius:8px;margin-left:6.5%','','Вниз')+
   pb('left:'+(compact?'4%':'6%')+';top:calc('+y+' + 6.5cqw);width:6.5%;aspect-ratio:1;border-radius:8px','','Влево')+
   pb('left:'+(compact?'4%':'6%')+';top:calc('+y+' + 6.5cqw);width:6.5%;aspect-ratio:1;border-radius:8px;margin-left:13%','','Вправо')+
   pb('left:'+(compact?'22%':'18%')+';top:'+(compact?'30%':'22%')+';width:10%;aspect-ratio:1;border-radius:50%','<span style="width:45%;height:45%;border-radius:50%;background:#fff"></span>','Стик')+
   pb('right:'+(compact?'10%':'12%')+';top:'+y+';width:7%;aspect-ratio:1;border-radius:50%;color:#45E3A0;border-color:#45E3A0;font-size:2.6cqw','△','Треугольник')+
   pb('right:'+(compact?'3%':'4.5%')+';top:calc('+y+' + 7cqw);width:7%;aspect-ratio:1;border-radius:50%;color:#FF5C8A;border-color:#FF5C8A;font-size:2.6cqw','○','Круг')+
   pb('right:'+(compact?'10%':'12%')+';top:calc('+y+' + 14cqw);width:7%;aspect-ratio:1;border-radius:50%;color:#7FA6FF;border-color:#7FA6FF;font-size:2.6cqw','✕','Крест')+
   pb('right:'+(compact?'17%':'19.5%')+';top:calc('+y+' + 7cqw);width:7%;aspect-ratio:1;border-radius:50%;color:#E8A0F0;border-color:#E8A0F0;font-size:2.6cqw','□','Квадрат')+
   pb('left:39%;bottom:5%;width:9%;height:7%;border-radius:20px;font-size:1.3cqw;letter-spacing:1px','SELECT','Select')+
   pb('left:52%;bottom:5%;width:9%;height:7%;border-radius:20px;font-size:1.3cqw;letter-spacing:1px','START','Start')+
   '</div>';
  var h='<div class="page-head"><div><div class="eyebrow">Управление</div><h1 class="page">Экранный геймпад</h1></div><div style="display:flex;gap:8px;flex-wrap:wrap"><button class="btn btn-ghost btn-sm" data-act="padreset">Сбросить</button><button class="btn btn-primary btn-sm" data-act="toast" data-msg="Раскладка сохранена">'+ic('check',16)+'Сохранить</button></div></div>';
  h+='<div class="pad-wrap"><div style="display:flex;flex-direction:column;gap:12px;min-width:0"><div class="pad-screen'+(S.padEdit?' edit':'')+'" style="background-image:'+art(g,1600,900,9)+'">'+(S.padEdit?'<span class="pad-hint">Режим перемещения: перетаскивай кнопки</span>':'')+layer+'</div>'+
    '<div style="display:flex;justify-content:space-between;gap:10px;flex-wrap:wrap;color:var(--muted);font-size:12px"><span>Предпросмотр поверх кадра игры</span><span>'+ic('phone',14)+' Альбомная ориентация</span></div></div>';
  h+='<div style="display:flex;flex-direction:column;gap:16px;min-width:0"><section class="panel"><h2>Раскладка</h2><div class="presets">'+[['classic','Классика'],['compact','Компакт'],['custom','Своя']].map(function(p){return '<button class="preset'+(S.padLayout===p[0]?' on':'')+'" data-act="set" data-k="padLayout" data-v="'+p[0]+'">'+ic(p[0]==='custom'?'sparkle':'pad',22)+p[1]+'</button>';}).join('')+'</div></section>';
  h+='<div class="set-group">'+row('Прозрачность','',rng_('padOp',15,100,'%'))+row('Размер кнопок','',rng_('padSc',70,130,'%'))+row('Перемещать кнопки','Включи и двигай кнопки пальцем',sw('padEdit'))+row('Вибрация','',sw('padHaptic'))+row('Скрывать с геймпадом','Когда подключён физический контроллер',sw('padHide'))+'</div>';
  h+='<section class="panel"><h2>Физический геймпад</h2><p style="font-size:13px;margin-bottom:12px">Не подключён. Подключи контроллер по Bluetooth или USB, и NEXUS сам предложит раскладку.</p><button class="btn btn-ghost btn-sm" data-act="toast" data-msg="Здесь будет назначение кнопок">'+ic('pad',16)+'Назначить кнопки</button></section></div></div>';
  return h;
}

function xpHtml(play,saves){var xp=play*2+saves*25+Object.keys(lib).length*50,lvl=Math.floor(xp/1000)+1,cur=xp%1000;
  return '<div class="xp"><div class="xp-top"><span>УРОВЕНЬ <b>'+lvl+'</b></span><span>'+cur+' / 1000 XP</span></div><div class="xp-bar"><i style="width:'+(cur/10)+'%"></i></div></div>';}
function vProfile(){
  var ids=Object.keys(lib), inst=ids.filter(function(i){return lib[i].status==='installed';});
  var play=inst.reduce(function(a,i){return a+lib[i].play;},0), saves=inst.reduce(function(a,i){return a+lib[i].saves;},0), maxPlay=Math.max.apply(null,inst.map(function(i){return lib[i].play;}).concat([0]));
  var genres={};inst.forEach(function(i){genres[byId[i].genre]=1;});
  var B=[['bolt','Первый запуск','Запусти любую игру',inst.length>0],['lib','Коллекционер','5 игр в библиотеке',ids.length>=5],['clock','Марафонец','10 часов в одной игре',maxPlay>=600],['save','Осторожный игрок','10 сохранений',saves>=10],['sparkle','Исследователь','Игры 5 разных жанров',Object.keys(genres).length>=5],['trophy','Легенда','100 часов в NEXUS',play>=6000]];
  var h='<div class="p-head"><span class="p-ava">N</span><div style="flex:1;min-width:0;display:flex;flex-direction:column;gap:6px"><span class="eyebrow">NEXUS ID</span><h1>Твой ник</h1><span class="p-id">NEXUS ID · профиль на этом устройстве</span>'+xpHtml(play,saves)+'</div><div style="display:flex;gap:8px;flex-wrap:wrap"><button class="btn btn-ghost btn-sm" data-act="toast" data-msg="Здесь будет смена ника и аватара">Изменить</button><button class="btn btn-primary btn-sm" data-act="toast" data-msg="Вход через аккаунт — в планах">'+ic('user',16)+'Войти и синхронизировать</button></div></div>';
  h+='<div class="tiles"><div class="tile">'+ic('clock',20,' style="color:var(--cyan)"')+'<b>'+fmtTime(play).split(' ').slice(0,2).join(' ')+'</b><span>наиграно всего</span></div><div class="tile">'+ic('lib',20,' style="color:var(--cyan)"')+'<b>'+ids.length+'</b><span>игр в библиотеке</span></div><div class="tile">'+ic('save',20,' style="color:var(--cyan)"')+'<b>'+saves+'</b><span>сохранений</span></div><div class="tile">'+ic('trophy',20,' style="color:var(--cyan)"')+'<b>'+B.filter(function(b){return b[3];}).length+' / '+B.length+'</b><span>достижений</span></div></div>';
  h+='<div class="p-cols"><section class="panel"><h2>Облачные сохранения <span class="soon-label">ПЛАНИРУЕТСЯ</span></h2><p style="font-size:13px;margin-bottom:14px">Сохранения копируются в облако. Сменишь телефон — продолжишь с того же места.</p><div class="set-group" style="background:var(--panel2)">'+row('Синхронизация','Последняя: только что',sw('cloud'))+row('Этот телефон','Основное устройство','<span class="st-ok" style="font-size:12px;font-weight:700">'+ic('check',12)+' Активно</span>')+'</div></section>';
  h+='<section class="panel"><h2>Недавняя активность</h2><div class="saves">'+inst.map(function(i){var g=byId[i];return '<a class="save" href="#game-'+g.id+'"><span class="cover" style="width:40px;border-radius:7px;background-image:'+art(g,300,420)+'"></span><div style="flex:1;min-width:0"><b>'+g.title+'</b><span>'+lib[i].last+' · '+fmtTime(lib[i].play)+'</span></div>'+ic('right',16)+'</a>';}).join('')+'</div></section></div>';
  h+='<section><div class="sec-head"><h2>Достижения NEXUS</h2><span class="card-meta" style="margin:0">Считаются по всем играм в приложении</span></div><div class="badges">'+B.map(function(b){return '<div class="badge-a'+(b[3]?'':' locked')+'"><span class="bi">'+ic(b[0],22)+'</span><b>'+b[1]+'</b><span>'+b[2]+'</span></div>';}).join('')+'</div></section>';
  return h;
}

/* ---------- routing ---------- */
var view=document.getElementById('viewInner'), viewBox=document.getElementById('view');
function route(){var h='';try{h=(location.hash||'').slice(1);}catch(e){}return h||'home';}
function render(keepScroll){
  var r=route(), base=r.indexOf('game-')===0?'game':r, html;
  if(base==='home')html=vHome();else if(base==='library')html=vLibrary();else if(base==='catalog')html=vCatalog();else if(base==='favorites')html=vFavorites();
  else if(base==='dl')html=vDownloads();else if(base==='options')html=vSettings();else if(base==='gamepad')html=vGamepad();else if(base==='profile')html=vProfile();
  else if(base==='game')html=vGame(r.slice(5));else html=vHome();
  var sc=viewBox.scrollTop; view.innerHTML=html; if(keepScroll)viewBox.scrollTop=sc;
  var navKey=base==='game'?'':base==='gamepad'?'':base;
  document.querySelectorAll('[data-nav]').forEach(function(a){var on=a.getAttribute('data-nav')===navKey;a.classList.toggle('active',on);if(on)a.setAttribute('aria-current','page');else a.removeAttribute('aria-current');});
  updateBadge();
}
window.addEventListener('hashchange',function(){viewBox.scrollTop=0;closePops();render();});

/* ---------- downloads engine ---------- */
function activeCount(){return Object.keys(lib).filter(function(i){return lib[i].status==='downloading';}).length;}
function pump(){
  var max=parseInt(S.parallel,10)||1;
  Object.keys(lib).forEach(function(i){if(lib[i].status==='queued'&&activeCount()<max){lib[i].status='downloading';lib[i].speed=9+Math.random()*5;}});
}
function updateBadge(){var n=Object.keys(lib).filter(function(i){return lib[i].status!=='installed';}).length,b=document.getElementById('dlBadge');b.textContent=n;b.hidden=n===0;}
function tick(){
  var changed=false;
  Object.keys(lib).forEach(function(id){var L=lib[id];if(L.status!=='downloading')return;
    L.speed=Math.max(4,Math.min(16,L.speed+(Math.random()-.5)*1.4));
    L.progress=Math.min(100,L.progress+L.speed/byId[id].size*100*1.6);
    if(L.progress>=100){L.status='installed';L.progress=100;L.play=0;L.last='Ещё не запускалась';L.saves=0;changed=true;
      notifs.unshift({ic:'download',color:'var(--cyan)',text:'«'+byId[id].title+'» установлена и готова к запуску',time:'Только что',unread:true});
      document.getElementById('bellDot').hidden=false;toast(ic('check',16)+'«'+byId[id].title+'» установлена');}
    else updateLive(id);});
  if(changed){pump();render(true);}
}
function updateLive(id){
  var L=lib[id], g=byId[id], C=2*Math.PI*38;
  document.querySelectorAll('[data-live="'+id+'"]').forEach(function(el){
    el.querySelectorAll('[data-pct]').forEach(function(n){n.textContent=Math.floor(L.progress)+'%';});
    el.querySelectorAll('[data-pct2]').forEach(function(n){n.textContent=Math.floor(L.progress)+'%';});
    el.querySelectorAll('[data-bar]').forEach(function(n){n.style.width=L.progress+'%';});
    el.querySelectorAll('[data-ring]').forEach(function(n){n.setAttribute('stroke-dashoffset',(C*(1-L.progress/100)).toFixed(1));});
    el.querySelectorAll('[data-clip]').forEach(function(n){n.style.clipPath='inset('+(100-L.progress)+'% 0 0 0)';});
    el.querySelectorAll('[data-scan]').forEach(function(n){n.style.top='calc('+(100-L.progress)+'% - 1px)';});
    el.querySelectorAll('[data-speed]').forEach(function(n){n.textContent=L.speed.toFixed(1).replace('.',',')+' МБ/с';});
    el.querySelectorAll('[data-left]').forEach(function(n){n.textContent=fmtSize(g.size*(1-L.progress/100))+(n.closest('.lcard')?' осталось':' осталось');});
  });
}
setInterval(tick,500);

/* ---------- toast ---------- */
var toastEl=document.getElementById('toast'),toastT;
function toast(html){toastEl.innerHTML=html;toastEl.classList.add('show');clearTimeout(toastT);toastT=setTimeout(function(){toastEl.classList.remove('show');},2400);}

/* ---------- actions ---------- */
document.addEventListener('click',function(e){
  var t=e.target.closest('[data-act]'); if(!t)return;
  var a=t.getAttribute('data-act'), id=t.getAttribute('data-id'), g=byId[id];
  if(a==='get'){lib[id]={status:'queued',progress:0};pump();toast(ic('download',16)+'«'+g.title+'» '+(lib[id].status==='downloading'?'загружается':'добавлена в очередь'));render(true);}
  else if(a==='pause'){e.preventDefault();lib[id].status='paused';pump();render(true);}
  else if(a==='resume'){lib[id].status='queued';pump();if(lib[id].status!=='downloading'){Object.keys(lib).forEach(function(i){if(i!==id&&lib[i].status==='downloading')lib[i].status='queued';});lib[id].status='downloading';lib[id].speed=10;}render(true);}
  else if(a==='startnow'){Object.keys(lib).forEach(function(i){if(lib[i].status==='downloading')lib[i].status='queued';});lib[id].status='downloading';lib[id].speed=10;render(true);}
  else if(a==='cancel'){delete lib[id];pump();toast('Загрузка «'+g.title+'» отменена');render(true);}
  else if(a==='remove'){delete lib[id];toast(ic('trash',16)+'«'+g.title+'» удалена с устройства');render(true);}
  else if(a==='play'){toast(ic('play',16)+'Запуск «'+g.title+'»… Эмулятор будет подключён позже');}
  else if(a==='fav'){e.preventDefault();favs[id]=!favs[id];toast(favs[id]?ic('heartf',16,' style="color:var(--hot)"')+'Добавлено в избранное':'Убрано из избранного');render(true);}
  else if(a==='watch'){soonWatch[id]=!soonWatch[id];toast(soonWatch[id]?'Пришлём уведомление о выходе «'+g.title+'»':'Напоминание отключено');render(true);}
  else if(a==='hero'){heroIdx=+t.getAttribute('data-i');render(true);restartHero();}
  else if(a==='genre'){catFilter=t.getAttribute('data-g');catQuery='';location.hash='catalog';}
  else if(a==='cat'){catFilter=t.getAttribute('data-g');render(true);}
  else if(a==='clearq'){catQuery='';document.getElementById('q').value='';render(true);}
  else if(a==='libtab'){libTab=t.getAttribute('data-t');render(true);}
  else if(a==='settab'){setTab=t.getAttribute('data-t');}
  else if(a==='settab2'){setTab=t.getAttribute('data-t');render(true);}
  else if(a==='toggle'){var k=t.getAttribute('data-k');S[k]=!S[k];saveS();render(true);}
  else if(a==='set'){var k2=t.getAttribute('data-k');S[k2]=t.getAttribute('data-v');saveS();if(k2==='accent')applyAccent();if(k2==='parallel')pump();render(true);}
  else if(a==='padreset'){['padOp','padSc','padLayout','padEdit'].forEach(function(k){S[k]=defaults[k];});saveS();render(true);toast('Раскладка сброшена');}
  else if(a==='toast'){toast(t.getAttribute('data-msg'));}
  else if(a==='back'){if(history.length>1)history.back();else location.hash='home';}
});
document.addEventListener('input',function(e){
  var t=e.target; if(t.type==='range'&&t.dataset.k){var k=t.dataset.k;S[k]=+t.value;var o=document.getElementById('o-'+k);if(o)o.textContent=t.value+'%';
    var pl=document.getElementById('padLayer');if(pl){pl.style.setProperty('--pad-op',S.padOp/100);pl.style.setProperty('--pad-sc',S.padSc/100);}saveS();}
});
document.addEventListener('change',function(e){
  var t=e.target; if(t.id==='catSort'){catSort=t.value;render(true);} else if(t.tagName==='SELECT'&&t.dataset.k){S[t.dataset.k]=t.value;saveS();toast('Сохранено');}
});

/* ---------- search ---------- */
var q=document.getElementById('q'), pop=document.getElementById('pop');
q.addEventListener('input',function(){
  var v=q.value.trim().toLowerCase(); if(!v){pop.hidden=true;return;}
  var res=G.concat(SOON).filter(function(g){return (g.title+' '+g.genre+' '+(g.tags||[]).join(' ')).toLowerCase().indexOf(v)>=0;}).slice(0,6);
  pop.innerHTML=res.length?res.map(function(g){return '<a class="pop-row" href="#game-'+g.id+'"><span class="pop-thumb" style="background-image:'+art(g,300,420)+'"></span><span style="display:flex;flex-direction:column;gap:2px;min-width:0"><b>'+g.title+'</b><span class="card-meta" style="margin:0">'+g.genre+(st(g.id)==='installed'?' · установлена':'')+'</span></span></a>';}).join('')+'<a class="pop-row" href="#catalog" data-act="allres" style="color:var(--neon);font-weight:600;justify-content:center">Все результаты в каталоге →</a>'
    :'<div class="pop-empty">По запросу «'+esc(q.value)+'» ничего нет</div>';
  pop.hidden=false;
});
q.addEventListener('keydown',function(e){if(e.key==='Enter'){catQuery=q.value.trim();catFilter='Все';pop.hidden=true;if(route()==='catalog')render();else location.hash='catalog';}if(e.key==='Escape'){pop.hidden=true;q.blur();}});
pop.addEventListener('click',function(e){var a=e.target.closest('[data-act="allres"]');if(a){catQuery=q.value.trim();catFilter='Все';}pop.hidden=true;});
document.addEventListener('keydown',function(e){if(e.key==='/'&&document.activeElement!==q&&!/INPUT|SELECT|TEXTAREA/.test(document.activeElement.tagName)){e.preventDefault();q.focus();}});

/* ---------- notifications ---------- */
var bell=document.getElementById('bellBtn'), notif=document.getElementById('notif');
function drawNotif(){notif.innerHTML='<div class="notif-head"><h3>Уведомления</h3><button class="link-btn" id="readAll">Прочитать все</button></div>'+notifs.map(function(n){return '<div class="notif-row'+(n.unread?' unread':'')+'"><span class="notif-ic" style="color:'+n.color+'">'+ic(n.ic,18)+'</span><div><p>'+n.text+'</p><span>'+n.time+'</span></div></div>';}).join('');}
bell.addEventListener('click',function(e){e.stopPropagation();var open=notif.hidden;closePops();if(open){drawNotif();notif.hidden=false;bell.setAttribute('aria-expanded','true');}});
notif.addEventListener('click',function(e){e.stopPropagation();if(e.target.id==='readAll'){notifs.forEach(function(n){n.unread=false;});document.getElementById('bellDot').hidden=true;drawNotif();}});
function closePops(){notif.hidden=true;bell.setAttribute('aria-expanded','false');pop.hidden=true;}
document.addEventListener('click',function(e){if(!e.target.closest('.search')&&!e.target.closest('#notif')&&!e.target.closest('#bellBtn')){notif.hidden=true;bell.setAttribute('aria-expanded','false');pop.hidden=true;}});

/* ---------- rail ---------- */
var rail=document.getElementById('rail'), rt=document.getElementById('railToggle');
function drawRail(){rt.innerHTML=ic(rail.classList.contains('collapsed')?'right':'left',20);rt.setAttribute('aria-label',rail.classList.contains('collapsed')?'Развернуть меню':'Свернуть меню');}
try{if(localStorage.getItem('nexus-proto-rail')==='1')rail.classList.add('collapsed');}catch(e){}
rt.addEventListener('click',function(){rail.classList.toggle('collapsed');try{localStorage.setItem('nexus-proto-rail',rail.classList.contains('collapsed')?'1':'0');}catch(e){}drawRail();});
document.querySelectorAll('[data-ic]').forEach(function(el){el.outerHTML=ic(el.getAttribute('data-ic'),el.closest('.nav-item')||el.closest('.rail-fs')?20:20);});
drawRail();

/* ---------- hero autoplay ---------- */
var reduce=false;try{reduce=window.matchMedia('(prefers-reduced-motion: reduce)').matches;}catch(e){}
function restartHero(){clearInterval(heroTimer);if(reduce)return;heroTimer=setInterval(function(){if(route()!=='home'||document.hidden)return;if(view.querySelector('.hero:hover'))return;heroIdx=(heroIdx+1)%FEATURED.length;
  var heroEl=view.querySelector('.hero');if(!heroEl)return;var tmp=document.createElement('div');tmp.innerHTML=vHome();heroEl.replaceWith(tmp.querySelector('.hero'));},7000);}


/* ---------- fullscreen + orientation ---------- */
var fsBtn=document.getElementById('fsBtn');
fsBtn.addEventListener('click',function(){
  var d=document, el=d.documentElement;
  if(d.fullscreenElement){d.exitFullscreen&&d.exitFullscreen();return;}
  if(!el.requestFullscreen){toast('Браузер не поддерживает полноэкранный режим');return;}
  el.requestFullscreen({navigationUI:'hide'}).then(function(){
    if(screen.orientation&&screen.orientation.lock)screen.orientation.lock('landscape').catch(function(){});
  }).catch(function(){toast('Не удалось включить полноэкранный режим');});
});
document.addEventListener('fullscreenchange',function(){var on=!!document.fullscreenElement;fsBtn.setAttribute('aria-label',on?'Выйти из полноэкранного режима':'На весь экран');fsBtn.lastElementChild.textContent=on?'Свернуть':'На весь экран';});
document.getElementById('rotateSkip').addEventListener('click',function(){document.getElementById('rotate').classList.add('dismissed');});

(function(){var items=G.slice(0,6).map(function(g){return '<span><em>▲</em> НОВИНКА · <b>'+g.title.toUpperCase()+'</b> · '+g.genre+'</span>';});
  items.push('<span><em>●</em> NEXUS 0.3.1 · улучшена совместимость</span>','<span><em>■</em> СКОРО · <b>ЗВЁЗДНЫЙ КУРЬЕР</b> · декабрь</span>');
  var t=items.join('');document.getElementById('tickerTrack').innerHTML=t+t;})();
applyAccent(); pump(); render(); restartHero();
})();
