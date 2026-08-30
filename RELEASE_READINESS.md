# Best Nikah Bridge — Production Release Gate

## Current build
- Product: **Best Nikah Bridge**
- Application ID: `com.nikahbridge`
- Release candidate: **2.1** (`versionCode 6`)
- Compile/target SDK: **36**
- Launcher: `ProductionMainActivity`
- Backend: Firebase Authentication + Firestore + callable Cloud Functions

## Core production pillars
1. Nikah Readiness Score
2. Family/Wali Connect
3. Why We Matched
4. Intent Verification
5. Scam Shield
6. Privacy / Delete Account
7. Mutual-Only Communication
8. Compatibility Deal-Breakers
9. Marriage Timeline
10. Nikah Assistant / Help

## Implemented and hardened
- Real Firebase email/password authentication.
- Real Firestore profile storage and active-profile matching.
- Mutual-interest flow with Firebase-backed connection authorization.
- Firestore rules restrict chat to active mutual connections and block unauthorized connection writes.
- Verification requests are stored in Firebase; verification status is admin-controlled by Cloud Functions.
- Reports are stored and queued for moderation.
- Wali connection requests use a trusted callable function.
- Account deletion is handled by a trusted callable function and removes the user's primary data plus related records.
- Release signing is supplied through GitHub Actions secrets.
- Android Release AAB workflow successfully builds and uploads the signed artifact.
- API 36 is used, which meets the Google Play target requirement effective August 31, 2026.

## Remaining launch gates — do not pretend these are complete
These items require real operational setup/testing and are not safely solved by UI text alone:
- Actual identity/document verification process and admin verification operations.
- Full server-side matching/compatibility scoring rather than generic match explanations.
- A real AI provider/service and secured server-side API credential for the Nikah Assistant.
- Complete block/unblock UI and abuse/rate-limit automation.
- Production privacy policy URL, support contact, Data Safety declarations and Play Console forms.
- Real-device smoke tests covering account creation, profile, interest, mutual chat, report, Wali request and account deletion.
- Firebase production rules/functions deployment must be confirmed after the latest commit.

## Release rule
A green GitHub Actions build is necessary but **not sufficient** for public Play Store launch. The final AAB should only be submitted after the remaining operational gates above pass.
