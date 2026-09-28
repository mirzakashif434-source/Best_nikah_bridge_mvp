# Step 13 — Final Master Release Gate

Target branch: `azure-backend-additive`

This is the single final technical release gate before phone testing and Golden Lock.

It combines all prior protections:
- Azure-only production runtime
- Firebase production runtime/configuration = zero
- exact 50 Activity surface
- 42 canonical home/navigation release contract
- buttons, Back navigation, UI safety, no null/raw JSON/blur regressions
- Microsoft Entra auth, issuer/audience/JWKS/token validation
- Play upload SHA and Play App Signing SHA separation
- MSAL redirect hash lock
- versionCode 34 Play Console compatibility
- Google Play Billing + Azure purchase verification
- AdMob + UMP + rewarded SSV
- notifications
- 18+, identity/four-photo verification, Family/Wali, Community, safety, mutual chat
- account deletion, Terms, Privacy
- backend JavaScript syntax/module load
- Android compile/lint/release build
- signed APK + signed AAB verification
- final artifact upload

No existing feature Activity is deleted or replaced by Step 13.
