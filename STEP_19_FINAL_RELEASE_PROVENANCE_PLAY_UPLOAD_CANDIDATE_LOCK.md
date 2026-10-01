# Step 19 — Final Release Provenance + Play Upload Candidate Lock

Additive only. Step 1–18 remain preserved.

Final checks:
- package/version/SDK/50-screen zero-drift
- four Play signing identities preserved
- Azure/MSAL redirect preserved
- Azure-only runtime / Firebase-zero
- Play Billing 20/40/60 + purchase verification source
- UI/auth safety contracts
- live Azure readiness
- Maestro final flow
- real upload key matches Play Console upload certificate
- clean compile + lint
- signed APK + signed AAB
- source commit + APK SHA-256 + AAB SHA-256 provenance handoff
- Master Gate -> Maestro E2E -> Self-Healing -> Auto-Rollback safety -> Fortress Budget

Azure Portal and Play Console are not changed. Previous work is not deleted or replaced.
