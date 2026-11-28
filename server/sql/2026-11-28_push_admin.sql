-- 0.51.1 / Админ 1.3.0: свои пуши (всем и одному игроку), советы-пуши по расписанию, новости с заголовком и картинкой, подарки в админке.
-- Откат: 2026-11-28_rollback_push_admin.sql

create extension if not exists pg_cron;

-- пуш всем: свой (админ) или совет из списка. Триггер отправляет его через функцию push (тема news или tips).
create table if not exists public.push_msgs (
  id bigserial primary key,
  title text not null,
  body text not null default '',
  image text,
  tip int,                                -- id совета (push_tips), null — свой пуш админа
  created_at timestamptz not null default now(),
  pushed boolean not null default false
);
alter table public.push_msgs enable row level security;

create table if not exists public.push_tips (
  id serial primary key,
  title text not null,
  body text not null,
  image text,
  active boolean not null default true,
  sent int not null default 0,
  last_sent timestamptz
);
alter table public.push_tips enable row level security;

alter table public.news add column if not exists title text;

drop trigger if exists kapsula_push_msg on public.push_msgs;
create trigger kapsula_push_msg after insert on public.push_msgs for each row execute function public.kapsula_push_notify('msg');

-- расписание советов: включено, часы по Москве; last — последний отправленный слот
insert into public.app_settings (key, value) values ('tips', '{"enabled": true, "hours": [10, 16, 21]}') on conflict (key) do nothing;

create or replace function public.kapsula_push_take(p_kind text, p_id bigint)
returns jsonb language plpgsql security definer set search_path to '' as $$
declare r record;
begin
  if p_kind = 'inbox' then
    update public.inbox set pushed = true where id = p_id and not pushed and at > now() - interval '10 minutes'
      returning id, player, kind, title, body, data into r;
    if r is null then return null; end if;
    return jsonb_build_object('id', r.id, 'kind', r.kind, 'title', r.title, 'body', r.body, 'image', r.data->>'image',
      'tokens', coalesce((select jsonb_agg(token) from public.push_tokens where player = r.player), '[]'));
  elsif p_kind = 'news' then
    update public.news set pushed = true where id = p_id and not pushed and active
      returning id, text, image, title into r;
    if r is null then return null; end if;
    return jsonb_build_object('id', r.id, 'kind', 'news', 'title', coalesce(nullif(r.title, ''), 'Новости KAPSULA'), 'body', r.text, 'image', r.image);
  elsif p_kind = 'msg' then
    update public.push_msgs set pushed = true where id = p_id and not pushed and created_at > now() - interval '10 minutes'
      returning id, title, body, image, tip into r;
    if r is null then return null; end if;
    return jsonb_build_object('id', r.id, 'kind', case when r.tip is null then 'msg' else 'tip' end, 'title', r.title, 'body', r.body, 'image', r.image,
      'topic', case when r.tip is null then 'news' else 'tips' end);
  end if;
  return null;
end $$;

-- раз в час (pg_cron): если сейчас час из расписания — следующий совет (давно не отправлявшийся) уходит всем
create or replace function public.kapsula_tip_tick(force boolean default false, p_tip int default null)
returns jsonb language plpgsql security definer set search_path to '' as $$
declare cfg jsonb; h int; slot text; t public.push_tips; mid bigint;
begin
  select value into cfg from public.app_settings where key = 'tips';
  h := extract(hour from now() at time zone 'Europe/Moscow');
  slot := to_char(now() at time zone 'Europe/Moscow', 'YYYY-MM-DD HH24');
  if not force then
    if cfg is null or not coalesce((cfg->>'enabled')::boolean, false) then return null; end if;
    if not (cfg->'hours' @> to_jsonb(h)) then return null; end if;
    if cfg->>'last' = slot then return null; end if;
  end if;
  if p_tip is not null then select * into t from public.push_tips where id = p_tip;
  else select * into t from public.push_tips where active order by last_sent nulls first, id limit 1; end if;
  if t.id is null then return null; end if;
  update public.push_tips set sent = sent + 1, last_sent = now() where id = t.id;
  if not force then update public.app_settings set value = value || jsonb_build_object('last', slot), updated_at = now() where key = 'tips'; end if;
  insert into public.push_msgs (title, body, image, tip) values (t.title, t.body, t.image, t.id) returning id into mid;
  return jsonb_build_object('id', mid, 'tip', t.id);
end $$;
revoke all on function public.kapsula_tip_tick(boolean, int) from public, anon, authenticated;

select cron.unschedule(jobid) from cron.job where jobname = 'kapsula-tips';
select cron.schedule('kapsula-tips', '0 * * * *', 'select public.kapsula_tip_tick()');

-- действия админки (вызываются только после проверки кода + PIN + телефона в kapsula-admin)
create or replace function public.kapsula_admin_ext(p_code_hash text, action text, args jsonb)
returns jsonb language plpgsql security definer set search_path to '' as $$
declare me public.players; t public.players; res jsonb; k text; tg int; rw int; w date := public.kapsula_week();
  ti text; bo text; im text;
begin
  select * into me from public.players where code_hash = p_code_hash;
  if not found or not me.is_admin then return jsonb_build_object('error', 'not_admin'); end if;
  ti := left(trim(coalesce(args->>'title', '')), 80); bo := left(trim(coalesce(args->>'body', '')), 300);
  im := nullif(left(trim(coalesce(args->>'image', '')), 500), '');
  if im is not null and im !~ '^https://' then return jsonb_build_object('error', 'bad_image'); end if;
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

  -- свой пуш: всем (push_msgs → тема news) или одному игроку (inbox → его телефоны + колокольчик в приложении)
  when 'push_send' then
    if ti = '' then return jsonb_build_object('error', 'empty'); end if;
    if coalesce(args->>'target', '') <> '' then
      select * into t from public.players where short_id = upper(args->>'target');
      if not found then return jsonb_build_object('error', 'no_player'); end if;
      perform public.kapsula_inbox_add(t.id, 'custom', ti, bo, case when im is null then '{}'::jsonb else jsonb_build_object('image', im) end);
      res := jsonb_build_object('to', t.short_id, 'phones', (select count(*) from public.push_tokens where player = t.id));
      insert into public.admin_log (admin, action, target, details) values (me.id, 'push_send', t.id, jsonb_build_object('title', ti, 'body', bo, 'image', im));
    else
      insert into public.push_msgs (title, body, image) values (ti, bo, im) returning jsonb_build_object('id', id) into res;
      insert into public.admin_log (admin, action, details) values (me.id, 'push_send', jsonb_build_object('title', ti, 'body', bo, 'image', im, 'to', 'all'));
    end if;
  when 'push_list' then
    res := jsonb_build_object(
      'msgs', coalesce((select jsonb_agg(jsonb_build_object('id', id, 'title', title, 'body', body, 'image', image, 'tip', tip, 'at', created_at, 'pushed', pushed) order by id desc)
        from (select * from public.push_msgs order by id desc limit 40) x), '[]'),
      'personal', coalesce((select jsonb_agg(jsonb_build_object('title', i.title, 'body', i.body, 'to', p.short_id, 'at', i.at, 'pushed', i.pushed) order by i.id desc)
        from (select * from public.inbox where kind = 'custom' order by id desc limit 20) i join public.players p on p.id = i.player), '[]'),
      'phones', (select count(*) from public.push_tokens),
      'players_with_push', (select count(distinct player) from public.push_tokens));
  when 'tips_list' then
    res := jsonb_build_object('cfg', coalesce((select value from public.app_settings where key = 'tips'), '{}'),
      'tips', coalesce((select jsonb_agg(jsonb_build_object('id', id, 'title', title, 'body', body, 'image', image, 'active', active, 'sent', sent, 'last', last_sent) order by id) from public.push_tips), '[]'));
  when 'tip_set' then
    if (args ? 'id') and not (args ? 'title') then
      update public.push_tips set active = coalesce((args->>'active')::boolean, active) where id = (args->>'id')::int;
    else
      if ti = '' or bo = '' then return jsonb_build_object('error', 'empty'); end if;
      if args ? 'id' then update public.push_tips set title = ti, body = bo, image = im, active = coalesce((args->>'active')::boolean, active) where id = (args->>'id')::int;
      else insert into public.push_tips (title, body, image) values (ti, bo, im); end if;
    end if;
    insert into public.admin_log (admin, action, details) values (me.id, 'tip_set', args);
    res := '{}';
  when 'tip_del' then
    delete from public.push_tips where id = (args->>'id')::int;
    insert into public.admin_log (admin, action, details) values (me.id, 'tip_del', args);
    res := '{}';
  when 'tip_send' then
    res := public.kapsula_tip_tick(true, (args->>'id')::int);
    insert into public.admin_log (admin, action, details) values (me.id, 'tip_send', args);
  when 'tips_cfg' then
    if jsonb_typeof(args->'hours') <> 'array' or jsonb_array_length(args->'hours') > 12
       or exists (select 1 from jsonb_array_elements(args->'hours') e where jsonb_typeof(e) <> 'number' or (e::text)::int not between 0 and 23) then
      return jsonb_build_object('error', 'bad_hours'); end if;
    insert into public.app_settings (key, value) values ('tips', jsonb_build_object('enabled', coalesce((args->>'enabled')::boolean, true), 'hours', args->'hours'))
      on conflict (key) do update set value = public.app_settings.value || jsonb_build_object('enabled', coalesce((args->>'enabled')::boolean, true), 'hours', args->'hours'), updated_at = now();
    insert into public.admin_log (admin, action, details) values (me.id, 'tips_cfg', args);
    res := (select value from public.app_settings where key = 'tips');

  -- новость с заголовком и картинкой (уходит пушем всем и в колокольчик)
  when 'news_post' then
    if bo = '' then return jsonb_build_object('error', 'empty'); end if;
    insert into public.news (text, title, image) values (bo, nullif(ti, ''), im) returning jsonb_build_object('id', id, 'text', text) into res;
    insert into public.admin_log (admin, action, details) values (me.id, 'news_add', res);

  -- подарки по коду
  when 'gifts' then
    res := coalesce((select jsonb_agg(jsonb_build_object('code', g.code, 'item', g.item, 'title', g.title, 'active', g.active,
      'claims', (select count(*) from public.gift_claims c where c.code = g.code)) order by g.created_at desc) from public.gifts g), '[]');
  when 'gift_set' then
    k := upper(left(regexp_replace(coalesce(args->>'code', ''), '[^A-Za-z0-9]', '', 'g'), 24));
    if k = '' then return jsonb_build_object('error', 'no_code'); end if;
    if coalesce(args->>'item', '') <> '' then
      insert into public.gifts (code, item, title, active) values (k, left(args->>'item', 24), left(coalesce(nullif(args->>'name', ''), args->>'item'), 40), coalesce((args->>'active')::boolean, true))
        on conflict (code) do update set item = excluded.item, title = excluded.title, active = excluded.active;
    else update public.gifts set active = coalesce((args->>'active')::boolean, active) where code = k; end if;
    insert into public.admin_log (admin, action, details) values (me.id, 'gift_set', args);
    res := '{}';
  else return jsonb_build_object('error', 'action');
  end case;
  return jsonb_build_object('ok', true, 'data', res);
end $$;
