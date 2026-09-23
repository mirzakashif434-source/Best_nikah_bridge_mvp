const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!user.uid||!email){const e=new Error("AUTH_IDENTITY_REQUIRED");e.statusCode=401;throw e;}
  const azureSubject=typeof user.azure_subject==="string"&&user.azure_subject.trim()?user.azure_subject.trim():null;
  let r;
  if(azureSubject){
    r=await query("SELECT id,status FROM users WHERE azure_subject=$1 OR firebase_uid=$2 LIMIT 1",[azureSubject,user.uid]);
    if(!r.rows[0]) r=await query("INSERT INTO users(firebase_uid,azure_subject,email,email_verified_at) VALUES($1,$2,$3,CASE WHEN $4 THEN now() ELSE NULL END) RETURNING id,status",[user.uid,azureSubject,email,Boolean(user.email_verified)]);
  }else{
    r=await query("INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status",[user.uid,email,Boolean(user.email_verified)]);
  }
  if(r.rows[0].status!=="active"){const e=new Error("USER_NOT_ACTIVE");e.statusCode=403;throw e;}
  return r.rows[0];
}

app.http("privacyGet",{
  methods:["GET"],authLevel:"anonymous",route:"privacy",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const r=await query("SELECT profile_discoverable,show_city,show_photo_to_matches,updated_at FROM privacy_settings WHERE user_id=$1",[me.id]);
      return {status:200,jsonBody:{ok:true,privacy:r.rows[0]||{profile_discoverable:true,show_city:true,show_photo_to_matches:true}}};
    }catch(e){context.error("PRIVACY_GET_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PRIVACY_GET_FAILED"}};}
  })
});

app.http("privacyUpdate",{
  methods:["PATCH"],authLevel:"anonymous",route:"privacy",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const b=await request.json();
      const profileDiscoverable=typeof b.profileDiscoverable==="boolean"?b.profileDiscoverable:true;
      const showCity=typeof b.showCity==="boolean"?b.showCity:true;
      const showPhotoToMatches=typeof b.showPhotoToMatches==="boolean"?b.showPhotoToMatches:true;
      const r=await query(
        "INSERT INTO privacy_settings(user_id,profile_discoverable,show_city,show_photo_to_matches) VALUES($1,$2,$3,$4) ON CONFLICT(user_id) DO UPDATE SET profile_discoverable=EXCLUDED.profile_discoverable,show_city=EXCLUDED.show_city,show_photo_to_matches=EXCLUDED.show_photo_to_matches,updated_at=now() RETURNING profile_discoverable,show_city,show_photo_to_matches,updated_at",
        [me.id,profileDiscoverable,showCity,showPhotoToMatches]
      );
      await query("UPDATE profiles SET is_visible=$2,updated_at=now() WHERE user_id=$1",[me.id,profileDiscoverable]);
      await query("UPDATE photos SET visibility=$2 WHERE user_id=$1 AND moderation_status='approved'",[me.id,showPhotoToMatches?"matches":"private"]);
      return {status:200,jsonBody:{ok:true,privacy:r.rows[0]}};
    }catch(e){context.error("PRIVACY_UPDATE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PRIVACY_UPDATE_FAILED"}};}
  })
});
