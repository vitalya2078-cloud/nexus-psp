#!/usr/bin/env python3
"""
Зеркало KAPSULA на GitVerse (работает без VPN): после сборки на GitHub
  1) отправляет код (ветку и тег версии) в репозиторий GitVerse;
  2) создаёт выпуск «KAPSULA <версия>» с тегом v<версия> и прикладывает kapsula.apk;
  3) удаляет старые выпуски, оставляя последние KEEP (лимит места на GitVerse — 4 ГБ).

Переменные окружения: GV_TOKEN (токен GitVerse, «Репозитории → Запись»), GV_REPO (владелец/репозиторий),
VER (версия), APK (путь к APK), NOTES (текст выпуска, необязательно).
API: https://gitverse.ru/docs/developers/public-api — Authorization: Bearer, Accept: application/vnd.gitverse.object+json;version=1
"""
import json, os, subprocess, sys, urllib.request, urllib.error, uuid

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
    r = urllib.request.Request(API + path, data=data, headers=h, method=method)
    try:
        with urllib.request.urlopen(r, timeout=600) as resp:
            raw = resp.read()
            return resp.status, (json.loads(raw) if raw.strip() else None)
    except urllib.error.HTTPError as e:
        raw = e.read().decode(errors="replace")
        return e.code, raw


def push_code():
    """Код и тег версии — в GitVerse. Токен как пароль HTTPS; пробуем принятые варианты имени пользователя."""
    owner = REPO.split("/")[0]
    for user in (owner, "oauth2", "token"):
        url = f"https://{user}:{TOKEN}@gitverse.ru/{REPO}.git"
        subprocess.run(["git", "tag", "-f", TAG], check=False)
        r = subprocess.run(["git", "push", "--force", url, "HEAD:refs/heads/main", f"refs/tags/{TAG}"], capture_output=True, text=True)
        if r.returncode == 0:
            print(f"код отправлен в GitVerse (пользователь «{user}»), тег {TAG}")
            return True
        print(f"git push как «{user}» не прошёл: " + r.stderr.replace(TOKEN, "***")[-300:])
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
    print("APK загружен:", (a or {}).get("browser_download_url"))
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
