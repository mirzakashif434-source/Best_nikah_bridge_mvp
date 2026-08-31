# BEST NIKAH BRIDGE — STEP 4 PRODUCTION DATA

**Status: FINAL LOCKED — 2026-08-31**

This is the mandatory production-data baseline for the Best Nikah Bridge release. It defines how real production data must be handled; it does not falsely certify that an external Firebase project, payout provider, or production dataset is already configured.

## Production-data rules
- Production Firebase project/configuration must be explicitly separated from development/test environments.
- Real users create real accounts; no fabricated member profiles are required for production launch.
- No fake matches, fake verification badges, fake wallet balances, fake payments, fake withdrawals, or demo transactions may be presented as real.
- Firestore collections and documents must have defined schemas and server-authoritative ownership.
- User profile, preference, interest, match, chat, safety/report, family/wali, roadmap and trust data must use real persisted backend records.
- Wallet must use an auditable server-side transaction ledger; client-provided balances are never trusted.
- Payment records must map to verified provider transactions and idempotent entitlements.
- Withdrawal records must map to eligible balances and supported licensed payout rails; availability depends on the configured provider and user's jurisdiction/KYC requirements.
- Supported currencies and displayed conversion rates must come from a reliable configured source; the app must not imply that every currency or withdrawal route is universally available.
- Production database indexes, retention rules, timestamps, status transitions and duplicate protection must be defined.
- Personal/contact information must follow the app's privacy and mutual/family access rules.
- Account deletion must remove or anonymize appropriate production data according to the documented retention policy and applicable requirements.
- Development/test data must never be silently promoted into production.
- Monitoring, audit events and error records must avoid unnecessary sensitive personal/payment data.
- Production secrets and service credentials must remain outside client source code.

## Data-quality release gate
Before Play Store release, production configuration must be validated with real test accounts and controlled test transactions without presenting them as ordinary user data. The production project must pass authorization, deletion, duplicate, payment and rollback tests.

## Owner lock
The owner explicitly requested Step 4 to be 100% final and locked as a real, non-demo production-data requirement. This lock freezes the requirement baseline; actual production configuration and verification remain mandatory release work.

Changes require explicit owner approval.
