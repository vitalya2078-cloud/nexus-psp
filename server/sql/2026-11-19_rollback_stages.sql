-- ОТКАТ 2026-11-19_stages: kapsula_sync — как после token_limits (без учёта испытаний); новые функции убрать.
-- Таблицы (cloud_saves, challenges, week_progress, challenge_claims) и bucket saves не удаляются — в них данные игроков.
CREATE OR REPLACE FUNCTION public.kapsula_sync(p_code_hash text, p jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare
  r public.players;
  today date := (now() at time zone 'Europe/Moscow')::date;
  got jsonb := '[]';
  el numeric; wall int; d_play int; d_saves int; n_games int; give int; chunks int; lv int; n int; k text; v int; cost int; item text;
  old_cons jsonb; new_cons jsonb; mult numeric; earned int;
  extras jsonb := public.kapsula_extras(nullif(p->>'news_after', '')::bigint);
begin
  select * into r from public.players where code_hash = p_code_hash for update;
  if not found then return null; end if;
  if p ? 'ver' then r.app_version := left(p->>'ver', 20); end if;

  -- заблокированный: только состояние, без начислений и покупок
  if r.banned then
    update public.players set seen_at = now(), app_version = r.app_version where players.id = r.id;
    return public.kapsula_state_of(r) || extras;
  end if;

  if r.earn_day is distinct from today then r.earn_day := today; r.earn_play := 0; r.earn_games := 0;
    r.earn_saves := 0; r.earn_level := 0; r.earn_min := 0; end if;

  -- время в игре: не больше, чем прошло с прошлой синхронизации (+1 мин на округление), и не больше 20 ч в сутки.
  -- seen_at двигают presence/poll во время игры, поэтому меряем от sync_at; у старых записей — по-старому от seen_at + 60
  if r.sync_at is null then
    wall := greatest(0, extract(epoch from (now() - r.seen_at))::int / 60) + 60;
  else
    el := extract(epoch from (now() - r.sync_at));
    wall := ceil(greatest(el, 0) / 60)::int + 1;
  end if;
  d_play := least(greatest(coalesce((p->>'d_play')::int, 0), 0), wall, greatest(0, 1200 - r.earn_min));
  r.earn_min := r.earn_min + d_play;
  chunks := (r.play + d_play) / 15 - r.play / 15;
  r.play := r.play + d_play;
  give := least(chunks * 5, greatest(0, 60 - r.earn_play));
  if give > 0 then r.earn_play := r.earn_play + give; r.balance := r.balance + give; got := got || jsonb_build_object('reason', 'Время в игре', 'amount', give); end if;

  -- сохранения: в уровень идёт не больше 40 в сутки
  d_saves := least(greatest(coalesce((p->>'d_saves')::int, 0), 0), greatest(0, 40 - r.earn_saves));
  r.earn_saves := r.earn_saves + d_saves;
  r.saves := r.saves + d_saves;
  lv := (r.play * 2 + r.saves * 30) / 1000 + 1;
  if lv > r.level then
    -- за уровни — не больше 300 жетонов в сутки; уровень растёт в любом случае
    give := least(100 * (lv - r.level), greatest(0, 300 - r.earn_level)); r.level := lv;
    if give > 0 then
      r.earn_level := r.earn_level + give; r.balance := r.balance + give;
      got := got || jsonb_build_object('reason', 'Уровень ' || lv, 'amount', give);
    end if;
  end if;

  old_cons := r.consoles; new_cons := r.consoles; n := 0;
  for k, v in select key, least(greatest(coalesce(value::text::int, 0), 0), d_play) from jsonb_each(coalesce(p->'cons', '{}')) loop
    if not (k = any(array['psp','ps1','ps2','nes','md','sms','gg','dc','saturn','pce','neogeo','ngp','ws','a26','a78','lynx'])) then continue; end if; -- только настоящие консоли KAPSULA
    if v > 0 and not (old_cons ? k) then n := n + 1; end if;
    new_cons := jsonb_set(new_cons, array[k], to_jsonb(coalesce((new_cons->>k)::int, 0) + v));
  end loop;
  r.consoles := new_cons;
  if n > 0 then r.balance := r.balance + 30 * n; got := got || jsonb_build_object('reason', 'Новая консоль', 'amount', 30 * n); end if;

  n_games := least(greatest(coalesce((p->>'games')::int, 0), 0), 5000);
  if n_games > r.games then
    give := least((n_games - r.games) * 5, greatest(0, 50 - r.earn_games));
    if give > 0 then r.earn_games := r.earn_games + give; r.balance := r.balance + give; got := got || jsonb_build_object('reason', 'Новые игры', 'amount', give); end if;
    r.games := n_games;
  end if;
  r.streak := greatest(r.streak, least(greatest(coalesce((p->>'streak')::int, 0), 0), 3650));

  if r.visit_day is distinct from today then
    r.visit_streak := case when r.visit_day = today - 1 then r.visit_streak + 1 else 1 end;
    give := least(50, 10 + (r.visit_streak - 1) * 5);
    r.visit_day := today; r.balance := r.balance + give;
    got := got || jsonb_build_object('reason', case when r.visit_streak > 1 then 'Заход ' || r.visit_streak || '-й день подряд' else 'Заход сегодня' end, 'amount', give);
  end if;

  -- событие «×N жетонов»: прибавка ко всему заработанному сейчас
  mult := nullif(extras->'event'->>'mult', '')::numeric;
  if mult is not null and mult > 1 and jsonb_array_length(got) > 0 then
    select sum((e->>'amount')::int) into earned from jsonb_array_elements(got) e;
    give := least(floor(earned * (least(mult, 5) - 1))::int, 500);
    if give > 0 then r.balance := r.balance + give; got := got || jsonb_build_object('reason', coalesce(extras->'event'->>'title', 'Событие'), 'amount', give); end if;
  end if;

  if jsonb_array_length(got) > 0 then
    insert into public.ledger (player, reason, amount) select r.id, e->>'reason', (e->>'amount')::int from jsonb_array_elements(got) e;
  end if;

  for item in select jsonb_array_elements_text(coalesce(p->'buys', '[]')) loop
    if item = any(r.owned) then continue; end if;
    cost := null;
    select c.price into cost from public.cos_prices c where c.id = item;
    if cost is null or r.balance < cost then continue; end if;
    r.balance := r.balance - cost; r.owned := r.owned || item;
    insert into public.ledger (player, reason, amount) values (r.id, 'buy:' || item, -cost);
  end loop;

  if jsonb_typeof(p->'library') = 'array' then
    r.library := coalesce((select jsonb_agg(jsonb_build_object('t', left(e->>'t', 60), 'p', left(e->>'p', 12),
        'm', case when e->>'m' ~ '^[0-9]{1,7}$' then (e->>'m')::int else 0 end,
        'f', coalesce(e->>'f' = 'true', false),
        'l', case when e->>'l' ~ '^[0-9]{1,14}$' then (e->>'l')::bigint else 0 end))
      from (select e from jsonb_array_elements(p->'library') e where jsonb_typeof(e) = 'object' limit 200) x), '[]');
  end if;
  if p ? 'lib_public' then r.lib_public := coalesce(p->>'lib_public' = 'true', true); end if;

  if p ? 'profile' then
    r.profile := jsonb_strip_nulls(jsonb_build_object(
      'nick', coalesce(public.kapsula_nick_ok(p->'profile'->>'nick', r.is_admin), r.profile->>'nick'), 'motto', left(p->'profile'->>'motto', 48),
      'avMode', left(p->'profile'->>'avMode', 10), 'avItem', left(p->'profile'->>'avItem', 24),
      'avColor', case when (p->'profile'->>'avColor') ~ '^[0-9]{1,2}$' then (p->'profile'->>'avColor')::int end,
      'frame', left(p->'profile'->>'frame', 24), 'banner', left(p->'profile'->>'banner', 24), 'titleSel', left(p->'profile'->>'titleSel', 24),
      'show', (select jsonb_agg(jsonb_build_object('t', left(e->>'t', 60), 'p', left(e->>'p', 12)))
               from (select e from jsonb_array_elements(case when jsonb_typeof(p->'profile'->'show') = 'array' then p->'profile'->'show' else '[]' end) e limit 3) x),
      'badges', (select jsonb_agg(left(b, 24)) from (select jsonb_array_elements_text(case when jsonb_typeof(p->'profile'->'badges') = 'array' then p->'profile'->'badges' else '[]' end) b limit 6) y)));
  end if;

  r.seen_at := now(); r.sync_at := r.seen_at;
  update public.players set seen_at = r.seen_at, sync_at = r.sync_at, earn_saves = r.earn_saves, earn_level = r.earn_level, earn_min = r.earn_min, balance = r.balance, owned = r.owned, profile = r.profile, library = r.library, lib_public = r.lib_public, play = r.play, saves = r.saves,
    games = r.games, streak = r.streak, consoles = r.consoles, level = r.level, earn_day = r.earn_day, earn_play = r.earn_play,
    earn_games = r.earn_games, visit_day = r.visit_day, visit_streak = r.visit_streak, app_version = r.app_version where players.id = r.id;
  return public.kapsula_state_of(r, got) || extras;
end $function$;

drop function if exists public.kapsula_invite(text, text, jsonb);
drop function if exists public.kapsula_admin_ext(text, text, jsonb);
drop function if exists public.kapsula_claim(text, bigint);
drop function if exists public.kapsula_challenges(text);
drop function if exists public.kapsula_save_get(text, text);
drop function if exists public.kapsula_save_done(text, text, text, int);
drop function if exists public.kapsula_save_begin(text, text, int);
drop function if exists public.kapsula_saves(text);
