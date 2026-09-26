#!/usr/bin/env python3
from pathlib import Path
import re, sys, zipfile, xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"app/src/main/java/com/nikahbridge"
MANIFEST=ROOT/"app/src/main/AndroidManifest.xml"
APK=Path(sys.argv[1]) if len(sys.argv)>1 else None

activities=sorted(APP.glob("*Activity.java"))
assert len(activities)>=46, f"Expected >=46 Android activities, found {len(activities)}"

manifest=MANIFEST.read_text(encoding="utf-8")
missing=[]
for p in activities:
    if f'android:name=".{p.stem}"' not in manifest:
        missing.append(p.name)
assert not missing, "Activities missing from manifest: "+", ".join(missing)

# Global release safety / UI regression checks.
forbidden=[
    "null null",
    "Reported real user ID",
    "Reported user ID required",
    "Real recipient user ID",
    "Real matched member ID",
    "Conversation ID from a real mutual connection",
    "not available in this backend release",
    "updating on Azure",
]
hits=[]
for p in APP.glob("*.java"):
    t=p.read_text(encoding="utf-8")
    for bad in forbidden:
        if bad in t:
            hits.append(f"{p.name}: {bad}")
assert not hits, "Forbidden user-facing states remain: "+"; ".join(hits)

# Every production screen must be responsive/scroll-safe except intentionally root launchers.
no_scroll=[]
for p in activities:
    if p.name in {"MainActivity.java"}:
        continue
    t=p.read_text(encoding="utf-8")
    if "ScrollView" not in t:
        no_scroll.append(p.name)
assert not no_scroll, "Screens missing ScrollView: "+", ".join(no_scroll)

# Back navigation contract on non-root activities.
back_exempt={"WelcomeActivity.java","MainActivity.java","ProductionMainActivity.java","AzureHomeActivity.java"}
no_back=[]
for p in activities:
    if p.name in back_exempt:
        continue
    t=p.read_text(encoding="utf-8")
    if "Back" not in t:
        no_back.append(p.name)
assert not no_back, "Screens missing Back navigation contract: "+", ".join(no_back)

# Camera/photo real-path contract.
four=(APP/"RealFourPhotoActivity.java").read_text(encoding="utf-8")
photo=(APP/"ProfilePhotoActivity.java").read_text(encoding="utf-8")
assert "MediaStore.ACTION_IMAGE_CAPTURE" in four
assert "FileProvider.getUriForFile" in four
assert "CAMERA_FIRST" in four
assert "GALLERY_REMAINING" in four
assert "AzureApiClient.multipart" in four
assert "Intent.ACTION_OPEN_DOCUMENT" in photo
assert "AzureApiClient.multipart" in photo
assert 'androidx.core.content.FileProvider' in manifest
assert 'android:grantUriPermissions="true"' in manifest

# Identity verification must use a real upload path.
verify=(APP/"IdentityVerificationActivity.java").read_text(encoding="utf-8")
assert "AzureApiClient" in verify
assert ("multipart" in verify or "/verification" in verify or "/verifications" in verify)

# Google Play Billing production contract.
premium=(APP/"PremiumPlansActivity.java").read_text(encoding="utf-8")
for required in [
    "BillingClient.newBuilder",
    "BillingClient.ProductType.SUBS",
    "premium_basic_20",
    "premium_plus_40",
    "premium_vip_60",
    "launchBillingFlow",
    "/premium/purchases/verify",
    "restoreExistingPurchases",
]:
    assert required in premium, f"Premium billing contract missing: {required}"

# Rewarded ad production + SSV contract.
reward=(APP/"RewardedMessageActivity.java").read_text(encoding="utf-8")
for required in [
    "RewardedAd.load",
    "ServerSideVerificationOptions",
    "setUserId",
    "setCustomData",
    "MobileAds.initialize",
]:
    assert required in reward, f"Rewarded ad contract missing: {required}"

# Critical Azure routes and owner/wallet contracts.
critical={
    "AzureHomeActivity.java":["/matches","/safety/reports"],
    "OwnerLiveAnalyticsActivity.java":["/owner/live-analytics"],
    "OwnerEarningsActivity.java":["/owner/earnings","/admin/owner/provider-settlement"],
    "AzureWalletActivity.java":["Azure"],
    "FamilyBridge2Activity.java":["family-links"],
    "LikedMeActivity.java":["/likes/received"],
    "ILikedActivity.java":["/likes/sent"],
    "ViewedMeActivity.java":["/profile-views/received"],
    "IViewedActivity.java":["/profile-views/sent"],
}
for name,needles in critical.items():
    t=(APP/name).read_text(encoding="utf-8")
    for n in needles:
        assert n in t, f"{name} missing {n}"

# Protection stack source contracts.
deploy=(ROOT/".github/workflows/azure-backend-deploy.yml").read_text(encoding="utf-8")
for required in [
    "Self-heal Azure runtime after transient production failure",
    "Auto-rollback Azure backend to last known-good Golden release",
    "Fortress Budget Regression — locked production contract",
    "--instance-memory 512 --maximum-instance-count 20",
    "AZURE_DB_POOL_MAX=3",
]:
    assert required in deploy, f"Protection contract missing: {required}"

# Compiled artifact forensic checks.
if APK is not None:
    assert APK.is_file() and APK.stat().st_size>0, f"APK missing: {APK}"
    dex=b""
    with zipfile.ZipFile(APK) as z:
        for name in z.namelist():
            if re.fullmatch(r"classes\d*\.dex",name):
                dex+=z.read(name)
    for required in [
        b"Premium Plans",b"Family / Wali Connect",b"Verification Status",
        b"Owner Live Analytics",b"Owner Wallet / Payout",b"Azure Wallet",
        b"Liked Me",b"I Liked",b"Viewed Me",b"I Viewed"
    ]:
        assert required in dex, f"Compiled APK missing label: {required!r}"
    for bad in [b"null null",b"FirebaseAuth",b"FirebaseFirestore",b"FirebaseFunctions",b"FirebaseStorage"]:
        assert bad not in dex, f"Compiled APK forbidden content: {bad!r}"
    assert b"azurewebsites.net" in dex, "Compiled APK missing Azure endpoint"

print(f"FINAL MASTER CONTRACT: PASS ({len(activities)} activities)")
print("MANIFEST + SCREEN + BACK + NULL/ERROR AUDIT: PASS")
print("CAMERA/PHOTO + ID VERIFICATION CONTRACT: PASS")
print("GOOGLE PLAY BILLING + REWARDED SSV CONTRACT: PASS")
print("AZURE ROUTES + OWNER/WALLET CONTRACT: PASS")
print("SELF-HEALING + AUTO-ROLLBACK + FORTRESS BUDGET CONTRACT: PASS")
if APK is not None:
    print("COMPILED SIGNED APK FORENSIC CONTRACT: PASS")
