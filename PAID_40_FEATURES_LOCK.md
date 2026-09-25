# Step 2 — 40 SAR Paid Feature Lock

Status: production entitlement contract for Google Play plan `premium_plus_40`.

## Entitlement rule

`paid40Features` is enabled for:
- 40 SAR Plus
- 60 SAR VIP

It is disabled for:
- Free
- 20 SAR Basic

## Locked 40 SAR feature set

1. Global Community Chat
2. Safe Communication / Chat
3. Conversation Health
4. Identity ID + Selfie Verification
5. Four-Photo Verification
6. Nikah Success Plan
7. Nikah Success Network

## Safety / cost boundaries

- General verification status remains available outside this paid gate.
- Safety reports, block/unblock, privacy, account deletion and sign-out remain untouched.
- Community Chat APIs and mutual chat/message APIs are enforced on Azure.
- ID/selfie submission and four-photo verification create/upload/submit operations are enforced on Azure.
- Nikah Success Network settings and mentor routes are enforced on Azure.
- Nikah Success Plan uses shared safe read APIs, so its screen entitlement is enforced in Android without breaking shared free/basic routes.
- 20 SAR Step 1 features remain unchanged.
- 60 SAR Azure AI remains a separate VIP-only entitlement.
