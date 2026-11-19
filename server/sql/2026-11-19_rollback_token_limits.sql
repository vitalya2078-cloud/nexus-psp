-- ОТКАТ миграции 2026-11-19_token_limits: возвращает kapsula_sync и kapsula_register как были до неё.
-- Новые столбцы (sync_at, earn_saves, earn_level, earn_min) не удаляются — старым функциям они не мешают.

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
  wall int; d_play int; d_saves int; n_games int; give int; chunks int; lv int; n int; k text; v int; cost int; item text;
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

  if r.earn_day is distinct from today then r.earn_day := today; r.earn_play := 0; r.earn_games := 0; end if;

  wall := greatest(0, extract(epoch from (now() - r.seen_at))::int / 60) + 60;
  d_play := least(greatest(coalesce((p->>'d_play')::int, 0), 0), wall);
  chunks := (r.play + d_play) / 15 - r.play / 15;
  r.play := r.play + d_play;
  give := least(chunks * 5, greatest(0, 60 - r.earn_play));
  if give > 0 then r.earn_play := r.earn_play + give; r.balance := r.balance + give; got := got || jsonb_build_object('reason', 'Время в игре', 'amount', give); end if;

  d_saves := least(greatest(coalesce((p->>'d_saves')::int, 0), 0), 1000);
  r.saves := r.saves + d_saves;
  lv := (r.play * 2 + r.saves * 30) / 1000 + 1;
  if lv > r.level then
    give := 100 * (lv - r.level); r.balance := r.balance + give; r.level := lv;
    got := got || jsonb_build_object('reason', 'Уровень ' || lv, 'amount', give);
  end if;

  old_cons := r.consoles; new_cons := r.consoles; n := 0;
  for k, v in select key, least(greatest(coalesce(value::text::int, 0), 0), d_play) from jsonb_each(coalesce(p->'cons', '{}')) loop
    if length(k) > 12 then continue; end if;
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

  r.seen_at := now();
  update public.players set seen_at = r.seen_at, balance = r.balance, owned = r.owned, profile = r.profile, library = r.library, lib_public = r.lib_public, play = r.play, saves = r.saves,
    games = r.games, streak = r.streak, consoles = r.consoles, level = r.level, earn_day = r.earn_day, earn_play = r.earn_play,
    earn_games = r.earn_games, visit_day = r.visit_day, visit_streak = r.visit_streak, app_version = r.app_version where players.id = r.id;
  return public.kapsula_state_of(r, got) || extras;
end $function$;

CREATE OR REPLACE FUNCTION public.kapsula_register(p_code_hash text, base jsonb)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
declare r public.players; cons jsonb := '{}'; k text; v int; bal int; own text[];
begin
  for k, v in select key, least(greatest(coalesce(value::text::int, 0), 0), 500000) from jsonb_each(coalesce(base->'cons', '{}')) loop
    if length(k) <= 12 then cons := jsonb_set(cons, array[k], to_jsonb(v)); end if;
  end loop;
  bal := greatest(100, least(coalesce((base->>'balance')::int, 0), 3000));
  select coalesce(array_agg(c.id), '{}') into own from public.cos_prices c
    where c.id in (select jsonb_array_elements_text(coalesce(base->'owned', '[]')));
  insert into public.players (code_hash, balance, owned, play, saves, games, streak, consoles, level)
  values (p_code_hash, bal, own,
    least(greatest(coalesce((base->>'play')::int, 0), 0), 500000),
    least(greatest(coalesce((base->>'saves')::int, 0), 0), 100000),
    least(greatest(coalesce((base->>'games')::int, 0), 0), 5000),
    least(greatest(coalesce((base->>'streak')::int, 0), 0), 3650),
    cons, 1)
  returning * into r;
  r.level := (r.play * 2 + r.saves * 30) / 1000 + 1;
  update public.players set level = r.level where id = r.id;
  insert into public.ledger (player, reason, amount) values (r.id, case when bal > 100 then 'carry' else 'welcome' end, bal);
  return public.kapsula_state_of(r, case when bal > 100 then '[]'::jsonb else '[{"reason":"Добро пожаловать","amount":100}]'::jsonb end);
end $function$;
