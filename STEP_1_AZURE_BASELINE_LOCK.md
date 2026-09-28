# Step 1 — Azure Additive Baseline Lock

Target branch: `azure-backend-additive`
Backup branch: `backup-step1-azure-additive-before-final-repair-20260928`

## Purpose
This checkpoint protects the existing Azure production work before the 14-step final repair cycle.

## Rules
- Existing feature files are not deleted or replaced by Step 1.
- Firebase cleanup is handled only after Azure replacements are verified in later steps.
- All final work remains on `azure-backend-additive`.
- The backup branch is the rollback reference for the pre-repair state.
- Step 1 is non-deploying and non-destructive.

## Protection layers
1. Master Gate — verifies the Azure branch, package, backend directory, manifest, Gradle config and critical Azure auth/backend source are present.
2. Maestro E2E Baseline — verifies launcher/navigation source and the registered activity surface exist before later screen-by-screen tests.
3. Self-Healing — restores any CI workspace-only changes before completion.
4. Auto-Rollback — on a failed gate, resets only the temporary CI workspace to the exact checked-out commit; the remote branch is never rewritten by this workflow.
5. Fortress Budget — forbids cloud deployment/provisioning or destructive remote mutation from this Step 1 workflow.

## Locked outcome
The current Azure work is preserved and a rollback branch exists before any repair or cleanup work begins.
