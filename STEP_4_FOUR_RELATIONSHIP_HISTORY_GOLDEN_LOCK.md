# Step 4 — Four Relationship History Screens — GOLDEN LOCK

Status: GOLDEN LOCKED
Branch: azure-backend-additive
Verified workflow: Step 4 Four History Screens Golden Gate
Verified run: 36231462737
Verified result: SUCCESS

## Locked screens

1. Liked Me
   - Real Azure received-like list
   - Privacy-safe profile card
   - View Profile
   - Like Back
   - Ignore / Remove via authenticated Azure DELETE route
   - Premium capability gate remains intact

2. I Liked
   - Real Azure sent-like list
   - Pending status
   - Mutual status
   - Open profile
   - Cancel Like via authenticated Azure DELETE route

3. Viewed Me
   - Real Azure explicit profile viewers
   - Real view count
   - Real last-view time returned by Azure
   - Open profile
   - Like action

4. I Viewed
   - Real Azure explicit viewed-profile history
   - Open profile again
   - Like Now action

## Backend contracts

- GET /likes/received
- GET /likes/sent
- DELETE /likes/{targetUserId}
- DELETE /likes/received/{sourceUserId}
- POST /profile-views
- GET /profile-views/received
- GET /profile-views/sent

## Protection rule

This lock is additive. Earlier production work must not be deleted or replaced to change Step 4.
Any future change to these four screens must keep the Step 4 Golden Gate green and preserve the real Azure contracts above.
