#!/usr/bin/env python3
"""
Сборка сайта KAPSULA v3 (GitVerse Pages) — 13 языков, отдельные страницы.
  site/pages/*.html   — шаблоны страниц ({{ключ}} — текст, {{{ключ}}} — HTML, {{L}} — префикс языка, {{on:стр}} — пункт меню)
  site/pages/_layout.html, _foot.html — общий каркас и подвал
  site/i18n/<язык>.json — тексты (ru — основа; чего нет в языке — берётся из en, потом из ru)
  site/assets/        — стили и скрипты (app.js — общее, eggs.js — секреты)
  site/tools/*.js, site/game.js — скрипты «Полезного» и мини-игры
Общие с приложением: маскот (ui/img/mascot-l-*.webp), консоли (Catalog.kt, ui/img/con-*), BIOS PS1 (Bios.kt).
Русский — в корне (/), остальные — /en/, /ja/ … (для поисковиков — hreflang).

  python3 tools/site_build.py [папка_вывода]               # по умолчанию build/site
  python3 tools/site_build.py --shots app/build/site-shots # снимки SiteShotsTest → site/screens/*.webp
Переменные: SITE_URL (по умолчанию https://kapsula.gitverse.site/), NO_NET=1 — без запроса версии, APK_FILE — размер APK.
Нужно: pip install pillow
"""
import html
import json
import os
import re
import shutil
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SITE = ROOT / "site"
GH_API = "https://api.github.com/repos/vitalya2078-cloud/nexus-psp/releases/tags/latest"
GH_APK = "https://github.com/vitalya2078-cloud/nexus-psp/releases/download/latest/kapsula.apk"
SITE_URL = os.environ.get("SITE_URL", "https://kapsula.gitverse.site/")

F_RU = '<svg class="flag" viewBox="0 0 3 2"><rect width="3" height="2" fill="#fff"/><rect y=".667" width="3" height=".667" fill="#0039a6"/><rect y="1.333" width="3" height=".667" fill="#d52b1e"/></svg>'
LANGS = [  # код, название на своём языке, флаг
    ("ru", "Русский", F_RU),
    ("en", "English", '<svg class="flag" viewBox="0 0 60 40"><rect width="60" height="40" fill="#012169"/><path d="M0 0L60 40M60 0L0 40" stroke="#fff" stroke-width="8"/><path d="M0 0L60 40M60 0L0 40" stroke="#c8102e" stroke-width="3"/><path d="M30 0v40M0 20h60" stroke="#fff" stroke-width="12"/><path d="M30 0v40M0 20h60" stroke="#c8102e" stroke-width="7"/></svg>'),
    ("es", "Español", '<svg class="flag" viewBox="0 0 3 2"><rect width="3" height="2" fill="#c60b1e"/><rect y=".5" width="3" height="1" fill="#ffc400"/></svg>'),
    ("pt", "Português", '<svg class="flag" viewBox="0 0 30 20"><rect width="30" height="20" fill="#009c3b"/><path d="M15 2.5 27 10 15 17.5 3 10z" fill="#ffdf00"/><circle cx="15" cy="10" r="4.6" fill="#002776"/></svg>'),
    ("de", "Deutsch", '<svg class="flag" viewBox="0 0 5 3"><rect width="5" height="3" fill="#ffce00"/><rect width="5" height="2" fill="#dd0000"/><rect width="5" height="1" fill="#000"/></svg>'),
    ("fr", "Français", '<svg class="flag" viewBox="0 0 3 2"><rect width="3" height="2" fill="#ef4135"/><rect width="2" height="2" fill="#fff"/><rect width="1" height="2" fill="#0055a4"/></svg>'),
    ("it", "Italiano", '<svg class="flag" viewBox="0 0 3 2"><rect width="3" height="2" fill="#ce2b37"/><rect width="2" height="2" fill="#fff"/><rect width="1" height="2" fill="#009246"/></svg>'),
    ("pl", "Polski", '<svg class="flag" viewBox="0 0 8 5"><rect width="8" height="5" fill="#dc143c"/><rect width="8" height="2.5" fill="#fff"/></svg>'),
    ("tr", "Türkçe", '<svg class="flag" viewBox="0 0 30 20"><rect width="30" height="20" fill="#e30a17"/><circle cx="11" cy="10" r="5" fill="#fff"/><circle cx="12.3" cy="10" r="4" fill="#e30a17"/><path d="M17.5 10l-2.6.9 1.6-2.2v2.6l-1.6-2.2z" fill="#fff"/></svg>'),
    ("uk", "Українська", '<svg class="flag" viewBox="0 0 3 2"><rect width="3" height="2" fill="#ffd700"/><rect width="3" height="1" fill="#0057b7"/></svg>'),
    ("ja", "日本語", '<svg class="flag" viewBox="0 0 3 2"><rect width="3" height="2" fill="#fff"/><circle cx="1.5" cy="1" r=".6" fill="#bc002d"/></svg>'),
    ("zh", "中文", '<svg class="flag" viewBox="0 0 30 20"><rect width="30" height="20" fill="#de2910"/><path d="M5 3l1.2 3.6H10L7 8.8l1.1 3.6L5 10.2 1.9 12.4 3 8.8 0 6.6h3.8z" fill="#ffde00"/></svg>'),
    ("ko", "한국어", '<svg class="flag" viewBox="0 0 30 20"><rect width="30" height="20" fill="#fff"/><circle cx="15" cy="10" r="5" fill="#cd2e3a"/><path d="M10 10a5 5 0 0 0 10 0 2.5 2.5 0 0 0-5 0 2.5 2.5 0 0 1-5 0" fill="#0047a0"/><g stroke="#000" stroke-width="1.2"><path d="M4 4l3 2M5 3l3 2M23 15l3 2M22 16l3 2M23 5l3-2M22 4l3-2M4 16l3 2M5 17l3 2"/></g></svg>'),
]
# страница: заголовок (ключ), скрипты, класс обёртки, класс body, подвал
PAGES = {
    "index": ("home.title", [], "home", "is-home", False),
    "consoles": ("con.h1", [], "", "", True),
    "tools": ("tools.h1", [], "", "", True),
    "faq": ("faq.h1", [], "", "", True),
    "news": ("news.h1", [], "", "", True),
    "secrets": ("sec.h1", ["/assets/secrets.js"], "", "", True),
    "privacy": ("priv.title", [], "", "", True),
    "gift": ("gift.h1", ["/assets/gift.js"], "", "", True),
    "game": ("game.h1", ["/game.js"], "", "", True),
    "check": ("check.h1", ["/data.js", "/check.js"], "", "", True),
    "pad": ("pad.h1", ["/pad.js"], "", "", True),
    "phone": ("phone.h1", ["/data.js", "/phone.js"], "", "", True),
    "quiz": ("quiz.h1", ["/data.js", "/quiz.js"], "", "", True),
    "shelf": ("shelf.h1", ["/data.js", "/shelf.js"], "", "", True),
    "404": ("lost.h1", [], "", "is-404", True),
}
# ключи, которые нужны скриптам в браузере (остальное — только в разметке)
JS_KEYS = ("js.", "egg.", "cheat.", "u.", "ck.", "ph.", "qz.", "sh.", "pd.", "gm.", "get.", "sec.", "gift.retry", "game.h1", "nav.", "lang")


def shots(src: Path):
    from PIL import Image
    dst = SITE / "screens"; dst.mkdir(exist_ok=True)
    for f in sorted(src.glob("*.png")):
        Image.open(f).convert("RGB").save(dst / (f.stem + ".webp"), "WEBP", quality=86, method=6)
        print("снимок", f.stem)


def platforms():
    """Консоли из Catalog.kt: id, короткое имя, полное, цвет, BIOS, картинка, тестовая ли, расширения."""
    src = (ROOT / "app/src/main/java/com/nexus/psp/data/Catalog.kt").read_text(encoding="utf-8")
    out = []
    for m in re.finditer(r'Platform\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*Color\(0xFF([0-9A-Fa-f]{6})\),\s*(\d),\s*"[^"]*",\s*"[^"]*",\s*"([^"]+)"([^\n]*)', src):
        pid, short, full, col, bios, img, rest = m.groups()
        e = re.search(r'exts = listOf\(([^)]*)\)', rest)
        out.append(dict(id=pid, short=short, full=full, color="#" + col.lower(), bios=int(bios), img=img, exp="exp = true" in rest,
                        exts=re.findall(r'"([^"]+)"', e.group(1)) if e else [], webp="/img/con/" + img.rsplit(".", 1)[0] + ".webp"))
    if len(out) < 10: sys.exit(f"site: в Catalog.kt найдено только {len(out)} консолей — разбор сломался")
    return out


def con_images(pl, out: Path):
    from PIL import Image
    (out / "img/con").mkdir(parents=True, exist_ok=True)
    for p in pl:
        im = Image.open(ROOT / "ui/img" / p["img"]).convert("RGBA"); im.thumbnail((360, 240))
        im.save(out / p["webp"].lstrip("/"), "WEBP", quality=88, method=6)


def data_js(pl, out: Path):
    """data.js — общие с приложением данные для «Полезного»: консоли и известные BIOS PS1 (Bios.kt)."""
    plats = [{"id": p["id"], "short": p["short"], "full": p["full"], "color": p["color"], "bios": p["bios"], "exp": p["exp"], "img": p["webp"], "exts": p["exts"]} for p in pl]
    bios = (ROOT / "app/src/main/java/com/nexus/psp/emulator/Bios.kt").read_text(encoding="utf-8")
    ps1 = {md5: label for md5, label in re.findall(r'"([0-9a-f]{32})" to \("[^"]+" to "([^"]+)"\)', bios)}
    if len(ps1) < 5: sys.exit("site: таблица BIOS PS1 в Bios.kt не разобралась")
    (out / "data.js").write_text("window.KAPSULA_DATA = " + json.dumps({"platforms": plats, "ps1bios": ps1}, ensure_ascii=False) + ";\n", encoding="utf-8")


def mascot(out: Path):
    """Позы приложения (ui/img/mascot-l-*) и сайта (site/art/mascot) — на один холст: ноги на одной линии, смена позы не дёргает."""
    from PIL import Image
    d = out / "img/mascot"; d.mkdir(parents=True, exist_ok=True)
    src = {f.name.removeprefix("mascot-l-").removesuffix(".webp"): f for f in (ROOT / "ui/img").glob("mascot-l-*.webp")}
    src.update({f.stem: f for f in (SITE / "art/mascot").glob("*.webp")})
    ims = {n: Image.open(f).convert("RGBA") for n, f in src.items()}
    W = max(i.width for i in ims.values()); H = max(i.height for i in ims.values())
    for n, im in ims.items():
        cv = Image.new("RGBA", (W, H), (0, 0, 0, 0)); cv.paste(im, ((W - im.width) // 2, H - im.height), im)
        cv.save(d / f"{n}.webp", "WEBP", quality=88, method=6)
    return W, H


MONTHS_RU = ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"]


def changes_html(lang):
    out = []
    for r in json.loads((SITE / "changes.json").read_text(encoding="utf-8")):
        y, m, d = r["date"].split("-")
        when = f"{int(d)} {MONTHS_RU[int(m) - 1]} {y}" if lang == "ru" else f"{y}-{m}-{d}"
        items = "".join(f"<li>{html.escape(i)}</li>" for i in r["items"])
        out.append(f'<article class="rel"><b>{html.escape(r["ver"])}</b><time datetime="{r["date"]}">{when}</time><ul>{items}</ul></article>')
    return "\n    ".join(out)


LOGO = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48"><rect width="48" height="48" rx="13" fill="#2a2338"/>
<rect x="7" y="14.5" width="34" height="19" rx="9.5" fill="none" stroke="#fbf0e4" stroke-width="2.6"/>
<path d="M24 14.5v19" stroke="#fbf0e4" stroke-width="2.2"/><path d="M13.5 19.5v9l7-4.5z" fill="#f4b26b"/>
<path d="M27.5 20h7M27.5 23h9M27.5 26h9M27.5 29h7" stroke="#fbf0e4" stroke-width="1.6" stroke-linecap="round"/></svg>"""


def latest(out: Path):
    """latest.json — версия и ссылка на APK. Качаем с GitHub: он докачивает после обрыва (решение пользователя 2026-11-27)."""
    ver = None; mb = 0
    apk = os.environ.get("APK_FILE")
    if apk and os.path.exists(apk): mb = round(os.path.getsize(apk) / 1048576)
    if not os.environ.get("NO_NET"):
        try:
            rel = json.load(urllib.request.urlopen(urllib.request.Request(GH_API, headers={"Accept": "application/json", "User-Agent": "KAPSULA-site"}), timeout=30))
            ver = re.search(r"\d+\.\d+(?:\.\d+)?", rel.get("name", "")).group(0)
            a = next((x for x in rel.get("assets", []) if x["name"] == "kapsula.apk"), None)
            if a and not mb: mb = round(a["size"] / 1048576)
        except Exception as e:
            print(f"::warning::выпуск GitHub не прочитался: {e}")
    if apk or not ver:
        ver = re.search(r'versionName = "([^"]+)"', (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")).group(1)
    (out / "latest.json").write_text(json.dumps({"ver": ver, "url": GH_APK, "mb": mb}), encoding="utf-8")
    print(f"версия {ver}, APK — с GitHub ({mb} МБ)")


def load_texts():
    base = json.loads((SITE / "i18n/ru.json").read_text(encoding="utf-8"))
    en = json.loads((SITE / "i18n/en.json").read_text(encoding="utf-8"))
    texts, missing = {}, {}
    for code, _, _ in LANGS:
        own = json.loads((SITE / f"i18n/{code}.json").read_text(encoding="utf-8"))
        extra = set(own) - set(base)
        if extra: sys.exit(f"site: в {code}.json лишние ключи (нет в ru): {sorted(extra)[:8]}")
        missing[code] = [k for k in base if k not in own]
        texts[code] = {**base, **(en if code != "ru" else {}), **own}
    return texts, missing


def render(tpl, T, ctx, page):
    tpl = re.sub(r"\{\{on:([\w-]+)\}\}", lambda m: "on" if m.group(1) == page else "", tpl)
    def val(k):
        if k in ctx: return ctx[k]
        if k not in T: raise KeyError(f"нет текста «{k}» (страница {page})")
        return T[k]
    tpl = re.sub(r"\{\{\{([\w.-]+)\}\}\}", lambda m: str(val(m.group(1))), tpl)
    return re.sub(r"\{\{([\w.-]+)\}\}", lambda m: html.escape(str(val(m.group(1)))), tpl)


def build(out: Path):
    if out.exists(): shutil.rmtree(out)
    out.mkdir(parents=True)
    shutil.copytree(SITE / "assets", out / "assets")
    shutil.copytree(SITE / "fonts", out / "fonts")
    shutil.copytree(SITE / "art/items", out / "img/items")
    (out / "img/screens").mkdir()
    for f in (SITE / "screens").glob("*.webp"): shutil.copy(f, out / "img/screens" / f.name)
    for f in list((SITE / "tools").glob("*.js")) + [SITE / "game.js"]: shutil.copy(f, out / f.name)
    pl = platforms(); con_images(pl, out); data_js(pl, out)
    (out / "img/logo.svg").write_text(LOGO, encoding="utf-8")
    from PIL import Image
    if (SITE / "screens/1-home.webp").exists():
        Image.open(SITE / "screens/1-home.webp").convert("RGB").resize((1200, 554)).save(out / "img/og.jpg", quality=85)
    mw, mh = mascot(out); latest(out)
    v = str(int(max(f.stat().st_mtime for f in (SITE / "assets").glob("*"))))
    texts, missing = load_texts()
    layout = (SITE / "pages/_layout.html").read_text(encoding="utf-8")
    foot = (SITE / "pages/_foot.html").read_text(encoding="utf-8")
    ribbon = "".join(f'<a href="{{{{L}}}}consoles.html#{p["id"]}" data-id="{p["id"]}" title="{html.escape(p["full"])}"><img src="{p["webp"]}" alt="{html.escape(p["short"])}" width="68" height="46"></a>' for p in pl)
    (out / "i18n").mkdir()
    for code, name, flag in LANGS:
        T = texts[code]; L = "/" if code == "ru" else f"/{code}/"
        dst = out if code == "ru" else out / code; dst.mkdir(exist_ok=True)
        js = {k: v2 for k, v2 in T.items() if k.startswith(JS_KEYS)}
        (out / f"i18n/{code}.js").write_text(
            "window.T=" + json.dumps(js, ensure_ascii=False) + ";window.LANG=" + json.dumps(code) + ";window.L=" + json.dumps(L) +
            ";window.KLANGS=" + json.dumps([[c, n] for c, n, _ in LANGS], ensure_ascii=False) + ";\n", encoding="utf-8")
        cons = "\n      ".join(
            f'<div class="con" id="{p["id"]}" style="--c:{p["color"]}"><img src="{p["webp"]}" alt="" loading="lazy" width="116" height="77"><b>{html.escape(p["full"])}</b>'
            + ("<small>" + " · ".join(([f'<span class="tag">{html.escape(T["con.exp"])}</span>'] if p["exp"] else []) + ([html.escape(T["con.bios"])] if p["bios"] == 2 else [])) + "</small>" if p["exp"] or p["bios"] == 2 else "")
            + "</div>" for p in pl)
        for page, (tkey, scripts, pcls, bcls, has_foot) in PAGES.items():
            if page == "404" and code != "ru": continue
            fname = "index.html" if page == "index" else f"{page}.html"
            path = "" if page in ("index", "404") else fname
            langs = "".join(f'<a href="{"/" if c == "ru" else "/" + c + "/"}{path}" data-l="{c}" hreflang="{c}" lang="{c}"{" class=\"on\"" if c == code else ""}>{f}{n}</a>' for c, n, f in LANGS)
            hreflang = "\n".join([f'<link rel="alternate" hreflang="{c}" href="{SITE_URL}{"" if c == "ru" else c + "/"}{path}">' for c, _, _ in LANGS]
                                 + [f'<link rel="alternate" hreflang="x-default" href="{SITE_URL}{path}">', f'<link rel="canonical" href="{SITE_URL}{"" if code == "ru" else code + "/"}{path}">']) if page != "404" else ""
            ctx = {"L": L, "LANG": code, "LANGNAME": name, "FLAG": flag, "LANGS": langs, "HREFLANG": hreflang, "MW": mw, "MH": mh, "V": v, "SITE": SITE_URL,
                   "RIBBON": render(ribbon, T, {"L": L}, page), "CONSOLES": cons, "CHANGES": changes_html(code),
                   "NEWSNOTE": "" if code == "ru" else f'<p class="hint">{html.escape(T["news.ru"])}</p>',
                   "PRIVNOTE": "" if code in ("ru", "en") else f'<p class="hint">{html.escape(T["priv.en"])}</p>',
                   "BODY": bcls, "PAGECLASS": pcls,
                   "SCRIPTS": "\n".join(f'<script src="{s}?v={v}" defer></script>' for s in scripts)}
            ctx["TITLE"] = T[tkey] if page == "index" else f"{T[tkey]} — KAPSULA"
            ctx["DESC"] = T.get(f"{page}.desc", T["home.desc"])
            ctx["CONTENT"] = render((SITE / f"pages/{page}.html").read_text(encoding="utf-8"), T, ctx, page)
            ctx["FOOT"] = render(foot, T, ctx, page) if has_foot else ""
            (dst / fname).write_text(render(layout, T, ctx, page), encoding="utf-8")
        if code != "ru" and missing[code]: print(f"  {code}: без перевода {len(missing[code])} строк (взяты из en): {missing[code][:6]}…")
    (out / ".nojekyll").write_text("")
    check(out, texts["ru"])
    size = sum(f.stat().st_size for f in out.rglob("*") if f.is_file())
    print(f"сайт собран: {out} ({size // 1024} КБ, {len(LANGS)} языков)")


def check(out: Path, ru):
    """Проверки: ссылки на свои файлы существуют; ключи t("…") из скриптов есть в ru.json; позы маскота существуют."""
    for page in out.rglob("*.html"):
        s = page.read_text(encoding="utf-8")
        for ref in re.findall(r'(?:src|href)="(/[^"?#]*)', s):
            if ref.endswith("/"): ref += "index.html"
            if not (out / ref.lstrip("/")).exists(): sys.exit(f"site: {page.relative_to(out)} ссылается на отсутствующий {ref}")
        if "{{" in s: sys.exit(f"site: в {page.relative_to(out)} остался шаблон: {s[s.index('{{'):s.index('{{') + 40]}")
    code = "".join(f.read_text(encoding="utf-8") for f in list((out / "assets").glob("*.js")) + list(out.glob("*.js")) if f.name != "data.js")
    used = set(re.findall(r'\bt[f]?\("([a-z0-9_.]+)"', code))
    eggs = (out / "assets/eggs.js").read_text(encoding="utf-8")
    ach = re.findall(r'\["([a-z0-9_]+)", "[^"]+"(?:, [01])?\]', eggs[eggs.index("var ACH"):eggs.index("var CHEATS")])
    cheats = re.findall(r'"([a-z0-9]+)"', re.search(r"var CHEATS = \[([^\]]+)\]", eggs).group(1))
    used |= {f"egg.{a}" for a in ach} | {f"egg.{a}.d" for a in ach} | {f"cheat.{c}" for c in cheats}
    lack = sorted(k for k in used if k not in ru and not k.endswith(".") and k != "ph.t")
    if lack: sys.exit(f"site: в ru.json нет ключей из скриптов: {lack}")
    if len(ach) < 50: sys.exit(f"site: достижений {len(ach)} — меньше 50")
    poses = set(re.findall(r'/img/mascot/([\w-]+)\.webp', code + "".join(p.read_text(encoding="utf-8") for p in out.rglob("*.html"))))
    poses |= set(re.findall(r'(?:(?<!store)(?<!\bK)\.(?:set|act)|\bP)\("([\w-]+)"', code))
    for p in sorted(poses):
        if not (out / f"img/mascot/{p}.webp").exists(): sys.exit(f"site: нет позы маскота {p}")
    print(f"проверено: {len(list(out.rglob('*.html')))} страниц, {len(ach)} достижений, {len(cheats)} читов, ключей в скриптах {len(used)}")


if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "--shots": shots(Path(sys.argv[2])); sys.exit(0)
    build(Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "build/site")
