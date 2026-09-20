const admin = require("firebase-admin");
const { verifyAzureExternalIdToken, ensureAzureUser } = require("./azureExternalAuth");

let initialized = false;

function getFirebaseAdmin() {
  if (initialized) return admin;
  const projectId = process.env.FIREBASE_PROJECT_ID;
  const serviceAccountJson = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!projectId || !serviceAccountJson) throw new Error("Firebase Admin authentication configuration is missing");
  let serviceAccount;
  try { serviceAccount = JSON.parse(serviceAccountJson); } catch { throw new Error("FIREBASE_SERVICE_ACCOUNT is not valid JSON"); }
  if (!serviceAccount.client_email || !serviceAccount.private_key) throw new Error("FIREBASE_SERVICE_ACCOUNT is missing required credentials");
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId,
      clientEmail: serviceAccount.client_email,
      privateKey: serviceAccount.private_key.replace(/\\n/g, "\n")
    }),
    projectId
  });
  initialized = true;
  return admin;
}

async function verifyFirebaseIdToken(request) {
  const authorization = request.headers.get("authorization") || "";
  if (!authorization.startsWith("Bearer ")) { const e = new Error("Missing bearer token"); e.statusCode = 401; throw e; }
  const token = authorization.slice(7).trim();
  if (!token) { const e = new Error("Missing bearer token"); e.statusCode = 401; throw e; }
  try { return await getFirebaseAdmin().auth().verifyIdToken(token, true); }
  catch { const e = new Error("Invalid or revoked authentication token"); e.statusCode = 401; throw e; }
}

async function verifyAnyIdToken(request) {
  let azureClaims = null;
  try {
    azureClaims = await verifyAzureExternalIdToken(request);
  } catch {
    azureClaims = null;
  }

  if (azureClaims) {
    const azureUser = await ensureAzureUser(azureClaims);
    return {
      ...azureClaims,
      uid: azureUser.firebase_uid || `azure:${azureClaims.sub}`,
      email: azureUser.email,
      email_verified: Boolean(azureClaims.email_verified) || Boolean(azureUser.email_verified_at),
      auth_provider: "azure_external_id",
      azure_subject: azureUser.azure_subject
    };
  }

  const firebaseUser = await verifyFirebaseIdToken(request);
  return { ...firebaseUser, auth_provider: "firebase" };
}

function requireAuth(handler) {
  return async (request, context) => {
    try { return await handler(request, context, await verifyAnyIdToken(request)); }
    catch (error) {
      const status = error.statusCode || 500;
      if (status >= 500) context.error("AUTHENTICATION_FAILED", error);
      return { status, jsonBody: { ok: false, error: status === 401 ? "UNAUTHENTICATED" : "AUTHENTICATION_ERROR" } };
    }
  };
}

module.exports = { getFirebaseAdmin, verifyFirebaseIdToken, verifyAnyIdToken, requireAuth };