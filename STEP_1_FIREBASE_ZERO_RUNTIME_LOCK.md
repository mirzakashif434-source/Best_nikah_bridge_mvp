# Step 1 — Firebase Zero Runtime Lock

Target: `azure-backend-additive`

This additive lock verifies that the production Android/runtime path is Azure-only while preserving the existing 42-screen work.

Verified absent:
- Firebase Auth / Firestore / Functions / Storage runtime calls
- Firebase Gradle dependencies/plugins
- google-services.json
- firebase.json
- Firestore rules/indexes
- Storage rules
- Firebase production deploy workflow
- legacy Firebase AI compatibility files

No existing feature file is changed by this lock. The pre-repair backup remains:
`backup-step1-azure-additive-before-final-repair-20260928`
