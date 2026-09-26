#!/usr/bin/env python3
from pathlib import Path
import re, sys, zipfile

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"app/src/main/java/com/nikahbridge"
HOME=APP/"AzureHomeActivity.java"
MANIFEST=ROOT/"app/src/main/AndroidManifest.xml"
FLOW=ROOT/".maestro/final-release-42-screen-master.yaml"

screens=[
"My Real Azure Profile","Real Compatibility Matches","Liked Me","I Liked","Viewed Me","I Viewed",
"Serious Nikah Plus","Premium Plans — 20 / 40 / 60 SAR","Mutual Interests","Family / Wali Connect",
"Privacy Controls","Verification Status","Safety Reports","Azure AI Nikah Assistant","Real Profile Photo",
"Four-Photo Verification","Global Community Chat","Earn Message Credits","Blocked Members",
"Terms & Community Guidelines","Nikah Journey","Nikah Blueprint","Smart Serious Questions","Why We Matched",
"Advanced Match Filters","Compatibility Deal-Breakers","Compatibility Traffic Light","Marriage Timeline Matching",
"Family Circle","Trust Passport","Safe Communication","Nikah Intelligence","Conversation Health",
"AI Nikah Mediator","Nikah Success Plan","Nikah Success Network","Future Life Simulation",
"Owner Live Analytics","Owner Wallet / Payout","Azure Wallet","Permanently Delete Account","Sign out of Azure"
]
assert len(screens)==42

home=HOME.read_text(encoding="utf-8")
flow=FLOW.read_text(encoding="utf-8")
manifest=MANIFEST.read_text(encoding="utf-8")

for label in screens:
    if label not in home:
        raise SystemExit(f"HOME LABEL MISSING: {label}")
    if label.startswith("Premium Plans"):
        if "Premium Plans.*" not in flow and label not in flow:
            raise SystemExit("FINAL MAESTRO PREMIUM LABEL MISSING")
    elif label not in flow:
        raise SystemExit(f"FINAL MAESTRO FLOW MISSING: {label}")

listeners=re.findall(r'([A-Za-z_][A-Za-z0-9_]*)\.setOnClickListener\(',home)
home_buttons=re.findall(r'Button\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*button\(',home)
missing=[b for b in home_buttons if b not in listeners]
if missing:
    raise SystemExit("HOME BUTTONS WITHOUT CLICK LISTENER: "+", ".join(missing))

activities=sorted(APP.glob("*Activity.java"))
if len(activities)<46:
    raise SystemExit(f"ACTIVITY COUNT TOO LOW: {len(activities)}")

for p in activities:
    name=p.stem
    if name in {"AzureExternalAuthActivity"}:
        continue
    if f'android:name=".{name}"' not in manifest and name not in {"WelcomeActivity"}:
        raise SystemExit(f"MANIFEST REGISTRATION MISSING: {name}")

bad=[]
for p in APP.glob("*.java"):
    t=p.read_text(encoding="utf-8")
    for forbidden in ["null null","not available in this backend release","updating on Azure",
                      "Reported real user ID","Real matched member ID","Conversation ID from a real mutual connection"]:
        if forbidden in t:
            bad.append(f"{p.name}: {forbidden}")
if bad:
    raise SystemExit("FORBIDDEN USER-FACING STATES: "+" | ".join(bad))

camera=(APP/"IdentityVerificationActivity.java").read_text(encoding="utf-8")
four=(APP/"RealFourPhotoActivity.java").read_text(encoding="utf-8")
photo=(APP/"ProfilePhotoActivity.java").read_text(encoding="utf-8")
for required in ["MediaStore.ACTION_IMAGE_CAPTURE","FileProvider.getUriForFile","EXTRA_OUTPUT"]:
    if required not in camera and required not in four and required not in photo:
        raise SystemExit(f"CAMERA CONTRACT MISSING: {required}")
for required in ["image/jpeg","image/png"]:
    if required not in camera and required not in four and required not in photo:
        raise SystemExit(f"PHOTO MIME CONTRACT MISSING: {required}")
if "androidx.core.content.FileProvider" not in manifest:
    raise SystemExit("FILEPROVIDER MISSING")

for required in ["CANCEL","Delete Azure account data permanently","Sign out of Azure"]:
    if required not in flow:
        raise SystemExit(f"DESTRUCTIVE SAFETY MISSING: {required}")

apk=Path(sys.argv[1]) if len(sys.argv)>1 else None
if apk:
    if not apk.is_file() or apk.stat().st_size==0:
        raise SystemExit("APK MISSING OR EMPTY")
    dex=b""
    with zipfile.ZipFile(apk) as z:
        for n in z.namelist():
            if re.fullmatch(r"classes\d*\.dex",n):
                dex+=z.read(n)
    for label in screens:
        b=label.encode()
        if b not in dex:
            if label.startswith("Premium Plans") and b"Premium Plans" in dex:
                continue
            raise SystemExit(f"COMPILED APK LABEL MISSING: {label}")
    for forbidden in [b"FirebaseAuth",b"FirebaseFirestore",b"FirebaseFunctions",b"FirebaseStorage",b"null null"]:
        if forbidden in dex:
            raise SystemExit(f"FORBIDDEN APK CONTENT: {forbidden.decode(errors='ignore')}")
    if b"azurewebsites.net" not in dex:
        raise SystemExit("AZURE ENDPOINT STRING MISSING FROM APK")

print(f"FINAL RELEASE MASTER: PASS — {len(screens)} home screens")
print(f"ALL HOME BUTTON LISTENERS: PASS — {len(home_buttons)} buttons")
print(f"ACTIVITY/MANIFEST AUDIT: PASS — {len(activities)} activities")
print("CAMERA/PHOTO CONTRACT: PASS")
print("NULL/STALE ERROR SOURCE SCAN: PASS")
if apk:
    print("SIGNED APK FORENSIC LABEL/AZURE SCAN: PASS")
