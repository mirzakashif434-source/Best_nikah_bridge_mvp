# STEP 33 — REAL EXTERNAL SERVICES TEST LOCK

Status target: external-service verification only. No Play Console upload/publish.

Preservation contract:
- Branch: `azure-backend-additive`
- Existing Steps 1-32 are preserved.
- No Firebase runtime reintroduction.
- Azure backend remains authoritative.
- Package/version/signing values are unchanged.

Step 33 verifies:
1. Google Play Billing client wiring for purchase, restore and Azure entitlement verification.
2. Rewarded AdMob path with Azure authenticated config + SSV verifier contract.
3. Camera-first Photo 1 and exactly three gallery photos, with Azure upload path.
4. Live Azure production health is reachable.
5. Protected Azure endpoints reject unauthenticated requests.
6. Microsoft External ID authority metadata and AdMob verifier-key endpoint are reachable.
7. Signed release APK still builds using the locked upload key.
8. Master Gate -> Maestro E2E -> Self-Healing -> Auto-Rollback -> Fortress Budget.

Important external-device boundary:
A real licensed Play purchase/restore, a real rewarded-ad completion, and a successful authenticated-user API call require a real signed-in test user/device and cannot be truthfully fabricated by CI. The gate therefore proves all automatable production dependencies now and keeps those three device-only proofs explicit for final physical-device acceptance.