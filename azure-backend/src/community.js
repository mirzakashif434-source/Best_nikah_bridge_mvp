const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";
const MAX_TEXT=500, WINDOW_SECONDS=60, MAX_PER_WINDOW=5;

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!email) throw Object.assign(new Error("AUTH_IDENTITY_REQUIRED"),{statusCode:401});
  if(user.auth_provider==="azure_external_id" && user.azure_subject){
    const r=await query("SELECT id,status,email,terms_accepted,profile_completed FROM users WHERE azure_subject=$1 LIMIT 1",[String(user.azure_subject)]);
    if(!r.rows[0]) throw Object.assign(new Error("AZURE_USER_NOT_FOUND"),{statusCode:404});
    if(r.rows[0].status!=="active") throw Object.assign(new Error("USER_NOT_ACTIVE"),{statusCode:403});
    if(!r.rows[0].terms_accepted || !r.rows[0].profile_completed) throw Object.assign(new Error("ACTIVE_NIKAH_PROFILE_REQUIRED"),{statusCode:403});
    if(!user.email_verified) throw Object.assign(new Error("EMAIL_VERIFICATION_REQUIRED"),{statusCode:403});
    return r.rows[0];
  }
  if(!user.uid) throw Object.assign(new Error("AUTH_IDENTITY_REQUIRED"),{statusCode:401});
  const r=await query("SELECT id,status,email,terms_accepted,profile_completed FROM users WHERE firebase_uid=$1 LIMIT 1",[user.uid]);
  if(!r.rows[0]) throw Object.assign(new Error("USER_NOT_FOUND"),{statusCode:404});
  if(r.rows[0].status!=="active") throw Object.assign(new Error("USER_NOT_ACTIVE"),{statusCode:403});
  if(!r.rows[0].terms_accepted || !r.rows[0].profile_completed) throw Object.assign(new Error("ACTIVE_NIKAH_PROFILE_REQUIRED"),{statusCode:403});
  if(!user.email_verified) throw Object.assign(new Error("EMAIL_VERIFICATION_REQUIRED"),{statusCode:403});
  return r.rows[0];
}
function unsafe(s){
  const x=s.toLowerCase();
  return [/(?:\+?\d[\d\s().-]{7,}\d)/,/(?:whatsapp|telegram|snapchat|instagram|t\.me|wa\.me)/i,/(?:otp|one[- ]time password|password|recovery code|verification code)/i,/(?:send money|transfer money|gift card|crypto|bitcoin|usdt|bank account|iban)/i,/(?:https?:\/\/|www\.)/i].some(p=>p.test(x));
}
async function rateLimit(userId){
  const r=await query("SELECT window_started_at,message_count FROM community_rate_limits WHERE user_id=$1",[userId]);
  if(!r.rows[0]){await query("INSERT INTO community_rate_limits(user_id,window_started_at,message_count) VALUES($1,now(),1) ON CONFLICT(user_id) DO NOTHING",[userId]);return false;}
  const row=r.rows[0], elapsed=(Date.now()-new Date(row.window_started_at).getTime())/1000;
  if(elapsed>=WINDOW_SECONDS){await query("UPDATE community_rate_limits SET window_started_at=now(),message_count=1 WHERE user_id=$1",[userId]);return false;}
  if(Number(row.message_count)>=MAX_PER_WINDOW)return true;
  await query("UPDATE community_rate_limits SET message_count=message_count+1 WHERE user_id=$1",[userId]);return false;
}

app.http("communityMessagesList",{methods:["GET"],authLevel:"anonymous",route:"community/messages",handler:requireAuth(async(request,context,user)=>{
 try{const me=await ensureUser(user);const r=await query("SELECT m.id,m.author_user_id AS author_uid,m.author_name,m.country,m.body AS text,m.created_at FROM community_messages m WHERE m.moderation_status='visible' AND NOT EXISTS (SELECT 1 FROM community_mutes x WHERE x.user_id=$1 AND x.muted_user_id=m.author_user_id) ORDER BY m.created_at DESC LIMIT 100",[me.id]);return {status:200,jsonBody:{ok:true,messages:r.rows.reverse()}};}
 catch(e){context.error("COMMUNITY_LIST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"COMMUNITY_LIST_FAILED"}};}
})});

app.http("communityMessageSend",{methods:["POST"],authLevel:"anonymous",route:"community/messages",handler:requireAuth(async(request,context,user)=>{
 try{const me=await ensureUser(user);const b=await request.json();const body=text(b.text,MAX_TEXT);if(!body)return {status:400,jsonBody:{ok:false,error:"MESSAGE_REQUIRED"}};if(unsafe(body))return {status:422,jsonBody:{ok:false,error:"PRIVATE_OR_UNSAFE_CONTENT"}};if(await rateLimit(me.id))return {status:429,jsonBody:{ok:false,error:"RATE_LIMIT_REACHED"}};const p=await query("SELECT display_name,country FROM profiles WHERE user_id=$1",[me.id]);if(!p.rows[0])return {status:403,jsonBody:{ok:false,error:"PROFILE_REQUIRED"}};const r=await query("INSERT INTO community_messages(author_user_id,author_name,country,body) VALUES($1,$2,$3,$4) RETURNING id,author_user_id AS author_uid,author_name,country,body AS text,created_at",[me.id,text(p.rows[0].display_name,80),text(p.rows[0].country,80),body]);return {status:201,jsonBody:{ok:true,message:r.rows[0]}};}
 catch(e){context.error("COMMUNITY_SEND_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"COMMUNITY_SEND_FAILED"}};}
})});

app.http("communityMutesList",{methods:["GET"],authLevel:"anonymous",route:"community/mutes",handler:requireAuth(async(request,context,user)=>{
 try{const me=await ensureUser(user);const r=await query("SELECT muted_user_id FROM community_mutes WHERE user_id=$1 ORDER BY created_at DESC LIMIT 100",[me.id]);return {status:200,jsonBody:{ok:true,mutedUids:r.rows.map(x=>String(x.muted_user_id))}};}catch(e){context.error("COMMUNITY_MUTES_LIST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"COMMUNITY_MUTES_LIST_FAILED"}};}
})});

app.http("communityMute",{methods:["POST"],authLevel:"anonymous",route:"community/mutes",handler:requireAuth(async(request,context,user)=>{
 try{const me=await ensureUser(user);const b=await request.json();const target=String(b.userId||"");if(!target||target===String(me.id))return {status:400,jsonBody:{ok:false,error:"INVALID_MEMBER"}};const r=await query("SELECT id FROM users WHERE id=$1 OR firebase_uid=$1 OR azure_subject=$1 LIMIT 1",[target]);if(!r.rows[0])return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND"}};await query("INSERT INTO community_mutes(user_id,muted_user_id) VALUES($1,$2) ON CONFLICT DO NOTHING",[me.id,r.rows[0].id]);return {status:201,jsonBody:{ok:true,muted:true}};}catch(e){context.error("COMMUNITY_MUTE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"COMMUNITY_MUTE_FAILED"}};}
})});

app.http("communityUnmute",{methods:["DELETE"],authLevel:"anonymous",route:"community/mutes/{userId}",handler:requireAuth(async(request,context,user)=>{
 try{const me=await ensureUser(user);const target=String(request.params?.userId||"");const r=await query("SELECT id FROM users WHERE id=$1 OR firebase_uid=$1 OR azure_subject=$1 LIMIT 1",[target]);if(!r.rows[0])return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND"}};await query("DELETE FROM community_mutes WHERE user_id=$1 AND muted_user_id=$2",[me.id,r.rows[0].id]);return {status:200,jsonBody:{ok:true,muted:false}};}catch(e){context.error("COMMUNITY_UNMUTE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"COMMUNITY_UNMUTE_FAILED"}};}
})});

app.http("communityReport",{methods:["POST"],authLevel:"anonymous",route:"community/reports",handler:requireAuth(async(request,context,user)=>{
 try{const me=await ensureUser(user);const b=await request.json();const messageId=String(b.messageId||""),reported=String(b.reportedUid||""),reason=text(b.reason,120);if(!messageId||!reported||!reason||reported===String(me.id))return {status:400,jsonBody:{ok:false,error:"REPORT_DETAILS_REQUIRED"}};const msg=await query("SELECT id,author_user_id FROM community_messages WHERE id=$1",[messageId]);if(!msg.rows[0]||String(msg.rows[0].author_user_id)!==reported)return {status:404,jsonBody:{ok:false,error:"MESSAGE_NOT_FOUND"}};const r=await query("INSERT INTO community_reports(reporter_user_id,reported_user_id,message_id,reason) VALUES($1,$2,$3,$4) RETURNING id,status,created_at",[me.id,msg.rows[0].author_user_id,messageId,reason]);return {status:201,jsonBody:{ok:true,report:r.rows[0]}};}
 catch(e){context.error("COMMUNITY_REPORT_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"COMMUNITY_REPORT_FAILED"}};}
})});
