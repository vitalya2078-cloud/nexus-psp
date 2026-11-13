#!/usr/bin/env bash
# Готовит исходники LibretroDroid 0.14.0 с патчем KAPSULA в ./libretrodroid-src (запускать из корня репозитория).
set -euo pipefail
DIR=libretrodroid-src
STAMP="$DIR/.kapsula-$(sha1sum tools/libretrodroid/patch.py tools/libretrodroid/build.gradle.kts tools/libretrodroid/kapsula_ra.cpp | sha1sum | cut -c1-12)"
if [ -f "$STAMP" ]; then echo "libretrodroid: готово"; exit 0; fi
rm -rf "$DIR"
git clone -q --depth 1 --branch 0.14.0 https://github.com/Swordfish90/LibretroDroid.git "$DIR"
(cd "$DIR" && git submodule update -q --init --depth 1)
# rcheevos (MIT) — достижения RetroAchievements
git -c advice.detachedHead=false clone -q --depth 1 --branch v12.5.0 https://github.com/RetroAchievements/rcheevos.git "$DIR/libretrodroid/src/main/cpp/rcheevos"
python3 tools/libretrodroid/patch.py "$DIR"
rm -f "$DIR/libretrodroid/build.gradle"
cp tools/libretrodroid/build.gradle.kts "$DIR/libretrodroid/build.gradle.kts"
touch "$STAMP"
echo "libretrodroid: исходники и патч готовы"
