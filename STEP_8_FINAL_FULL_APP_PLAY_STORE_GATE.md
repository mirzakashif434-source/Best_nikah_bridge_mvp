# Step 8 — Final Full App + Play Store Release Gate

Status: FINAL ADDITIVE GATE

This step does not delete or replace earlier work. It validates the exact main-branch production source after Steps 1–7.

## Gate coverage

- Main production screens and manifest registrations
- Critical Wallet, Owner Earnings, Premium 20/40/60, Account Deletion, Privacy, Verification, Family/Wali and matching screens
- Back/navigation hooks on critical release screens
- Rejects obvious demo, fake, localhost, emulator-only and `null null` release blockers
- Release config: package, SDK levels, Billing, AdMob, UMP, cleartext disabled and backup disabled
- Real callable wiring for wallet, withdrawal, transaction history, premium verification/entitlement and account deletion
- Real release keystore validation
- Signed release APK + signed release AAB
- SHA-256 checksum generation
- Final artifact upload

## Human external gate

After this workflow is green, do one final real-device end-to-end test of the generated APK. Play Console submission/declarations remain external account actions and cannot be proven by repository CI alone.

Earlier locked features and workflows remain untouched.
