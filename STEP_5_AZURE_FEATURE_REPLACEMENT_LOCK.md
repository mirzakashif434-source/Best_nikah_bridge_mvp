# Step 5 — Firebase-to-Azure Feature Replacement Lock

Target branch: `azure-backend-additive`

Purpose: prove that every critical production feature has a real Azure path before any legacy implementation is considered removable.

## Locked replacement map
- Auth/session -> Microsoft Entra External ID + Azure token verification
- Profile -> Azure profile API
- Matches -> Azure matches API
- Likes / Liked Me / I Liked / Viewed -> Azure feature APIs
- Mutual chat / messages -> Azure conversations/messages APIs
- Family / Wali -> Azure family-links APIs
- Family Circle -> Azure family-circle APIs
- Verification -> Azure verification/admin APIs
- Photos -> Azure photo/storage APIs
- Privacy -> Azure privacy API
- Safety report / block -> Azure safety + blocks APIs
- Community chat -> Azure community APIs
- Help line -> Azure help/admin APIs
- Wallet -> Azure wallet ledger APIs
- Premium -> Google Play Billing + Azure premium verification
- Rewarded messages -> AdMob config/reward + Azure entitlement
- AI / mediator / questions / planning -> Azure AI/backend routes
- Owner analytics / earnings -> Azure owner routes
- Account deletion -> Azure permanent account deletion endpoint

## Preservation
- all 50 Activities remain present
- existing screen code is not deleted/replaced by this lock
- Firebase runtime/config stays zero
- Azure remains the only production backend target
