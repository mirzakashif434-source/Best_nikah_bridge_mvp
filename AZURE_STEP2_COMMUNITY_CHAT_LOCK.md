# Azure Step 2 — Community Chat Lock

Production migration checkpoint. Community Chat is Azure-primary with Firebase retained only for rollback. No previous implementation is deleted or replaced.

Verified on commit 82f721b402053c725c7cf5bad4fb590634a7d809:
- Azure PostgreSQL community tables
- Azure Functions community APIs
- Azure External ID authenticated Android path
- Real send/read/mute/unmute/report/block flows
- Safety/rate-limit checks
- Android debug APK verification passed
