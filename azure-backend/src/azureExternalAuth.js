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
  const tenantSubdomain = "bestnikahbridge";
  const audience = process.env.AZURE_EXTERNAL_ID_AUDIENCE || "4733aae0-3b89-4994-b99b-3890bf87e876";
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
  try {
    const {payload}=await jwtVerify(token,jwks,{issuer:cfg.issuer,audience:cfg.audience});
    if (!payload.sub) throw new Error("Token subject missing");
    return payload;
  } catch {
    const e=new Error("Invalid Azure External ID authentication token");
    e.statusCode=401;
    throw e;
  }
}
async function ensureAzureUser(claims) {
  const subject=String(claims.sub);
  const email=String(claims.email||claims.preferred_username||claims.emails?.[0]||"").trim().toLowerCase();
  if (!email) { const e=new Error("AUTH_EMAIL_REQUIRED"); e.statusCode=400; throw e; }
  const result=await query(
    `INSERT INTO users (azure_subject,email,email_verified_at)
     VALUES ($1,$2,CASE WHEN $3 THEN now() ELSE NULL END)
     ON CONFLICT (azure_subject) DO UPDATE SET
       email=EXCLUDED.email,
       email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),
       updated_at=now()
     RETURNING id,email,status,role,azure_subject`,
    [subject,email,Boolean(claims.email_verified)]
  );
  return result.rows[0];
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
