-- Откат push-уведомлений (2026-11-28_push.sql)
drop trigger if exists kapsula_push_inbox on public.inbox;
drop trigger if exists kapsula_push_news on public.news;
drop function if exists public.kapsula_push_notify();
drop function if exists public.kapsula_push_token(text, text);
drop function if exists public.kapsula_push_take(text, bigint);
drop function if exists public.kapsula_push_forget(text[]);
drop table if exists public.push_tokens;
alter table public.inbox drop column if exists pushed;
alter table public.news drop column if exists pushed;
