# Step 11 — Notifications, Google Play Billing and Ads Master Lock

Target branch: `azure-backend-additive`

Step 11 locks the three release-critical platform systems:
1. Azure-backed notifications
2. Google Play Billing + Azure purchase verification
3. AdMob rewarded ads + UMP consent + Azure SSV reward verification

## Notifications
- Android POST_NOTIFICATIONS permission
- notification channel
- WorkManager periodic sync
- Azure /notifications endpoint
- notification deep-links to real app Activities
- Azure scheduled notification dispatcher

## Billing
- Google Play Billing client
- products: premium_basic_20 / premium_plus_40 / premium_vip_60
- Azure premium catalog
- Azure purchase verification through Google Play Developer API
- package-name/base-plan/state/expiry validation
- purchase-token hashing and duplicate-account protection
- entitlement activation only after verified purchase

## Ads
- Google Mobile Ads SDK
- UMP consent flow
- rewarded-ad config from Azure
- AdMob Server Side Verification signature validation
- max 2 rewarded message credits per UTC day
- premium members remain ad-free
- Azure entitlement/message-credit update only after valid SSV

## Preservation
- all 50 Activities remain present
- Firebase production runtime/config stays zero
- no existing feature Activity is deleted or replaced by Step 11
