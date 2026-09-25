#!/usr/bin/env python3
from pathlib import Path
import re, sys, zipfile

ROOT = Path(__file__).resolve().parents[2]
HOME = ROOT / "app/src/main/java/com/nikahbridge/AzureHomeActivity.java"
FLOW = ROOT / ".maestro/signed-in-38-screen-master.yaml"

SCREENS = [
    ("My Real Azure Profile", "profile.setOnClickListener"),
    ("Real Compatibility Matches", "matches.setOnClickListener"),
    ("Serious Nikah Plus", "seriousPlus.setOnClickListener"),
    ("Premium Plans — 20 / 40 / 60 SAR", "premiumPlans.setOnClickListener"),
    ("Mutual Interests", "interests.setOnClickListener"),
    ("Family / Wali Connect", "family.setOnClickListener"),
    ("Privacy Controls", "privacy.setOnClickListener"),
    ("Verification Status", "verification.setOnClickListener"),
    ("Safety Reports", "safety.setOnClickListener"),
    ("Azure AI Nikah Assistant", "ai.setOnClickListener"),
    ("Real Profile Photo", "profilePhoto.setOnClickListener"),
    ("Four-Photo Verification", "fourPhotos.setOnClickListener"),
    ("Global Community Chat", "communityChat.setOnClickListener"),
    ("Earn Message Credits", "rewardedMessage.setOnClickListener"),
    ("Blocked Members", "blockedMembers.setOnClickListener"),
    ("Terms & Community Guidelines", "terms.setOnClickListener"),
    ("Nikah Journey", "nikahJourney.setOnClickListener"),
    ("Nikah Blueprint", "nikahBlueprint.setOnClickListener"),
    ("Smart Serious Questions", "smartQuestions.setOnClickListener"),
    ("Why We Matched", "whyMatched.setOnClickListener"),
    ("Advanced Match Filters", "advancedFilters.setOnClickListener"),
    ("Compatibility Deal-Breakers", "dealBreakers.setOnClickListener"),
    ("Compatibility Traffic Light", "trafficLight.setOnClickListener"),
    ("Marriage Timeline Matching", "timeline.setOnClickListener"),
    ("Family Circle", "familyCircle.setOnClickListener"),
    ("Trust Passport", "trustPassport.setOnClickListener"),
    ("Safe Communication", "safeCommunication.setOnClickListener"),
    ("Nikah Intelligence", "nikahIntelligence.setOnClickListener"),
    ("Conversation Health", "conversationHealth.setOnClickListener"),
    ("AI Nikah Mediator", "nikahMediator.setOnClickListener"),
    ("Nikah Success Plan", "successPlan.setOnClickListener"),
    ("Nikah Success Network", "successNetwork.setOnClickListener"),
    ("Future Life Simulation", "futureSimulation.setOnClickListener"),
    ("Owner Live Analytics", "ownerAnalytics.setOnClickListener"),
    ("Owner Wallet / Payout", "ownerWallet.setOnClickListener"),
    ("Azure Wallet", "wallet.setOnClickListener"),
    ("Permanently Delete Account", "delete.setOnClickListener"),
    ("Sign out of Azure", "out.setOnClickListener"),
]

assert len(SCREENS) == 38

home = HOME.read_text(encoding="utf-8")
flow = FLOW.read_text(encoding="utf-8")

for label, listener in SCREENS:
    if label not in home:
        raise SystemExit(f"HOME LABEL MISSING: {label}")
    if listener not in home:
        raise SystemExit(f"CLICK LISTENER MISSING: {label} -> {listener}")

    # Maestro may intentionally use a stable regex for labels with punctuation/price text.
    if label.startswith("Premium Plans"):
        maestro_ok = ("Premium Plans.*" in flow) or (label in flow)
    else:
        maestro_ok = label in flow
    if not maestro_ok:
        raise SystemExit(f"MAESTRO FLOW MISSING: {label}")

# Destructive controls must be test-safe: verify dialogs, never confirm destructive actions.
for required in ["CANCEL", "Delete Azure account data permanently", "Sign out of Azure"]:
    if required not in flow:
        raise SystemExit(f"DESTRUCTIVE-SAFETY FLOW CONTRACT MISSING: {required}")

apk = Path(sys.argv[1]) if len(sys.argv) > 1 else None
if apk:
    if not apk.is_file() or apk.stat().st_size == 0:
        raise SystemExit(f"APK MISSING/EMPTY: {apk}")
    dex = b""
    with zipfile.ZipFile(apk) as z:
        for name in z.namelist():
            if re.fullmatch(r"classes\d*\.dex", name):
                dex += z.read(name)
    for label, _ in SCREENS:
        if label.encode("utf-8") not in dex:
            # Premium label is compiled with the long dash and may be encoded through resource processing;
            # accept the stable prefix if the full string is transformed.
            if label.startswith("Premium Plans") and b"Premium Plans" in dex:
                continue
            raise SystemExit(f"COMPILED APK SCREEN LABEL MISSING: {label}")
    for forbidden in [b"FirebaseAuth", b"FirebaseFirestore", b"FirebaseFunctions", b"FirebaseStorage"]:
        if forbidden in dex:
            raise SystemExit(f"FORBIDDEN FIREBASE RUNTIME STRING FOUND: {forbidden.decode()}")
    if b"azurewebsites.net" not in dex:
        raise SystemExit("COMPILED APK AZURE ENDPOINT STRING MISSING")

print("38-SCREEN MASTER CONTRACT: PASS")
print("38/38 HOME LABELS: PASS")
print("38/38 CLICK LISTENERS: PASS")
print("38/38 MAESTRO COVERAGE: PASS")
if apk:
    print("38/38 COMPILED APK LABELS + AZURE RUNTIME: PASS")
