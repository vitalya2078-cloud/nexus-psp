-- 2026-11-19 stats: анонимная статистика. В таблице только счётчики по дням — без игрока, без названий файлов.
-- Код игрока нужен только для ограничения частоты (не чаще раза в 10 минут), в статистику он не попадает.
-- Откат: drop function public.kapsula_stats(text, jsonb); drop function public.kapsula_stats_report(int);
--        drop table public.stats_daily; alter table public.players drop column stats_at;
create table if not exists public.stats_daily (
  day date not null,
  kind text not null,
  k text not null default '',
  n bigint not null default 0,
  primary key (day, kind, k)
);
alter table public.stats_daily enable row level security;
revoke all on public.stats_daily from anon, authenticated;

alter table public.players add column if not exists stats_at timestamptz;

create or replace function public.kapsula_stats(p_code_hash text, p jsonb)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players; today date := (now() at time zone 'Europe/Moscow')::date; e jsonb; kd text; kk text; nn int; took int := 0;
  cons text[] := array['psp','ps1','ps2','nes','md','sms','gg','dc','saturn','pce','neogeo','ngp','ws','a26','a78','lynx'];
begin
  select * into me from public.players where code_hash = p_code_hash for update;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  if me.stats_at is not null and me.stats_at > now() - interval '10 minutes' then return jsonb_build_object('error', 'wait'); end if;
  update public.players set stats_at = now() where id = me.id;
  for e in select x from jsonb_array_elements(case when jsonb_typeof(p->'items') = 'array' then p->'items' else '[]' end) x limit 200 loop
    kd := e->>'kind'; kk := coalesce(e->>'k', '');
    nn := case when (e->>'n') ~ '^[0-9]{1,4}$' then least((e->>'n')::int, 500) else 0 end;
    if nn <= 0 then continue; end if;
    -- только известные события и значения
    if not (
      (kd = 'app_open' and kk = '') or
      (kd = 'game_start' and kk = any(cons)) or
      (kd = 'session' and kk in ('0-5', '5-30', '30-120', '120+')) or
      (kd = 'crash' and kk in ('game_native', 'game_anr', 'game_mem', 'main_native', 'main_anr', 'main_mem')) or
      (kd = 'add_fail' and kk in ('format', 'text', 'small', 'unread', 'nomark')) or
      (kd = 'screen' and kk in ('games', 'my', 'ctl'))
    ) then continue; end if;
    insert into public.stats_daily (day, kind, k, n) values (today, kd, kk, nn)
      on conflict (day, kind, k) do update set n = public.stats_daily.n + excluded.n;
    took := took + 1;
  end loop;
  -- уникальные игроки за день (без того, кто именно)
  if took > 0 and (me.stats_at is null or (me.stats_at at time zone 'Europe/Moscow')::date < today) then
    insert into public.stats_daily (day, kind, k, n) values (today, 'players', '', 1)
      on conflict (day, kind, k) do update set n = public.stats_daily.n + 1;
  end if;
  return jsonb_build_object('ok', true, 'took', took);
end $function$;
revoke all on function public.kapsula_stats(text, jsonb) from public, anon, authenticated;
grant execute on function public.kapsula_stats(text, jsonb) to service_role;

-- отчёт для KAPSULA Админ: по дням за N суток
create or replace function public.kapsula_stats_report(p_days int)
returns jsonb language sql security definer set search_path to '' as $function$
  select coalesce(jsonb_agg(jsonb_build_object('day', day, 'kind', kind, 'k', k, 'n', n) order by day desc, kind, k), '[]')
  from public.stats_daily where day > (now() at time zone 'Europe/Moscow')::date - least(greatest(p_days, 1), 90);
$function$;
revoke all on function public.kapsula_stats_report(int) from public, anon, authenticated;
grant execute on function public.kapsula_stats_report(int) to service_role;
