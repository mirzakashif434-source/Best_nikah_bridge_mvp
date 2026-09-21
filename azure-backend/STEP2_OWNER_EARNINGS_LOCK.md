# STEP 2 LOCK — Owner Dashboard / Earnings

Status: LOCKED
Production path: Azure

- Android OwnerEarningsActivity uses Azure API as primary.
- Azure Functions owner earnings APIs are deployed.
- Verified Google Play purchases are recorded into Azure PostgreSQL owner_earnings.
- Azure PostgreSQL owner earnings/settlement tables are provisioned and verified.
- Existing Firebase owner dashboard/earnings code remains intact for rollback safety.
- Latest Android verification build: PASS.
- Latest Azure backend deployment containing the migration: PASS.

Do not remove Firebase owner earnings code until final Firebase retirement after full migration audit.
