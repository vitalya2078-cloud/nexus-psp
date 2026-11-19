-- 2026-11-19 stages: облачные сохранения (этап 2), недельные испытания (этап 3), «Позвать в игру» (этап 4), действия админки.
-- Откат: server/sql/2026-11-19_rollback_stages.sql

-- ===== этап 2: облачные сохранения =====
-- файлы — в приватном хранилище saves/<player>/<key> (до 2 МБ), здесь — опись. Доступ только через edge function kapsula.
insert into storage.buckets (id, name, public, file_size_limit) values ('saves', 'saves', false, 2097152) on conflict (id) do nothing;

create table if not exists public.cloud_saves (
  player uuid not null references public.players(id) on delete cascade,
  key text not null,
  sha text not null,
  size int not null,
  updated_at timestamptz not null default now(),
  primary key (player, key)
);
alter table public.cloud_saves enable row level security;
revoke all on public.cloud_saves from anon, authenticated;

create or replace function public.kapsula_saves(p_code_hash text)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players;
begin
  select * into me from public.players where code_hash = p_code_hash;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  return jsonb_build_object('player', me.id, 'used', (select coalesce(sum(size), 0) from public.cloud_saves where player = me.id),
    'items', coalesce((select jsonb_agg(jsonb_build_object('key', key, 'sha', sha, 'size', size, 'at', updated_at)) from public.cloud_saves where player = me.id), '[]'));
end $function$;

-- можно ли положить файл: ключ, размер (до 2 МБ), место (до 20 МБ на игрока, до 300 файлов)
create or replace function public.kapsula_save_begin(p_code_hash text, p_key text, p_size int)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players; used bigint; cnt int;
begin
  select * into me from public.players where code_hash = p_code_hash;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  if me.banned then return jsonb_build_object('error', 'banned'); end if;
  if p_key !~ '^[a-z0-9_]{3,64}$' then return jsonb_build_object('error', 'bad_key'); end if;
  if p_size <= 0 or p_size > 2097152 then return jsonb_build_object('error', 'too_big'); end if;
  select coalesce(sum(size), 0), count(*) into used, cnt from public.cloud_saves where player = me.id and key <> p_key;
  if used + p_size > 20971520 or cnt >= 300 then return jsonb_build_object('error', 'quota'); end if;
  return jsonb_build_object('player', me.id);
end $function$;

create or replace function public.kapsula_save_done(p_code_hash text, p_key text, p_sha text, p_size int)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players;
begin
  select * into me from public.players where code_hash = p_code_hash;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  if p_key !~ '^[a-z0-9_]{3,64}$' or p_sha !~ '^[0-9a-f]{64}$' then return jsonb_build_object('error', 'bad_key'); end if;
  insert into public.cloud_saves (player, key, sha, size, updated_at) values (me.id, p_key, p_sha, p_size, now())
    on conflict (player, key) do update set sha = excluded.sha, size = excluded.size, updated_at = now();
  return jsonb_build_object('ok', true, 'at', now());
end $function$;

create or replace function public.kapsula_save_get(p_code_hash text, p_key text)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players; s public.cloud_saves;
begin
  select * into me from public.players where code_hash = p_code_hash;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  select * into s from public.cloud_saves where player = me.id and key = p_key;
  if not found then return jsonb_build_object('error', 'no_save'); end if;
  return jsonb_build_object('player', me.id, 'sha', s.sha, 'size', s.size);
end $function$;

-- ===== этап 3: недельные испытания =====
create table if not exists public.challenges (
  id bigserial primary key,
  week date not null,
  kind text not null check (kind in ('play_min', 'consoles', 'days')),
  target int not null check (target > 0),
  reward int not null check (reward between 1 and 500),
  title text not null,
  active boolean not null default true,
  created_at timestamptz not null default now()
);
create index if not exists challenges_week on public.challenges (week);
create table if not exists public.week_progress (
  player uuid not null references public.players(id) on delete cascade,
  week date not null,
  play_min int not null default 0,
  consoles text[] not null default '{}',
  days date[] not null default '{}',
  primary key (player, week)
);
create table if not exists public.challenge_claims (
  player uuid not null references public.players(id) on delete cascade,
  challenge bigint not null references public.challenges(id) on delete cascade,
  at timestamptz not null default now(),
  primary key (player, challenge)
);
create index if not exists challenge_claims_ch on public.challenge_claims (challenge);
alter table public.challenges enable row level security;
alter table public.week_progress enable row level security;
alter table public.challenge_claims enable row level security;
revoke all on public.challenges, public.week_progress, public.challenge_claims from anon, authenticated;

create or replace function public.kapsula_week() returns date language sql stable set search_path to '' as $function$
  select date_trunc('week', now() at time zone 'Europe/Moscow')::date;
$function$;

create or replace function public.kapsula_week_track(p_player uuid, p_min int, p_cons text[])
returns void language plpgsql security definer set search_path to '' as $function$
declare w date := public.kapsula_week(); today date := (now() at time zone 'Europe/Moscow')::date;
begin
  insert into public.week_progress (player, week) values (p_player, w) on conflict do nothing;
  update public.week_progress set
    play_min = play_min + greatest(p_min, 0),
    consoles = (select coalesce(array_agg(distinct c), '{}') from unnest(consoles || coalesce(p_cons, '{}')) c where c = any(array['psp','ps1','ps2','nes','md','sms','gg','dc','saturn','pce','neogeo','ngp','ws','a26','a78','lynx'])),
    days = case when p_min > 0 and not (today = any(days)) then days || today else days end
  where player = p_player and week = w;
end $function$;

-- нет испытаний на эту неделю — три по умолчанию (админ может заменить)
create or replace function public.kapsula_challenges_ensure()
returns void language plpgsql security definer set search_path to '' as $function$
declare w date := public.kapsula_week();
begin
  perform pg_advisory_xact_lock(4207);
  if exists (select 1 from public.challenges where week = w) then return; end if;
  insert into public.challenges (week, kind, target, reward, title) values
    (w, 'play_min', 300, 60, 'Наиграй 5 часов'),
    (w, 'days', 4, 50, 'Играй 4 разных дня'),
    (w, 'consoles', 2, 40, 'Сыграй на 2 консолях');
end $function$;

create or replace function public.kapsula_challenges(p_code_hash text)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players; w date := public.kapsula_week(); pr public.week_progress;
begin
  select * into me from public.players where code_hash = p_code_hash;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  perform public.kapsula_challenges_ensure();
  select * into pr from public.week_progress where player = me.id and week = w;
  return jsonb_build_object('week', w, 'until', (w + 7)::timestamp at time zone 'Europe/Moscow',
    'items', coalesce((select jsonb_agg(jsonb_build_object('id', c.id, 'kind', c.kind, 'title', c.title, 'target', c.target, 'reward', c.reward,
        'progress', least(c.target, case c.kind when 'play_min' then coalesce(pr.play_min, 0) when 'consoles' then coalesce(cardinality(pr.consoles), 0) else coalesce(cardinality(pr.days), 0) end),
        'claimed', exists (select 1 from public.challenge_claims x where x.player = me.id and x.challenge = c.id)) order by c.id)
      from public.challenges c where c.week = w and c.active), '[]'));
end $function$;

create or replace function public.kapsula_claim(p_code_hash text, p_id bigint)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players; c public.challenges; pr public.week_progress; have int; ins int;
begin
  select * into me from public.players where code_hash = p_code_hash for update;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  if me.banned then return jsonb_build_object('error', 'banned'); end if;
  select * into c from public.challenges where id = p_id and active and week = public.kapsula_week();
  if not found then return jsonb_build_object('error', 'no_challenge'); end if;
  select * into pr from public.week_progress where player = me.id and week = c.week;
  have := case c.kind when 'play_min' then coalesce(pr.play_min, 0) when 'consoles' then coalesce(cardinality(pr.consoles), 0) else coalesce(cardinality(pr.days), 0) end;
  if have < c.target then return jsonb_build_object('error', 'not_yet'); end if;
  insert into public.challenge_claims (player, challenge) values (me.id, c.id) on conflict do nothing;
  get diagnostics ins = row_count;
  if ins = 0 then return jsonb_build_object('error', 'claimed'); end if;
  update public.players set balance = balance + c.reward where id = me.id;
  insert into public.ledger (player, reason, amount) values (me.id, 'Испытание: ' || c.title, c.reward);
  return jsonb_build_object('ok', true, 'amount', c.reward, 'balance', me.balance + c.reward);
end $function$;

-- ===== этап 4: «Позвать в игру» (одна Wi-Fi сеть) =====
create or replace function public.kapsula_invite(p_code_hash text, p_to text, p jsonb)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players; t public.players; ip text := coalesce(p->>'ip', ''); ti text := left(coalesce(p->>'t', ''), 60); pl text := coalesce(p->>'p', '');
begin
  select * into me from public.players where code_hash = p_code_hash;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  if me.banned then return jsonb_build_object('error', 'banned'); end if;
  select * into t from public.players where short_id = upper(left(p_to, 10));
  if not found then return jsonb_build_object('error', 'no_player'); end if;
  if t.id = me.id then return jsonb_build_object('error', 'self'); end if;
  if not exists (select 1 from public.friends f where f.status = 'accepted' and ((f.a = me.id and f.b = t.id) or (f.a = t.id and f.b = me.id))) then
    return jsonb_build_object('error', 'not_friends');
  end if;
  if ip !~ '^(25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])(\.(25[0-5]|2[0-4][0-9]|1?[0-9]?[0-9])){3}$' or ti = '' or not (pl = any(array['psp','ps1','ps2','nes','md','sms','gg','dc','saturn','pce','neogeo','ngp','ws','a26','a78','lynx'])) then
    return jsonb_build_object('error', 'bad_invite');
  end if;
  if (select count(*) from public.inbox i where i.kind = 'invite' and i.data->>'from' = me.short_id and i.at > now() - interval '1 day') >= 30 then
    return jsonb_build_object('error', 'daily');
  end if;
  perform public.kapsula_inbox_add(t.id, 'invite', left(coalesce(nullif(me.profile->>'nick', ''), me.short_id), 24) || ' зовёт в «' || ti || '»',
    'Одна Wi-Fi сеть с другом · нажми «Присоединиться»', jsonb_build_object('from', me.short_id, 't', ti, 'p', pl, 'ip', ip));
  return jsonb_build_object('ok', true);
end $function$;

-- ===== админка: статистика и испытания (вызывается edge function kapsula-admin после проверки PIN) =====
create or replace function public.kapsula_admin_ext(p_code_hash text, action text, args jsonb)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players; res jsonb; k text; tg int; rw int; w date := public.kapsula_week();
begin
  select * into me from public.players where code_hash = p_code_hash;
  if not found or not me.is_admin then return jsonb_build_object('error', 'not_admin'); end if;
  case action
  when 'stats' then res := public.kapsula_stats_report(coalesce((args->>'days')::int, 7));
  when 'ch_list' then
    perform public.kapsula_challenges_ensure();
    res := coalesce((select jsonb_agg(jsonb_build_object('id', c.id, 'kind', c.kind, 'title', c.title, 'target', c.target, 'reward', c.reward, 'active', c.active,
      'claims', (select count(*) from public.challenge_claims x where x.challenge = c.id)) order by c.id) from public.challenges c where c.week = w), '[]');
  when 'ch_set' then
    k := args->>'kind'; tg := coalesce((args->>'target')::int, 0); rw := coalesce((args->>'reward')::int, 0);
    if k not in ('play_min', 'consoles', 'days') then return jsonb_build_object('error', 'bad_kind'); end if;
    if (k = 'play_min' and tg not between 30 and 3000) or (k = 'consoles' and tg not between 1 and 16) or (k = 'days' and tg not between 1 and 7) then
      return jsonb_build_object('error', 'bad_target'); end if;
    if rw not between 10 and 300 then return jsonb_build_object('error', 'bad_reward'); end if;
    perform public.kapsula_challenges_ensure();
    insert into public.challenges (week, kind, target, reward, title)
      values (w, k, tg, rw, left(coalesce(nullif(trim(args->>'title'), ''), case k when 'play_min' then 'Наиграй ' || tg || ' мин' when 'consoles' then 'Сыграй на ' || tg || ' консолях' else 'Играй ' || tg || ' разных дня' end), 40))
      returning jsonb_build_object('id', id) into res;
    insert into public.admin_log (admin, action, details) values (me.id, 'ch_set', args);
  when 'ch_off' then
    update public.challenges set active = false where id = (args->>'id')::bigint;
    insert into public.admin_log (admin, action, details) values (me.id, 'ch_off', args);
    res := '{}';
  else return jsonb_build_object('error', 'action');
  end case;
  return jsonb_build_object('ok', true, 'data', res);
end $function$;

-- ===== kapsula_sync: + учёт для недельных испытаний =====
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
  old_cons jsonb; new_cons jsonb; mult numeric; earned int; played text[] := '{}';
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
    if v > 0 then played := played || k; end if;
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

  -- недельные испытания: принятые минуты и консоли
  if d_play > 0 or cardinality(played) > 0 then perform public.kapsula_week_track(r.id, d_play, played); end if;

  r.seen_at := now(); r.sync_at := r.seen_at;
  update public.players set seen_at = r.seen_at, sync_at = r.sync_at, earn_saves = r.earn_saves, earn_level = r.earn_level, earn_min = r.earn_min, balance = r.balance, owned = r.owned, profile = r.profile, library = r.library, lib_public = r.lib_public, play = r.play, saves = r.saves,
    games = r.games, streak = r.streak, consoles = r.consoles, level = r.level, earn_day = r.earn_day, earn_play = r.earn_play,
    earn_games = r.earn_games, visit_day = r.visit_day, visit_streak = r.visit_streak, app_version = r.app_version where players.id = r.id;
  return public.kapsula_state_of(r, got) || extras;
end $function$;

-- права: только сервер (service_role)
do $g$ declare f text; begin
  foreach f in array array['kapsula_saves(text)', 'kapsula_save_begin(text,text,int)', 'kapsula_save_done(text,text,text,int)', 'kapsula_save_get(text,text)',
    'kapsula_week()', 'kapsula_week_track(uuid,int,text[])', 'kapsula_challenges_ensure()', 'kapsula_challenges(text)', 'kapsula_claim(text,bigint)',
    'kapsula_invite(text,text,jsonb)', 'kapsula_admin_ext(text,text,jsonb)'] loop
    execute format('revoke all on function public.%s from public, anon, authenticated', f);
    execute format('grant execute on function public.%s to service_role', f);
  end loop;
end $g$;
