#!/usr/bin/env python3
from pathlib import Path
import re, sys, zipfile

ROOT=Path(__file__).resolve().parents[2]
APP=ROOT/"app/src/main/java/com/nikahbridge"
MANIFEST=(ROOT/"app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")

EXPECTED=[
"AccountDeletionActivity","AdvancedMatchFiltersActivity","AzureExternalAuthActivity","AzureHomeActivity",
"AzureWalletActivity","BlockedMembersActivity","CommunityChatActivity","CompatibilityDealBreakerActivity",
"CompatibilityTrafficLightActivity","ConversationHealthActivity","FamilyBridge2Activity","FamilyCircleActivity",
"FutureLifeSimulationActivity","GenderFilteredMatchesActivity","HelpLineActivity","HelpLineAdminActivity",
"ILikedActivity","IViewedActivity","IdentityVerificationActivity","LikedMeActivity","MainActivity",
"MarriageTimelineMatchingActivity","NikahAssistantActivity","NikahBlueprintActivity","NikahIntelligenceActivity",
"NikahJourneyActivity","NikahMediatorActivity","NikahSuccessNetworkActivity","NikahSuccessPlanActivity",
"OwnerEarningsActivity","OwnerLiveAnalyticsActivity","PostVerificationFeatureHubActivity","PremiumPlansActivity",
"PrivacyControlCenterActivity","ProductionCompletionActivity","ProductionMainActivity","ProfilePhotoActivity",
"RealFourPhotoActivity","RewardedMessageActivity","SafeCommunicationActivity","SeriousNikahPlusActivity",
"SmartSeriousQuestionsActivity","TermsAndCommunityGuidelinesActivity","TrustPassportActivity","UpgradeActivity",
"VerificationAdminActivity","ViewedMeActivity","WalletActivity","WelcomeActivity","WhyWeMatchedActivity"
]
actual=sorted(p.stem for p in APP.glob("*Activity.java"))
assert len(actual)==50, f"Expected exactly 50 Activity screens, found {len(actual)}"
assert actual==sorted(EXPECTED), "50-screen registry changed: "+str(sorted(set(actual)^set(EXPECTED)))

ROOT_EXEMPT={"WelcomeActivity","MainActivity","ProductionMainActivity","AzureHomeActivity"}
SCROLL_EXEMPT={"MainActivity"}
forbidden=[
"null null","not available in this backend release","updating on Azure",
"Reported real user ID","Reported user ID required","Real recipient user ID",
"Real matched member ID","Conversation ID from a real mutual connection",
"FirebaseAuth","FirebaseFirestore","FirebaseFunctions","FirebaseStorage","FirebaseAI",
"getHttpsCallable","com.google.firebase","DEMO ONLY","mock auth","fake data"
]

issues=[]
for name in EXPECTED:
    p=APP/(name+".java")
    t=p.read_text(encoding="utf-8")
    if f'android:name=".{name}"' not in MANIFEST:
        issues.append(f"{name}: missing manifest registration")
    if "setContentView(" not in t:
        issues.append(f"{name}: no setContentView")
    if name not in SCROLL_EXEMPT and "ScrollView" not in t:
        issues.append(f"{name}: no ScrollView / scroll-safe root")
    if name not in ROOT_EXEMPT and not re.search(r'\bBack\b|onBackPressed|finish\(\)',t):
        issues.append(f"{name}: no back-navigation contract")
    for bad in forbidden:
        if bad.lower() in t.lower():
            issues.append(f"{name}: forbidden/stale state -> {bad}")
    if re.search(r'setAlpha\(0\.[0-4]|alpha\s*=\s*0\.[0-4]',t):
        issues.append(f"{name}: blur/low-alpha pattern <= 0.4")
    if "Button" in t and "setOnClickListener(" not in t and "setPositiveButton(" not in t:
        issues.append(f"{name}: Button exists without any click-listener binding")
    # Guard against obvious clipped controls. Global app normalizer also enforces min-height/wrap.
    if re.search(r'new (?:LinearLayout|FrameLayout)\.LayoutParams\([^,]+,\s*dp\((?:[1-3]?\d)\)\)',t):
        issues.append(f"{name}: suspicious fixed control height under 40dp")

app=(APP/"NikahBridgeApplication.java").read_text(encoding="utf-8")
for required in [
"WindowCompat.setDecorFitsSystemWindows(a.getWindow(),false)",
"WindowInsetsCompat.Type.systemBars()","normalizeControls(a)","setMinHeight(dp(a,56))"
]:
    if required not in app:
        issues.append("GLOBAL UI SAFETY MISSING: "+required)

auth=(APP/"AzureAuthManager.java").read_text(encoding="utf-8")
for required in [
"return R.raw.auth_config;",
"85:A5:2E:95:4F:31:DF:AC:2C:10:62:A8:D6:4E:DA:2D:9B:48:5B:48",
"haUulU8x36wsEGKo1k7aLZtIW0g="
]:
    if required not in auth:
        issues.append("MSAL CURRENT-SIGNER CONTRACT MISSING: "+required)
for forbidden_auth in ["getSigningCertificateHistory()","R.raw.auth_config_play_current"]:
    if forbidden_auth in auth:
        issues.append("MSAL RETIRED-SIGNER REGRESSION: "+forbidden_auth)

for cfg in ["app/src/main/res/raw/auth_config.json","app/src/release/res/raw/auth_config.json"]:
    t=(ROOT/cfg).read_text(encoding="utf-8")
    if 'msauth://com.nikahbridge/haUulU8x36wsEGKo1k7aLZtIW0g%3D' not in t:
        issues.append(cfg+": current Play redirect missing")

if issues:
    print("50-SCREEN ULTRA FINAL STATIC GATE: FAIL")
    for x in issues: print(" -",x)
    sys.exit(1)

def scan_artifact(path: Path):
    assert path.is_file() and path.stat().st_size>0, f"Artifact missing: {path}"
    blob=b""
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            if re.search(r'(^|/)classes\d*\.dex$',n):
                blob+=z.read(n)
    assert blob, f"No compiled DEX found in {path}"
    for bad in [b"null null",b"FirebaseAuth",b"FirebaseFirestore",b"FirebaseFunctions",b"FirebaseStorage"]:
        assert bad not in blob, f"{path.name}: forbidden compiled content {bad!r}"
    for req in [b"azurewebsites.net",b"haUulU8x36wsEGKo1k7aLZtIW0g="]:
        assert req in blob, f"{path.name}: required compiled marker missing {req!r}"
    print(f"COMPILED FORENSIC PASS: {path.name}")

for arg in sys.argv[1:]:
    scan_artifact(Path(arg))

print("50-SCREEN ULTRA FINAL STATIC GATE: PASS")
print("Exact 50-screen registry + manifest + scroll/back + button-binding + blur/null/stale/Firebase + MSAL current-signer checks: PASS")
