# Step 3 — Azure Auth, SHA, MSAL Redirect and 401 Lock

Target branch: `azure-backend-additive`

## Locked identities
- Android package: `com.nikahbridge`
- Microsoft client ID: `f5fc2b11-420f-4741-9a52-65d60cbddb7b`
- API audience: `4733ae40-3b89-4994-b99b-3890bf87e876`
- API scope: `api://4733ae40-3b89-4994-b99b-3890bf87e876/access_as_user`
- Tenant ID: `c4ac0560-df59-48d4-af1b-bb0ed127ce6d`
- Tenant subdomain: `bestnikahbredge`
- Expected upload certificate SHA-1: `41097230F8DBA1DB824958C851D8284C65BA5048`
- Expected Play App Signing SHA-1: `85A52E954F31DFAC2C1062A8D64EDA2D9B485B48`
- Play App Signing MSAL redirect hash: `haUulU8x36wsEGKo1k7aLZtIW0g=`

## Critical correction
The GitHub upload keystore is used only to sign the AAB uploaded to Play Console.
It must NOT rewrite the MSAL redirect URI. The Play-installed application is signed by
Google Play App Signing, so the production MSAL redirect stays tied to the Play App
Signing certificate.

The Azure release workflow now verifies these two certificates separately and leaves
main/release auth configuration unchanged.

## Backend token validation
Azure backend verifies:
- Bearer token presence
- cryptographic JWT signature through tenant JWKS
- API audience
- token expiry/validity through jose `jwtVerify`
- issuer against the allowed tenant issuer list
- subject presence
- Azure user mapping/account state

## Preservation
- existing 50 Activities/features are preserved
- Firebase runtime remains zero
- no feature file is deleted or replaced in Step 3
