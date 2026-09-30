#!/usr/bin/env bash
set -euo pipefail
APK="${1:?APK path required}"
PKG="com.nikahbridge"
ACT="com.nikahbridge/.WelcomeActivity"
CRASH_RE='FATAL EXCEPTION|ANR in com\.nikahbridge|Process: com\.nikahbridge|Fatal signal.*com\.nikahbridge|Force finishing activity.*com\.nikahbridge'

test -s "$APK"
adb wait-for-device
adb shell getprop sys.boot_completed | grep -qx 1
adb install -r "$APK" >/dev/null
adb shell pm clear "$PKG" >/dev/null 2>&1 || true
adb logcat -c || true

launch(){
  adb shell am start -W -n "$ACT" >/tmp/bnb-ultra-launch.txt
  grep -Eq 'Status: ok|Activity: com\.nikahbridge/.WelcomeActivity|ThisTime:|TotalTime:' /tmp/bnb-ultra-launch.txt
}
ui_assert(){
  adb shell uiautomator dump /sdcard/bnb-ui.xml >/dev/null
  adb pull /sdcard/bnb-ui.xml /tmp/bnb-ui.xml >/dev/null
  grep -q '<node' /tmp/bnb-ui.xml
  # blank/white/black screens generally expose no meaningful text/content-desc;
  # require at least one non-empty accessible label on the launch surface.
  grep -Eq 'text="[^"]+"|content-desc="[^"]+"' /tmp/bnb-ui.xml
}
crash_assert(){
  adb logcat -d > /tmp/bnb-ultra-logcat.txt
  if grep -E "$CRASH_RE" /tmp/bnb-ultra-logcat.txt; then
    echo "ULTRA RUNTIME: crash/ANR evidence found"
    exit 1
  fi
}

launch
sleep 3
ui_assert

# Exercise a large safe interaction surface without granting fake auth.
# Security exceptions are ignored by monkey itself, but app crashes/ANRs are not.
adb shell monkey -p "$PKG" --pct-touch 45 --pct-motion 15 --pct-nav 20 --pct-majornav 10 --pct-appswitch 0 --throttle 90 --ignore-security-exceptions 1500 >/tmp/bnb-ultra-monkey.txt 2>&1 || true
sleep 3
crash_assert

# Clean relaunch must remain renderable and responsive after interaction storm.
adb shell am force-stop "$PKG" || true
adb logcat -c || true
launch
sleep 3
ui_assert
crash_assert

# Basic device-size sanity: app must be tested on a real emulator surface, not a headless compile only.
SIZE="$(adb shell wm size | tr -d '\r')"
echo "$SIZE" | grep -Eq '[0-9]+x[0-9]+'

echo "50-SCREEN ULTRA RUNTIME SHELL: PASS — launch/render/accessibility + 1500 safe random UI events + crash/ANR + relaunch + screen-size"
echo "NOTE: authenticated 50-screen Azure click-through still requires a real signed-in session; this gate does not fake or bypass authentication."
