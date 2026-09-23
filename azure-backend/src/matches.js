const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { entitlementForUser, capabilitiesFor } = require("./premiumAccess");

function norm(v){return typeof v==="string"?v.toLowerCase().replace(/[\\/,_-]+/g," ").trim():"";}
function overlap(a,b){
  const aa=norm(a).split(/\s+/).filter(x=>x.length>3);
  const bb=norm(b).split(/\s+/).filter(x=>x.length>3);
  return aa.some(x=>bb.includes(x));
}
function compatible(looking,gender){
  const a=norm(looking), b=norm(gender);
  return !!a && !!b && (a.includes("any") || a.includes("no preference") || a.includes(b) || b.includes(a));
}
function dealConflict(deal,prefs){
  const d=norm(deal);
  if(!d||!prefs)return false;
  return d.split(/\s+/).filter(x=>x.length>4).some(x=>norm(prefs).includes(x));
}

app.http("matches",{
  methods:["GET"],authLevel:"anonymous",route:"matches",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await query(`
        SELECT u.id,u.azure_subject,u.firebase_uid,p.*,EXTRACT(YEAR FROM age(CURRENT_DATE,p.date_of_birth))::int AS age,pp.min_age,pp.max_age,pp.preferred_gender,pp.countries,pp.cities,
               pp.preferred_marriage_timeline,pp.deal_breakers,pp.preferences,pp.education_levels,pp.family_involvement,ps.show_city
        FROM users u JOIN profiles p ON p.user_id=u.id
        LEFT JOIN partner_preferences pp ON pp.user_id=u.id
        LEFT JOIN privacy_settings ps ON ps.user_id=u.id
        LEFT JOIN user_presence up ON up.user_id=u.id
        WHERE u.status='active' AND (u.azure_subject=$1 OR u.firebase_uid=$2)`,[user.azure_subject||"",user.uid]);
      if(!me.rows[0]||!me.rows[0].profile_completed||!me.rows[0].is_visible)
        return {status:409,jsonBody:{ok:false,error:"PROFILE_NOT_READY"}};

      const m=me.rows[0];
      const viewerPremium=await entitlementForUser(m.id);
      const viewerCaps=capabilitiesFor(viewerPremium);
      const candidates=await query(`
        SELECT u.id,u.azure_subject,u.firebase_uid,p.*,EXTRACT(YEAR FROM age(CURRENT_DATE,p.date_of_birth))::int AS age,pp.min_age,pp.max_age,pp.preferred_gender,pp.countries,pp.cities,
               pp.preferred_marriage_timeline,pp.deal_breakers,pp.preferences,ps.show_city,
               COALESCE(ps.show_photo_to_matches,true) AS show_photo_to_matches,
               EXISTS(
                 SELECT 1 FROM premium_entitlements pe
                 WHERE pe.user_id=u.id AND pe.status='active' AND pe.expires_at>now()
                   AND pe.plan_key='premium_vip_60'
               ) AS premium_priority,
               COALESCE(up.last_seen_at >= now()-interval '2 minutes',false) AS is_online,
               up.last_seen_at,
               (SELECT ph.id FROM photos ph WHERE ph.user_id=u.id AND ph.moderation_status='approved' ORDER BY ph.created_at ASC LIMIT 1) AS photo_id
        FROM users u JOIN profiles p ON p.user_id=u.id
        LEFT JOIN partner_preferences pp ON pp.user_id=u.id
        LEFT JOIN privacy_settings ps ON ps.user_id=u.id
        WHERE u.status='active' AND p.profile_completed=true AND p.is_visible=true
          AND COALESCE(ps.profile_discoverable,true)=true
          AND u.id<>$1
          AND NOT EXISTS (SELECT 1 FROM blocked_users b WHERE b.blocker_user_id=$1 AND b.blocked_user_id=u.id)
          AND NOT EXISTS (SELECT 1 FROM blocked_users b WHERE b.blocker_user_id=u.id AND b.blocked_user_id=$1)
        LIMIT 500`,[m.id]);

      const matches=[];
      for(const c of candidates.rows){
        const myMin=Number(m.min_age||18),myMax=Number(m.max_age||100);
        const theirMin=Number(c.min_age||18),theirMax=Number(c.max_age||100);
        const ageOk=Number(c.age||0)>=myMin&&Number(c.age||0)<=myMax;
        const reciprocalAge=Number(m.age||0)>=theirMin&&Number(m.age||0)<=theirMax;
        if(!ageOk||!reciprocalAge)continue;
        if(!compatible(m.preferred_gender||"",c.gender)||!compatible(c.preferred_gender||"",m.gender))continue;
        if(viewerCaps.advancedMatching){
          const wantedCountries=Array.isArray(m.countries)?m.countries.filter(Boolean):[];
          const wantedCities=Array.isArray(m.cities)?m.cities.filter(Boolean):[];
          const wantedEducation=Array.isArray(m.education_levels)?m.education_levels.filter(Boolean):[];
          const wantedFamily=norm(m.family_involvement);
          if(wantedCountries.length && !wantedCountries.some(x=>norm(x)===norm(c.country))) continue;
          if(wantedCities.length && !wantedCities.some(x=>norm(x)===norm(c.city))) continue;
          if(wantedEducation.length && !wantedEducation.some(x=>norm(x)===norm(c.education))) continue;
          if(wantedFamily && wantedFamily!==norm(c.family_involvement)) continue;
        }

        let score=40;
        const reasons=["reciprocal age and gender preferences"];
        if(norm(m.marriage_intention) && norm(m.marriage_intention)===norm(c.marriage_intention)){score+=15;reasons.push("same marriage intention");}
        if(norm(m.preferred_marriage_timeline) && norm(m.preferred_marriage_timeline)===norm(c.preferred_marriage_timeline)){score+=12;reasons.push("same marriage timeline");}
        if(norm(m.country) && norm(m.country)===norm(c.country)){score+=8;reasons.push("same country");}
        if(norm(m.city) && norm(m.city)===norm(c.city)){score+=5;reasons.push("same city");}
        if(overlap(JSON.stringify(m.preferences),JSON.stringify(c.preferences))){score+=8;reasons.push("overlapping partner preferences");}
        if(!dealConflict(JSON.stringify(m.deal_breakers),JSON.stringify(c.preferences)) &&
           !dealConflict(JSON.stringify(c.deal_breakers),JSON.stringify(m.preferences))){score+=7;reasons.push("no detected deal-breaker conflict");}
        score=Math.min(100,score);
        matches.push({
          userId:c.azure_subject||c.firebase_uid||null,displayName:c.display_name,age:c.age,gender:c.gender,
          country:c.country,city:c.show_city===false?null:c.city,marriageIntention:c.marriage_intention,
          marriageTimeline:viewerCaps.marriageTimeline?c.preferred_marriage_timeline:null,
          readinessScore:viewerCaps.advancedMatching?c.readiness_score:null,
          compatibilityScore:score,
          whyWeMatched:viewerCaps.whyWeMatched?reasons:[],
          advancedLocked:!viewerCaps.advancedMatching,
          premiumPriority:Boolean(c.premium_priority),
          isOnline:Boolean(c.is_online),
          lastSeenAt:c.last_seen_at||null,
          rankingScore:score+(c.premium_priority?3:0),
          photoId:c.show_photo_to_matches===true?c.photo_id:null,
          photoBlurred:!(c.show_photo_to_matches===true&&c.photo_id)
        });
      }
      matches.sort((a,b)=>(Number(b.isOnline)-Number(a.isOnline)) || b.rankingScore-a.rankingScore || b.compatibilityScore-a.compatibilityScore);
      return {status:200,jsonBody:{ok:true,count:matches.length,premium:viewerPremium.active,capabilities:viewerCaps,matches:matches.slice(0,50).map(({rankingScore,...m})=>m)}};
    }catch(error){
      context.error("MATCHES_FAILED",error);
      return {status:500,jsonBody:{ok:false,error:"MATCHES_FAILED"}};
    }
  })
});
