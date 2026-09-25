# Step 1 — 20 SAR Paid Feature Lock

Status: production entitlement contract for Google Play plan `premium_basic_20`.

## Entitlement rule

`paid20Features` is enabled for every active paid plan:
- 20 SAR Basic
- 40 SAR Plus
- 60 SAR VIP

It is disabled for free accounts.

## Locked 20 SAR feature set

1. Nikah Journey
2. Nikah Blueprint
3. Smart Serious Questions
4. Why We Matched
5. Advanced Match Filters
6. Compatibility Deal-Breakers
7. Compatibility Traffic Light
8. Marriage Timeline Matching
9. Family Circle expanded capacity
10. Trust Passport
11. Nikah Intelligence core (non-AI)
12. Real Profile Photo upload
13. Azure Wallet

## Safety / cost boundaries

- Free profile, basic matching, essential privacy/safety, account deletion and sign-out remain untouched.
- Family Circle itself remains usable at the free capacity; the paid plan expands its member capacity.
- Shared `/matches` APIs remain available to free matching. Paid feature screens that reuse match data are gated in Android so free matching is not broken.
- Dedicated paid-cost routes are also enforced on Azure for Journey, Advanced Filters, Profile Photo upload and Wallet.
- Azure AI remains a separate 60 SAR VIP-only entitlement.
- Existing Google Play purchase verification remains the source of paid entitlement truth.
