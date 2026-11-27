-- Подарок за секреты на сайте: код SEEKER → титул «Искатель секретов» (tt_seeker, приложение 0.51+).
insert into public.gifts (code, item, title, active) values ('SEEKER', 'tt_seeker', 'Искатель секретов', true)
on conflict (code) do update set item = excluded.item, title = excluded.title, active = true;
-- откат: delete from public.gifts where code = 'SEEKER';
