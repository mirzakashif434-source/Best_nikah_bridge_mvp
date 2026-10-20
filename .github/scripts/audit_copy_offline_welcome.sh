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
# Do not use -W: emulators can time out waiting for a launch report even
# when the app process starts. Validate the running process separately.
adb shell am start -n com.nikahbridge/.WelcomeActivity | tee /tmp/bnb-safe-welcome.txt
sleep 12
if ! adb shell pidof com.nikahbridge | grep -Eq '[0-9]+'; then
  echo "FAIL: Welcome app process is not running"
  adb logcat -d | tail -n 100 || true
  exit 1
fi
# Android emulators may deny pulling files from /sdcard. Capture the screen
# directly over adb stdout instead; assert a real PNG was produced.
adb exec-out screencap -p > /tmp/bnb-offline-welcome.png
test -s /tmp/bnb-offline-welcome.png
python3 -c 'from pathlib import Path; p=Path("/tmp/bnb-offline-welcome.png"); assert p.read_bytes()[:8] == bytes([137,80,78,71,13,10,26,10]), "Bad screenshot PNG"'
adb logcat -d > /tmp/bnb-safe-logcat.txt
if grep -E 'FATAL EXCEPTION|ANR in com\.nikahbridge|Process: com\.nikahbridge' /tmp/bnb-safe-logcat.txt; then
  echo "FAIL: app crash/ANR detected"
  exit 1
fi
echo "OFFLINE WELCOME/CRASH SMOKE PASS — not full signed-in 50-screen E2E"
