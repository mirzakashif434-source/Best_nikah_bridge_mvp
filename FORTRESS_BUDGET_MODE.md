# Fortress Budget Mode

Status: additive production protection layer above the existing Self-Healing + Golden Master + Auto-Rollback system.

## Locked budget/runtime controls

- Azure Functions Flex Consumption stays On-Demand.
- Always Ready baseline is removed to avoid fixed warm-instance cost.
- Function instance memory: 512 MB.
- Maximum on-demand instances: 20 during the low-budget phase.
- PostgreSQL application pool: 3 connections per Function instance.
- Application Insights sampling: max 2 telemetry items/second; exceptions preserved.
- Azure AI is available only to the 60 SAR VIP plan; Free, 20 SAR Basic, and 40 SAR Plus do not receive Azure AI access.
- Premium VIP AI: 12 requests/day per user.
- Nikah Assistant max output: 350 tokens.
- Advanced AI max output: 500 tokens.
- Verification documents move to Cool tier after 30 days.
- Profile photos remain Hot for normal UX.
- No multi-region duplicate infrastructure in this budget phase.
- Existing premium plans, features, routes, Golden release, Self-Healing and Auto-Rollback remain preserved.

## Scale strategy

The architecture remains designed to scale far beyond the current user count. The low-budget caps are configuration limits, not a rewrite of the application. When traffic genuinely requires more capacity, the maximum instance count, database compute and other capacity can be increased without replacing the feature architecture.

## Cost target

Operational planning target: 150-200 SAR/month during early/light usage. This is not a hard invoice ceiling. Azure Cost Management budgets and alerts notify about spending but do not automatically stop consumption.

## Release contract

A release must fail its Fortress Budget contract if:
- 512 MB Function memory is removed,
- max instance count 20 is removed from the low-budget phase,
- Always Ready protection is removed,
- DB pool rises above the locked default without an intentional budget-mode change,
- VIP-only AI access is loosened to Free/20/40 SAR,
- AI quotas/output caps are removed,
- telemetry sampling is loosened unexpectedly,
- verification-document lifecycle is removed,
- Self-Healing/Auto-Rollback protection is deleted or bypassed.
