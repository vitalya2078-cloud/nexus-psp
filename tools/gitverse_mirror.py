#!/usr/bin/env python3
"""
Зеркало KAPSULA на GitVerse (работает без VPN): после сборки на GitHub
  1) ветка main на GitVerse — только README со ссылкой на код и лицензии на GitHub (на GitVerse — только готовое);
  2) создаёт выпуск «KAPSULA <версия>» с тегом v<версия> и прикладывает kapsula.apk;
  3) удаляет старые выпуски, оставляя последние KEEP (лимит места на GitVerse — 4 ГБ).

Переменные окружения: GV_TOKEN (токен GitVerse, «Репозитории → Запись»), GV_REPO (владелец/репозиторий),
VER (версия), APK (путь к APK), NOTES (текст выпуска, необязательно).
API: https://gitverse.ru/docs/developers/public-api — Authorization: Bearer, Accept: application/vnd.gitverse.object+json;version=1
"""
import json
import time, os, subprocess, sys, urllib.request, urllib.error, uuid

API = "https://api.gitverse.ru"
KEEP = 5
TOKEN = os.environ.get("GV_TOKEN", "")
REPO = os.environ.get("GV_REPO", "")
VER = os.environ.get("VER", "")
APK = os.environ.get("APK", "kapsula.apk")
NOTES = os.environ.get("NOTES", "")
TAG = "v" + VER


def req(method, path, body=None, data=None, ctype=None):
    h = {"Authorization": "Bearer " + TOKEN, "Accept": "application/vnd.gitverse.object+json;version=1"}
    if body is not None:
        data = json.dumps(body).encode(); ctype = "application/json"
    if ctype: h["Content-Type"] = ctype
    # GitVerse иногда отвечает 502 / рвёт соединение — повторяем с паузой (10, 20, 40, 80 с)
    for attempt in range(5):
        r = urllib.request.Request(API + path, data=data, headers=h, method=method)
        try:
            with urllib.request.urlopen(r, timeout=600) as resp:
                raw = resp.read()
                return resp.status, (json.loads(raw) if raw.strip() else None)
        except urllib.error.HTTPError as e:
            raw = e.read().decode(errors="replace")
            if e.code < 500 or attempt == 4: return e.code, raw
            print(f"GitVerse ответил {e.code} — повтор через {10 * 2 ** attempt} с")
        except (urllib.error.URLError, OSError) as e:
            if attempt == 4: raise
            print(f"GitVerse недоступен ({e}) — повтор через {10 * 2 ** attempt} с")
        time.sleep(10 * 2 ** attempt)


GITHUB = "https://github.com/vitalya2078-cloud/nexus-psp"
README = f"""# KAPSULA

Здесь — только готовое: сборки KAPSULA (APK в разделе «Выпуски») и сайт https://kapsula.gitverse.site/.

Исходный код, история изменений и лицензии (GNU GPL ядер эмуляции, шрифты OFL и др.) — на GitHub:
{GITHUB}
"""


def git_url(user):
    return f"https://{user}:{TOKEN}@gitverse.ru/{REPO}.git"


def push_code():
    """На GitVerse — только готовое (решение пользователя 2026-11-27): ветка main — один коммит с README
    (ссылка на код и лицензии на GitHub), тег версии — на нём. Старый код с GitVerse убираем: лишние ветки
    удаляем (кроме main и pages — там сайт), теги версий переводим на этот же коммит."""
    import tempfile
    owner = REPO.split("/")[0]
    with tempfile.TemporaryDirectory() as tmp:
        g = lambda *a: subprocess.run(["git", *a], cwd=tmp, capture_output=True, text=True)
        g("init", "-q", "-b", "main")
        open(os.path.join(tmp, "README.md"), "w", encoding="utf-8").write(README)
        g("add", "README.md")
        g("-c", "user.name=KAPSULA", "-c", "user.email=build@kapsula.invalid", "commit", "-q", "-m", f"KAPSULA {VER}")
        g("tag", "-f", TAG)
        for attempt in range(4):
            for user in (owner, "oauth2", "token"):
                r = g("push", "--force", git_url(user), "HEAD:refs/heads/main", f"+refs/tags/{TAG}:refs/tags/{TAG}")
                if r.returncode != 0:
                    print(f"git push как «{user}» не прошёл: " + r.stderr.replace(TOKEN, "***")[-300:]); continue
                print(f"GitVerse: main — только README, тег {TAG}")
                # что ещё лежит на GitVerse: ветки с кодом — удалить, старые теги — на коммит с README
                ls = g("ls-remote", git_url(user)).stdout.split("\n")
                refs = [l.split("\t")[1] for l in ls if "\t" in l and not l.endswith("^{}")]
                drop = [x for x in refs if x.startswith("refs/heads/") and x not in ("refs/heads/main", "refs/heads/pages")]
                tags = [x for x in refs if x.startswith("refs/tags/") and x != f"refs/tags/{TAG}"]
                if drop:
                    r2 = g("push", git_url(user), *[":" + x for x in drop])
                    print("удалены ветки:", drop, "" if r2.returncode == 0 else r2.stderr.replace(TOKEN, "***")[-200:])
                if tags:
                    r2 = g("push", "--force", git_url(user), *[f"HEAD:{x}" for x in tags])
                    print("теги переведены на README:", len(tags), "" if r2.returncode == 0 else r2.stderr.replace(TOKEN, "***")[-200:])
                return True
            if attempt < 3:
                print(f"GitVerse недоступен — повтор отправки через {15 * 2 ** attempt} с"); time.sleep(15 * 2 ** attempt)
    return False


def release():
    st, rel = req("GET", f"/repos/{REPO}/releases/tags/{TAG}")
    if st == 200 and isinstance(rel, dict):
        print(f"выпуск {TAG} уже есть (id {rel['id']}) — заменяю APK")
        for a in rel.get("assets") or []:
            if a.get("name") == "kapsula.apk":
                req("DELETE", f"/repos/{REPO}/releases/{rel['id']}/assets/{a['id']}")
    else:
        st, rel = req("POST", f"/repos/{REPO}/releases", body={
            "tag_name": TAG, "target_commitish": "main", "name": f"KAPSULA {VER}", "body": NOTES or f"KAPSULA {VER}",
            "draft": False, "prerelease": False,
        })
        if st not in (200, 201) or not isinstance(rel, dict):
            print(f"::error::выпуск на GitVerse не создан: {st} {rel}"); return None
        print(f"выпуск {TAG} создан (id {rel['id']})")
    # файл — multipart/form-data, поле attachment
    b = uuid.uuid4().hex
    with open(APK, "rb") as f: blob = f.read()
    body = (f"--{b}\r\nContent-Disposition: form-data; name=\"attachment\"; filename=\"kapsula.apk\"\r\n"
            f"Content-Type: application/vnd.android.package-archive\r\n\r\n").encode() + blob + f"\r\n--{b}--\r\n".encode()
    st, a = req("POST", f"/repos/{REPO}/releases/{rel['id']}/assets?name=kapsula.apk", data=body, ctype=f"multipart/form-data; boundary={b}")
    if st not in (200, 201):
        print(f"::error::APK на GitVerse не загружен: {st} {a}"); return None
    url = (a or {}).get("browser_download_url")
    print("APK загружен:", url)
    # скачивается ли APK без входа (нужно приложению для обновлений без VPN)
    if url:
        try:
            r = urllib.request.Request(url, method="GET", headers={"Range": "bytes=0-3"})
            with urllib.request.urlopen(r, timeout=60) as resp:
                print(f"без входа: {resp.status}, начало файла {resp.read(4)!r}")
        except urllib.error.HTTPError as e:
            print(f"без входа: {e.code} — APK по этой ссылке скачивается только с токеном")
        except Exception as e:
            print("без входа: не проверилось:", e)
    return rel


def cleanup():
    st, rels = req("GET", f"/repos/{REPO}/releases?per_page=30")
    if st != 200 or not isinstance(rels, list): return
    old = [r for r in rels if r.get("tag_name", "").startswith("v")][KEEP:]
    for r in old:
        req("DELETE", f"/repos/{REPO}/releases/{r['id']}")
        print("удалён старый выпуск", r.get("tag_name"))


if __name__ == "__main__":
    if not TOKEN or not REPO or not VER:
        print("::warning::зеркало GitVerse не настроено (нет GITVERSE_TOKEN / GITVERSE_REPO) — пропускаю"); sys.exit(0)
    ok_code = push_code()
    rel = release()
    if rel: cleanup()
    sys.exit(0 if rel else 1)
