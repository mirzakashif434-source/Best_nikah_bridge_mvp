# Step 17 — Final Play Store Preflight + Zero-Drift Lock

This step is additive and read-only with respect to Azure Portal and Google Play Console.

It preserves Step 1–16 and verifies:
- package/version/SDK/50-screen inventory
- all four Play SHA-1 identities
- Azure/MSAL redirect derived from current Play App Signing certificate
- Azure-only backend and Firebase runtime zero
- Play Billing 20/40/60 product contracts and purchase verification source
- UI safety contracts for buttons, back navigation, insets/screen size, loading, null/blur regressions
- live Azure health/database/storage/auth/release readiness
- negative auth guard (401 without Bearer token)
- Maestro frozen final user-flow contract
- real upload keystore matches Play Console upload certificate
- clean release compile + lint
- signed APK + AAB with SHA-256 handoff
- Master Gate -> Maestro E2E -> Self-Healing -> Auto-Rollback safety -> Fortress Budget

No previous work is deleted or replaced. Azure Portal and Play Console are not mutated.
