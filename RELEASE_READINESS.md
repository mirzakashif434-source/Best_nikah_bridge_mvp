# Best Nikah Bridge — Release Readiness

## Product identity
- Product name: **Best Nikah Bridge**
- Android application ID: `com.nikahbridge`
- Current release: `1.2` (`versionCode 4`)
- Target/compile SDK: 36

## Product pillars
1. Nikah Readiness Score
2. Family/Wali Connect
3. Why We Matched
4. Intent Verification
5. Scam Shield
6. Privacy Control Center
7. Mutual-Only Communication
8. Compatibility Deal-Breakers
9. Marriage Timeline Matching
10. AI Nikah Assistant

## Current repository status
- Firebase Authentication and Firestore dependencies are configured.
- Release signing is supplied through GitHub Actions secrets.
- Release AAB workflow builds `bundleRelease` and uploads the AAB artifact.
- Gradle wrapper is committed to the repository.
- The Android manifest uses the single visible product name `Best Nikah Bridge`.

## Production blockers that must not be faked as complete
The client application alone cannot safely implement server-authoritative identity verification, moderation, Firestore security rules, abuse/rate limiting, secure mutual-chat authorization, account deletion, or an actual AI service key. These require backend/admin configuration and must be verified before public launch.

## Release gate
A build succeeding is necessary but not sufficient for Play Store launch. Before production publication, verify:
- Firestore Security Rules deny unauthorized reads/writes.
- Interest acceptance is server-authoritative and cannot be forged by a client.
- Chat access is allowed only after mutual acceptance.
- Verification status can only be changed by the trusted verification/admin path.
- Report/block and moderation flows are enforced server-side.
- Account deletion removes or anonymizes the user's data as required.
- Privacy policy, data-safety declarations, support contact, and Play Console forms are complete.
- The signed release AAB installs and passes a real-device smoke test.
