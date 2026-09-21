# STEP 3 LOCK — Firebase Functions Production Deployment

Status: LOCKED
Production backend: Azure Functions

## Real migration status
The Firebase Functions source and deployment workflow are preserved for rollback safety, but automatic Firebase production deployment is now on migration hold. Production pushes on the Azure migration branch deploy through the existing Azure Functions production workflow.

Firebase source-to-Azure production mapping verified in the repository:
- wallet / premium / purchase verification → Azure wallet, premiumPlans, premiumPurchaseVerification
- community / community safety → Azure community/contentSafety
- family bridge → Azure family
- Help Line AI/admin → Azure helpLine
- owner dashboard/earnings → Azure ownerEarnings
- terms/community → Azure terms/community
- verification/admin → Azure verification/moderation
- rewarded ads → Azure rewardedAds
- auth/account/privacy/blocks/chat/matches/profile/photos → Azure corresponding APIs

Azure production deployment gate validates every azure-backend/src/*.js, loads src/index.js, authenticates to Azure using OIDC, deploys the existing Function App, and verifies live External ID + health endpoints.

## Preservation rule
Do NOT delete Firebase Functions, rules, or Firebase configuration during this step. Final Firebase removal remains a later cutover step after all migration tests pass.
