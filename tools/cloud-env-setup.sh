#!/bin/bash
# KAPSULA: Android SDK + Gradle 8.9 для :app:testDebugUnitTest и компиляции
set -euo pipefail
# только для сессий с репозиторием KAPSULA — остальные ничего не ставят
[ -d nexus-psp ] || exit 0
export ANDROID_HOME=/opt/android-sdk
SM=$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager
if [ ! -x "$SM" ]; then
  mkdir -p $ANDROID_HOME/cmdline-tools
  curl -fsSL --retry 4 -o /tmp/cmdline.zip https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
  unzip -q -o /tmp/cmdline.zip -d $ANDROID_HOME/cmdline-tools
  rm -rf $ANDROID_HOME/cmdline-tools/latest
  mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest
  rm -f /tmp/cmdline.zip
fi
yes | "$SM" --licenses >/dev/null 2>&1 || true
"$SM" "platform-tools" "platforms;android-35" "build-tools;35.0.0" "ndk;27.2.12479018" "cmake;3.22.1" >/dev/null
if [ ! -x /opt/gradle-8.9/bin/gradle ]; then
  curl -fsSL --retry 4 -o /tmp/gradle.zip https://services.gradle.org/distributions/gradle-8.9-bin.zip
  unzip -q -o /tmp/gradle.zip -d /opt
  rm -f /tmp/gradle.zip
fi
ln -sf /opt/gradle-8.9/bin/gradle /usr/local/bin/gradle-8.9
cd nexus-psp
echo "sdk.dir=$ANDROID_HOME" > local.properties
tools/libretrodroid/setup.sh
tools/j2me/setup.sh
