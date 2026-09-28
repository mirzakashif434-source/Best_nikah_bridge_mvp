# Step 6 — Final Firebase Production Removal Lock

Target branch: `azure-backend-additive`

Step 6 confirms that Firebase has been removed from the production application/runtime/configuration surface.

## Final production removal state
- no Firebase Android SDK/runtime imports or calls
- no Firebase Gradle plugin/dependency
- no google-services.json
- no firebase.json
- no Firestore rules/indexes
- no Firebase Storage rules
- no legacy Firebase Functions directory
- no Firebase production deploy workflow
- no Firebase callable/runtime API path

## Safety guard exception
The only remaining files whose names contain "Firebase" are the Step 1 and Step 4 safety locks/documentation. They are not runtime code and exist only to prevent Firebase from being reintroduced accidentally.

## Preservation
- full 50 Activities remain present
- existing feature Activities are not deleted or replaced
- Azure remains the only production backend target
- Microsoft Entra auth/signing lock remains intact
