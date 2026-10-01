# Step 32 — APK Runtime + 50-Screen Forensic Lock

Additive only. Prior release work remains preserved.

This gate covers:
- signed release APK with the confirmed upload key
- exact upload-key SHA-1 + SHA-256 verification
- exact 50-Activity source registry
- compiled APK forensic scan
- Azure endpoint presence in compiled DEX
- no Firebase runtime strings in compiled artifact
- emulator install / launch / relaunch
- crash + ANR detection
- 1500 safe UI events
- Maestro release contract
- Master Gate -> Maestro E2E -> Self-Healing -> Auto-Rollback -> Fortress Budget

Important boundary:
- This CI gate does not fake authenticated Azure login, real Play Billing, rewarded ads, camera/gallery permissions, or tester-account entitlement.
- Those real external-service checks remain Step 33.

No Azure Portal mutation. No Play Console mutation. No previous feature deletion or replacement.
