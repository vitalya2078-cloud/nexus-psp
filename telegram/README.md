# Канал KAPSULA в Telegram

- `drafts/` — черновики, никуда не уходят.
- `outbox/` — положил JSON сюда и запушил: workflow «Telegram channel» отправит его в канал один раз.
- Секреты репозитория: `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_ID` (`@адрес_канала`).
- Формат файлов — в начале `send.py`.
