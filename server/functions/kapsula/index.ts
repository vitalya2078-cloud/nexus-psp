// KAPSULA: аккаунт без регистрации. Вход — код восстановления (KPS-XXXX-XXXX-XXXX), на сервере хранится только его хэш.
// Действия: register, sync, restore, profile (публичная карточка по ID, без кода), presence («сейчас играет»; видят друзья),
// cheer («поприветствовать создателя»), chest (сундук создателя — раз в сутки), friends / friend (друзья),
// poll (новые личные уведомления, новости, баланс — для фоновой проверки и раз в минуту),
// stats (анонимная статистика), challenges / claim (недельные испытания), invite («Позвать в игру»),
// saves (опись облачных сохранений), gift (подарок по коду), push_token (телефон для push), delete (удалить профиль целиком). Файлы сейвов — отдельными запросами с телом-файлом: POST ?op=put / ?op=get,
// код и ключ — в заголовках x-kapsula-code / x-kapsula-key (не в адресе — чтобы не попадали в журналы).
import { createClient } from "jsr:@supabase/supabase-js@2";

const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false } });
const ABC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // без 0/O и 1/I — их путают
const OPS = ["add", "accept", "decline", "cancel", "remove"];

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json; charset=utf-8" } });
}

function newCode(): string {
  const b = crypto.getRandomValues(new Uint8Array(12));
  const s = Array.from(b, (x) => ABC[x % 32]).join("");
  return `KPS-${s.slice(0, 4)}-${s.slice(4, 8)}-${s.slice(8, 12)}`;
}

function norm(code: unknown): string | null {
  if (typeof code !== "string") return null;
  let s = code.toUpperCase().replace(/[^A-Z0-9]/g, "");
  if (s.startsWith("KPS") && s.length === 15) s = s.slice(3);
  if (s.length !== 12 || [...s].some((c) => !ABC.includes(c))) return null;
  return s;
}

async function hash(c: string): Promise<string> {
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode("kapsula:" + c));
  return Array.from(new Uint8Array(d), (x) => x.toString(16).padStart(2, "0")).join("");
}

function shortId(v: unknown): string {
  return typeof v === "string" ? v.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 10) : "";
}

function num(v: unknown): number {
  const n = typeof v === "number" ? v : parseInt(String(v ?? "0"), 10);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : 0;
}

const MAX_SAVE = 2 * 1024 * 1024;

async function sha256hex(b: Uint8Array<ArrayBuffer>): Promise<string> {
  const d = await crypto.subtle.digest("SHA-256", b);
  return Array.from(new Uint8Array(d), (x) => x.toString(16).padStart(2, "0")).join("");
}

/** Облачные сохранения: put — тело запроса = файл (до 2 МБ), get — ответ = файл. */
async function saveFile(req: Request, op: string): Promise<Response> {
  const c = norm(req.headers.get("x-kapsula-code"));
  const key = req.headers.get("x-kapsula-key") ?? "";
  if (!c) return json({ error: "bad_code" }, 400);
  if (!/^[a-z0-9_]{3,64}$/.test(key)) return json({ error: "bad_key" }, 400);
  const h = await hash(c);
  if (op === "put") {
    const len = parseInt(req.headers.get("content-length") ?? "0", 10);
    if (len > MAX_SAVE) return json({ error: "too_big" }, 413);
    const buf = new Uint8Array(await req.arrayBuffer());
    if (buf.length === 0) return json({ error: "empty" }, 400);
    if (buf.length > MAX_SAVE) return json({ error: "too_big" }, 413);
    const b = await db.rpc("kapsula_save_begin", { p_code_hash: h, p_key: key, p_size: buf.length });
    if (b.error) throw b.error;
    if (b.data?.error) return json(b.data, b.data.error === "unknown_code" ? 404 : 400);
    const sha = await sha256hex(buf);
    const up = await db.storage.from("saves").upload(`${b.data.player}/${key}`, buf, { upsert: true, contentType: "application/octet-stream" });
    if (up.error) throw up.error;
    const d = await db.rpc("kapsula_save_done", { p_code_hash: h, p_key: key, p_sha: sha, p_size: buf.length });
    if (d.error) throw d.error;
    return json({ ok: true, sha, size: buf.length, at: d.data?.at });
  }
  const g = await db.rpc("kapsula_save_get", { p_code_hash: h, p_key: key });
  if (g.error) throw g.error;
  if (g.data?.error) return json(g.data, 404);
  const dl = await db.storage.from("saves").download(`${g.data.player}/${key}`);
  if (dl.error) throw dl.error;
  return new Response(dl.data, { status: 200, headers: { "Content-Type": "application/octet-stream", "x-kapsula-sha": g.data.sha } });
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method" }, 405);
  const op = new URL(req.url).searchParams.get("op");
  if (op === "put" || op === "get") {
    try { return await saveFile(req, op); } catch (e) { console.error("kapsula save", e); return json({ error: "server" }, 500); }
  }
  let body: any;
  try { body = await req.json(); } catch { return json({ error: "bad_json" }, 400); }
  if (JSON.stringify(body).length > 20000) return json({ error: "too_big" }, 413);
  try {
    if (body.action === "register") {
      const code = newCode();
      const { data, error } = await db.rpc("kapsula_register", { p_code_hash: await hash(norm(code)!), base: body.base ?? {} });
      if (error) throw error;
      return json({ code, state: data });
    }
    if (body.action === "profile") {
      const id = shortId(body.id);
      if (id.length < 6) return json({ error: "bad_id" }, 400);
      const { data, error } = await db.rpc("kapsula_public", { p_short_id: id });
      if (error) throw error;
      return data ? json({ profile: data }) : json({ error: "no_player" }, 404);
    }
    const c = norm(body.code);
    if (!c) return json({ error: "bad_code" }, 400);
    const h = await hash(c);
    if (body.action === "sync") {
      const { data, error } = await db.rpc("kapsula_sync", { p_code_hash: h, p: body.p ?? {} });
      if (error) throw error;
      return data ? json({ state: data }) : json({ error: "unknown_code" }, 404);
    }
    if (body.action === "restore") {
      const { data, error } = await db.rpc("kapsula_restore", { p_code_hash: h });
      if (error) throw error;
      return data ? json({ state: data }) : json({ error: "unknown_code" }, 404);
    }
    if (body.action === "poll") {
      const { data, error } = await db.rpc("kapsula_poll", { p_code_hash: h, p_after: num(body.after), p_news_after: num(body.news_after) });
      if (error) throw error;
      return json(data, data?.error ? 404 : 200);
    }
    if (body.action === "presence") {
      const { data, error } = await db.rpc("kapsula_presence", { p_code_hash: h, p: body.p ?? {} });
      if (error) throw error;
      return json(data, data?.error ? 404 : 200);
    }
    if (body.action === "cheer" || body.action === "chest") {
      const to = shortId(body.to);
      if (to.length < 6) return json({ error: "bad_id" }, 400);
      const fn = body.action === "cheer" ? "kapsula_cheer" : "kapsula_chest";
      const { data, error } = await db.rpc(fn, { p_code_hash: h, p_to: to });
      if (error) throw error;
      return json(data, data?.error ? 400 : 200);
    }
    if (body.action === "friends") {
      const { data, error } = await db.rpc("kapsula_friends", { p_code_hash: h });
      if (error) throw error;
      return json(data, data?.error ? 404 : 200);
    }
    if (body.action === "friend") {
      const to = shortId(body.to);
      if (to.length < 6) return json({ error: "bad_id" }, 400);
      if (!OPS.includes(body.op)) return json({ error: "op" }, 400);
      const { data, error } = await db.rpc("kapsula_friend", { p_code_hash: h, op: body.op, p_to: to });
      if (error) throw error;
      return json(data, data?.error ? 400 : 200);
    }
    if (body.action === "stats") {
      const { data, error } = await db.rpc("kapsula_stats", { p_code_hash: h, p: body.p ?? {} });
      if (error) throw error;
      return json(data, data?.error ? (data.error === "wait" ? 429 : 404) : 200);
    }
    if (body.action === "challenges") {
      const { data, error } = await db.rpc("kapsula_challenges", { p_code_hash: h });
      if (error) throw error;
      return json(data, data?.error ? 404 : 200);
    }
    if (body.action === "claim") {
      const { data, error } = await db.rpc("kapsula_claim", { p_code_hash: h, p_id: num(body.id) });
      if (error) throw error;
      return json(data, data?.error ? 400 : 200);
    }
    if (body.action === "invite") {
      const to = shortId(body.to);
      if (to.length < 6) return json({ error: "bad_id" }, 400);
      const { data, error } = await db.rpc("kapsula_invite", { p_code_hash: h, p_to: to, p: body.p ?? {} });
      if (error) throw error;
      return json(data, data?.error ? 400 : 200);
    }
    if (body.action === "saves") {
      const { data, error } = await db.rpc("kapsula_saves", { p_code_hash: h });
      if (error) throw error;
      return json({ used: data?.used ?? 0, items: data?.items ?? [] }, data?.error ? 404 : 200);
    }
    if (body.action === "gift") {
      const { data, error } = await db.rpc("kapsula_gift", { p_code_hash: h, p_gift: String(body.gift ?? "").slice(0, 32) });
      if (error) throw error;
      return json(data, data?.error ? (data.error === "unknown_code" ? 404 : 400) : 200);
    }
    if (body.action === "push_token") {
      // токен Firebase телефона — для push-уведомлений
      const { data, error } = await db.rpc("kapsula_push_token", { p_code_hash: h, p_token: String(body.token ?? "").slice(0, 400) });
      if (error) throw error;
      return json(data, data?.error ? (data.error === "unknown_code" ? 404 : 400) : 200);
    }
    if (body.action === "delete") {
      // профиль игрока целиком: строки — в базе, файлы сейвов — из хранилища по id
      const { data, error } = await db.rpc("kapsula_delete", { p_code_hash: h });
      if (error) throw error;
      if (data?.error) return json(data, data.error === "unknown_code" ? 404 : 400);
      const id = String(data.id);
      const list = await db.storage.from("saves").list(id, { limit: 1000 });
      const names = (list.data ?? []).map((f: { name: string }) => `${id}/${f.name}`);
      if (names.length) { const r = await db.storage.from("saves").remove(names); if (r.error) console.error("delete saves", r.error); }
      return json({ ok: true });
    }
    return json({ error: "action" }, 400);
  } catch (e) {
    console.error("kapsula", e);
    return json({ error: "server" }, 500);
  }
});
