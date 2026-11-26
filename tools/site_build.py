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
GH_API = "https://api.github.com/repos/vitalya2078-cloud/nexus-psp/releases/tags/latest"
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


def data_js(pl, out: Path):
    """data.js — общие с приложением данные для страниц «Полезное»: консоли (Catalog.kt) и известные BIOS PS1 (Bios.kt)."""
    src = (ROOT / "app/src/main/java/com/nexus/psp/data/Catalog.kt").read_text(encoding="utf-8")
    plats = []
    for p in pl:
        m = re.search(r'Platform\("' + re.escape(p["id"]) + r'"[^\n]*?exts = listOf\(([^)]*)\)', src)
        exts = re.findall(r'"([^"]+)"', m.group(1)) if m else []
        plats.append({"id": p["id"], "short": p["short"], "full": p["full"], "color": p["color"], "bios": p["bios"], "exp": p["exp"],
                      "img": "img/con/" + p["img"].rsplit(".", 1)[0] + ".webp", "exts": exts})
    bios = (ROOT / "app/src/main/java/com/nexus/psp/emulator/Bios.kt").read_text(encoding="utf-8")
    ps1 = {md5: label for md5, label in re.findall(r'"([0-9a-f]{32})" to \("[^"]+" to "([^"]+)"\)', bios)}
    if len(ps1) < 5: sys.exit("site: таблица BIOS PS1 в Bios.kt не разобралась")
    (out / "data.js").write_text("window.KAPSULA_DATA = " + json.dumps({"platforms": plats, "ps1bios": ps1}, ensure_ascii=False) + ";\n", encoding="utf-8")


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
    """APK кладётся прямо на сайт (kapsula.apk) — GitVerse ограничивает скачивание с выпусков без входа (429),
    а сайт отдаётся без лимита и с того же адреса (полоса загрузки работает). latest.json — версия и ссылка.
      APK_FILE=путь — готовый APK (сборка выпуска); INCLUDE_APK=1 — взять из выпуска latest на GitHub."""
    apk = os.environ.get("APK_FILE"); ver = None
    if not apk and os.environ.get("INCLUDE_APK"):
        try:
            rel = json.load(urllib.request.urlopen(urllib.request.Request(GH_API, headers={"Accept": "application/json", "User-Agent": "KAPSULA-site"}), timeout=30))
            ver = re.search(r"\d+\.\d+(?:\.\d+)?", rel.get("name", "")).group(0)
            a = next(x for x in rel["assets"] if x["name"] == "kapsula.apk")
            apk = str(out / "kapsula.apk")
            with urllib.request.urlopen(urllib.request.Request(a["browser_download_url"], headers={"User-Agent": "KAPSULA-site"}), timeout=300) as r, open(apk, "wb") as f:
                shutil.copyfileobj(r, f)
        except Exception as e:
            print(f"::warning::APK с GitHub не получен: {e}"); apk = None
    if apk and os.path.getsize(apk) > 1_000_000:
        if os.path.abspath(apk) != os.path.abspath(out / "kapsula.apk"): shutil.copy(apk, out / "kapsula.apk")
        ver = ver or re.search(r'versionName = "([^"]+)"', (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")).group(1)
        size = os.path.getsize(out / "kapsula.apk"); mb = round(size / 1048576)
        # куски по 8 МБ: сервер сайта не докачивает с места обрыва (Range не работает) — страница качает по кускам
        # и при обрыве перекачивает только упавший кусок. Целый файл остаётся — для прямой ссылки и обновлений в приложении.
        PART = 8 * 1048576; parts = 0
        with open(out / "kapsula.apk", "rb") as src:
            while True:
                chunk = src.read(PART)
                if not chunk: break
                (out / f"kapsula.apk.{parts:02d}").write_bytes(chunk); parts += 1
        (out / "latest.json").write_text(json.dumps({"ver": ver, "url": "kapsula.apk", "mb": mb, "size": size, "parts": parts}), encoding="utf-8")
        print(f"APK {ver} ({mb} МБ) — на сайте"); return
    # без APK — ссылка на выпуск GitVerse по версии проекта (может упереться в лимит GitVerse для гостей)
    m = re.search(r'versionName = "([^"]+)"', (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8"))
    if m:
        ver = m.group(1)
        (out / "latest.json").write_text(json.dumps({"ver": ver, "url": f"https://gitverse.ru/kapsula/KAPSULA/releases/download/v{ver}/kapsula.apk", "mb": 0}), encoding="utf-8")
        print(f"::warning::APK не положен на сайт — ссылка на GitVerse, версия {ver}")


def build(out: Path):
    if out.exists(): shutil.rmtree(out)
    (out / "img/screens").mkdir(parents=True)
    for f in ("index.html", "privacy.html", "404.html", "gift.html", "site.css", "site.js", "game.js", "menu.js"): shutil.copy(SITE / f, out / f)
    # «Полезное»: страницы и их скрипты (site/tools/*.html, *.js, *.css)
    for f in (SITE / "tools").glob("*"): shutil.copy(f, out / f.name)
    shutil.copytree(SITE / "art/items", out / "img/items")
    for f in (SITE / "screens").glob("*.webp"): shutil.copy(f, out / "img/screens" / f.name)
    idx = out / "index.html"
    s = idx.read_text(encoding="utf-8")
    for mark in ("<!--CONSOLES-->", "<!--CHANGES-->"):
        if mark not in s: sys.exit(f"site: в index.html нет места {mark}")
    s = s.replace("<!--CHANGES-->", changes_html())
    # картинка для превью ссылки (Telegram, ВК) — только с полным адресом
    s = s.replace('content="img/og.jpg"', f'content="{SITE_URL}img/og.jpg"')
    pl = platforms()
    idx.write_text(s.replace("<!--CONSOLES-->", consoles_html(pl, out)), encoding="utf-8")
    data_js(pl, out)
    (out / "img/logo.svg").write_text(LOGO, encoding="utf-8")
    from PIL import Image
    if (SITE / "screens/1-home.webp").exists():
        Image.open(SITE / "screens/1-home.webp").convert("RGB").resize((1200, 554)).save(out / "img/og.jpg", quality=85)
    fonts(out); mw, mh = mascot(out); qr(out); latest(out)
    for page in ["index.html", "404.html", "privacy.html", "gift.html"] + [f.name for f in (SITE / "tools").glob("*.html")]:  # размеры холста маскота — в разметку (без скачка при загрузке)
        f = out / page; f.write_text(f.read_text(encoding="utf-8").replace('width="321" height="311"', f'width="{mw}" height="{mh}"'), encoding="utf-8")
    # GitVerse Pages по умолчанию прогоняет Jekyll — у нас готовая статика
    (out / ".nojekyll").write_text("")
    # проверка: все ссылки на свои файлы ведут на существующие файлы
    for page in ["index.html", "privacy.html", "404.html", "gift.html"] + [f.name for f in (SITE / "tools").glob("*.html")]:
        for ref in re.findall(r'(?:src|href)="((?:img|fonts)/[^"]+|[\w-]+\.(?:js|css|html))"', (out / page).read_text(encoding="utf-8")):
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
