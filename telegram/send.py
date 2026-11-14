#!/usr/bin/env python3
"""Публикация в Telegram-канал KAPSULA из GitHub Actions.

Каждый JSON-файл из telegram/outbox/, добавленный или изменённый в пуше, отправляется один раз.
Токен и канал берутся из секретов TELEGRAM_BOT_TOKEN и TELEGRAM_CHAT_ID и в лог не попадают.

Форматы файла:
  {"setup": {"channel": {"title", "description", "photo"}, "bot": {"name", "short", "description"}}}
  {"text": "<b>HTML</b>", "photo": "telegram/img/x.png", "buttons": [[{"text", "url"}]], "pin": true, "silent": false}
В текстах {CHANNEL} заменяется на ссылку t.me/<канал>.
"""
import json, os, sys, uuid, urllib.request, urllib.error

TOKEN = os.environ["TELEGRAM_BOT_TOKEN"].strip()
CHAT = os.environ["TELEGRAM_CHAT_ID"].strip()
CHANNEL = "https://t.me/" + CHAT.lstrip("@") if CHAT.startswith("@") else ""
API = "https://api.telegram.org/bot" + TOKEN + "/"


def sub(s):
    return s.replace("{CHANNEL}", CHANNEL) if isinstance(s, str) else s


def call(method, fields=None, files=None):
    fields = {k: v for k, v in (fields or {}).items() if v is not None}
    if files:
        b = uuid.uuid4().hex
        body = bytearray()
        for k, v in fields.items():
            v = json.dumps(v, ensure_ascii=False) if isinstance(v, (dict, list, bool)) else str(v)
            body += f'--{b}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode()
        for k, path in files.items():
            body += f'--{b}\r\nContent-Disposition: form-data; name="{k}"; filename="{os.path.basename(path)}"\r\nContent-Type: application/octet-stream\r\n\r\n'.encode()
            body += open(path, "rb").read() + b"\r\n"
        body += f"--{b}--\r\n".encode()
        req = urllib.request.Request(API + method, data=bytes(body), headers={"Content-Type": f"multipart/form-data; boundary={b}"})
    else:
        req = urllib.request.Request(API + method, data=json.dumps(fields, ensure_ascii=False).encode(), headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=60) as r:
            res = json.load(r)
    except urllib.error.HTTPError as e:
        res = json.load(e)
    ok = res.get("ok")
    print(f"  {method}: {'ok' if ok else 'ОШИБКА ' + str(res.get('description'))}")
    return res if ok else None


def setup(s):
    ch, bot = s.get("channel", {}), s.get("bot", {})
    if "title" in ch: call("setChatTitle", {"chat_id": CHAT, "title": ch["title"]})
    if "description" in ch: call("setChatDescription", {"chat_id": CHAT, "description": sub(ch["description"])})
    if "photo" in ch: call("setChatPhoto", {"chat_id": CHAT}, {"photo": ch["photo"]})
    if "name" in bot: call("setMyName", {"name": bot["name"]})
    if "short" in bot: call("setMyShortDescription", {"short_description": sub(bot["short"])})
    if "description" in bot: call("setMyDescription", {"description": sub(bot["description"])})
    return True


def post(p):
    kb = {"inline_keyboard": [[{"text": b["text"], "url": sub(b["url"])} for b in row] for row in p["buttons"]]} if p.get("buttons") else None
    base = {"chat_id": CHAT, "parse_mode": "HTML", "reply_markup": kb, "disable_notification": bool(p.get("silent", False))}
    if p.get("photo"):
        res = call("sendPhoto", {**base, "caption": sub(p["text"])}, {"photo": p["photo"]})
    else:
        res = call("sendMessage", {**base, "text": sub(p["text"]), "link_preview_options": {"is_disabled": True}})
    if res and p.get("pin"):
        call("pinChatMessage", {"chat_id": CHAT, "message_id": res["result"]["message_id"], "disable_notification": True})
    return res is not None


ok = True
for path in sorted(a for a in sys.argv[1:] if a.endswith(".json") and os.path.isfile(a)):
    print(path)
    data = json.load(open(path, encoding="utf-8"))
    ok &= setup(data["setup"]) if "setup" in data else post(data)
sys.exit(0 if ok else 1)
