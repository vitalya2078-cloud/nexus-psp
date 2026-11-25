#!/usr/bin/env python3
"""
Сборка сайта KAPSULA (GitVerse Pages) из site/ + общих файлов приложения — облик не расходится с приложением:
  маскот — ui/img/mascot-l-*.webp, картинки консолей — ui/img/con-*.png, список консолей — Catalog.kt,
  шрифты — app/src/main/res/font (переводятся в woff2).
Ещё: QR-код адреса сайта (для компьютера), latest.json — версия и прямая ссылка на APK с GitVerse (без VPN).

  python3 tools/site_build.py [папка_вывода]          # по умолчанию build/site
  python3 tools/site_build.py --shots app/build/site-shots   # снимки SiteShotsTest → site/screens/*.webp
Переменные: SITE_URL (для QR, по умолчанию https://kapsula.gitverse.site/), NO_NET=1 — без запроса версии.
Нужно: pip install fonttools brotli segno pillow
"""
import html
import json
import os
import re
import shutil
import sys
import time
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SITE = ROOT / "site"
GV_API = "https://gitverse.ru/api/repos/kapsula/KAPSULA/releases?limit=1"
SITE_URL = os.environ.get("SITE_URL", "https://kapsula.gitverse.site/")


def shots(src: Path):
    from PIL import Image
    dst = SITE / "screens"; dst.mkdir(exist_ok=True)
    for f in sorted(src.glob("*.png")):
        Image.open(f).convert("RGB").save(dst / (f.stem + ".webp"), "WEBP", quality=86, method=6)
        print("снимок", f.stem)


def platforms():
    """Консоли из Catalog.kt: id, короткое имя, полное, картинка, тестовая ли, нужен ли BIOS."""
    src = (ROOT / "app/src/main/java/com/nexus/psp/data/Catalog.kt").read_text(encoding="utf-8")
    out = []
    for m in re.finditer(r'Platform\("([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*Color\(0xFF([0-9A-Fa-f]{6})\),\s*(\d),\s*"[^"]*",\s*"[^"]*",\s*"([^"]+)"([^\n]*)', src):
        pid, short, full, col, bios, img, rest = m.groups()
        out.append(dict(id=pid, short=short, full=full, color="#" + col.lower(), bios=int(bios), img=img, exp="exp = true" in rest))
    if len(out) < 10: sys.exit(f"site: в Catalog.kt найдено только {len(out)} консолей — разбор сломался")
    return out


def consoles_html(pl, out: Path):
    (out / "img/con").mkdir(parents=True, exist_ok=True)
    cards = []
    for p in pl:
        name = p["img"].rsplit(".", 1)[0] + ".webp"
        if not (out / "img/con" / name).exists():
            from PIL import Image
            im = Image.open(ROOT / "ui/img" / p["img"]).convert("RGBA"); im.thumbnail((360, 240))
            im.save(out / "img/con" / name, "WEBP", quality=88, method=6)
        tags = []
        if p["exp"]: tags.append('<span class="tag">тестовая</span>')
        if p["bios"] == 2: tags.append("нужен BIOS")
        cards.append(f'<div class="con" style="--c:{p["color"]}"><img src="img/con/{name}" alt="" loading="lazy"><b>{html.escape(p["full"])}</b>'
                     + (f'<small>{" · ".join(tags)}</small>' if tags else "") + "</div>")
    return "\n    ".join(cards)


def fonts(out: Path):
    from fontTools.ttLib import TTFont
    (out / "fonts").mkdir(parents=True, exist_ok=True)
    for src, name in (("balsamiq_bold", "balsamiq"), ("mplus_medium", "mplus-500"), ("mplus_extrabold", "mplus-800")):
        f = TTFont(ROOT / f"app/src/main/res/font/{src}.ttf"); f.flavor = "woff2"; f.save(out / f"fonts/{name}.woff2")


def mascot(out: Path):
    """Позы приложения (ui/img/mascot-l-*) и позы только для сайта (site/art/mascot) — на один общий холст:
    по центру тела, ноги на одной линии, чтобы смена позы в одном <img> не дёргала маскота."""
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


def changes_html():
    """«Что нового» — из site/changes.json (пишется по-человечески при выпуске)."""
    out = []
    for r in json.loads((SITE / "changes.json").read_text(encoding="utf-8")):
        y, m, d = r["date"].split("-")
        mon = ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"][int(m) - 1]
        items = "".join(f"<li>{html.escape(i)}</li>" for i in r["items"])
        out.append(f'<article class="rel"><b>{html.escape(r["ver"])}</b><time datetime="{r["date"]}">{int(d)} {mon} {y}</time><ul>{items}</ul></article>')
    return "\n    ".join(out)


LOGO = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48"><rect width="48" height="48" rx="13" fill="#1b2238"/>
<rect x="7" y="14.5" width="34" height="19" rx="9.5" fill="none" stroke="#f4eee4" stroke-width="2.6"/>
<path d="M24 14.5v19" stroke="#f4eee4" stroke-width="2.2"/><path d="M13.5 19.5v9l7-4.5z" fill="#f2a65a"/>
<path d="M27.5 20h7M27.5 23h9M27.5 26h9M27.5 29h7" stroke="#f4eee4" stroke-width="1.6" stroke-linecap="round"/></svg>"""


def qr(out: Path):
    import segno
    segno.make(SITE_URL, error="m").save(str(out / "img/qr.svg"), scale=4, border=2, dark="#121828", light="#f4eee4")


def latest(out: Path):
    """Последний выпуск на GitVerse → latest.json (сайт читает его со своего адреса — без запросов к чужим API из браузера)."""
    if os.environ.get("NO_NET"): return
    for i in range(4):
        try:
            req = urllib.request.Request(GV_API, headers={"Accept": "application/json", "User-Agent": "KAPSULA-site"})
            r = json.load(urllib.request.urlopen(req, timeout=30))["data"][0]
            a = next(x for x in r["attachments"] if x["name"].endswith(".apk"))
            ver = re.search(r"\d+\.\d+(?:\.\d+)?", r.get("title") or r.get("tagName", "")).group(0)
            (out / "latest.json").write_text(json.dumps({"ver": ver, "url": a["downloadUrl"], "mb": round(a.get("size", 0) / 1048576)}), encoding="utf-8")
            print("версия на GitVerse:", ver); return
        except Exception as e:
            print(f"GitVerse: {e} — повтор через {10 * 2 ** i} с"); time.sleep(10 * 2 ** i)
    print("::warning::версию с GitVerse получить не удалось — кнопка ведёт на страницу выпусков")


def build(out: Path):
    if out.exists(): shutil.rmtree(out)
    (out / "img/screens").mkdir(parents=True)
    for f in ("index.html", "privacy.html", "404.html", "site.css", "site.js", "game.js"): shutil.copy(SITE / f, out / f)
    shutil.copytree(SITE / "art/items", out / "img/items")
    for f in (SITE / "screens").glob("*.webp"): shutil.copy(f, out / "img/screens" / f.name)
    idx = out / "index.html"
    s = idx.read_text(encoding="utf-8")
    for mark in ("<!--CONSOLES-->", "<!--CHANGES-->"):
        if mark not in s: sys.exit(f"site: в index.html нет места {mark}")
    s = s.replace("<!--CHANGES-->", changes_html())
    # картинка для превью ссылки (Telegram, ВК) — только с полным адресом
    s = s.replace('content="img/og.jpg"', f'content="{SITE_URL}img/og.jpg"')
    idx.write_text(s.replace("<!--CONSOLES-->", consoles_html(platforms(), out)), encoding="utf-8")
    (out / "img/logo.svg").write_text(LOGO, encoding="utf-8")
    from PIL import Image
    if (SITE / "screens/1-home.webp").exists():
        Image.open(SITE / "screens/1-home.webp").convert("RGB").resize((1200, 554)).save(out / "img/og.jpg", quality=85)
    fonts(out); mw, mh = mascot(out); qr(out); latest(out)
    for page in ("index.html", "404.html", "privacy.html"):  # размеры холста маскота — в разметку (без скачка при загрузке)
        f = out / page; f.write_text(f.read_text(encoding="utf-8").replace('width="321" height="311"', f'width="{mw}" height="{mh}"'), encoding="utf-8")
    # GitVerse Pages по умолчанию прогоняет Jekyll — у нас готовая статика
    (out / ".nojekyll").write_text("")
    # проверка: все ссылки на свои файлы ведут на существующие файлы
    for page in ("index.html", "privacy.html", "404.html"):
        for ref in re.findall(r'(?:src|href)="((?:img|fonts)/[^"]+|site\.\w+|game\.js)"', (out / page).read_text(encoding="utf-8")):
            if not (out / ref).exists(): sys.exit(f"site: {page} ссылается на отсутствующий {ref}")
    # позы и предметы, которые зовут скрипты и разметка (P("…"), data-buddy, img/items/…)
    code = "".join((out / f).read_text(encoding="utf-8") for f in ("site.js", "game.js", "index.html"))
    poses = set(re.findall(r'P\("(\w[\w-]*)"\)', code)) | set(re.findall(r'data-buddy="([\w-]+)\|', code))
    poses |= {p for arr in re.findall(r'\[((?:"[\w-]+",\s*)+"[\w-]+")\]\.forEach\(function \(n\) \{ (?:new Image|var i = new Image)', code) for p in re.findall(r'"([\w-]+)"', arr)}
    for p in sorted(poses):
        if not (out / f"img/mascot/{p}.webp").exists(): sys.exit(f"site: нет позы маскота {p}")
    for ref in re.findall(r'url\((fonts/[^)]+)\)', (out / "site.css").read_text(encoding="utf-8")):
        if not (out / ref).exists(): sys.exit(f"site: site.css ссылается на отсутствующий {ref}")
    size = sum(f.stat().st_size for f in out.rglob("*") if f.is_file())
    print(f"сайт собран: {out} ({size // 1024} КБ)")


if __name__ == "__main__":
    if len(sys.argv) > 2 and sys.argv[1] == "--shots": shots(Path(sys.argv[2])); sys.exit(0)
    build(Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "build/site")
