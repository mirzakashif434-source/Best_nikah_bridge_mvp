# Ultra-Low-Cost Production Lock

Target: architecture scale-ready toward 1,000,000 users while early/controlled usage aims for 150–200 SAR/month.

Locked controls:
- Azure Functions Flex stays On-Demand; no Always Ready baseline.
- Function memory target: 512 MB.
- PostgreSQL pool default: 3 connections per Function instance.
- Application Insights: max 2 telemetry items/second; exceptions preserved.
- Premium Plus advanced AI: 5 requests/day per user.
- Premium VIP AI: 12 requests/day per user.
- Nikah Assistant output target: 350 tokens.
- Advanced AI output target: 500 tokens.
- Profile photos remain Hot.
- Verification documents move to Cool tier after 30 days without auto-delete.
- Existing features, premium plans, Golden release, Self-Healing, and Auto-Rollback remain preserved.
- No multi-region duplicate infrastructure in this low-budget phase.

150–200 SAR is a planning target, not a guaranteed invoice ceiling. Actual Azure usage and provider pricing determine the bill.
