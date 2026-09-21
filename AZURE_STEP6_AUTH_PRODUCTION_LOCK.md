# Step 6 — Azure Authentication Production Lock

Production authentication cutover checkpoint.

- Azure External ID is the production authentication authority.
- Azure bearer tokens are verified against the real tenant/audience/JWKS.
- Existing Firebase identities are mapped to Azure by verified email and Azure subject.
- Production Function App is configured with AZURE_AUTH_ONLY=true, so Firebase token fallback is disabled at runtime.
- Legacy Firebase authentication source/configuration remains in the repository for migration rollback and was not deleted or replaced.
- Azure backend deployment completed successfully after this cutover.
- Live Azure External ID configuration check passed.
- Live Azure health check passed.
- Android debug APK build and artifact upload passed on the same Step 6 verification cycle.
