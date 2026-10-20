#!/usr/bin/env bash
# AUDIT COPY ONLY: offline WelcomeActivity smoke. No login, no API requests, no deploy.
set -euo pipefail
APK="app/build/outputs/apk/debug/app-debug.apk"
test -s "$APK"
adb wait-for-device
ready=0
for n in $(seq 1 90); do
  boot="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [[ "$boot" == "1" ]] && adb shell pm list packages android 2>/dev/null | grep -q 'package:android'; then
    ready=1
    break
  fi
  sleep 3
done
if [[ "$ready" != "1" ]]; then
  echo "FAIL: emulator boot/package service unavailable"
  exit 86
fi
# Switch off networking before installing and opening app.
adb shell svc wifi disable
adb shell svc data disable || true
adb shell settings put global airplane_mode_on 1
test "$(adb shell settings get global airplane_mode_on | tr -d '\r')" = "1"
adb install -r "$APK"
adb shell pm clear com.nikahbridge
adb logcat -c
adb shell am start -W -n com.nikahbridge/.WelcomeActivity | tee /tmp/bnb-safe-welcome.txt
sleep 8
adb shell uiautomator dump /sdcard/bnb-offline-ui.xml
adb pull /sdcard/bnb-offline-ui.xml /tmp/bnb-offline-ui.xml
grep -q '<node' /tmp/bnb-offline-ui.xml
adb logcat -d > /tmp/bnb-safe-logcat.txt
if grep -E 'FATAL EXCEPTION|ANR in com\.nikahbridge|Process: com\.nikahbridge' /tmp/bnb-safe-logcat.txt; then
  echo "FAIL: app crash/ANR detected"
  exit 1
fi
echo "OFFLINE WELCOME/CRASH SMOKE PASS — not full signed-in 50-screen E2E"
