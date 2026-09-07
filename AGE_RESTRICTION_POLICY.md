# Best Nikah Bridge — Adult-Only Access Lock

Status: production lock for matchmaking safety.

Best Nikah Bridge is an adults-only Muslim matrimonial service. Users must be 18 or older to activate a discoverable profile or participate in matching and private connection flows.

## Enforcement

1. The real profile flow requires an age of 18–100 before activation.
2. Firestore security rules require an integer age of at least 18 whenever a profile is activated/discoverable.
3. Firestore rules only expose profiles for matching when the profile is active, discoverable, terms-accepted, intent-confirmed, and age-eligible.
4. Interest creation requires both the sender and recipient to be active, discoverable, terms-accepted, intent-confirmed, and age-eligible.
5. The app must be configured in Google Play Console with the matchmaking-app minor restriction/age controls before production publication.

This document is a policy/implementation lock. It does not replace the required Play Console declarations.
