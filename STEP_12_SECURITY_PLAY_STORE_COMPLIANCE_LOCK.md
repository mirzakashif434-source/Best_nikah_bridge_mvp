# Step 12 — Security and Play Store Compliance Master Lock

Target branch: `azure-backend-additive`

Step 12 locks the release-critical security and Play Store / Play Console compliance surface.

## Android / Play requirements
- package: `com.nikahbridge`
- compileSdk 36
- targetSdk 36
- minSdk 24
- release signing through GitHub secrets only
- upload certificate SHA-1 verified separately from Google Play App Signing SHA-1
- Microsoft MSAL redirect tied to Play App Signing certificate
- cleartext traffic disabled
- Android backup disabled
- FileProvider is non-exported
- only required launcher/deep-link/MSAL components are exported
- notification permission declared for Android 13+
- Ad ID permission + AdMob application ID declared
- no Firebase production runtime/configuration

## User-data / safety requirements
- permanent Azure account deletion flow
- Terms & Community Guidelines acceptance stored in Azure
- privacy controls available
- report/block moderation paths available
- 18+ verification protections
- Google Play Billing used for subscriptions
- UMP consent flow used for ads
- rewarded credits granted only after AdMob SSV validation

## Preservation
- all 50 Activities remain present
- no existing feature Activity is deleted or replaced
- Azure remains the only production backend target
