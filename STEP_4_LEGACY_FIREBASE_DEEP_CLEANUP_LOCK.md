# Step 4 — Legacy Firebase Deep Cleanup Lock

Target branch: `azure-backend-additive`

Purpose: prove that old Firebase production work is gone while preserving the complete 50-Activity Azure app.

## Forbidden legacy production items
- firebase.json
- firestore.rules
- firestore.indexes.json
- storage.rules
- app/google-services.json
- legacy Firebase Functions directory/workflow
- Firebase Auth / Firestore / Functions / Storage / AI Android SDK imports or calls
- Firebase Gradle dependencies/plugins
- getHttpsCallable and other Firebase callable runtime use

## Allowed Firebase mentions
Only the new zero-runtime safety lock files may contain the word Firebase. They are guards, not Firebase runtime/infrastructure.

## Preservation
- all 50 Activities remain present
- no Activity source is deleted or replaced in Step 4
- Azure backend stays authoritative
- later Firebase reintroduction will fail the Step 4 master gate
