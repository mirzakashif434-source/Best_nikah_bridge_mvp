const { app } = require("@azure/functions");
const { createRemoteJWKSet, jwtVerify } = require("jose");
const { query } = require("./db");

function required(name) {
  const v = process.env[name];
  if (!v) throw new Error(`Missing required environment variable: ${name}`);
  return v;
}
function externalIdConfig() {
  const tenantId = "c4ac0560-df59-48d4-af1b-bb0ed127ce6d";
  const tenantSubdomain = "bestnikahbredge";
  const audience = process.env.AZURE_EXTERNAL_ID_AUDIENCE || "4733ae40-3b89-4994-b99b-3890bf87e876";
  const issuer = process.env.AZURE_EXTERNAL_ID_ISSUER || `https://${tenantSubdomain}.ciamlogin.com/${tenantId}/v2.0/`;
  const jwksUri = process.env.AZURE_EXTERNAL_ID_JWKS_URI || `https://${tenantSubdomain}.ciamlogin.com/${tenantId}/discovery/v2.0/keys`;
  return { tenantId, tenantSubdomain, issuer, audience, jwksUri };
}
async function verifyAzureExternalIdToken(request) {
  const authorization = request.headers.get("authorization") || "";
  if (!authorization.startsWith("Bearer ")) { const e=new Error("Missing bearer token"); e.statusCode=401; throw e; }
  const token=authorization.slice(7).trim();
  if (!token) { const e=new Error("Missing bearer token"); e.statusCode=401; throw e; }
  const cfg=externalIdConfig();
  const jwks=createRemoteJWKSet(new URL(cfg.jwksUri));
  // External ID metadata has appeared in both tenant-name and tenant-ID issuer forms.
  // Accept only the two issuer forms belonging to this exact tenant; audience and
  // signature validation remain mandatory. This is additive and preserves the
  // existing configured issuer for backward compatibility.
  const tenantIdIssuer = `https://${cfg.tenantId}.ciamlogin.com/${cfg.tenantId}/v2.0/`;
  const allowedIssuers = [...new Set([cfg.issuer, tenantIdIssuer])];
  try {
    const {payload}=await jwtVerify(token,jwks,{issuer:allowedIssuers,audience:cfg.audience});
    if (!payload.sub) throw new Error("Token subject missing");
    return payload;
  } catch (error) {
    // Safe production diagnostic: log validation metadata only; never log the access token.
    console.warn("AZURE_EXTERNAL_ID_TOKEN_VERIFY_FAILED", {
      code: error?.code || null,
      name: error?.name || null,
      message: error?.message || null
    });
    const e=new Error("Invalid Azure External ID authentication token");
    e.statusCode=401;
    throw e;
  }
}
async function ensureAzureUser(claims) {
  const subject=String(claims.sub);
  const email=String(claims.email||claims.preferred_username||claims.emails?.[0]||"").trim().toLowerCase();
  if (!email) { const e=new Error("AUTH_EMAIL_REQUIRED"); e.statusCode=400; throw e; }
  // Preserve existing users during Firebase -> Azure migration.
  // First match by Azure subject, then by verified email; only create a new row when neither exists.
  const bySubject=await query(
    `SELECT id,email,status,role,azure_subject,firebase_uid FROM users WHERE azure_subject=$1 LIMIT 1`,
    [subject]
  );
  if(bySubject.rows[0]) return bySubject.rows[0];

  const byEmail=await query(
    `SELECT id,email,status,role,azure_subject,firebase_uid FROM users WHERE lower(email)=lower($1) LIMIT 1`,
    [email]
  );
  if(byEmail.rows[0]){
    if (!Boolean(claims.email_verified)) {
      const e=new Error("AZURE_EMAIL_VERIFICATION_REQUIRED");
      e.statusCode=403;
      throw e;
    }
    const linked=await query(
      `UPDATE users SET azure_subject=$1,
        firebase_uid=COALESCE(firebase_uid,$4),
        email_verified_at=COALESCE(email_verified_at,CASE WHEN $3 THEN now() ELSE NULL END),
        updated_at=now()
       WHERE id=$2
       RETURNING id,email,status,role,azure_subject,firebase_uid`,
      [subject,byEmail.rows[0].id,Boolean(claims.email_verified),`azure:${subject}`]
    );
    return linked.rows[0];
  }

  const created=await query(
    `INSERT INTO users (azure_subject,firebase_uid,email,email_verified_at)
     VALUES ($1,$4,$2,CASE WHEN $3 THEN now() ELSE NULL END)
     RETURNING id,email,status,role,azure_subject,firebase_uid`,
    [subject,email,Boolean(claims.email_verified),`azure:${subject}`]
  );
  return created.rows[0];
}
async function requireAzureAuth(handler) {
  return async (request,context) => {
    try {
      const claims=await verifyAzureExternalIdToken(request);
      return await handler(request,context,claims,await ensureAzureUser(claims));
    } catch(error) {
      const status=error.statusCode||500;
      if(status>=500) context.error("AZURE_AUTHENTICATION_FAILED",error);
      return {status,jsonBody:{ok:false,error:status===401?"UNAUTHENTICATED":status===400?error.message:"AZURE_AUTHENTICATION_ERROR"}};
    }
  };
}
app.http("azureExternalAuthHealth",{methods:["GET"],authLevel:"anonymous",route:"auth/azure/health",handler:async()=>{
  try { const cfg=externalIdConfig(); return {status:200,jsonBody:{ok:true,configured:true,issuerConfigured:!!cfg.issuer,audienceConfigured:!!cfg.audience,jwksConfigured:!!cfg.jwksUri}}; }
  catch { return {status:503,jsonBody:{ok:false,configured:false,error:"AZURE_EXTERNAL_ID_NOT_CONFIGURED"}}; }
}});
app.http("azureExternalAuthMe",{methods:["GET"],authLevel:"anonymous",route:"auth/azure/me",handler:async (request,context)=>{
  try {
    const claims=await verifyAzureExternalIdToken(request);
    const user=await ensureAzureUser(claims);
    return {status:user.status==="active"?200:403,jsonBody:{ok:user.status==="active",user:{id:user.id,email:user.email,role:user.role,status:user.status,azureSubject:user.azure_subject}}};
  } catch(error) {
    const status=error.statusCode||500;
    if(status>=500) context.error("AZURE_AUTHENTICATION_FAILED",error);
    return {status,jsonBody:{ok:false,error:status===401?"UNAUTHENTICATED":status===400?error.message:"AZURE_AUTHENTICATION_ERROR"}};
  }
}});
module.exports={verifyAzureExternalIdToken,ensureAzureUser,requireAzureAuth};
