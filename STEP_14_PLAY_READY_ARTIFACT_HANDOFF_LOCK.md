# Step 14 — Play-ready Artifact Handoff Lock

Target branch: `azure-backend-additive`

Purpose: final post-build handoff verification after Step 13, without modifying Azure Portal, Play Console, or completed app/backend work.

Locked checks:
- Step 1–13 workflow contracts preserved
- package `com.nikahbridge`
- versionCode 36 / versionName 2.25
- compileSdk 36 / targetSdk 36 / minSdk 24
- exactly 50 Activities
- all four Play SHA-1 identities preserved
- Play App Signing SHA drives MSAL redirect
- GitHub upload keystore SHA matches Play Console upload certificate
- Azure-only production runtime / Firebase runtime zero
- 20/40/60 monthly premium contracts
- UI/back/loading/null/blur safety contracts
- Maestro final navigation contract
- release compile + lint
- signed APK + signed AAB verification
- SHA-256 manifests for both artifacts
- Master Gate -> Maestro E2E -> Self-Healing -> Auto-Rollback safety -> Fortress Budget

No production feature file is deleted or replaced by Step 14.
