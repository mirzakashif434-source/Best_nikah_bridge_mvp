# Release 34 Pre-Upload Golden Lock

Status: PRE-UPLOAD GOLDEN LOCKED

This checkpoint freezes the verified release-34 production truth before Play Console upload.

## Exact verified AAB
- File: app-release(20260928-215020).aab
- SHA-256: 6e9d5610f5dfdc35930a262f87a1193d07892e734c85aa06b1b28e835e7005ed
- Package: com.nikahbridge
- versionCode: 34
- versionName: 2.5
- minSdk: 24
- targetSdk: 36
- Upload certificate SHA-1: 41097230F8DBA1DB824958C851D8284C65BA5048

## Play / Microsoft identity truth
- Actual Google Play app-signing SHA-1: 592FB39683C3697B2C9FF448AF400699DE2257E9
- MSAL signature hash: WS+zloPDaXssn/RIr0AGmd4iV+k=
- Encoded redirect: msauth://com.nikahbridge/WS%2BzloPDaXssn%2FRIr0AGmd4iV%2Bk%3D
- Azure App Registration redirect was updated through Microsoft Graph and verified green.

## Frozen production checks
- 50/50 app Activities present.
- Azure production API URL and Azure API scope present.
- Firebase Auth / Firestore / Functions / Storage runtime signatures absent.
- Android backup disabled.
- Cleartext traffic disabled.
- Billing 9.1.0 present.
- Google Mobile Ads 25.4.0 present.
- UMP 4.0.0 present.
- Premium products present.
- Exact AAB ZIP integrity passed.
- AAB signature verification passed.

## Green gates
- Step 4 Azure Android Redirect Cleanup #4 — success
- Final Auth Signing Play Store Match #4 — success
- Step 3 Azure Auth SHA Play Store Master Lock #11 — success
- Step 12 Security Play Store Compliance Master Lock #12 — success
- Step 13 Final Master Release Gate #9 — success
- Azure Additive Backend + Android Verification #737 — success

## Preservation rule
Do not delete, replace, or rewrite previously locked feature work to change this release.

## Runtime completion rule
This is the final PRE-UPLOAD lock, not final runtime acceptance.
Final Runtime Golden Lock is permitted only after version 34 is installed from Google Play Internal Testing and real Microsoft/Azure sign-in succeeds on device.
