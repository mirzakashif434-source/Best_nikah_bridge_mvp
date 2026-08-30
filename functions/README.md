# Best Nikah Bridge — Production Backend

This directory is reserved for the real Firebase Cloud Functions backend.

Production rules:
- Never put provider/API secrets in source control.
- Mutual connections, chat authorization, moderation actions and account deletion must be server-authoritative.
- Client-side UI is not a security boundary.
- Provider integrations (SMS, identity/liveness, calling, AI) must use secret-managed configuration.

The Android client must not claim a feature is verified/available until the corresponding backend/provider integration is active and tested.
