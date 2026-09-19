const { app } = require("@azure/functions");
const crypto = require("crypto");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { getVerificationDocumentsContainer } = require("./storage");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";
const ALLOWED_TYPES=new Set(["image/jpeg","image/png","application/pdf"]);
const MAX_BYTES=8*1024*1024;

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!user.uid||!email) throw new Error("AUTH_IDENTITY_REQUIRED");
  const r=await query("INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status",[user.uid,email,Boolean(user.email_verified)]);
  return r.rows[0];
}

app.http("verificationSubmit",{
  methods:["POST"],authLevel:"anonymous",route:"verification/identity",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      if(me.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      const form=await request.formData();
      const documentType=text(form.get("documentType"),30).toLowerCase();
      const file=form.get("document");
      if(!["identity","manual"].includes(documentType)) return {status:400,jsonBody:{ok:false,error:"INVALID_VERIFICATION_TYPE"}};
      if(!file||typeof file.arrayBuffer!=="function") return {status:400,jsonBody:{ok:false,error:"DOCUMENT_REQUIRED"}};
      const contentType=text(file.type,100).toLowerCase();
      if(!ALLOWED_TYPES.has(contentType)) return {status:400,jsonBody:{ok:false,error:"UNSUPPORTED_DOCUMENT_TYPE"}};
      const bytes=Buffer.from(await file.arrayBuffer());
      if(bytes.length===0||bytes.length>MAX_BYTES) return {status:400,jsonBody:{ok:false,error:"DOCUMENT_SIZE_INVALID"}};
      const ext=contentType==="application/pdf"?"pdf":contentType==="image/png"?"png":"jpg";
      const key=me.id+"/"+crypto.randomUUID()+"."+ext;
      const blob=getVerificationDocumentsContainer().getBlockBlobClient(key);
      await blob.uploadData(bytes,{blobHTTPHeaders:{blobContentType:contentType,blobCacheControl:"no-store"}});
      const r=await query("INSERT INTO verifications(user_id,verification_type,status,provider,document_blob_key,document_content_type,submitted_at) VALUES($1,$2,'pending','azure-private-storage',$3,$4,now()) RETURNING id,verification_type,status,submitted_at",[me.id,documentType,key,contentType]);
      return {status:201,jsonBody:{ok:true,verification:r.rows[0]}};
    }catch(e){context.error("VERIFICATION_SUBMIT_FAILED",e);return {status:500,jsonBody:{ok:false,error:"VERIFICATION_SUBMIT_FAILED"}};}
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
