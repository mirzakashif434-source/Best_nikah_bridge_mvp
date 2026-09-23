const { verifyAzureExternalIdToken, ensureAzureUser } = require("./azureExternalAuth");

async function verifyAnyIdToken(request) {
  const claims = await verifyAzureExternalIdToken(request);
  const azureUser = await ensureAzureUser(claims);
  return {
    ...claims,
    uid: azureUser.firebase_uid || `azure:${claims.sub}`,
    email: azureUser.email,
    email_verified: Boolean(claims.email_verified) || Boolean(azureUser.email_verified_at),
    auth_provider: "azure_external_id",
    azure_subject: azureUser.azure_subject,
    azure_user_id: azureUser.id,
    role: azureUser.role,
    status: azureUser.status
  };
}

function requireAuth(handler) {
  return async (request, context) => {
    try {
      const user = await verifyAnyIdToken(request);
      if (user.status && user.status !== "active") {
        return { status: 403, jsonBody: { ok: false, error: "ACCOUNT_NOT_ACTIVE" } };
      }
      return await handler(request, context, user);
    } catch (error) {
      const status = error.statusCode || 500;
      if (status >= 500) context.error("AUTHENTICATION_FAILED", error);
      return {
        status,
        jsonBody: {
          ok: false,
          error: status === 401 ? "UNAUTHENTICATED" : status === 403 ? (error.message || "FORBIDDEN") : "AUTHENTICATION_ERROR",
          diagnostic: status === 401 ? error.message : null,
          diagnosticDetails: status === 401 ? error.diagnosticDetails || null : null
        }
      };
    }
  };
}

module.exports = { verifyAnyIdToken, requireAuth };
