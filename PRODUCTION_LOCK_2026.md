# Best Nikah Bridge — Production Lock

## Authoritative Android launcher
`app/src/main/java/com/nikahbridge/MainActivity.java`

## Rules
- Never restore the historical demo/local-data MainActivity as the production launcher.
- No hard-coded member names, ages, cities, countries, photos, match records or fake verification badges.
- All member profiles and matches come from authenticated Firebase data.
- Users must be 18+ and email-verified before profile activation.
- Matching uses reciprocal age/gender preferences, intent, timeline, location, values, deal-breakers and trust signals.
- Interest/chat access is backend-controlled; chat is mutual-only.
- Reporting and blocking are required safety controls.
- Family/Wali participation is consent-based.
- Verification is a real request/review state, not a decorative client-side badge.
- Privacy controls are stored in Firebase.
- Account deletion is handled by the trusted backend.
- AI assistant is safety-aware and does not present itself as a religious authority.
- Real profile photos are uploaded by the authenticated user to Firebase Storage; no bundled/demo photos are used.

## Historical recovery
Git history preserves the previous large MainActivity source. It is recovery material only and must not replace the production launcher if it contains demo/local profile data.
