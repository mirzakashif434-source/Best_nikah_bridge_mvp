const { app } = require("@azure/functions");
const crypto = require("crypto");
const { query, getPool } = require("./db");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");
const { getProfilePhotosContainer } = require("./storage");
const { analyzeImage, shouldReject } = require("./contentSafety");
const { requireAdultProfile } = require("./ageGate");

const ALLOWED=new Set(["image/jpeg","image/png","image/webp"]);
const MAX=4*1024*1024;
const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function ensureUser(user){
  const key=(user.azure_subject||user.uid||"").trim();
  const r=await query("SELECT id,status FROM users WHERE azure_subject=$1 OR firebase_uid=$1 LIMIT 1",[key]);
  if(!r.rows[0]){const e=new Error("USER_NOT_FOUND");e.statusCode=404;throw e;}
  if(r.rows[0].status!=="active"){const e=new Error("ACCOUNT_NOT_ACTIVE");e.statusCode=403;throw e;}
  return r.rows[0];
}
function ext(type){return type==="image/png"?"png":type==="image/webp"?"webp":"jpg";}
async function requireAdmin(user){
  const me=await ensureUser(user);
  const r=await query("SELECT role FROM users WHERE id=$1",[me.id]);
  if(!["admin","moderator"].includes(r.rows[0]?.role)){const e=new Error("ADMIN_REQUIRED");e.statusCode=403;throw e;}
  return me;
}
async function readPhotoBytes(photo){
  const blob=getProfilePhotosContainer().getBlockBlobClient(photo.blob_key);
  return await blob.downloadToBuffer();
}
async function detectFace(bytes){
  const endpoint=(process.env.AZURE_FACE_ENDPOINT||"").replace(/\/$/,"");
  const key=process.env.AZURE_FACE_KEY||"";
  if(!endpoint||!key) throw new Error("FACE_VERIFY_NOT_CONFIGURED");
  const url=endpoint+"/face/v1.0/detect?returnFaceId=true&recognitionModel=recognition_04&detectionModel=detection_03";
  const res=await fetch(url,{method:"POST",headers:{"Ocp-Apim-Subscription-Key":key,"Content-Type":"application/octet-stream"},body:bytes});
  if(!res.ok) throw new Error("FACE_DETECT_FAILED_"+res.status);
  const faces=await res.json();
  if(!Array.isArray(faces)||faces.length!==1||!faces[0]?.faceId) throw new Error("EXACTLY_ONE_FACE_REQUIRED");
  return faces[0].faceId;
}
async function verifyFace(faceId1,faceId2){
  const endpoint=(process.env.AZURE_FACE_ENDPOINT||"").replace(/\/$/,"");
  const key=process.env.AZURE_FACE_KEY||"";
  const res=await fetch(endpoint+"/face/v1.0/verify",{method:"POST",headers:{"Ocp-Apim-Subscription-Key":key,"Content-Type":"application/json"},body:JSON.stringify({faceId1,faceId2})});
  if(!res.ok) throw new Error("FACE_VERIFY_FAILED_"+res.status);
  return await res.json();
}
async function tryAutomatedFaceMatch(photos){
  if(String(process.env.AZURE_FACE_VERIFY_ENABLED||"").toLowerCase()!=="true") return {available:false};
  const main=photos.find(p=>Number(p.verification_slot)===1);
  if(!main) return {available:false};
  const mainFace=await detectFace(await readPhotoBytes(main));
  let minConfidence=1;
  for(const p of photos.filter(x=>Number(x.verification_slot)!==1)){
    const face=await detectFace(await readPhotoBytes(p));
    const result=await verifyFace(mainFace,face);
    if(!result?.isIdentical) return {available:true,identical:false,confidence:Number(result?.confidence||0)};
    minConfidence=Math.min(minConfidence,Number(result?.confidence||0));
  }
  return {available:true,identical:true,confidence:minConfidence};
}

app.http("photoVerificationStart",{
  methods:["POST"],authLevel:"anonymous",route:"photo-verification/start",
  handler:requireAuth(async(req,ctx,user)=>{
    try{
      const access=await accessForAuth(user);if(!access.capabilities.paid40Features)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};const me=access.user;
      await requireAdultProfile(me.id);
      await query("UPDATE photo_verification_sets SET status='rejected',updated_at=now() WHERE user_id=$1 AND status='draft'",[me.id]);
      const r=await query("INSERT INTO photo_verification_sets(user_id,status,provider) VALUES($1,'draft','manual-review') RETURNING id,status,created_at",[me.id]);
      return {status:201,jsonBody:{ok:true,set:r.rows[0]}};
    }catch(e){ctx.error("PHOTO_VERIFICATION_START_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PHOTO_VERIFICATION_START_FAILED"}};}
  })
});

app.http("photoVerificationUpload",{
  methods:["POST"],authLevel:"anonymous",route:"photo-verification/{setId}/photos/{slot}",
  handler:requireAuth(async(req,ctx,user)=>{
    try{
      const access=await accessForAuth(user);if(!access.capabilities.paid40Features)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};const me=access.user;
      await requireAdultProfile(me.id);
      const setId=text(req.params?.setId||ctx.triggerMetadata?.setId,100);
      const slot=Number(req.params?.slot||ctx.triggerMetadata?.slot);
      if(![1,2,3,4].includes(slot))return {status:400,jsonBody:{ok:false,error:"INVALID_PHOTO_SLOT"}};
      const form=await req.formData();
      const source=text(form.get("source"),20).toLowerCase();
      if(slot===1&&source!=="camera")return {status:400,jsonBody:{ok:false,error:"MAIN_PHOTO_CAMERA_REQUIRED"}};
      if(slot>1&&source!=="gallery")return {status:400,jsonBody:{ok:false,error:"GALLERY_SOURCE_REQUIRED"}};
      const set=await query("SELECT id,status FROM photo_verification_sets WHERE id=$1 AND user_id=$2 LIMIT 1",[setId,me.id]);
      if(!set.rows[0]||set.rows[0].status!=="draft")return {status:409,jsonBody:{ok:false,error:"VERIFICATION_SET_NOT_EDITABLE"}};
      const file=form.get("photo");
      if(!file||typeof file.arrayBuffer!=="function")return {status:400,jsonBody:{ok:false,error:"PHOTO_REQUIRED"}};
      const type=text(file.type,100).toLowerCase();
      if(!ALLOWED.has(type))return {status:400,jsonBody:{ok:false,error:"UNSUPPORTED_IMAGE_TYPE"}};
      const bytes=Buffer.from(await file.arrayBuffer());
      if(bytes.length===0||bytes.length>MAX)return {status:400,jsonBody:{ok:false,error:"PHOTO_SIZE_INVALID"}};
      const moderation=await analyzeImage(bytes);
      if(shouldReject(moderation))return {status:422,jsonBody:{ok:false,error:"PHOTO_REJECTED_BY_SAFETY"}};
      const key=me.id+"/verification-"+setId+"/slot-"+slot+"-"+crypto.randomUUID()+"."+ext(type);
      await getProfilePhotosContainer().getBlockBlobClient(key).uploadData(bytes,{blobHTTPHeaders:{blobContentType:type,blobCacheControl:"no-store"}});
      const old=await query("SELECT id,blob_key FROM photos WHERE verification_set_id=$1 AND verification_slot=$2 AND user_id=$3 LIMIT 1",[setId,slot,me.id]);
      if(old.rows[0]){
        try{await getProfilePhotosContainer().getBlockBlobClient(old.rows[0].blob_key).deleteIfExists();}catch(_){}
        await query("DELETE FROM photos WHERE id=$1",[old.rows[0].id]);
      }
      const r=await query(
        "INSERT INTO photos(user_id,blob_key,visibility,moderation_status,moderation_provider,moderation_result,moderated_at,verification_set_id,verification_slot,is_main_profile_photo,face_match_status) VALUES($1,$2,'private','approved','azure-ai-content-safety',$3::jsonb,now(),$4,$5,$6,'pending') RETURNING id,verification_slot,is_main_profile_photo",
        [me.id,key,JSON.stringify(moderation),setId,slot,slot===1]
      );
      return {status:201,jsonBody:{ok:true,photo:r.rows[0],source}};
    }catch(e){ctx.error("PHOTO_VERIFICATION_UPLOAD_FAILED",e);return {status:500,jsonBody:{ok:false,error:"PHOTO_VERIFICATION_UPLOAD_FAILED"}};}
  })
});

app.http("photoVerificationSubmit",{
  methods:["POST"],authLevel:"anonymous",route:"photo-verification/{setId}/submit",
  handler:requireAuth(async(req,ctx,user)=>{
    const client=await getPool().connect();
    try{
      const access=await accessForAuth(user);if(!access.capabilities.paid40Features)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};const me=access.user;
      await requireAdultProfile(me.id);
      const setId=text(req.params?.setId||ctx.triggerMetadata?.setId,100);
      await client.query("BEGIN");
      const set=await client.query("SELECT id,status FROM photo_verification_sets WHERE id=$1 AND user_id=$2 FOR UPDATE",[setId,me.id]);
      if(!set.rows[0]||set.rows[0].status!=="draft"){await client.query("ROLLBACK");return {status:409,jsonBody:{ok:false,error:"VERIFICATION_SET_NOT_EDITABLE"}};}
      const photos=await client.query("SELECT id,blob_key,verification_slot,is_main_profile_photo FROM photos WHERE verification_set_id=$1 AND user_id=$2 ORDER BY verification_slot",[setId,me.id]);
      if(photos.rows.length!==4||photos.rows.map(p=>Number(p.verification_slot)).join(",")!=="1,2,3,4"){await client.query("ROLLBACK");return {status:409,jsonBody:{ok:false,error:"FOUR_PHOTOS_REQUIRED"}};}
      await client.query("COMMIT");

      let auto={available:false};
      try{auto=await tryAutomatedFaceMatch(photos.rows);}catch(e){ctx.warn("AZURE_FACE_VERIFY_UNAVAILABLE",e.message);auto={available:false,error:e.message};}
      if(auto.available&&auto.identical){
        await query("UPDATE photo_verification_sets SET status='approved',provider='azure-face-verify',automated_face_match=true,face_match_confidence=$2,submitted_at=now(),reviewed_at=now(),updated_at=now() WHERE id=$1",[setId,auto.confidence]);
        await query("UPDATE photos SET face_match_status='matched',visibility=CASE WHEN verification_slot=1 THEN 'matches' ELSE visibility END WHERE verification_set_id=$1",[setId]);
        await query("UPDATE profiles SET photo_verified=true,updated_at=now() WHERE user_id=$1",[me.id]);
        return {status:200,jsonBody:{ok:true,status:"approved",automated:true,confidence:auto.confidence}};
      }
      if(auto.available&&auto.identical===false){
        await query("UPDATE photo_verification_sets SET status='rejected',provider='azure-face-verify',automated_face_match=true,face_match_confidence=$2,submitted_at=now(),reviewed_at=now(),updated_at=now() WHERE id=$1",[setId,auto.confidence]);
        await query("UPDATE photos SET face_match_status='mismatch' WHERE verification_set_id=$1",[setId]);
        return {status:422,jsonBody:{ok:false,error:"PHOTOS_NOT_SAME_PERSON",status:"rejected"}};
      }
      await query("UPDATE photo_verification_sets SET status='pending',provider='manual-review',submitted_at=now(),updated_at=now() WHERE id=$1",[setId]);
      return {status:202,jsonBody:{ok:true,status:"pending",automated:false,reviewRequired:true}};
    }catch(e){
      await client.query("ROLLBACK").catch(()=>{});
      ctx.error("PHOTO_VERIFICATION_SUBMIT_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"PHOTO_VERIFICATION_SUBMIT_FAILED"}};
    }finally{client.release();}
  })
});

app.http("photoVerificationStatus",{
  methods:["GET"],authLevel:"anonymous",route:"photo-verification/status",
  handler:requireAuth(async(req,ctx,user)=>{
    try{
      const me=await ensureUser(user);
      const r=await query("SELECT id,status,provider,automated_face_match,face_match_confidence,submitted_at,reviewed_at,created_at FROM photo_verification_sets WHERE user_id=$1 ORDER BY created_at DESC LIMIT 1",[me.id]);
      const p=await query("SELECT photo_verified FROM profiles WHERE user_id=$1",[me.id]);
      return {status:200,jsonBody:{ok:true,photoVerified:Boolean(p.rows[0]?.photo_verified),set:r.rows[0]||null}};
    }catch(e){return {status:500,jsonBody:{ok:false,error:"PHOTO_VERIFICATION_STATUS_FAILED"}};}
  })
});

app.http("photoVerificationAdminList",{
  methods:["GET"],authLevel:"anonymous",route:"admin/photo-verifications",
  handler:requireAuth(async(req,ctx,user)=>{
    try{
      await requireAdmin(user);
      const r=await query(`SELECT s.id,s.user_id,s.status,s.provider,s.submitted_at,p.display_name,u.email
        FROM photo_verification_sets s JOIN users u ON u.id=s.user_id LEFT JOIN profiles p ON p.user_id=s.user_id
        WHERE s.status='pending' ORDER BY s.submitted_at ASC LIMIT 100`);
      return {status:200,jsonBody:{ok:true,items:r.rows}};
    }catch(e){return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PHOTO_VERIFICATION_ADMIN_LIST_FAILED"}};}
  })
});

app.http("photoVerificationAdminContent",{
  methods:["GET"],authLevel:"anonymous",route:"admin/photo-verifications/{setId}/photos/{slot}/content",
  handler:requireAuth(async(req,ctx,user)=>{
    try{
      await requireAdmin(user);
      const setId=text(req.params?.setId||ctx.triggerMetadata?.setId,100),slot=Number(req.params?.slot||ctx.triggerMetadata?.slot);
      const r=await query("SELECT blob_key FROM photos WHERE verification_set_id=$1 AND verification_slot=$2 LIMIT 1",[setId,slot]);
      if(!r.rows[0])return {status:404,jsonBody:{ok:false,error:"PHOTO_NOT_FOUND"}};
      const blob=getProfilePhotosContainer().getBlockBlobClient(r.rows[0].blob_key);
      const bytes=await blob.downloadToBuffer(),props=await blob.getProperties();
      return {status:200,headers:{"Content-Type":props.contentType||"image/jpeg","Cache-Control":"private, no-store"},body:bytes};
    }catch(e){return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PHOTO_VERIFICATION_CONTENT_FAILED"}};}
  })
});

app.http("photoVerificationAdminReview",{
  methods:["PATCH"],authLevel:"anonymous",route:"admin/photo-verifications/{setId}",
  handler:requireAuth(async(req,ctx,user)=>{
    const client=await getPool().connect();
    try{
      await requireAdmin(user);
      const setId=text(req.params?.setId||ctx.triggerMetadata?.setId,100);
      const body=await req.json(),decision=text(body?.decision,20).toLowerCase();
      if(!["approved","rejected"].includes(decision))return {status:400,jsonBody:{ok:false,error:"INVALID_DECISION"}};
      await client.query("BEGIN");
      const r=await client.query("UPDATE photo_verification_sets SET status=$2,provider='authorized-manual-review',reviewed_at=now(),updated_at=now() WHERE id=$1 AND status='pending' RETURNING user_id",[setId,decision]);
      if(!r.rows[0]){await client.query("ROLLBACK");return {status:404,jsonBody:{ok:false,error:"PENDING_SET_NOT_FOUND"}};}
      if(decision==="approved"){
        await client.query("UPDATE photos SET face_match_status='matched',visibility=CASE WHEN verification_slot=1 THEN 'matches' ELSE visibility END WHERE verification_set_id=$1",[setId]);
        await client.query("UPDATE profiles SET photo_verified=true,updated_at=now() WHERE user_id=$1",[r.rows[0].user_id]);
      }else{
        await client.query("UPDATE photos SET face_match_status='mismatch' WHERE verification_set_id=$1",[setId]);
        await client.query("UPDATE profiles SET photo_verified=false,updated_at=now() WHERE user_id=$1",[r.rows[0].user_id]);
      }
      await client.query("COMMIT");
      return {status:200,jsonBody:{ok:true,status:decision}};
    }catch(e){await client.query("ROLLBACK").catch(()=>{});return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PHOTO_VERIFICATION_ADMIN_REVIEW_FAILED"}};}
    finally{client.release();}
  })
});
