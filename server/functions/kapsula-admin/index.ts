// KAPSULA Админ: все действия проверяет функция базы kapsula_admin (код аккаунта + PIN + телефон, пауза после 5 ошибок, журнал).
// Здесь только хэши: код и PIN на сервере не хранятся.
// creator_set (оформление создателя) и EXT (статистика, испытания, пуши, советы, подарки) — только после успешной проверки той же функцией (ping).
import { createClient } from "jsr:@supabase/supabase-js@2";

const db = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!, { auth: { persistSession: false } });
const ABC = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
const EXT = ["stats", "ch_list", "ch_set", "ch_off", "push_send", "push_list", "tips_list", "tip_set", "tip_del", "tip_send", "tips_cfg", "news_post", "gifts", "gift_set"];

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json; charset=utf-8" } });
}
function norm(code: unknown, prefix: string, len: number): string | null {
  if (typeof code !== "string") return null;
  let s = code.toUpperCase().replace(/[^A-Z0-9]/g, "");
  if (s.startsWith(prefix) && s.length === len + prefix.length) s = s.slice(prefix.length);
  if (s.length !== len || [...s].some((c) => !ABC.includes(c))) return null;
  return s;
}
async function sha(s: string): Promise<string> {
  const d = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(s));
  return Array.from(new Uint8Array(d), (x) => x.toString(16).padStart(2, "0")).join("");
}
const status = (data: any) => data?.error ? (data.error === "bad_pin" || data.error === "locked" || data.error === "not_admin" ? 403 : 400) : 200;

Deno.serve(async (req) => {
  if (req.method !== "POST") return json({ error: "method" }, 405);
  let b: any;
  try { b = await req.json(); } catch { return json({ error: "bad_json" }, 400); }
  if (JSON.stringify(b).length > 8000) return json({ error: "too_big" }, 413);
  const code = norm(b.code, "KPS", 12);
  const pin = typeof b.pin === "string" && /^[0-9]{6,12}$/.test(b.pin) ? b.pin : null;
  const device = typeof b.device === "string" ? b.device.slice(0, 64) : "";
  const action = typeof b.action === "string" ? b.action.slice(0, 20) : "";
  if (!code) return json({ error: "bad_code" }, 400);
  if (!pin) return json({ error: "bad_pin_format" }, 400);
  if (!device) return json({ error: "no_device" }, 400);
  const args = typeof b.args === "object" && b.args ? { ...b.args } : {};
  if (action === "setup") {
    const t = norm(args.token, "SETUP", 16);
    if (!t) return json({ error: "bad_setup" }, 400);
    args.token_hash = await sha("kapsula-boot:" + t);
    delete args.token;
  }
  try {
    const codeHash = await sha("kapsula:" + code);
    const pinHash = await sha("kapsula-pin:" + code + ":" + pin);
    if (action === "creator_set" || EXT.includes(action)) {
      const auth = await db.rpc("kapsula_admin", { p_code_hash: codeHash, p_pin_hash: pinHash, p_device: device, action: "ping", args: {} });
      if (auth.error) throw auth.error;
      if (!auth.data?.ok) return json(auth.data, status(auth.data));
      const { data, error } = action === "creator_set"
        ? await db.rpc("kapsula_creator_set", { p_code_hash: codeHash, args })
        : await db.rpc("kapsula_admin_ext", { p_code_hash: codeHash, action, args });
      if (error) throw error;
      return json(data, status(data));
    }
    const { data, error } = await db.rpc("kapsula_admin", { p_code_hash: codeHash, p_pin_hash: pinHash, p_device: device, action, args });
    if (error) throw error;
    return json(data, status(data));
  } catch (e) {
    console.error("kapsula-admin", e);
    return json({ error: "server" }, 500);
  }
});
