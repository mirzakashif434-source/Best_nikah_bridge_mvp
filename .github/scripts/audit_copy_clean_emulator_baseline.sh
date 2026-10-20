#!/usr/bin/env bash
# No app installed or launched. Read-only emulator health baseline on audit branch.
set -euo pipefail
adb wait-for-device
echo "BOOT COMPLETED: $(adb shell getprop sys.boot_completed | tr -d '\r')"
adb shell dumpsys activity processes > clean-emulator-processes.txt 2>&1 || true
adb shell dumpsys window > clean-emulator-window.txt 2>&1 || true
adb logcat -d -v threadtime > clean-emulator-logcat.txt 2>&1 || true
if grep -Ei 'ANR in com\.android\.(phone|systemui)|Process system isn.t responding|System UI isn.t responding' clean-emulator-logcat.txt; then
  echo 'CLEAN EMULATOR SYSTEM ANR FOUND'
  exit 40
fi
echo 'NO SYSTEM ANR OBSERVED IN CLEAN EMULATOR LOG'
