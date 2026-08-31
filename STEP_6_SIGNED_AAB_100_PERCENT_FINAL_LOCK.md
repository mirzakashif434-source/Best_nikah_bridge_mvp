# BEST NIKAH BRIDGE — STEP 6 SIGNED AAB

**Status: FINAL LOCKED — 2026-08-31**

This is the mandatory production AAB/release-engineering baseline. It is a release gate, not a claim that a signed AAB has already been built or that Play Console submission has already occurred.

## AAB requirements
- Build the Android App Bundle from the intended production source and production configuration.
- Use a secure production release signing key; never commit the private signing key or passwords to GitHub.
- Verify application ID/package identity and release versionCode/versionName.
- Build with the project's current supported SDK/toolchain and resolve all build warnings/errors that affect release quality.
- Run the complete Step 5 release test suite against the exact release candidate.
- Verify Firebase production configuration and release-only behavior.
- Verify Play Billing entitlement/restore behavior and payment configuration where applicable.
- Verify wallet/payout configuration where applicable; no demo balances or demo transactions.
- Verify manifest permissions, privacy-sensitive behavior and release configuration.
- Verify that no debug/test/mock data, test endpoints or development secrets are included in the release artifact.
- Install the exact AAB-derived release build on a real Android device and perform final smoke testing.
- Preserve a reproducible build path and record the release commit SHA and artifact checksum.
- Keep the signing key backed up securely outside the repository.

## Play Store gate
The AAB is accepted as a release candidate only after Steps 1–5 are satisfied and the exact artifact passes final smoke/security checks. Play Console declarations, store listing and submission are separate operational tasks after this gate.

## Non-demo rule
No demo AAB, fake production data, fake payment/withdrawal flow, placeholder feature or debug build may be treated as the production release artifact.

## Owner lock
The owner explicitly requested Step 6 to be 100% final and locked for a real Play Store release. This file freezes the AAB/release requirement baseline; it does not falsely claim that an AAB has already been successfully produced, signed or submitted.

Changes require explicit owner approval.
