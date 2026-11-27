-- 0.51: push-уведомления (Firebase Cloud Messaging). Новое в inbox / news → триггер → функция push → FCM.
-- Каждое уведомление уходит в push не больше одного раза (pushed), поэтому функции push не нужен пароль.
-- Откат: 2026-11-28_rollback_push.sql
create extension if not exists pg_net;

create table if not exists public.push_tokens (
  token text primary key,                                   -- токен FCM телефона
  player uuid not null references public.players(id) on delete cascade,
  updated_at timestamptz not null default now()
);
create index if not exists push_tokens_player on public.push_tokens(player);
alter table public.push_tokens enable row level security;
revoke all on public.push_tokens from anon, authenticated;

alter table public.inbox add column if not exists pushed boolean not null default false;
alter table public.news add column if not exists pushed boolean not null default false;

-- телефон сообщает свой токен (после входа и при смене токена)
create or replace function public.kapsula_push_token(p_code_hash text, p_token text)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare me uuid;
begin
  select id into me from public.players where code_hash = p_code_hash;
  if me is null then return jsonb_build_object('error', 'unknown_code'); end if;
  if p_token is null or length(p_token) < 20 or length(p_token) > 400 then return jsonb_build_object('error', 'bad_token'); end if;
  insert into public.push_tokens(token, player, updated_at) values (p_token, me, now())
    on conflict (token) do update set player = excluded.player, updated_at = now();
  -- не больше 5 телефонов на игрока: самые старые токены — прочь
  delete from public.push_tokens where player = me and token not in
    (select token from public.push_tokens where player = me order by updated_at desc limit 5);
  return jsonb_build_object('ok', true);
end $function$;

-- функция push забирает уведомление (один раз) и токены адресата
create or replace function public.kapsula_push_take(p_kind text, p_id bigint)
returns jsonb language plpgsql security definer set search_path to '' as $function$
declare r record;
begin
  if p_kind = 'inbox' then
    update public.inbox set pushed = true where id = p_id and not pushed and at > now() - interval '10 minutes'
      returning id, player, kind, title, body into r;
    if r is null then return null; end if;
    return jsonb_build_object('id', r.id, 'kind', r.kind, 'title', r.title, 'body', r.body,
      'tokens', coalesce((select jsonb_agg(token) from public.push_tokens where player = r.player), '[]'));
  elsif p_kind = 'news' then
    update public.news set pushed = true where id = p_id and not pushed and active
      returning id, text into r;
    if r is null then return null; end if;
    return jsonb_build_object('id', r.id, 'kind', 'news', 'title', 'Новости KAPSULA', 'body', r.text);
  end if;
  return null;
end $function$;

create or replace function public.kapsula_push_forget(p_tokens text[])
returns void language sql security definer set search_path to '' as $function$
  delete from public.push_tokens where token = any(p_tokens);
$function$;

do $$ declare f text; begin
  foreach f in array array['kapsula_push_token(text,text)', 'kapsula_push_take(text,bigint)', 'kapsula_push_forget(text[])'] loop
    execute format('revoke all on function public.%s from public, anon, authenticated', f);
    execute format('grant execute on function public.%s to service_role', f);
  end loop;
end $$;

-- триггеры: новое уведомление / новость → функция push (асинхронно, через pg_net; ошибки не мешают вставке)
create or replace function public.kapsula_push_notify()
returns trigger language plpgsql security definer set search_path to '' as $function$
begin
  begin
    perform net.http_post(
      url := 'https://borubfgvjhntepnijxxo.supabase.co/functions/v1/push',
      body := jsonb_build_object('kind', tg_argv[0], 'id', new.id),
      headers := '{"Content-Type": "application/json"}'::jsonb);
  exception when others then null;
  end;
  return new;
end $function$;
revoke all on function public.kapsula_push_notify() from public, anon, authenticated;

drop trigger if exists kapsula_push_inbox on public.inbox;
create trigger kapsula_push_inbox after insert on public.inbox for each row execute function public.kapsula_push_notify('inbox');
drop trigger if exists kapsula_push_news on public.news;
create trigger kapsula_push_news after insert or update of active on public.news for each row
  when (new.active and not new.pushed) execute function public.kapsula_push_notify('news');

-- картинка в push (применено отдельно, миграция push_images): news.image и inbox.data->>'image'
alter table public.news add column if not exists image text;
-- kapsula_push_take отдаёт поле image (см. миграцию push_images в базе)
