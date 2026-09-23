const { app } = require("@azure/functions");
const crypto = require("crypto");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { getVerificationDocumentsContainer } = require("./storage");
const { requireAdultProfile } = require("./ageGate");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";
const ALLOWED_TYPES=new Set(["image/jpeg","image/png","application/pdf"]);
const SELFIE_TYPES=new Set(["image/jpeg","image/png"]);
const MAX_BYTES=8*1024*1024;
const MAX_SELFIE_BYTES=4*1024*1024;

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!user.uid||!email) throw new Error("AUTH_IDENTITY_REQUIRED");
  const azureSubject = typeof user.azure_subject === "string" && user.azure_subject.trim() ? user.azure_subject.trim() : null;
  if (azureSubject) {
    const existing = await query("SELECT id,status FROM users WHERE azure_subject=$1 OR firebase_uid=$2 LIMIT 1",[azureSubject,user.uid]);
    if (existing.rows[0]) return existing.rows[0];
    const created = await query("INSERT INTO users(firebase_uid,azure_subject,email,email_verified_at) VALUES($1,$2,$3,CASE WHEN $4 THEN now() ELSE NULL END) RETURNING id,status",[user.uid,azureSubject,email,Boolean(user.email_verified)]);
    return created.rows[0];
  }
  const r=await query("INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status",[user.uid,email,Boolean(user.email_verified)]);
  return r.rows[0];
}

async function storeVerificationFile(me,file,verificationType,maxBytes,allowedTypes){
  if(!file||typeof file.arrayBuffer!=="function"){const e=new Error("DOCUMENT_REQUIRED");e.statusCode=400;throw e;}
  const contentType=text(file.type,100).toLowerCase();
  if(!allowedTypes.has(contentType)){const e=new Error("UNSUPPORTED_DOCUMENT_TYPE");e.statusCode=400;throw e;}
  const bytes=Buffer.from(await file.arrayBuffer());
  if(bytes.length===0||bytes.length>maxBytes){const e=new Error("DOCUMENT_SIZE_INVALID");e.statusCode=400;throw e;}
  const ext=contentType==="application/pdf"?"pdf":contentType==="image/png"?"png":"jpg";
  const key=me.id+"/"+verificationType+"/"+crypto.randomUUID()+"."+ext;
  const blob=getVerificationDocumentsContainer().getBlockBlobClient(key);
  await blob.uploadData(bytes,{blobHTTPHeaders:{blobContentType:contentType,blobCacheControl:"no-store"}});
  const r=await query("INSERT INTO verifications(user_id,verification_type,status,provider,document_blob_key,document_content_type,submitted_at) VALUES($1,$2,'pending','azure-private-storage',$3,$4,now()) RETURNING id,verification_type,status,submitted_at",[me.id,verificationType,key,contentType]);
  return r.rows[0];
}

app.http("verificationSubmit",{
  methods:["POST"],authLevel:"anonymous",route:"verification/identity",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      if(me.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      await requireAdultProfile(me.id);
      const form=await request.formData();
      const documentType=text(form.get("documentType"),30).toLowerCase();
      if(!["identity","manual"].includes(documentType)) return {status:400,jsonBody:{ok:false,error:"INVALID_VERIFICATION_TYPE"}};
      const verification=await storeVerificationFile(me,form.get("document"),documentType,MAX_BYTES,ALLOWED_TYPES);
      return {status:201,jsonBody:{ok:true,verification}};
    }catch(e){context.error("VERIFICATION_SUBMIT_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"VERIFICATION_SUBMIT_FAILED"}};}
  })
});

app.http("verificationSelfieSubmit",{
  methods:["POST"],authLevel:"anonymous",route:"verification/selfie",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      if(me.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      await requireAdultProfile(me.id);
      const form=await request.formData();
      const verification=await storeVerificationFile(me,form.get("selfie"),"selfie",MAX_SELFIE_BYTES,SELFIE_TYPES);
      return {status:201,jsonBody:{ok:true,verification,automatedLiveness:false,reviewRequired:true}};
    }catch(e){context.error("VERIFICATION_SELFIE_SUBMIT_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"VERIFICATION_SELFIE_SUBMIT_FAILED"}};}
  })
});

app.http("verificationStatus",{
  methods:["GET"],authLevel:"anonymous",route:"verification",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const r=await query("SELECT id,verification_type,status,provider,submitted_at,reviewed_at,created_at FROM verifications WHERE user_id=$1 ORDER BY created_at DESC LIMIT 20",[me.id]);
      return {status:200,jsonBody:{ok:true,verifications:r.rows}};
    }catch(e){context.error("VERIFICATION_STATUS_FAILED",e);return {status:500,jsonBody:{ok:false,error:"VERIFICATION_STATUS_FAILED"}};}
  })
});

async function requireVerificationAdmin(user){
  const subject=(user.azure_subject||"").trim();
  const r=await query("SELECT id,role,status FROM users WHERE (azure_subject=$1 OR firebase_uid=$2) LIMIT 1",[subject,user.uid]);
  const me=r.rows[0];
  if(!me || me.status!=="active"){const e=new Error("USER_NOT_ACTIVE");e.statusCode=403;throw e;}
  if(!["admin","moderator"].includes(me.role)){const e=new Error("ADMIN_REQUIRED");e.statusCode=403;throw e;}
  return me;
}

app.http("verificationAdminList",{
  methods:["GET"],authLevel:"anonymous",route:"admin/verifications",
  handler:requireAuth(async(request,context,user)=>{
    try{
      await requireVerificationAdmin(user);
      const r=await query(
        `SELECT v.id,v.verification_type,v.status,v.provider,v.document_content_type,v.submitted_at,v.created_at,
                u.id AS user_id,u.email,p.display_name
         FROM verifications v
         JOIN users u ON u.id=v.user_id
         LEFT JOIN profiles p ON p.user_id=u.id
         WHERE v.status='pending'
         ORDER BY COALESCE(v.submitted_at,v.created_at) ASC
         LIMIT 200`
      );
      return {status:200,jsonBody:{ok:true,items:r.rows}};
    }catch(e){context.error("VERIFICATION_ADMIN_LIST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"VERIFICATION_ADMIN_LIST_FAILED"}};}
  })
});

app.http("verificationAdminReview",{
  methods:["PATCH"],authLevel:"anonymous",route:"admin/verifications/{verificationId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const admin=await requireVerificationAdmin(user);
      const id=request.params?.verificationId||context.triggerMetadata?.verificationId;
      const body=await request.json();
      const decision=text(body?.decision,20).toLowerCase();
      if(!["approved","rejected"].includes(decision)) return {status:400,jsonBody:{ok:false,error:"INVALID_VERIFICATION_DECISION"}};
      const r=await query(
        `UPDATE verifications
         SET status=$2,reviewed_at=now(),provider=COALESCE(provider,'azure-admin-review')
         WHERE id=$1 AND status='pending'
         RETURNING id,user_id,verification_type,status,reviewed_at`,
        [id,decision]
      );
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"VERIFICATION_NOT_FOUND_OR_ALREADY_REVIEWED"}};
      return {status:200,jsonBody:{ok:true,verification:r.rows[0],reviewedBy:admin.id}};
    }catch(e){context.error("VERIFICATION_ADMIN_REVIEW_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"VERIFICATION_ADMIN_REVIEW_FAILED"}};}
  })
});
