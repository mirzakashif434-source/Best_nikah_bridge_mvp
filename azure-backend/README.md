# Additive Azure Backend

This directory is intentionally separate from the existing Firebase backend.

## Safety rule

- Do not delete or replace the existing Firebase functions.
- Do not change the existing Android/Firebase configuration yet.
- This Azure backend is deployed separately and tested before any app cutover.

## Current stage

The first function is a production-safe health endpoint:

GET /api/health

It verifies that the Azure Function App can execute Node.js 22 Functions code.

## Next migration stages

1. Azure authentication design.
2. Azure database schema and migration tooling.
3. Azure Blob Storage integration.
4. Server-authoritative profile/matching APIs.
5. Messaging/safety APIs.
6. Verification and moderation services.
7. Android integration only after Azure APIs are tested.

The existing project remains the source of truth until an explicit, tested cutover is performed.


## Locked production steps

- Step 8.1 — Real authenticated PostgreSQL profile API: LOCKED.
- Step 9.1 — Real PostgreSQL matching API with reciprocal preferences, compatibility reasons, timeline, location and deal-breaker checks: LOCKED after successful Azure deployment run #31 (commit 63d8f95c77f0a173348a3b0372ba730035e865c6).

Locked steps are not to be rebuilt or replaced; future work must be additive and build on them.
