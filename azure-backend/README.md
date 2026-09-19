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
- Step 4 — Real PostgreSQL Interest + Mutual Connection API (send/list/accept/decline/cancel, mutual conversation creation): LOCKED after successful Azure deployment runs #33–#34 (commits c91dcf2a3b013dcfac732b0ab484744cb3de7c85 and 40394e49d994985e63ce39cf00fbf08e22d6a58b).
- Step 5.1 — Real PostgreSQL Mutual-Only Chat + Messages API (conversations, send/read messages, authorization): LOCKED after successful Azure deployment run #39 (commit 802cd89a32d26ac6b38a60ad3fcc63dff804bc16).
- Step 6 — Real PostgreSQL Family/Wali API (create/list/verify/revoke): LOCKED after successful Azure deployment run #43 (commit 894f5b02380a2abdaaafce84875daafe7d9dc5d0).
- Step 7.1 — Real Azure identity-verification document upload/status API using private Blob Storage + PostgreSQL: LOCKED after successful Azure deployment run #47 (commit 163f3b45c18ed6e24c802fe8c3ca41a0be3ca7ec).
- Step 8 — Real authenticated profile-photo upload/list/visibility/delete API using private Azure Blob Storage + PostgreSQL: LOCKED after successful Azure deployment run #50 (commit 9e3de5e529b2386e722cdf6ca42ad712bf3dc1a1).
- Step 9.2 — Real authenticated match-detail API with reciprocal compatibility checks, compatibility score, and Why We Matched reasons: LOCKED after successful Azure deployment run #53 (commit 658d91d1a4f06ad6defb552af23ae70ae2a059f0).

Locked steps are not to be rebuilt or replaced; future work must be additive and build on them.
- Step 10.1 — Real Azure AI Nikah Assistant with authenticated AI chat, Azure OpenAI resource/model provisioning, managed-identity access, Function App deployment, and live health verification: LOCKED after successful Azure deployment run #61 (commit 4e03bb233d13d32418b20ca0a91e7e541057ce55).
- Step 11 — Real authenticated PostgreSQL Safety Report API (report creation + user report history with validated safety reasons): LOCKED after successful Azure deployment run #64 (commit 47ee931cc1838312df2bd1bb2b231cd2f06ebf34).
- Step 13 — Real authenticated Privacy Control Center with PostgreSQL persistence, profile discoverability, city visibility, photo privacy setting, and matching enforcement: LOCKED after successful Azure database run #17 and Azure Functions deployment run #77 (commit a673f83a21c4f8c3156d4b497e7116eafc776bcd).
- Step 14 — Real Azure AI Content Safety photo moderation with authenticated image analysis, unsafe-content rejection, PostgreSQL moderation audit fields, managed-identity access, and live Function App health verification: LOCKED after successful Azure Functions deployment run #82 (commit f5cf85529e66b2330a923c8a322b06dfe42a0be1).
