const { app } = require("@azure/functions");
const crypto = require("crypto");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { getProfilePhotosContainer } = require("./storage");
const { analyzeImage, shouldReject } = require("./contentSafety");
const { entitlementForUser, capabilitiesFor } = require("./premiumAccess");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";
const ALLOWED=new Set(["image/jpeg","image/png","image/webp"]);
const MAX=4*1024*1024;

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
  const r=await query(
    "INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status",
    [user.uid,email,Boolean(user.email_verified)]);
  return r.rows[0];
}

function ext(type){return type==="image/png"?"png":type==="image/webp"?"webp":"jpg";}

app.http("photoUpload",{
  methods:["POST"],authLevel:"anonymous",route:"photos",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      if(me.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      const premium=await entitlementForUser(me.id);
      if(!capabilitiesFor(premium).paid20Features) return {status:402,jsonBody:{ok:false,error:"PREMIUM_BASIC_REQUIRED",locked:true}};
      const form=await request.formData();
      const file=form.get("photo");
      const visibility=text(form.get("visibility"),20).toLowerCase()||"private";
      if(!file||typeof file.arrayBuffer!=="function") return {status:400,jsonBody:{ok:false,error:"PHOTO_REQUIRED"}};
      if(!["private","matches","public"].includes(visibility)) return {status:400,jsonBody:{ok:false,error:"INVALID_VISIBILITY"}};
      const type=text(file.type,100).toLowerCase();
      if(!ALLOWED.has(type)) return {status:400,jsonBody:{ok:false,error:"UNSUPPORTED_IMAGE_TYPE"}};
      const bytes=Buffer.from(await file.arrayBuffer());
      if(bytes.length===0||bytes.length>MAX) return {status:400,jsonBody:{ok:false,error:"PHOTO_SIZE_INVALID"}};
      const moderation=await analyzeImage(bytes);
      const rejected=shouldReject(moderation);
      const moderationStatus=rejected?"rejected":"approved";
      const key=me.id+"/"+crypto.randomUUID()+"."+ext(type);
      const blob=getProfilePhotosContainer().getBlockBlobClient(key);
      if(rejected) return {status:422,jsonBody:{ok:false,error:"PHOTO_REJECTED_BY_SAFETY",moderationStatus}};
      await blob.uploadData(bytes,{blobHTTPHeaders:{blobContentType:type,blobCacheControl:"no-store"}});
      const r=await query(
        "INSERT INTO photos(user_id,blob_key,visibility,moderation_status,moderation_provider,moderation_result,moderated_at) VALUES($1,$2,$3,$4,$5,$6::jsonb,now()) RETURNING id,visibility,moderation_status,created_at",
        [me.id,key,visibility,moderationStatus,"azure-ai-content-safety",JSON.stringify(moderation)]);
      return {status:201,jsonBody:{ok:true,photo:r.rows[0]}};
    }catch(e){context.error("PHOTO_UPLOAD_FAILED",e);return {status:500,jsonBody:{ok:false,error:"PHOTO_UPLOAD_FAILED"}};}
  })
});

app.http("photoList",{
  methods:["GET"],authLevel:"anonymous",route:"photos",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const r=await query(
        "SELECT id,visibility,moderation_status,created_at FROM photos WHERE user_id=$1 ORDER BY created_at DESC",
        [me.id]);
      return {status:200,jsonBody:{ok:true,photos:r.rows}};
    }catch(e){context.error("PHOTO_LIST_FAILED",e);return {status:500,jsonBody:{ok:false,error:"PHOTO_LIST_FAILED"}};}
  })
});


app.http("photoContent",{
  methods:["GET"],authLevel:"anonymous",route:"photos/{photoId}/content",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const id=request.params?.photoId||context.triggerMetadata?.photoId;
      const r=await query(
        "SELECT id,blob_key,visibility,moderation_status FROM photos WHERE id=$1 AND user_id=$2",
        [id,me.id]);
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"PHOTO_NOT_FOUND"}};
      if(r.rows[0].moderation_status!=="approved") return {status:403,jsonBody:{ok:false,error:"PHOTO_NOT_AVAILABLE"}};
      const blob=getProfilePhotosContainer().getBlockBlobClient(r.rows[0].blob_key);
      const exists=await blob.exists();
      if(!exists) return {status:404,jsonBody:{ok:false,error:"PHOTO_BLOB_NOT_FOUND"}};
      const downloaded=await blob.downloadToBuffer();
      const props=await blob.getProperties();
      return {status:200,headers:{"Content-Type":props.contentType||"application/octet-stream","Cache-Control":"private, no-store"},body:downloaded};
    }catch(e){
      context.error("PHOTO_CONTENT_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"PHOTO_CONTENT_FAILED"}};
    }
  })
});


app.http("matchPhotoContent",{
  methods:["GET"],authLevel:"anonymous",route:"matches/{targetIdentity}/photos/{photoId}/content",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      if(me.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      const targetIdentity=text(request.params?.targetIdentity||context.triggerMetadata?.targetIdentity,300);
      const photoId=text(request.params?.photoId||context.triggerMetadata?.photoId,100);
      if(!targetIdentity||!photoId) return {status:400,jsonBody:{ok:false,error:"PHOTO_REQUEST_INVALID"}};

      const target=await query(
        `SELECT u.id,u.status,p.profile_completed,p.is_visible,
                COALESCE(ps.profile_discoverable,true) AS discoverable,
                COALESCE(ps.show_photo_to_matches,true) AS show_photo
         FROM users u
         JOIN profiles p ON p.user_id=u.id
         LEFT JOIN privacy_settings ps ON ps.user_id=u.id
         WHERE (u.azure_subject=$1 OR u.firebase_uid=$1) LIMIT 1`,
        [targetIdentity]
      );
      const t=target.rows[0];
      if(!t||t.status!=="active"||!t.profile_completed||!t.is_visible||!t.discoverable)
        return {status:404,jsonBody:{ok:false,error:"MATCH_NOT_AVAILABLE"}};
      if(!t.show_photo)
        return {status:403,jsonBody:{ok:false,error:"PHOTO_PRIVATE"}};

      const blocked=await query(
        "SELECT 1 FROM blocked_users WHERE (blocker_user_id=$1 AND blocked_user_id=$2) OR (blocker_user_id=$2 AND blocked_user_id=$1) LIMIT 1",
        [me.id,t.id]
      );
      if(blocked.rows[0]) return {status:403,jsonBody:{ok:false,error:"PHOTO_NOT_AVAILABLE"}};

      const p=await query(
        "SELECT id,blob_key,visibility,moderation_status FROM photos WHERE id=$1 AND user_id=$2 LIMIT 1",
        [photoId,t.id]
      );
      const photo=p.rows[0];
      if(!photo||photo.moderation_status!=="approved"||!["matches","public"].includes(photo.visibility))
        return {status:404,jsonBody:{ok:false,error:"PHOTO_NOT_AVAILABLE"}};

      const blob=getProfilePhotosContainer().getBlockBlobClient(photo.blob_key);
      if(!(await blob.exists())) return {status:404,jsonBody:{ok:false,error:"PHOTO_BLOB_NOT_FOUND"}};
      const bytes=await blob.downloadToBuffer();
      const props=await blob.getProperties();
      return {status:200,headers:{"Content-Type":props.contentType||"image/jpeg","Cache-Control":"private, no-store"},body:bytes};
    }catch(e){
      context.error("MATCH_PHOTO_CONTENT_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"MATCH_PHOTO_CONTENT_FAILED"}};
    }
  })
});

app.http("photoVisibility",{
  methods:["PATCH"],authLevel:"anonymous",route:"photos/{photoId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const id=request.params?.photoId||context.triggerMetadata?.photoId;
      const b=await request.json();
      const visibility=text(b.visibility,20).toLowerCase();
      if(!["private","matches","public"].includes(visibility)) return {status:400,jsonBody:{ok:false,error:"INVALID_VISIBILITY"}};
      const r=await query("UPDATE photos SET visibility=$1 WHERE id=$2 AND user_id=$3 RETURNING id,visibility,moderation_status",[visibility,id,me.id]);
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"PHOTO_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true,photo:r.rows[0]}};
    }catch(e){context.error("PHOTO_VISIBILITY_FAILED",e);return {status:500,jsonBody:{ok:false,error:"PHOTO_VISIBILITY_FAILED"}};}
  })
});

app.http("photoDelete",{
  methods:["DELETE"],authLevel:"anonymous",route:"photos/{photoId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const id=request.params?.photoId||context.triggerMetadata?.photoId;
      const r=await query("DELETE FROM photos WHERE id=$1 AND user_id=$2 RETURNING id,blob_key",[id,me.id]);
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"PHOTO_NOT_FOUND"}};
      try{await getProfilePhotosContainer().getBlockBlobClient(r.rows[0].blob_key).deleteIfExists();}catch(e){context.warn("PHOTO_BLOB_DELETE_FAILED",e.message);}
      return {status:200,jsonBody:{ok:true,deletedPhotoId:r.rows[0].id}};
    }catch(e){context.error("PHOTO_DELETE_FAILED",e);return {status:500,jsonBody:{ok:false,error:"PHOTO_DELETE_FAILED"}};}
  })
});
