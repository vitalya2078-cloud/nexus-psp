-- 0.50: подарки (код → предмет оформления, один раз на игрока) и удаление профиля игроком.
-- Откат: 2026-11-26_rollback_gifts_delete.sql

create table if not exists public.gifts (
  code text primary key,                 -- код подарка, заглавными (PIONEER, NY2027…)
  item text not null,                    -- id предмета оформления (fr_pioneer …)
  title text not null,                   -- «Первопроходец»
  starts timestamptz,                    -- с какого момента выдаётся (null — сразу)
  ends timestamptz,                      -- до какого (null — без конца)
  active boolean not null default false, -- выключен, пока создатель не включит
  created_at timestamptz not null default now()
);
create table if not exists public.gift_claims (
  player uuid not null references public.players(id) on delete cascade,
  code text not null references public.gifts(code) on delete cascade,
  at timestamptz not null default now(),
  primary key (player, code)
);
alter table public.gifts enable row level security;
alter table public.gift_claims enable row level security;
revoke all on public.gifts, public.gift_claims from anon, authenticated;

-- «Первопроходец» к старту на 4PDA — выключен; включить: update public.gifts set active = true, starts = now(), ends = now() + interval '30 days' where code = 'PIONEER';
insert into public.gifts (code, item, title, active) values ('PIONEER', 'fr_pioneer', 'Первопроходец', false) on conflict (code) do nothing;

create or replace function public.kapsula_gift(p_code_hash text, p_gift text)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players; g public.gifts; ins int;
begin
  select * into me from public.players where code_hash = p_code_hash for update;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  if me.banned then return jsonb_build_object('error', 'banned'); end if;
  select * into g from public.gifts where code = upper(left(coalesce(p_gift, ''), 32));
  if not found or not g.active then return jsonb_build_object('error', 'no_gift'); end if;
  if g.starts is not null and now() < g.starts then return jsonb_build_object('error', 'not_yet'); end if;
  if g.ends is not null and now() > g.ends then return jsonb_build_object('error', 'ended'); end if;
  insert into public.gift_claims (player, code) values (me.id, g.code) on conflict do nothing;
  get diagnostics ins = row_count;
  if ins = 0 then return jsonb_build_object('error', 'claimed', 'item', g.item, 'title', g.title); end if;
  if not (g.item = any(me.owned)) then
    update public.players set owned = owned || g.item where id = me.id;
  end if;
  insert into public.inbox (player, kind, title, body, data) values (me.id, 'gift', 'Подарок: ' || g.title, 'Уже в «Оформлении»', jsonb_build_object('item', g.item));
  return jsonb_build_object('ok', true, 'item', g.item, 'title', g.title);
end $function$;

-- Удаление профиля самим игроком: все его строки на сервере. Файлы сейвов в хранилище удаляет функция kapsula (по id из ответа).
create or replace function public.kapsula_delete(p_code_hash text)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me public.players;
begin
  select * into me from public.players where code_hash = p_code_hash for update;
  if not found then return jsonb_build_object('error', 'unknown_code'); end if;
  if me.is_admin then return jsonb_build_object('error', 'admin'); end if;
  delete from public.friends where a = me.id or b = me.id;
  delete from public.cheers where to_id = me.id or from_id = me.id;
  delete from public.inbox where player = me.id;
  delete from public.chest_opens where player = me.id;
  update public.admin_log set target = null where target = me.id;
  delete from public.players where id = me.id; -- ledger, cloud_saves, week_progress, challenge_claims, gift_claims — каскадом
  return jsonb_build_object('ok', true, 'id', me.id);
end $function$;

do $$ declare f text; begin
  foreach f in array array['kapsula_gift(text, text)', 'kapsula_delete(text)'] loop
    execute format('revoke all on function public.%s from public, anon, authenticated', f);
    execute format('grant execute on function public.%s to service_role', f);
  end loop;
end $$;
