# Step 16 — Live Azure Readiness + Release Candidate No-Drift Lock

Target branch: `azure-backend-additive`

Purpose: read-only final confirmation that the already-configured live Azure production backend and the frozen Android/Play release candidate still match, without changing Azure Portal, Play Console, or completed production code.

Checks:
- Step 1–15 contracts preserved
- package/version/SDK/50-Activity freeze preserved
- all four Play SHA-1 identities preserved
- Azure/MSAL redirect remains tied to Play App Signing SHA
- live `/api/health` = Azure OK
- live `/api/database/health` = PostgreSQL connected
- live `/api/storage/health` = Azure Blob containers ready
- live `/api/auth/azure/health` = issuer/audience/JWKS configured
- live `/api/release/readiness` = ready
- live `/api/auth/azure/me` without bearer token = 401 UNAUTHENTICATED
- Azure-only runtime / Firebase runtime zero
- Maestro frozen final flow
- upload key still matches Play Console upload certificate
- clean release compile + lint
- signed release-candidate APK and AAB with SHA-256 handoff
- Master Gate -> Maestro E2E -> Self-Healing -> Auto-Rollback safety -> Fortress Budget

Step 16 is additive/read-only verification. It does not mutate Azure Portal or Play Console and does not delete or replace previous work.
