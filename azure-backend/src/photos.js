const { app } = require("@azure/functions");
const crypto = require("crypto");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { getProfilePhotosContainer } = require("./storage");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";
const ALLOWED=new Set(["image/jpeg","image/png","image/webp"]);
const MAX=8*1024*1024;

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!user.uid||!email) throw new Error("AUTH_IDENTITY_REQUIRED");
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
      const form=await request.formData();
      const file=form.get("photo");
      const visibility=text(form.get("visibility"),20).toLowerCase()||"private";
      if(!file||typeof file.arrayBuffer!=="function") return {status:400,jsonBody:{ok:false,error:"PHOTO_REQUIRED"}};
      if(!["private","matches","public"].includes(visibility)) return {status:400,jsonBody:{ok:false,error:"INVALID_VISIBILITY"}};
      const type=text(file.type,100).toLowerCase();
      if(!ALLOWED.has(type)) return {status:400,jsonBody:{ok:false,error:"UNSUPPORTED_IMAGE_TYPE"}};
      const bytes=Buffer.from(await file.arrayBuffer());
      if(bytes.length===0||bytes.length>MAX) return {status:400,jsonBody:{ok:false,error:"PHOTO_SIZE_INVALID"}};
      const key=me.id+"/"+crypto.randomUUID()+"."+ext(type);
      const blob=getProfilePhotosContainer().getBlockBlobClient(key);
      await blob.uploadData(bytes,{blobHTTPHeaders:{blobContentType:type,blobCacheControl:"no-store"}});
      const r=await query(
        "INSERT INTO photos(user_id,blob_key,visibility,moderation_status) VALUES($1,$2,$3,'pending') RETURNING id,visibility,moderation_status,created_at",
        [me.id,key,visibility]);
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
