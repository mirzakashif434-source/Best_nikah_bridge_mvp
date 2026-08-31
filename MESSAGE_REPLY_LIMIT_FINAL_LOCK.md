# BEST NIKAH BRIDGE — MESSAGE REPLY LIMIT FINAL LOCK

**Status: FINAL / LOCKED — 2026-08-31**

## Rule
For a one-to-one conversation initiated by a member:

- The sender may send **at most 2 messages while the recipient has not replied**.
- Message 1 → allowed.
- Message 2 → allowed.
- After message 2, sending is automatically locked until the recipient sends a reply.
- A recipient reply unlocks the conversation for normal two-way communication.
- This is a **per-conversation reply-gate**, not a daily message allowance.
- It must not be bypassable by reinstalling the app, logging out/in, changing devices, or creating client-side state.
- The limit must be enforced server-side using authenticated user IDs and conversation state.
- Mutual/Safe Chat remains subject to the platform's safety, report, block and moderation controls.
- Global Community Chat remains a separate free community room and is not converted into this one-to-one rule.

## Example
`You → Message 1 → Message 2 → LOCKED`  
`Them → Reply → UNLOCKED → normal conversation`

## Purpose
Reduce repeated unwanted messages, pressure and spam while allowing a genuine conversation to continue once the recipient responds.

## Final decision
The owner explicitly approved this rule for the Best Nikah Bridge release. It is a mandatory production requirement, not a demo/mock behavior. Changes require explicit owner approval.
