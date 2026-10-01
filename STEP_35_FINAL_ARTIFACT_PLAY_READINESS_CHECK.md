# STEP 35 — FINAL ARTIFACT + PLAY CONSOLE READINESS HEAVY LOCK

Purpose: last pre-upload verification for Best Nikah Bridge on `azure-backend-additive`.

This step does NOT upload or publish to Play Console.

Final checks:
- Preserve Steps 1-34.
- Verify all 50 Activities are still registered and source contracts pass.
- Verify button listeners, Back navigation, screen-size/insets, blur/null/stale error scans, camera/gallery flow, MSAL signer selection, and no Firebase runtime.
- Build final signed release APK + AAB.
- Compile + lint.
- Verify upload key SHA-1 + SHA-256.
- Verify Play Console SHA-1/SHA-256 matrix and redirects.
- Run signed APK/AAB forensic checks.
- Run runtime APK smoke + 50-screen ultra gate on emulator.
- Verify live Azure health/auth configuration and protected endpoint enforcement.
- Verify security/secret references only.
- Preserve Master Gate -> Maestro E2E -> Self-Healing -> Auto-Rollback -> Fortress Budget.
- Produce one final downloadable APK+AAB artifact pack.

Final artifact target:
`step35-final-play-ready-apk-aab-v36`

Play Console upload/publish is intentionally left for the manual release step after this gate is GREEN.