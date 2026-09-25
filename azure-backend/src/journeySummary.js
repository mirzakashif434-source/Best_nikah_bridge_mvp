const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");

async function currentUser(user){
  const key=String(user.azure_subject||user.uid||"").trim();
  const r=await query(
    "SELECT id,status FROM users WHERE (azure_subject=$1 OR firebase_uid=$1) LIMIT 1",
    [key]
  );
  if(!r.rows[0]){const e=new Error("USER_NOT_FOUND");e.statusCode=404;throw e;}
  return r.rows[0];
}

app.http("nikahJourneySummaryAzure",{
  methods:["GET"],
  authLevel:"anonymous",
  route:"journey/summary",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const access=await accessForAuth(user);
      if(!access.capabilities.paid20Features) return {status:402,jsonBody:{ok:false,error:"PREMIUM_BASIC_REQUIRED",locked:true}};
      const me=access.user;

      const [profile,living,verification,conversations,family,candidates]=await Promise.all([
        query("SELECT profile_completed,is_visible FROM profiles WHERE user_id=$1 LIMIT 1",[me.id]),
        query("SELECT marriage_timeline,family_involvement,children_expectation FROM living_compatibility WHERE user_id=$1 LIMIT 1",[me.id]),
        query("SELECT 1 FROM verifications WHERE user_id=$1 AND status='approved' LIMIT 1",[me.id]),
        query("SELECT 1 FROM conversations WHERE status='mutual' AND (user_a_id=$1 OR user_b_id=$1) LIMIT 1",[me.id]),
        query("SELECT 1 FROM family_links WHERE user_id=$1 AND status<>'revoked' LIMIT 1",[me.id]),
        query(`SELECT count(*)::int AS count
                 FROM users u
                 JOIN profiles p ON p.user_id=u.id
                 LEFT JOIN privacy_settings ps ON ps.user_id=u.id
                WHERE u.status='active'
                  AND u.id<>$1
                  AND p.profile_completed=true
                  AND p.is_visible=true
                  AND COALESCE(ps.profile_discoverable,false)=true
                  AND NOT EXISTS (
                    SELECT 1 FROM blocked_users b
                     WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=u.id)
                        OR (b.blocked_user_id=$1 AND b.blocker_user_id=u.id)
                  )`,[me.id])
      ]);

      const p=profile.rows[0]||{};
      const l=living.rows[0]||{};
      const payload={
        profileReady:Boolean(p.profile_completed),
        blueprintReady:Boolean(l.marriage_timeline&&l.family_involvement&&l.children_expectation),
        verified:Boolean(verification.rows[0]),
        hasMatches:Number(candidates.rows[0]?.count||0)>0,
        hasConversation:Boolean(conversations.rows[0]),
        hasFamily:Boolean(family.rows[0])
      };
      payload.completed=[payload.profileReady,payload.blueprintReady,payload.verified,payload.hasMatches,payload.hasConversation,payload.hasFamily].filter(Boolean).length;
      return {status:200,jsonBody:{ok:true,...payload}};
    }catch(e){
      context.error("NIKAH_JOURNEY_SUMMARY_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NIKAH_JOURNEY_SUMMARY_FAILED"}};
    }
  })
});
