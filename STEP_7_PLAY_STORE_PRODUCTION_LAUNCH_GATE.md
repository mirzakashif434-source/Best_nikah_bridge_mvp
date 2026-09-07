# BEST NIKAH BRIDGE — STEP 7 PLAY STORE PRODUCTION LAUNCH GATE

Status: STARTED — 2026-09-07

This is an additive production-launch gate. It does not replace or delete previous application features, release workflows, or backend modules.

## Real-only launch rules
- No demo/test AdMob IDs.
- No mock/fake user data, fake matches, fake balances, fake purchases, or fake verification states.
- No debug build may be submitted to Google Play.
- Production release must use the real application ID and production signing configuration.
- The exact release artifact must be traceable to a Git commit and checksum.

## Play Store launch checklist
1. Signed production AAB passes Step 6 release gate.
2. Application ID/package identity is verified.
3. VersionCode/versionName are verified and increased as required for the Play upload.
4. Target/compile SDK and release dependencies pass the current Play requirements.
5. Privacy policy is publicly reachable and matches the actual app behavior.
6. Data Safety declarations are prepared from the actual data flows.
7. Content rating questionnaire is completed accurately.
8. Target audience and age-related declarations are completed accurately.
9. App access instructions are prepared if Google reviewers need authenticated access.
10. Ads declaration is completed accurately because the app uses AdMob.
11. Play Billing declarations and product configuration are checked for any paid digital features.
12. Store listing assets and descriptions contain no misleading claims.
13. Account deletion/data deletion process is documented and operational.
14. Firebase production deployment is successful before claiming server-side production readiness.
15. Real-device smoke testing is completed on the exact release candidate.
16. Play Console upload and review are completed by the owner; repository automation cannot truthfully mark a Play submission as completed without Play Console evidence.

## Current known external gates
- Firebase Functions deployment remains dependent on the production Firebase billing/Blaze activation being available.
- AdMob serving remains dependent on completion of the AdMob payment setup/account requirements.
- Play Console declarations and submission require the owner's Play Console access and cannot be fabricated by repository code.

## Privacy policy
The repository contains `privacy.html`. It has been updated additively for the production feature set, including authentication/profile data, verification, community/safety, AI features, AdMob, service providers, purchases, retention, and deletion requests.

## Owner protection
Earlier work must remain intact. Step 7 changes must be additive unless the owner explicitly approves a replacement. A failed gate is a reason to fix the failing item, not to delete an existing production feature.
