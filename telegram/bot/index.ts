// Бот @kapsula_emu_bot: Supabase Edge Function, принимает вебхук Telegram.
// Секреты функции: TELEGRAM_BOT_TOKEN, TELEGRAM_CHAT_ID (@канал), OWNER_CHAT_ID (кому слать баги, необязательно),
// EMOJI (необязательно): JSON {"🎮": "<custom_emoji_id>", ...} — премиум-эмодзи вместо обычных.
const TOKEN = (Deno.env.get("TELEGRAM_BOT_TOKEN") ?? "").trim();
const CHAT = (Deno.env.get("TELEGRAM_CHAT_ID") ?? "").trim();
const OWNER = (Deno.env.get("OWNER_CHAT_ID") ?? "").trim();
const CHANNEL = CHAT.startsWith("@") ? "https://t.me/" + CHAT.slice(1) : "";
const APK = "https://github.com/vitalya2078-cloud/nexus-psp/releases/download/latest/kapsula.apk";
const BUG_MARK = "Опиши, что случилось";
let EMOJI: Record<string, string> = {};
try { EMOJI = JSON.parse(Deno.env.get("EMOJI") ?? "{}"); } catch { /* без премиум-эмодзи */ }

// Секрет вебхука выводится из токена — тот же расчёт в telegram/send.py
async function hookSecret(): Promise<string> {
  const h = await crypto.subtle.digest("SHA-256", new TextEncoder().encode("kapsula-hook:" + TOKEN));
  return Array.from(new Uint8Array(h)).map((b) => b.toString(16).padStart(2, "0")).join("").slice(0, 48);
}

const fx = (s: string) => Object.entries(EMOJI).reduce((t, [e, id]) => t.split(e).join(`<tg-emoji emoji-id="${id}">${e}</tg-emoji>`), s);

async function tg(method: string, body: Record<string, unknown>) {
  const r = await fetch(`https://api.telegram.org/bot${TOKEN}/${method}`, {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body),
  });
  const j = await r.json().catch(() => ({}));
  if (!j.ok) console.log(method, j.description);
  return j;
}

function menu() {
  const rows: unknown[][] = [[{ text: "⬇️ Скачать KAPSULA", url: APK }]];
  if (CHANNEL) rows.push([{ text: "📢 Канал с новостями", url: CHANNEL }]);
  rows.push([{ text: "🎮 Как начать", callback_data: "howto" }, { text: "🐞 Сообщить о баге", callback_data: "bug" }]);
  return { inline_keyboard: rows };
}

const HELLO = `<b>Привет! Это KAPSULA</b> 🎮

Эмулятор PSP и ретро-консолей для Android. Жми кнопку ниже — скачаешь свежую версию. Новости и обновления — в канале.`;

const HOWTO = `<b>Как начать</b> 🚀

1. Скачай APK и установи. Если телефон спросит — разреши установку из этого источника.
2. Закинь образы своих игр на телефон: ISO, CSO, CHD, PBP, CUE с BIN и другие.
3. Открой KAPSULA → «Найти на телефоне» и дай доступ к файлам. Игры сами разложатся по консолям.
4. Для PS2, Dreamcast, Saturn и Neo Geo нужен BIOS со своей приставки: Настройки → BIOS.

Игр в комплекте нет — играешь в то, что у тебя есть.`;

async function send(chat: number, text: string, extra: Record<string, unknown> = {}) {
  return tg("sendMessage", { chat_id: chat, text: fx(text), parse_mode: "HTML", link_preview_options: { is_disabled: true }, ...extra });
}

// deno-lint-ignore no-explicit-any
async function onMessage(m: any) {
  const chat = m.chat.id, text: string = m.text ?? m.caption ?? "";
  if (m.chat.type !== "private") return;
  // премиум-эмодзи в сообщении — подсказываем их ID (для настройки EMOJI)
  // deno-lint-ignore no-explicit-any
  const ids = (m.entities ?? m.caption_entities ?? []).filter((e: any) => e.type === "custom_emoji")
    // deno-lint-ignore no-explicit-any
    .map((e: any) => `${[...text.slice(e.offset, e.offset + e.length)].join("")} <code>${e.custom_emoji_id}</code>`);
  if (ids.length) return send(chat, "ID эмодзи:\n" + ids.join("\n"));
  if (m.reply_to_message?.text?.startsWith(BUG_MARK)) {
    if (OWNER) {
      await tg("forwardMessage", { chat_id: OWNER, from_chat_id: chat, message_id: m.message_id });
      await tg("sendMessage", { chat_id: OWNER, text: `Баг от ${m.from?.username ? "@" + m.from.username : m.from?.first_name ?? "игрока"} (id ${chat})` });
    }
    return send(chat, "Принято, спасибо! 🛠 Разберёмся и починим.", { reply_markup: menu() });
  }
  if (text.startsWith("/id")) return send(chat, `Твой ID: <code>${chat}</code>`);
  if (text.startsWith("/help")) return send(chat, HOWTO, { reply_markup: menu() });
  return send(chat, HELLO, { reply_markup: menu() });
}

// deno-lint-ignore no-explicit-any
async function onCallback(q: any) {
  await tg("answerCallbackQuery", { callback_query_id: q.id });
  const chat = q.message?.chat?.id;
  if (!chat) return;
  if (q.data === "howto") return send(chat, HOWTO, { reply_markup: menu() });
  if (q.data === "bug") {
    return send(chat, `${BUG_MARK} — ответь на это сообщение. Напиши модель телефона, игру и что пошло не так. Скриншот тоже можно.`,
      { reply_markup: { force_reply: true, input_field_placeholder: "Что случилось?" } });
  }
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return new Response("ok");
  if (!TOKEN || req.headers.get("X-Telegram-Bot-Api-Secret-Token") !== await hookSecret()) return new Response("forbidden", { status: 403 });
  try {
    const u = await req.json();
    if (u.message) await onMessage(u.message);
    else if (u.callback_query) await onCallback(u.callback_query);
  } catch (e) { console.log("update error", String(e)); }
  return new Response("ok");
});
