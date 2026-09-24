from pathlib import Path
import re, sys

ROOT=Path(".")
APP=ROOT/"app/src/main/java/com/nikahbridge"
AZ=ROOT/"azure-backend/src"

fail=[]

def read(p):
    p=Path(p)
    if not p.exists():
        fail.append(f"MISSING FILE: {p}")
        return ""
    return p.read_text(encoding="utf-8")

def need(label, cond):
    if not cond: fail.append(label)

def contains(path, *items):
    t=read(path)
    for item in items:
        need(f"{path}: missing required contract -> {item}", item in t)
    return t

def forbids(path, *items):
    t=read(path)
    for item in items:
        need(f"{path}: forbidden regression returned -> {item}", item not in t)
    return t

# CONTRACT 1 — Family/Wali: automatic secure IDs, real Azure routes, usable screen.
family=contains(APP/"FamilyBridge2Activity.java",
    'AzureApiClient.get("/family-links"',
    'AzureApiClient.get("/family-links/pending-for-wali"',
    'AzureApiClient.post("/family-links/"',
    'AzureApiClient.delete("/family-links/"',
    'Verify This Wali Request',
    'Revoke This Wali Connection',
    'Refresh Family / Wali',
    'btn("Back",false)',
    'WindowInsetsCompat.Type.systemBars()',
    'ScrollView')
for x in ('Family link ID','Wali phone (E.164)'):
    need(f"Family/Wali raw technical field returned: {x}", x not in family)
family_backend=contains(AZ/"family.js",
    'route:"family-links/pending-for-wali"',
    'route:"family-links/{familyLinkId}/verify"',
    'route:"family-links/{familyLinkId}"')
need("Family backend must verify Wali identity before accepting request",
     "WALI_IDENTITY_MISMATCH" in family_backend)

# CONTRACT 2 — Journey: one atomic summary, never partial/fake progress.
journey=contains(APP/"NikahJourneyActivity.java",
    'AzureApiClient.get("/journey/summary"',
    'Refresh My Journey',
    'btn("Back",false)',
    'Journey progress:',
    'WindowInsetsCompat.Type.systemBars()',
    'ScrollView')
for x in ('Some Azure journey data could not be loaded','Currently visible:','hadLoadError'):
    need(f"Journey partial/fallback regression returned: {x}", x not in journey)
journey_backend=contains(AZ/"journeySummary.js",'route:"journey/summary"')
for required in ("profileReady","blueprintReady","verified","hasMatches","hasConversation","hasFamily"):
    need(f"Journey summary missing field {required}", required in journey_backend)

# CONTRACT 3 — Safety report: chooser based, no manual technical ID.
home=contains(APP/"AzureHomeActivity.java",
    'Choose Member to Report',
    'AzureApiClient.get("/matches"',
    'selectedReportedUserId',
    'AzureApiClient.post("/safety/reports"',
    'Submit Safety Report')
for x in ('Reported real user ID','Reported user ID required'):
    need(f"Safety raw-ID regression returned: {x}", x not in home)
safety=contains(AZ/"safety.js",'route: "safety/reports"','CANNOT_REPORT_SELF','INVALID_REPORT_REASON')

# CONTRACT 4 — Premium gate: Back must return to source, not a destroyed/blank activity.
premium=read(APP/"PremiumFeatureGate.java")
marker='.setPositiveButton("View Plans"'
need("Premium gate View Plans action missing", marker in premium)
if marker in premium:
    st=premium.index(marker)
    en=premium.find('.setNegativeButton("Not now"', st)
    need("Premium gate dialog boundary missing", en>st)
    if en>st:
        block=premium[st:en]
        need("Premium plans navigation missing", "PremiumPlansActivity.class" in block)
        need("Premium source Activity is still destroyed on View Plans", "a.finish()" not in block)

# CONTRACT 5 — Silent auth must never launch surprise interactive UI.
auth=read(APP/"AzureAuthManager.java")
st=auth.find("private static void acquireTokenSilentForWaiters()")
en=auth.find("static void acquireTokenInteractive(",st)
need("Silent auth method missing", st>=0 and en>st)
if st>=0 and en>st:
    silent=auth[st:en]
    need("Silent auth unexpectedly launches interactive sign-in",
         "acquireTokenInteractiveForWaiters();" not in silent)
    need("Silent auth must return explicit interaction-required state",
         "AZURE_INTERACTION_REQUIRED" in silent)

# CONTRACT 6 — Rewarded messages: real AdMob + Azure SSV + consent, no profile visibility gate.
reward=contains(APP/"RewardedMessageActivity.java",
    'AzureApiClient.get("/admob/rewarded/config"',
    'ServerSideVerificationOptions',
    'setUserId(uid)',
    'UserMessagingPlatform',
    'Watch Rewarded Ad',
    'Back',
    'ScrollView')
forbids(APP/"RewardedMessageActivity.java",'Azure rewarded configuration unavailable.')
reward_backend=read(AZ/"rewardedAds.js")
a=reward_backend.find("async function activeRewardedUser")
b=reward_backend.find('app.http("getRewardedAdConfig"',a)
need("Rewarded backend eligibility function missing",a>=0 and b>a)
if a>=0 and b>a:
    block=reward_backend[a:b]
    need("Rewarded eligibility regressed to profile_completed gate","profile_completed" not in block)
    need("Rewarded eligibility regressed to is_visible gate","is_visible" not in block)

# CONTRACT 7 — Terms title wrapping.
terms=contains(APP/"TermsAndCommunityGuidelinesActivity.java",'title.setMaxLines(2)')
need("Terms title fixed-height clipping returned",
     'content.addView(title,new LinearLayout.LayoutParams(-1,dp(65)))' not in terms)

# CONTRACT 8 — Matching selectors: technical IDs stay internal.
selector_files=[
    "SmartSeriousQuestionsActivity.java",
    "FamilyCircleActivity.java",
    "TrustPassportActivity.java",
    "CompatibilityDealBreakerActivity.java",
    "CompatibilityTrafficLightActivity.java",
]
for name in selector_files:
    t=contains(APP/name,'AzureApiClient.get("/matches"','matchChoices.addView(choose')
    for bad in ("Real matched member ID","Real member ID"):
        need(f"{name}: manual ID field returned -> {bad}",bad not in t)
safe=read(APP/"SafeCommunicationActivity.java")
need("Safe Communication must load real conversations", 'AzureApiClient.get("/conversations"' in safe)
need("Safe Communication manual conversation ID returned",
     'Conversation ID from a real mutual connection' not in safe)

# CONTRACT 9 — Success Plan refresh acknowledgement.
contains(APP/"NikahSuccessPlanActivity.java",
    'Refreshed from Azure at',
    'Real progress refreshed from Azure.')

# CONTRACT 10 — Azure Wallet explicit real refresh results.
contains(APP/"AzureWalletActivity.java",
    'Wallet refreshed from Azure',
    'Azure transactions refreshed')

# CONTRACT 11 — Whole-screen structural safety for all production activities.
activities=sorted(APP.glob("*Activity.java"))
need(f"Expected >=46 Activity screens, found {len(activities)}",len(activities)>=46)
back_exempt={"WelcomeActivity.java","MainActivity.java","ProductionMainActivity.java","AzureHomeActivity.java"}
for p in activities:
    t=read(p)
    if p.name!="MainActivity.java":
        need(f"{p.name}: ScrollView missing","ScrollView" in t)
    if p.name not in back_exempt:
        need(f"{p.name}: Back navigation missing","Back" in t)
    need(f"{p.name}: null null regression","null null" not in t)
    need(f"{p.name}: blur-like low alpha regression",
         re.search(r"setAlpha\(0\.[0-4]|alpha\s*=\s*0\.[0-4]",t) is None)

# CONTRACT 12 — Azure-only runtime and release safety.
all_java="\n".join(read(p) for p in APP.glob("*.java"))
for bad in ("FirebaseAuth","FirebaseFirestore","FirebaseFunctions","FirebaseStorage","getHttpsCallable","com.google.firebase"):
    need(f"Firebase runtime regression returned: {bad}",bad not in all_java)
manifest=contains(ROOT/"app/src/main/AndroidManifest.xml",
    'android:usesCleartextTraffic="false"',
    'android:allowBackup="false"')

# Intentionally NOT checked here:
# 1) BlockedMembersActivity user-facing "Member ID:" (known pending issue)
# 2) ProductionMainActivity privileged owner analytics route (known pending issue)
# Those two are handled in the next repair step, not hidden by this lock.

if fail:
    print("LOCKED FEATURE CONTRACTS: FAIL")
    for x in fail: print(" -",x)
    sys.exit(1)

print(f"LOCKED FEATURE CONTRACTS: PASS ({len(activities)} activities)")
print("Protected contracts: Family/Wali, Journey, Safety, Premium Back, Auth, Rewarded, Terms, match selectors, Success Plan, Wallet, 46-screen UI, Azure-only runtime.")
