# Best Nikah Bridge — Play Store Data Safety Declaration Guide

Last reviewed: 2026-09-07

This file is a release-operations checklist, not a substitute for the Play Console declaration. The final answers must match the production APK/AAB, Firebase configuration, AdMob/Google services configuration, and actual data practices at submission time.

## Data categories to review before submission

- Account information: email/authentication identifiers and account/profile information.
- Personal/profile information: name, age, country/location or other profile fields actually collected.
- Photos/media: profile photos and any uploaded user-generated media.
- Messages/user content: private messages, community posts, reports, and other content actually stored.
- Identity/verification information: documents and verification information submitted for identity review.
- App activity / diagnostics: only categories actually collected by enabled SDKs and app code.
- Purchases: Google Play purchase/entitlement information used to provide premium access.
- Advertising data: AdMob/rewarded-ad related processing according to the configured consent and Google policies.

## Required release checks

1. Use the exact production release AAB when completing the final declaration.
2. Verify every selected data type against the live Firebase/Google configuration and app behavior.
3. Mark whether each category is collected, shared, required/optional, and the applicable purposes based on the actual implementation.
4. Confirm the public privacy policy URL is active and matches the release.
5. Confirm the external account/data deletion URL is active and matches the in-app deletion flow.
6. Complete the app's content rating, target audience, ads declaration, and required child-safety declarations.
7. Because this is a matchmaking service intended for adults, configure Play Console's applicable minor-access restriction/age controls.
8. Before production submission, perform real-device checks for sign-in, profile creation, profile visibility, photo upload, verification, messaging, community content, deletion, rewarded ads, premium billing, and privacy controls.

## Do not guess

If a Play Console question depends on actual SDK behavior or a production setting, verify the current production configuration first. This checklist intentionally avoids claiming a data category is or is not collected until the release configuration has been checked.
