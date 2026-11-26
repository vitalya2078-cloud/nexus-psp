-- Откат 2026-11-26_gifts_delete.sql (выданные подарки остаются в players.owned).
drop function if exists public.kapsula_gift(text, text);
drop function if exists public.kapsula_delete(text);
drop table if exists public.gift_claims;
drop table if exists public.gifts;
