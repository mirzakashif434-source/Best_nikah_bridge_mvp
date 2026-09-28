# Step 8 — Full 50 Buttons, Back, Loading and Navigation Lock

Target branch: `azure-backend-additive`

Step 8 protects the complete 50-Activity app surface against dead navigation, broken Back behavior,
stuck auth/loading regressions, raw JSON/error leakage, blur-like alpha, clipped controls and route loops.

## Locked contracts
- all 50 Activities remain registered and source-present
- all canonical Azure Home buttons have click listeners
- signed-in 42-screen Maestro navigation contract remains present
- non-root Activities keep a Back path
- Premium "View Plans" keeps the source Activity alive, preventing Back-to-blank loops
- Azure API authentication has a finite timeout and 401 recovery
- no raw JSON body is directly printed by critical wallet/home screens
- no literal "null null" user-facing state
- no blur-like alpha <= 0.4 on app source controls
- global safe system-bar insets and minimum control sizing remain enabled
- Firebase production runtime/config remains zero
- no existing feature Activity is deleted or replaced by this lock

Actual signed-in device testing remains part of the final release gate; this Step 8 lock preserves
and validates the source + Maestro contracts used for that test.
