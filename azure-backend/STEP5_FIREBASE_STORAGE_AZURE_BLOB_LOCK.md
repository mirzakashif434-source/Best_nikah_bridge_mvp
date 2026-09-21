# STEP 5 LOCK — Firebase Storage → Azure Blob Storage Parity

Status: AZURE STORAGE PRIMARY / LOCKED
Production storage: Azure Blob Storage
Legacy Firebase Storage: preserved for rollback and controlled migration

## Real Azure storage paths
- Profile photos → Azure Blob container `profile-photos`
- Verification documents → Azure Blob container `verification-documents`
- Metadata/ownership → Azure PostgreSQL
- Photo moderation → Azure AI Content Safety
- Auth → Azure External ID / authenticated Azure Functions

## Security parity
- Azure Functions require authenticated identity before storage access.
- Profile photo upload validates MIME type and size, performs Azure AI Content Safety moderation, then writes to Blob + PostgreSQL.
- Photo read is owner-authorized through the authenticated API.
- Photo visibility is owner-authorized in PostgreSQL.
- Photo deletion removes both PostgreSQL metadata and the Azure Blob.
- Verification documents are stored in the private verification container and referenced by PostgreSQL; direct public blob access is not enabled.
- Account deletion removes associated profile-photo and verification-document blobs before deleting the user record.

## Legacy preservation
Firebase Storage rules and existing Firebase Storage data are NOT deleted or overwritten in Step 5.

## Important migration boundary
This lock confirms the real Azure storage implementation and production authorization parity. Existing historical Firebase Storage files are not claimed as physically copied to Azure until an explicit export/import reconciliation is completed.

## Production gate
Azure deployment must validate Blob Storage configuration and live health, and Android verification must remain green.
