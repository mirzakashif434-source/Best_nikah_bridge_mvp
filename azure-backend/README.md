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
