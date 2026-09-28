# Step 9 — Full 50 UI Safety and Screen Quality Lock

Target branch: `azure-backend-additive`

Step 9 locks the full 50-Activity UI against the recurring release problems reported during phone testing:
blur-like controls, clipped content, black/blank screens, unsafe system bars, fixed-height buttons, raw JSON,
literal null/null-null text, and loading/auth states without bounded recovery.

## Locked UI contracts
- 50/50 Activity source + manifest inventory
- scrollable content for all content screens (router-only MainActivity exempt)
- global system-bar safe insets
- global minimum control height normalization
- no blur-like alpha <= 0.4
- no literal "null null"
- no direct raw JSON body rendering on critical Azure/home/wallet screens
- no stale backend/deployment placeholder text
- Azure auth/API loading has finite timeout and 401 recovery
- premium Back stack cannot destroy the source screen before opening plans
- signed-in Maestro navigation contract remains intact
- Firebase production runtime/config remains zero

No existing feature Activity is deleted or replaced by Step 9.
