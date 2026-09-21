# STEP 4 LOCK — Firestore Rules/Data → Azure PostgreSQL Authorization Parity

Status: AZURE PARITY LOCKED
Production authority: Azure External ID + Azure Functions + Azure PostgreSQL

## Firestore collections mapped to real Azure production storage/API
- users → PostgreSQL users + profiles + partner_preferences
- interests → PostgreSQL interests
- connections/messages → PostgreSQL conversations + messages
- blocks → PostgreSQL blocked_users
- reports → PostgreSQL safety_reports + community_reports
- verifications → PostgreSQL verifications + Azure Blob verification documents
- waliConnections/familyBridges → PostgreSQL family_links + Azure family APIs
- privacy → PostgreSQL privacy_settings
- wallets/walletLedger/withdrawals → PostgreSQL wallet_accounts + wallet_ledger + wallet_withdrawals
- entitlements → PostgreSQL entitlements + premium_entitlements
- playPurchases → PostgreSQL premium_purchase_tokens / Google Play verification API
- deletionRequests → Azure account deletion APIs / PostgreSQL account records
- moderationQueue/riskSignals → Azure moderation/safety services and PostgreSQL moderation/safety records
- rewardedAdTransactions/dailyRewardClaims → PostgreSQL rewarded_ad_transactions + daily_reward_claims
- publicConfig → Azure production configuration/secrets
- admin → Azure External ID roles + server-side authorization

## Authorization parity
The Azure APIs enforce authenticated identity, ownership, mutual-only communication, block checks, active-profile checks, adult/terms gates where applicable, and admin/moderator authorization server-side. The old Firestore rules remain untouched for rollback safety.

## Data preservation rule
No Firebase rules, collections, or existing Firebase data are deleted or overwritten in Step 4.

Historical Firestore data is NOT declared migrated merely from schema parity. A production data export/import must be executed and reconciled before final Firebase retirement. This lock therefore covers the real Azure authorization/storage replacement, while preserving Firebase data for the later controlled cutover.

## Production verification
The Azure production deployment workflow validates the backend modules and deploys the existing Azure Function App. Android verification must remain green before the final Firebase retirement step.
