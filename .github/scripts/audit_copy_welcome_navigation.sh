#!/usr/bin/env bash
# AUDIT COPY ONLY. Offline Welcome navigation; never tap Sign In or Get Started.
set -euo pipefail
PKG=com.nikahbridge
test "$(adb shell settings get global airplane_mode_on | tr -d '\r')" = "1"
adb shell svc wifi disable
adb shell svc data disable || true

capture_ui(){
  adb shell uiautomator dump /sdcard/bnb-audit-nav.xml >/dev/null
  adb shell cat /sdcard/bnb-audit-nav.xml | tr -d '\r' > /tmp/bnb-audit-nav.xml
  grep -q '<hierarchy' /tmp/bnb-audit-nav.xml
}

capture_ui
# Locate actual Language control bounds from accessibility hierarchy;
# do not hardcode display coordinates or touch Azure sign-in buttons.
python3 - <<'PY' > /tmp/bnb-audit-lang-coords
import re, xml.etree.ElementTree as ET
root=ET.parse('/tmp/bnb-audit-nav.xml').getroot()
matches=[n for n in root.iter('node') if n.attrib.get('resource-id')=='com.nikahbridge:id/welcome_language']
assert len(matches)==1, f'Expected one Language control, got {len(matches)}'
node=matches[0]
assert node.attrib.get('clickable')=='true', 'Language control not clickable'
bounds=node.attrib.get('bounds','')
m=re.fullmatch(r'\[(\d+),(\d+)\]\[(\d+),(\d+)\]',bounds)
assert m, f'Invalid bounds: {bounds}'
x1,y1,x2,y2=map(int,m.groups())
assert x2>x1 and y2>y1
print((x1+x2)//2,(y1+y2)//2)
PY
read -r x y < /tmp/bnb-audit-lang-coords
adb shell input tap "$x" "$y"
sleep 3
capture_ui
# Verify dialog's actual content before dismissing, without changing language.
python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse('/tmp/bnb-audit-nav.xml').getroot()
text=' '.join(n.attrib.get('text','')+' '+n.attrib.get('content-desc','') for n in root.iter('node'))
assert ('Cancel' in text or 'Choose Your Language' in text), 'Language dialog not visible'
print('LANGUAGE DIALOG OPEN: PASS')
PY
adb shell input keyevent KEYCODE_BACK
sleep 2
capture_ui
python3 - <<'PY'
import xml.etree.ElementTree as ET
root=ET.parse('/tmp/bnb-audit-nav.xml').getroot()
ids={n.attrib.get('resource-id','') for n in root.iter('node')}
required={'com.nikahbridge:id/welcome_get_started','com.nikahbridge:id/welcome_sign_in','com.nikahbridge:id/welcome_language'}
assert required.issubset(ids), 'Welcome controls not restored after dialog back'
print('LANGUAGE DIALOG BACK -> WELCOME: PASS')
PY
adb logcat -d > /tmp/bnb-audit-nav-logcat.txt
if grep -E 'FATAL EXCEPTION|ANR in com\.nikahbridge|Process: com\.nikahbridge' /tmp/bnb-audit-nav-logcat.txt; then
  echo "FAIL: Crash/ANR during Language dialog navigation"
  exit 1
fi
echo "SAFE OFFLINE WELCOME NAVIGATION: PASS (authenticated 50 screens not exercised)"
