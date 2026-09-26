const { query } = require("./db");

async function userFromAuth(authUser){
  const azureSubject=typeof authUser?.azure_subject==="string"?authUser.azure_subject.trim():"";
  const uid=typeof authUser?.uid==="string"?authUser.uid.trim():"";
  let r;
  if(azureSubject){
    r=await query("SELECT id,status,email,azure_subject,firebase_uid FROM users WHERE azure_subject=$1 OR firebase_uid=$2 LIMIT 1",[azureSubject,uid||azureSubject]);
  }else if(uid){
    r=await query("SELECT id,status,email,azure_subject,firebase_uid FROM users WHERE firebase_uid=$1 LIMIT 1",[uid]);
  }else{
    const e=new Error("AUTH_IDENTITY_REQUIRED");e.statusCode=401;throw e;
  }
  const user=r.rows[0];
  if(!user||user.status!=="active"){const e=new Error("ACTIVE_USER_REQUIRED");e.statusCode=403;throw e;}
  return user;
}

async function entitlementForUser(userId){
  const r=await query(
    `SELECT plan_key,status,expires_at
       FROM premium_entitlements
      WHERE user_id=$1 AND status='active' AND expires_at>now()
      ORDER BY expires_at DESC LIMIT 1`,
    [userId]
  );
  const ent=r.rows[0]||null;
  return {
    active:Boolean(ent),
    planKey:ent?.plan_key||null,
    expiresAt:ent?.expires_at||null
  };
}

function capabilitiesFor(premium){
  const plan=premium?.active?premium.planKey:null;
  const basic=plan==="premium_basic_20"||plan==="premium_plus_40"||plan==="premium_vip_60";
  const plus=plan==="premium_plus_40"||plan==="premium_vip_60";
  const vip=plan==="premium_vip_60";
  return {
    paid20Features:basic,
    paid40Features:plus,
    whoLikedYou:basic,
    whoViewedYou:basic,
    unlimitedInterests:basic,
    advancedMatching:plus,
    whyWeMatched:plus,
    marriageTimeline:plus,
    paid60Ai:vip,
    aiNikahAssistant:vip,
    aiAdvanced:vip,
    familyCircleLimit:vip?10:plus?7:basic?4:2,
    priorityVisibility:vip,
    adFree:basic
  };
}

async function accessForAuth(authUser){
  const user=await userFromAuth(authUser);
  const premium=await entitlementForUser(user.id);
  return {user,premium,capabilities:capabilitiesFor(premium)};
}

function premiumRequired(premium){
  if(premium?.active)return null;
  return {status:402,jsonBody:{ok:false,error:"PREMIUM_REQUIRED",locked:true}};
}

module.exports={userFromAuth,entitlementForUser,accessForAuth,premiumRequired,capabilitiesFor};
