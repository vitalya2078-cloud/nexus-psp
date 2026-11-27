// KAPSULA push: вызывается триггером базы (pg_net) на новое уведомление (inbox) или новость (news) и отправляет push
// через Firebase Cloud Messaging (HTTP v1). Защита без пароля: kapsula_push_take отдаёт уведомление один раз
// и только свежее (10 минут) — повторный вызов ничего не отправит.
// Ключ сервисного аккаунта Firebase — секрет FCM_SERVICE_ACCOUNT (JSON целиком).
import { createClient } from "jsr:@supabase/supabase-js@2";

const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false } });

type SA = { project_id: string; client_email: string; private_key: string };
let cached: { token: string; until: number } | null = null;

function b64url(b: Uint8Array | string): string {
  const s = typeof b === "string" ? btoa(unescape(encodeURIComponent(b))) : btoa(String.fromCharCode(...b));
  return s.replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

/** Токен доступа Google OAuth по ключу сервисного аккаунта (JWT RS256), кэш на ~50 минут. */
async function accessToken(sa: SA): Promise<string> {
  if (cached && cached.until > Date.now()) return cached.token;
  const now = Math.floor(Date.now() / 1000);
  const head = b64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = b64url(JSON.stringify({ iss: sa.client_email, scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token", iat: now, exp: now + 3600 }));
  const pem = sa.private_key.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(pem), (c) => c.charCodeAt(0));
  const key = await crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const sig = new Uint8Array(await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(`${head}.${claim}`)));
  const r = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST", headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${head}.${claim}.${b64url(sig)}` }),
  });
  const j = await r.json();
  if (!j.access_token) throw new Error("oauth: " + JSON.stringify(j).slice(0, 200));
  cached = { token: j.access_token, until: Date.now() + 50 * 60 * 1000 };
  return j.access_token;
}

/** Одно сообщение FCM. Только data (без notification): приложение само показывает уведомление — так оно приходит
 *  и в открытое приложение (плашкой), и в закрытое (системным), без дублей с фоновой проверкой. */
async function send(sa: SA, target: { token?: string; topic?: string }, data: Record<string, string>): Promise<number> {
  const r = await fetch(`https://fcm.googleapis.com/v1/projects/${sa.project_id}/messages:send`, {
    method: "POST", headers: { "Authorization": `Bearer ${await accessToken(sa)}`, "Content-Type": "application/json" },
    body: JSON.stringify({ message: { ...target, data, android: { priority: "HIGH", ttl: "86400s" } } }),
  });
  if (r.status >= 400) console.error("fcm", r.status, (await r.text()).slice(0, 300));
  return r.status;
}

Deno.serve(async (req) => {
  try {
    const raw = Deno.env.get("FCM_SERVICE_ACCOUNT");
    if (!raw) return new Response("no FCM_SERVICE_ACCOUNT", { status: 503 });
    const sa = JSON.parse(raw) as SA;
    const body = await req.json().catch(() => ({}));
    const kind = body.kind === "news" ? "news" : body.kind === "inbox" ? "inbox" : null;
    const id = Number(body.id);
    if (!kind || !Number.isFinite(id) || id <= 0) return new Response("bad", { status: 400 });
    const { data: it, error } = await db.rpc("kapsula_push_take", { p_kind: kind, p_id: id });
    if (error) throw error;
    if (!it) return new Response("skip");
    const data = { kind: String(it.kind ?? kind), id: String(it.id), title: String(it.title ?? "").slice(0, 120), body: String(it.body ?? "").slice(0, 400), src: kind };
    if (kind === "news") { await send(sa, { topic: "news" }, data); return new Response("ok"); }
    const gone: string[] = [];
    for (const t of (it.tokens ?? []) as string[]) {
      const st = await send(sa, { token: t }, data);
      if (st === 404 || st === 400) gone.push(t); // токен устарел или приложение удалено
    }
    if (gone.length) await db.rpc("kapsula_push_forget", { p_tokens: gone });
    return new Response("ok");
  } catch (e) {
    console.error("push", e);
    return new Response("error", { status: 500 });
  }
});
