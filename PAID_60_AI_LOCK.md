# Step 3 — 60 SAR VIP AI Feature Lock

Status: production entitlement contract for Google Play plan `premium_vip_60`.

## Entitlement rule

Azure AI access is enabled only for the 60 SAR VIP plan.

It is disabled for:
- Free
- 20 SAR Basic
- 40 SAR Plus

## Locked 60 SAR AI feature set

1. Azure AI Nikah Assistant
2. AI Nikah Mediator
3. Future Life Simulation

## Client protection

- Nikah Assistant requires `aiNikahAssistant` = VIP.
- AI Nikah Mediator requires `aiAdvanced` = VIP.
- Future Life Simulation requires `aiAdvanced` = VIP.

## Azure backend protection

- `POST /ai/nikah-assistant` returns `PREMIUM_VIP_REQUIRED` without VIP entitlement.
- `POST /ai/nikah-mediator` returns `PREMIUM_VIP_REQUIRED` without VIP entitlement.
- `POST /ai/future-life` returns `PREMIUM_VIP_REQUIRED` without VIP entitlement.

## Cost guard

- Only `premium_vip_60` receives Azure AI quota.
- VIP AI limit: 12 requests/day/user.
- Nikah Assistant output cap: 350 tokens.
- Advanced AI output cap: 500 tokens.
- Free / 20 / 40 SAR users must not receive Azure AI quota.

## Preservation

- Step 1 (20 SAR) remains unchanged.
- Step 2 (40 SAR) remains unchanged.
- Safety, privacy, account deletion and sign-out remain outside AI billing.
- Existing Golden+, Self-Healing, Auto-Rollback and Fortress Budget protections remain mandatory.
