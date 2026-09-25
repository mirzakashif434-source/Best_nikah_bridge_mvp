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

# CONTRACT 12 — Blocked Members: IDs stay internal, real Azure list/delete remain wired.
blocked=contains(APP/"BlockedMembersActivity.java",
    'AzureApiClient.get("/blocks"',
    'AzureApiClient.delete("/blocks/"+userId',
    'Button unblock=button("Unblock",true)',
    'unblock.setOnClickListener(v->unblock(target,row,unblock))',
    'final String target=userId;',
    'Button refresh=button("Refresh",false)',
    'Button back=button("Back",false)',
    'ScrollView',
    'WindowInsetsCompat.Type.systemBars()')
need("Blocked Members leaked a technical member ID", "Member ID:" not in blocked)
need("Blocked Members contains malformed literal \\n after a // comment",
     re.search(r'//[^\n]*\\n\s*(Button|TextView|LinearLayout|AzureApiClient)', blocked) is None)
blocks_backend=contains(AZ/"blocks.js",
    'route:"blocks"',
    'route:"blocks/{userId}"',
    'CANNOT_BLOCK_SELF',
    'BLOCK_NOT_FOUND')

# CONTRACT 13 — Production owner access: real owner analytics route only.
prod=contains(APP/"ProductionMainActivity.java",
    'AzureApiClient.get("/owner/live-analytics"',
    'AzureApiClient.get("/admin/verifications"')
need("ProductionMainActivity reverted to wrong admin owner analytics route",
     '/admin/owner/live-analytics' not in prod)
owner_backend=contains(AZ/"ownerAnalytics.js",
    'route:"owner/live-analytics"',
    'ADMIN_REQUIRED')

# CONTRACT 14 — Azure-only runtime and release safety.
all_java="\n".join(read(p) for p in APP.glob("*.java"))
for bad in ("FirebaseAuth","FirebaseFirestore","FirebaseFunctions","FirebaseStorage","getHttpsCallable","com.google.firebase"):
    need(f"Firebase runtime regression returned: {bad}",bad not in all_java)
manifest=contains(ROOT/"app/src/main/AndroidManifest.xml",
    'android:usesCleartextTraffic="false"',
    'android:allowBackup="false"')

# CONTRACT 15 — Step 1 20 SAR paid feature lock.
premium_access=read(AZ/"premiumAccess.js")
need("20 SAR capability missing", "paid20Features:basic" in premium_access)
need("20 SAR Basic must keep Family Circle expansion to 4", "familyCircleLimit:vip?10:plus?7:basic?4:2" in premium_access)

paid20_client_files=[
    "NikahJourneyActivity.java",
    "NikahBlueprintActivity.java",
    "SmartSeriousQuestionsActivity.java",
    "WhyWeMatchedActivity.java",
    "AdvancedMatchFiltersActivity.java",
    "CompatibilityDealBreakerActivity.java",
    "CompatibilityTrafficLightActivity.java",
    "MarriageTimelineMatchingActivity.java",
    "TrustPassportActivity.java",
    "NikahIntelligenceActivity.java",
    "AzureWalletActivity.java",
]
for name in paid20_client_files:
    t=read(APP/name)
    need(f"{name}: 20 SAR client entitlement gate missing",
         'PremiumFeatureGate.require(this,"paid20Features","20 SAR Basic or higher"' in t)

profile_photo=read(APP/"ProfilePhotoActivity.java")
need("Profile Photo upload 20 SAR gate missing",
     'PremiumFeatureGate.require(this,"paid20Features","20 SAR Basic or higher",this::uploadImageAzure)' in profile_photo)

journey_paid=read(AZ/"journeySummary.js")
need("Nikah Journey Azure route must enforce 20 SAR",
     "capabilities.paid20Features" in journey_paid and "PREMIUM_BASIC_REQUIRED" in journey_paid)

filters_paid=read(AZ/"seriousNikahPlus.js")
need("Advanced Filters Azure routes must enforce 20 SAR",
     filters_paid.count("capabilities.paid20Features") >= 2 and filters_paid.count("PREMIUM_BASIC_REQUIRED") >= 3)

photos_paid=read(AZ/"photos.js")
need("Profile Photo upload Azure route must enforce 20 SAR",
     "capabilitiesFor(premium).paid20Features" in photos_paid and "PREMIUM_BASIC_REQUIRED" in photos_paid)

wallet_paid=read(AZ/"wallet.js")
need("Azure Wallet routes must enforce 20 SAR",
     wallet_paid.count("capabilitiesFor(premium).paid20Features") >= 3 and wallet_paid.count("PREMIUM_BASIC_REQUIRED") >= 3)

paid20_doc=read(ROOT/"PAID_20_FEATURES_LOCK.md")
for required in ("premium_basic_20","Nikah Journey","Real Profile Photo upload","Azure Wallet","Azure AI remains a separate 60 SAR VIP-only entitlement"):
    need(f"20 SAR documentation contract missing: {required}", required in paid20_doc)

# CONTRACT 16 — Fortress Budget Mode: additive cost guards must stay intact.
deploy=read(ROOT/".github/workflows/azure-backend-deploy.yml")
host=read(ROOT/"azure-backend/host.json")
db=read(ROOT/"azure-backend/src/db.js")
cost_guard=read(ROOT/"azure-backend/src/costGuard.js")
fortress=read(ROOT/"FORTRESS_BUDGET_MODE.md")
for required in (
    '--instance-memory 512',
    '--maximum-instance-count 20',
    'always-ready delete',
    'AZURE_DB_POOL_MAX=3',
    'AI_DAILY_LIMIT_VIP=12',
    'AI_NIKAH_MAX_TOKENS=350',
    'AI_ADVANCED_MAX_TOKENS=500',
    'bnb-verification-documents-cool',
    'Self-Healing Golden Master',
    'Auto-rollback Azure backend to last known-good Golden release',
):
    need(f"Fortress Budget deploy contract missing: {required}", required in deploy)
need("Fortress Budget telemetry cap must remain 2/sec", '"maxTelemetryItemsPerSecond": 2' in host)
need("Fortress Budget DB pool default must remain 3", 'AZURE_DB_POOL_MAX || 3' in db)
need("Fortress Budget VIP AI quota default must remain 12", 'AI_DAILY_LIMIT_VIP", 12' in cost_guard)
need("AI quota must not be available to Premium Plus 40", 'premium_plus_40' not in cost_guard)
premium_access=read(ROOT/"azure-backend/src/premiumAccess.js")
need("Advanced Azure AI must be VIP-only", 'aiAdvanced:vip' in premium_access)
need("Nikah Assistant must be VIP-only", 'aiNikahAssistant:vip' in premium_access)
ai_compat=read(ROOT/"azure-backend/src/aiCompatibility.js")
need("AI Mediator/Future Life backend must enforce VIP", 'capabilities.aiAdvanced' in ai_compat and 'PREMIUM_VIP_REQUIRED' in ai_compat)
for required in ("512 MB","20","150-200 SAR","Self-Healing","Auto-Rollback"):
    need(f"Fortress Budget documentation contract missing: {required}", required in fortress)

if fail:
    print("LOCKED FEATURE CONTRACTS: FAIL")
    for x in fail: print(" -",x)
    sys.exit(1)

print(f"LOCKED FEATURE CONTRACTS: PASS ({len(activities)} activities)")
print("Protected contracts: Family/Wali, Journey, Safety, Premium Back, Auth, Rewarded, Terms, match selectors, Success Plan, Wallet, 46-screen UI, Azure-only runtime, Fortress Budget Mode.")
