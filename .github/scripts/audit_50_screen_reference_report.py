#!/usr/bin/env python3
"""Read-only 50-activity reference audit. No app execution or Azure requests."""
from pathlib import Path
import re, xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[2]
java=root/"app/src/main/java/com/nikahbridge"
manifest=root/"app/src/main/AndroidManifest.xml"
names={p.stem for p in java.glob("*Activity.java")}
tree=ET.parse(manifest)
registered={x.attrib.get("{http://schemas.android.com/apk/res/android}name","").split(".")[-1] for x in tree.iter("activity")}
issues=[]
lines=["screen,manifest,click_handlers,back_handling,java_navigation_targets"]
for name in sorted(names):
    text=(java/(name+".java")).read_text(encoding="utf-8")
    refs=set(re.findall(r"\b([A-Za-z]\w*Activity)\.class\b",text))
    missing=sorted(refs-names-{"Activity"})
    handlers=len(re.findall(r"setOnClickListener\s*\(|setPositiveButton\s*\(|setNegativeButton\s*\(|setOnItemClickListener\s*\(",text))
    back=bool(re.search(r"onBackPressed\s*\(|\bfinish\s*\(|\bBack\b|KEYCODE_BACK",text))
    is_registered=name in registered
    if not is_registered: issues.append(f"{name}: missing manifest")
    if missing: issues.append(f"{name}: unknown navigation destination(s): {','.join(missing)}")
    lines.append(f"{name},{str(is_registered).lower()},{handlers},{str(back).lower()},{'|'.join(sorted(refs))}")
if len(names)!=50: issues.append(f"Expected 50 Activity classes, found {len(names)}")
if names-registered: issues.append(f"Missing manifest screens: {sorted(names-registered)}")
out=root/"audit-50-screen-source-report.csv"
out.write_text("\n".join(lines)+"\n",encoding="utf-8")
print(f"Audited {len(names)} screens; report: {out}")
for i in issues: print("FAIL:",i)
if issues: raise SystemExit(1)
print("PASS: 50 files registered, navigation destination class references resolve.")
print("NOTE: This is static analysis, NOT 50-screen runtime button-click coverage.")
