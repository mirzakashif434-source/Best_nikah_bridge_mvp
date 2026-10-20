#!/usr/bin/env python3
"""Audit-only 50-screen robot readiness matrix. This does NOT launch any Android screen."""
from pathlib import Path
import re,csv,json
root=Path(__file__).resolve().parents[2]
app=root/"app/src/main/java/com/nikahbridge"
paths=sorted(app.glob("*Activity.java"))
assert len(paths)==50, f"Expected 50 screens, got {len(paths)}"
rows=[]
for p in paths:
 s=p.read_text(encoding="utf-8")
 # Conservative: no live app activity is started without a network-isolated Android runtime.
 auth=bool(re.search(r"AzureAuthManager|AzureExternalAuthActivity|requireOwner|PremiumFeatureGate",s))
 api=bool(re.search(r"AzureApiClient|AzureWalletApi|AzureAuthManager\.acquireToken|\bMobileAds\b|BillingClient|RewardedAd",s))
 mutation=bool(re.search(r"AzureApiClient\.(post|put|patch|delete)|AzureWalletApi\.post|\.launchBillingFlow\(",s))
 click=len(re.findall(r"setOnClickListener\s*\(",s))
 back=bool(re.search(r"\bBack\b|onBackPressed|finish\s*\(",s))
 reason="needs stable isolated Android runtime"
 if auth or api: reason+="; authenticated or API-dependent screen" if auth else "; API-dependent screen"
 if mutation: reason+="; mutable controls must not be auto-clicked"
 rows.append(dict(screen=p.stem,source_button_listeners=click,back_source_marker=back,
                  auth_or_premium_guard=auth,api_or_billing_reference=api,
                  mutation_reference=mutation,runtime_result="NOT_RUN",next_action=reason))
out=root/"audit-50-screen-robot-matrix.csv"
with out.open("w",newline="",encoding="utf-8") as f:
 w=csv.DictWriter(f,fieldnames=rows[0]);w.writeheader();w.writerows(rows)
print("AUDIT MATRIX WRITTEN:",out.name)
print("SCREENS:",len(rows),"WITH API/BILLING REFERENCES:",sum(x["api_or_billing_reference"] for x in rows))
print("WITH MUTATION REFERENCES:",sum(x["mutation_reference"] for x in rows))
print("RUNTIME CLICK PASS: 0/50 (NOT EXECUTED; never mark green from static checks)")
