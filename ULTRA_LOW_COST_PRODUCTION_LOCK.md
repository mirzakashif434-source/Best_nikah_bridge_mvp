# Ultra-Low-Cost Production Lock

Target operating mode: keep normal production usage as close as practical to the owner's 200–300 SAR/month planning target without deleting existing features.

Locked cost controls:
- Azure Functions remain consumption-oriented; no code change introduces an always-on duplicate backend.
- PostgreSQL application pool default capped at 4 connections per Function instance.
- Application Insights telemetry sampled at max 5 telemetry items/second; exceptions are preserved.
- Premium Plus (40 SAR) advanced AI requests: default 8/day per user.
- Premium VIP (60 SAR) AI requests: default 20/day per user.
- Nikah Assistant output cap: 450 tokens by default.
- Advanced AI output cap: 600 tokens by default.
- AI prompt sizes are bounded server-side.
- Quotas are enforced server-side in PostgreSQL, not only in Android UI.
- Existing features, premium plans, Azure routes, Golden release protection, and auto-rollback are not removed.

Important: this is a cost-control target, not a guaranteed Azure invoice ceiling. Database SKU, storage growth, bandwidth, AI model pricing, and actual usage still determine the bill.
