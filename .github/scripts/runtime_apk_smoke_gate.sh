#!/usr/bin/env bash
set -euo pipefail

APK="${1:?APK path required}"
PACKAGE="com.nikahbridge"
ACTIVITY="com.nikahbridge/.WelcomeActivity"
CRASH_RE='Process: com\.nikahbridge|ANR in com\.nikahbridge|Fatal signal.*com\.nikahbridge|Force finishing activity.*com\.nikahbridge'

test -s "$APK"

adb_recover() {
  local attempt
  for attempt in 1 2 3; do
    if adb get-state >/dev/null 2>&1; then
      return 0
    fi
    echo "ADB transport unavailable (attempt $attempt/3); self-healing ADB transport."
    adb kill-server >/dev/null 2>&1 || true
    adb start-server >/dev/null 2>&1 || true
    adb wait-for-device >/dev/null 2>&1 || true
    sleep 2
  done
  echo "ADB transport could not self-heal."
  return 1
}

capture_logcat() {
  local target="$1"
  local attempt
  for attempt in 1 2 3; do
    adb_recover || true
    if adb logcat -d > "$target" 2>/tmp/bnb-adb-logcat-error.txt; then
      return 0
    fi
    echo "logcat transport retry $attempt/3"
    cat /tmp/bnb-adb-logcat-error.txt || true
    sleep 2
  done
  echo "Unable to capture logcat after ADB self-heal retries."
  return 1
}

launch_and_assert() {
  local out="$1"
  adb_recover
  adb shell am start -W -n "$ACTIVITY" > "$out"
  grep -Eq 'Status: ok|Activity: com\.nikahbridge/.WelcomeActivity|ThisTime:|TotalTime:' "$out"
}

adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx 1
adb install -r "$APK"
adb shell pm clear "$PACKAGE" >/dev/null 2>&1 || true
adb logcat -c || true

launch_and_assert /tmp/bnb-launch.txt
sleep 5

# Exercise navigation; monkey exit is not used as the crash verdict.
adb shell monkey -p "$PACKAGE" --throttle 120 --ignore-security-exceptions 500 > /tmp/bnb-monkey.txt 2>&1 || true
sleep 3
capture_logcat /tmp/bnb-logcat.txt
if grep -E "$CRASH_RE" /tmp/bnb-logcat.txt; then
  echo "Runtime crash/ANR evidence found after navigation exercise."
  exit 1
fi

adb_recover
adb shell am force-stop "$PACKAGE" || true
adb logcat -c || true
launch_and_assert /tmp/bnb-relaunch.txt
sleep 5
capture_logcat /tmp/bnb-relaunch-logcat.txt
if grep -E "$CRASH_RE" /tmp/bnb-relaunch-logcat.txt; then
  echo "Runtime crash/ANR evidence found after clean relaunch."
  exit 1
fi

echo "RUNTIME APK SMOKE/CRASH GATE: PASS — STANDALONE ADB SELF-HEAL ENABLED"
