# BEST NIKAH BRIDGE — STEP 3 SECURITY

**Status: FINAL SECURITY GATE — 2026-08-31**

This is the mandatory production security baseline. It is a release requirement, not a claim that every control has already passed testing.

## Mandatory controls
- Firebase Authentication required for protected user actions.
- Firestore/Storage access must follow least privilege and authenticated ownership.
- Server-authoritative enforcement for daily likes, message limits, mutual-chat state, wallet balances and entitlements.
- The two-message reply gate must be enforced server-side; clients cannot bypass it by modifying local state.
- Wallet balance is ledger-derived and never trusted from client input.
- Payment purchases are verified server-side before premium entitlement is granted.
- Withdrawal requests are server-authorized, idempotent and auditable; no client can directly create a payout by changing a balance.
- No payment provider secret/private keys may be shipped in the APK.
- App Check and appropriate abuse/rate controls must protect callable/backend endpoints.
- Block/report/moderation controls must be enforced server-side where applicable.
- User privacy/contact information must not be exposed before the permitted mutual/family flow.
- Production logs must not expose passwords, authentication tokens, payment secrets or unnecessary private profile data.
- Account deletion must revoke/delete appropriate user data and credentials according to the documented retention policy.
- Backup, transport and storage security must be configured appropriately for production.
- Admin access must be restricted to explicitly authorized administrator accounts and sensitive operations must be auditable.
- Error handling must fail closed for authorization-sensitive operations.
- Dependency/security updates must be reviewed before release.

## Security release rule
No security control is considered complete merely because a UI control exists. Authorization-sensitive rules must be tested against unauthorized, cross-user, replay, duplicate-request and client-tampering scenarios.

## Owner lock
The owner requested Step 3 to be treated as 100% final and locked for the Play Store release plan. This file defines the mandatory security gate. Actual verification remains a prerequisite of Step 5 release testing.

Changes require explicit owner approval.
