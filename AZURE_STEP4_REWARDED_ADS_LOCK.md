# Azure Step 4 — Rewarded Ads Lock

Production checkpoint for Step 4.

- Android rewarded-ad configuration uses the Azure API.
- Azure External ID subject is used for rewarded identity.
- Azure Function verifies AdMob SSV signatures.
- Verified rewards are written to Azure PostgreSQL entitlements.
- Duplicate transaction protection and daily reward limits are enforced.
- Existing Firebase implementation remains preserved for rollback/migration safety.
- Latest Azure deployment workflow completed successfully, including live External ID and health verification.

This checkpoint is additive; no previous implementation was deleted or replaced.
