# KAPSULA

Эмулятор ретро-консолей для Android (пакет `com.nexus.psp`). Kotlin + Jetpack Compose, эмуляция — ядра libretro через LibretroDroid 0.14.0 с патчем KAPSULA.

Распространяется через сайт и публичные площадки, без магазинов приложений.

## Консоли
PSP, PS1, PS2 (экспериментально), Денди, Mega Drive, Master System, Game Gear, Dreamcast, Saturn (экспериментально), PC Engine, Neo Geo, Neo Geo Pocket, WonderSwan, Atari 2600/7800, Lynx. Список — `app/src/main/java/com/nexus/psp/data/Catalog.kt` (`PLATFORMS`).

## Устройство
- `app/` — приложение: библиотека игр, проводник и папка KAPSULA, запуск игр, достижения RetroAchievements, профиль и жетоны.
- `admin/` — «KAPSULA Админ», отдельное приложение для создателя.
- `tools/libretrodroid/` — патч LibretroDroid; `setup.sh` готовит исходники в `libretrodroid-src/`.
- `server/sql/` — изменения базы Supabase (сервер аккаунтов, edge function `kapsula`) и файлы отката.
- `docs/` — сайт, `telegram/` — бот, `plans/` — идеи, решения, дампы сессий.

## Сборка
Нужно: JDK 17+, Android SDK Platform 35, Build-Tools 35.0.0, NDK 27.2.12479018, CMake 3.22.1, Gradle 8.9. Только `arm64-v8a`.

```sh
tools/libretrodroid/setup.sh            # исходники LibretroDroid с патчем
gradle :app:testDebugUnitTest           # тесты (Robolectric + Roborazzi)
gradle :app:assembleDebug               # APK без ядер эмуляции
```

Ядра libretro и системные файлы в репозитории не лежат — их качает CI (см. `.github/workflows/test-build.yml`). В облачной среде Claude всё нужное ставит `tools/cloud-env-setup.sh` (вызывать `gradle-8.9`).

## CI
- `test-build.yml` — ветки `ccr-*` (кроме основной): тесты + подписанный APK в артефактах. **Ничего не публикует.**
- `android.yml` — `main` и `ccr-30f02f17-ph3ie4`: **публикует выпуск `latest` всем игрокам** и зеркалит в GitVerse.

## Лицензии
Ядра и LibretroDroid распространяются под своими лицензиями (PPSSPP — GPL-2.0-or-later и др.). При распространении сборок соблюдать их требования.
