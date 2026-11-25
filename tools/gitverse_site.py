#!/usr/bin/env python3
"""
Выкладка сайта на GitVerse Pages: собранная папка (tools/site_build.py) уходит отдельной веткой `pages`
в репозиторий GitVerse — одним коммитом без истории, не трогая ветку main с кодом (её ведёт зеркало выпусков).
В GitVerse: Настройки → Страницы → источник «ветка pages, корень».

  GV_TOKEN=… GV_REPO=kapsula/KAPSULA python3 tools/gitverse_site.py build/site
"""
import os, shutil, subprocess, sys, tempfile, time

TOKEN = os.environ.get("GV_TOKEN", "")
REPO = os.environ.get("GV_REPO", "")
BRANCH = "pages"


def run(*a, cwd=None):
    return subprocess.run(list(a), cwd=cwd, capture_output=True, text=True)


def main(src):
    if not TOKEN or not REPO:
        print("::warning::нет GITVERSE_TOKEN / GITVERSE_REPO — сайт не выложен"); return 0
    if not os.path.isfile(os.path.join(src, "index.html")):
        print(f"::error::в {src} нет index.html — сначала tools/site_build.py"); return 1
    with tempfile.TemporaryDirectory() as tmp:
        shutil.copytree(src, tmp, dirs_exist_ok=True)
        run("git", "init", "-q", "-b", BRANCH, cwd=tmp)
        run("git", "-c", "user.name=KAPSULA", "-c", "user.email=site@kapsula.invalid", "add", "-A", cwd=tmp)
        r = run("git", "-c", "user.name=KAPSULA", "-c", "user.email=site@kapsula.invalid", "commit", "-q", "-m", "Сайт KAPSULA", cwd=tmp)
        if r.returncode: print("::error::коммит сайта:", r.stderr[-300:]); return 1
        owner = REPO.split("/")[0]
        for attempt in range(4):
            # токен — как пароль HTTPS; имя пользователя пробуем в тех же вариантах, что и зеркало выпусков
            for user in (owner, "oauth2", "token"):
                r = run("git", "push", "--force", f"https://{user}:{TOKEN}@gitverse.ru/{REPO}.git", f"HEAD:refs/heads/{BRANCH}", cwd=tmp)
                if r.returncode == 0:
                    print(f"сайт отправлен в GitVerse, ветка {BRANCH}"); return 0
                print(f"git push как «{user}» не прошёл: " + r.stderr.replace(TOKEN, "***")[-300:])
            if attempt < 3: time.sleep(15 * 2 ** attempt)
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1] if len(sys.argv) > 1 else "build/site"))
