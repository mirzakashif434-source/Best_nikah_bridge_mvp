#!/usr/bin/env bash
# AUDIT COPY ONLY: evidence capture, not a pass/fail verdict for app UI.
set -euo pipefail
mkdir -p audit-anr-evidence
test "$(adb shell settings get global airplane_mode_on | tr -d '\r')" = "1"
echo "Collecting non-destructive offline Welcome/Language ANR diagnostics"
adb shell dumpsys window > audit-anr-evidence/window-before.txt 2>&1 || true
adb shell dumpsys activity activities > audit-anr-evidence/activities-before.txt 2>&1 || true
# Existing smoke confirmed only no-auth Welcome; never touch Sign In/Get Started.
# UIAutomator can generate an EMPTY file on transient "null root" failures.
# Retry safely, validate hierarchy before attempting XML parsing or tap.
valid_ui=0
for attempt in 1 2 3 4; do
  adb shell rm -f /data/local/tmp/anr-nav.xml || true
  timeout 50s adb shell uiautomator dump /data/local/tmp/anr-nav.xml > "audit-anr-evidence/ui-attempt-${attempt}.txt" 2>&1 || true
  adb exec-out cat /data/local/tmp/anr-nav.xml > audit-anr-evidence/ui-before.xml 2>/dev/null || true
  if python3 - <<'PY'
import xml.etree.ElementTree as ET
try:
    root=ET.parse('audit-anr-evidence/ui-before.xml').getroot()
    assert root.tag=='hierarchy' and any(n.tag=='node' for n in root.iter())
except (ET.ParseError,OSError,AssertionError):
    raise SystemExit(1)
PY
  then
    valid_ui=1
    echo "Valid hierarchy at attempt $attempt"
    break
  fi
  echo "UI hierarchy unavailable: attempt $attempt"
  sleep 3
done
if [[ "$valid_ui" != "1" ]]; then
  adb shell dumpsys window > audit-anr-evidence/window-after.txt 2>&1 || true
  adb logcat -d -v threadtime > audit-anr-evidence/logcat.txt 2>&1 || true
  echo "FAIL INFRA: no valid UI hierarchy; no blind tap performed; diagnostics saved"
  exit 86
fi
python3 - <<'PY' > audit-anr-evidence/coords.txt
import re, xml.etree.ElementTree as ET
root=ET.parse('audit-anr-evidence/ui-before.xml').getroot()
v=[n for n in root.iter('node') if n.attrib.get('resource-id')=='com.nikahbridge:id/welcome_language']
assert len(v)==1, 'Cannot safely locate Language button; no blind tap'
m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',v[0].attrib.get('bounds',''))
assert m, 'No valid click bounds'
x1,y1,x2,y2=map(int,m.groups())
assert x2>x1 and y2>y1
print((x1+x2)//2,(y1+y2)//2)
PY
read -r x y < audit-anr-evidence/coords.txt
adb exec-out screencap -p > audit-anr-evidence/before.png || true
adb shell input tap "$x" "$y"
sleep 8
adb exec-out screencap -p > audit-anr-evidence/after.png || true
adb shell dumpsys window > audit-anr-evidence/window-after.txt 2>&1 || true
adb shell dumpsys activity activities > audit-anr-evidence/activities-after.txt 2>&1 || true
adb shell dumpsys activity processes > audit-anr-evidence/processes-after.txt 2>&1 || true
adb shell pidof com.nikahbridge > audit-anr-evidence/app-pid.txt 2>&1 || true
# SIGQUIT asks ART to write thread stacks to logcat/anr diagnostics; it does not kill the app.
pid="$(head -n1 audit-anr-evidence/app-pid.txt | awk '{print $1}')"
if [[ "$pid" =~ ^[0-9]+$ ]]; then
  adb shell kill -3 "$pid" || true
  sleep 6
fi
adb logcat -d -v threadtime > audit-anr-evidence/logcat.txt 2>&1 || true
adb shell dumpsys activity anr > audit-anr-evidence/anr-dumpsys.txt 2>&1 || true
# On some emulator versions /data/anr is not readable, so ignore permission failures.
adb shell ls -lt /data/anr > audit-anr-evidence/anr-files.txt 2>&1 || true
echo "DIAGNOSTIC CAPTURE COMPLETED — no claim that Language test passed"
