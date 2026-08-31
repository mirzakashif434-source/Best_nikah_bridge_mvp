# BEST NIKAH BRIDGE — STEP 5 RELEASE TESTING

**Status: FINAL LOCKED — 2026-08-31**

This is the mandatory production release-testing gate. It defines the tests that must pass before a signed production AAB is accepted for Play Store submission.

## Required real-device tests
- Fresh install, upgrade and clean reinstall
- Account creation, login, logout and account recovery
- Profile creation/edit/delete and preference persistence
- Matching, explanations, likes and daily reset behavior
- Mutual-interest flow
- Two-message private reply gate, including recipient reply unlock
- Global Community Chat and private Safe Chat
- Block/report and moderation flows
- Verification, intent, readiness and trust flows
- Family/Wali and Family Bridge flows
- Nikah Roadmap, Promise Path and Living Compatibility
- AI Nikah Assistant failure/safety behavior
- Push notifications and deep links where configured
- Upgrade purchase, entitlement and restore purchase
- Wallet ledger, balance consistency and withdrawal lifecycle where payout rails are configured
- Currency/amount display and transaction history
- Offline/poor-network behavior and retry/idempotency
- Account deletion and data-deletion confirmation

## Security/abuse tests
- Unauthorized reads/writes
- Cross-user document access
- Client-side tampering of likes, message limits, matches, roles and wallet values
- Duplicate/replayed requests
- Rate-limit/abuse behavior
- Blocked-user bypass attempts
- Payment entitlement forgery
- Withdrawal/balance manipulation attempts
- Admin authorization checks
- Sensitive-log checks

## Release quality gates
- No known release-blocking crash
- No fake/demo production data
- No placeholder production behavior for locked features
- Correct production Firebase configuration
- Privacy policy/Data Safety/account deletion requirements prepared as applicable
- Release signing configuration verified
- Versioning verified
- Final AAB install/update smoke test passes

## Test evidence rule
A feature is not marked passed from a code review alone. Critical user, security and money flows require executable test evidence. Any failed critical test blocks release until fixed and retested.

## Owner lock
The owner explicitly requested Step 5 to be 100% final and locked for the release plan. This file freezes the mandatory testing gate; it does not falsely claim that the tests have already passed.

Changes require explicit owner approval.
