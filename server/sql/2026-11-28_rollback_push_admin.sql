-- Откат 2026-11-28_push_admin.sql (kapsula_push_take и kapsula_admin_ext — вернуть из 2026-11-28_push_images.sql / прежней версии).
select cron.unschedule(jobid) from cron.job where jobname = 'kapsula-tips';
drop function if exists public.kapsula_tip_tick(boolean, int);
drop trigger if exists kapsula_push_msg on public.push_msgs;
drop table if exists public.push_msgs;
drop table if exists public.push_tips;
delete from public.app_settings where key = 'tips';
-- news.title оставить (безвредно) или: alter table public.news drop column title;
