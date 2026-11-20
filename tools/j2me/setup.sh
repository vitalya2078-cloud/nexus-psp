#!/usr/bin/env bash
# Готовит движок Java-игр J2ME Loader (Apache-2.0, github.com/nikita36078/J2ME-Loader) в ./j2me-src
# с патчем KAPSULA: библиотека вместо приложения, без ffmpeg и отчётов ACRA. Запускать из корня репозитория.
set -euo pipefail
DIR=j2me-src
REV=360681dd5f6ccbc537e3334da1bf3d32201c684b
STAMP="$DIR/.kapsula-$(cat tools/j2me/patch.py tools/j2me/build.gradle.kts tools/j2me/dexlib.gradle.kts tools/j2me/AndroidManifest.xml | sha1sum | cut -c1-12)-${REV:0:8}"
if [ -f "$STAMP" ]; then echo "j2me: готово"; exit 0; fi
rm -rf "$DIR"
mkdir -p "$DIR"
(cd "$DIR" && git init -q && git remote add origin https://github.com/nikita36078/J2ME-Loader.git \
  && git fetch -q --depth 1 origin "$REV" && git -c advice.detachedHead=false checkout -q FETCH_HEAD)
python3 tools/j2me/patch.py "$DIR"
rm -f "$DIR/app/build.gradle" "$DIR/dexlib/build.gradle"
cp tools/j2me/build.gradle.kts "$DIR/app/build.gradle.kts"
cp tools/j2me/dexlib.gradle.kts "$DIR/dexlib/build.gradle.kts"
cp tools/j2me/AndroidManifest.xml "$DIR/app/src/main/AndroidManifest.xml"
touch "$STAMP"
echo "j2me: исходники и патч готовы"
