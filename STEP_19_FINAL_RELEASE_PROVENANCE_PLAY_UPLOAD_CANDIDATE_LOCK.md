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

## Play Console SHA-256 certificate lock

Exact values confirmed from Play Console and preserved additively:

- Current App signing key SHA-256: `66:AB:DE:AF:B0:41:77:16:C6:B1:00:3A:7A:28:FE:D0:D7:42:42:69:05:B2:51:3E:3D:36:A4:97:CF:5A:DD:AE`
- Previous App Signing — Classical key SHA-256: `70:A6:D7:65:B5:52:B6:49:E8:3F:CC:AC:37:8F:0E:EB:0A:3E:AC:B3:01:90:3B:F3:8E:F4:4F:22:A8:09:99:68`
- Post-quantum cryptography key SHA-256: `54:29:EB:F5:72:B0:4C:2B:48:A9:C2:56:AA:0E:65:9F:06:6B:7F:E4:6E:C0:6A:65:37:03:A8:FD:88:2C:0B:EB`
- Upload key certificate SHA-256: `AF:82:4C:01:B1:1D:28:0C:15:9A:4C:23:30:64:AB:86:39:15:4C:89:EE:E1:FA:BB:07:ED:2E:21:A5:8C:1E:7B`

The upload keystore is verified against both its Play Console SHA-1 and SHA-256 fingerprints. SHA-256 certificate identities are provenance/integrity locks; the Android MSAL redirect remains based on the Play signing SHA-1 and is not rewritten from SHA-256.
