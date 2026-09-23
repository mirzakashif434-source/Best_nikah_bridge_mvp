const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");

function norm(v){
  return typeof v === "string" ? v.toLowerCase().replace(/[\\/,_-]+/g," ").trim() : "";
}
function compatible(looking, gender){
  const a=norm(looking), b=norm(gender);
  return !!a && !!b && (a.includes("any") || a.includes("no preference") || a.includes(b) || b.includes(a));
}
function overlap(a,b){
  const aa=norm(a).split(/\s+/).filter(x=>x.length>3);
  const bb=norm(b).split(/\s+/).filter(x=>x.length>3);
  return aa.some(x=>bb.includes(x));
}
function dealConflict(deal,prefs){
  const d=norm(deal);
  if(!d||!prefs)return false;
  return d.split(/\s+/).filter(x=>x.length>4).some(x=>norm(prefs).includes(x));
}

app.http("matchDetail",{
  methods:["GET"],
  authLevel:"anonymous",
  route:"matches/{matchUserId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const access=await accessForAuth(user);
      if(!access.capabilities.advancedMatching) return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};
      const matchUserId=request.params?.matchUserId||context.triggerMetadata?.matchUserId;
      if(!matchUserId) return {status:400,jsonBody:{ok:false,error:"MATCH_USER_ID_REQUIRED"}};

      const me=await query(`
        SELECT u.id,u.firebase_uid,p.display_name,p.date_of_birth,p.gender,p.country,p.city,p.marriage_intention,
               p.readiness_score,p.profile_completed,p.is_visible,
               pp.min_age,pp.max_age,pp.preferred_gender,pp.countries,pp.cities,
               pp.preferred_marriage_timeline,pp.deal_breakers,pp.preferences,
               EXISTS(SELECT 1 FROM verifications v WHERE v.user_id=u.id AND v.status='approved') AS identity_verified,
               EXISTS(SELECT 1 FROM photos ph WHERE ph.user_id=u.id AND ph.moderation_status='approved') AS photo_present
        FROM users u
        JOIN profiles p ON p.user_id=u.id
        LEFT JOIN partner_preferences pp ON pp.user_id=u.id
        WHERE (u.azure_subject=$1 OR u.firebase_uid=$2) AND u.status='active'
      `,[user.azure_subject||"",user.uid||""]);

      const candidate=await query(`
        SELECT u.id,u.firebase_uid,u.azure_subject,p.display_name,p.date_of_birth,p.gender,p.country,p.city,p.marriage_intention,
               p.readiness_score,p.profile_completed,p.is_visible,
               pp.min_age,pp.max_age,pp.preferred_gender,pp.countries,pp.cities,
               pp.preferred_marriage_timeline,pp.deal_breakers,pp.preferences,
               EXISTS(SELECT 1 FROM verifications v WHERE v.user_id=u.id AND v.status='approved') AS identity_verified,
               EXISTS(SELECT 1 FROM photos ph WHERE ph.user_id=u.id AND ph.moderation_status='approved') AS photo_present
        FROM users u
        JOIN profiles p ON p.user_id=u.id
        LEFT JOIN partner_preferences pp ON pp.user_id=u.id
        WHERE (u.azure_subject=$1 OR u.firebase_uid=$1 OR u.id::text=$1) AND u.status='active'
      `,[matchUserId]);

      if(!me.rows[0] || !me.rows[0].profile_completed || !me.rows[0].is_visible)
        return {status:409,jsonBody:{ok:false,error:"PROFILE_NOT_READY"}};
      if(!candidate.rows[0] || !candidate.rows[0].profile_completed || !candidate.rows[0].is_visible)
        return {status:404,jsonBody:{ok:false,error:"MATCH_NOT_FOUND"}};
      if(me.rows[0].id===candidate.rows[0].id)
        return {status:400,jsonBody:{ok:false,error:"SELF_MATCH_NOT_ALLOWED"}};

      const m=me.rows[0], c=candidate.rows[0];
      const myAge=Math.floor((Date.now()-new Date(m.date_of_birth+"T00:00:00Z").getTime())/31557600000);
      const theirAge=Math.floor((Date.now()-new Date(c.date_of_birth+"T00:00:00Z").getTime())/31557600000);

      const myMin=Number(m.min_age||18), myMax=Number(m.max_age||100);
      const theirMin=Number(c.min_age||18), theirMax=Number(c.max_age||100);
      if(theirAge<myMin||theirAge>myMax||myAge<theirMin||myAge>theirMax)
        return {status:404,jsonBody:{ok:false,error:"MATCH_NOT_COMPATIBLE"}};
      if(!compatible(m.preferred_gender||"",c.gender)||!compatible(c.preferred_gender||"",m.gender))
        return {status:404,jsonBody:{ok:false,error:"MATCH_NOT_COMPATIBLE"}};

      let score=40;
      const reasons=["reciprocal age and gender preferences"];
      if(norm(m.marriage_intention)&&norm(m.marriage_intention)===norm(c.marriage_intention)){score+=15;reasons.push("same marriage intention");}
      if(norm(m.preferred_marriage_timeline)&&norm(m.preferred_marriage_timeline)===norm(c.preferred_marriage_timeline)){score+=12;reasons.push("same marriage timeline");}
      if(norm(m.country)&&norm(m.country)===norm(c.country)){score+=8;reasons.push("same country");}
      if(norm(m.city)&&norm(m.city)===norm(c.city)){score+=5;reasons.push("same city");}
      if(overlap(JSON.stringify(m.preferences),JSON.stringify(c.preferences))){score+=8;reasons.push("overlapping partner preferences");}
      if(!dealConflict(JSON.stringify(m.deal_breakers),JSON.stringify(c.preferences)) &&
         !dealConflict(JSON.stringify(c.deal_breakers),JSON.stringify(m.preferences))){score+=7;reasons.push("no detected deal-breaker conflict");}
      score=Math.min(100,score);

      return {status:200,jsonBody:{ok:true,match:{
        userId:c.azure_subject||c.firebase_uid||String(c.id),
        displayName:c.display_name,
        age:theirAge,
        gender:c.gender,
        country:c.country,
        city:c.city,
        marriageIntention:c.marriage_intention,
        marriageTimeline:c.preferred_marriage_timeline,
        readinessScore:c.readiness_score,
        identityVerified:Boolean(c.identity_verified),
        photoPresent:Boolean(c.photo_present),
        compatibilityScore:score,
        whyWeMatched:reasons
      }}};
    }catch(e){
      context.error("MATCH_DETAIL_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"MATCH_DETAIL_FAILED"}};
    }
  })
});
