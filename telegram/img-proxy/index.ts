// Запасной путь для картинок RetroAchievements (значки, аватары), если телефон не может скачать их напрямую.
// Пропускает только картинки RetroAchievements — открытым прокси не работает.
const HOSTS = new Set(["media.retroachievements.org", "i.retroachievements.org"]);
const PATH = /^\/(Badge|UserPic)\/[A-Za-z0-9_.\-]{1,64}\.png$/;

Deno.serve(async (req) => {
  let u: URL;
  try { u = new URL(new URL(req.url).searchParams.get("u") ?? ""); } catch { return new Response("bad url", { status: 400 }); }
  if (u.protocol !== "https:" || !HOSTS.has(u.hostname) || !PATH.test(u.pathname) || u.search) return new Response("not allowed", { status: 403 });
  const r = await fetch(u, { headers: { "User-Agent": "KAPSULA-img/1.0" } });
  if (!r.ok) return new Response("upstream " + r.status, { status: 502 });
  return new Response(r.body, { headers: { "Content-Type": "image/png", "Cache-Control": "public, max-age=604800" } });
});
