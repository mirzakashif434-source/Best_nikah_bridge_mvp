const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

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
        SELECT u.id,p.*,pp.min_age,pp.max_age,pp.preferred_gender,pp.countries,pp.cities,
               pp.preferred_marriage_timeline,pp.deal_breakers,pp.preferences
        FROM users u JOIN profiles p ON p.user_id=u.id
        LEFT JOIN partner_preferences pp ON pp.user_id=u.id
        WHERE u.firebase_uid=$1 AND u.status='active'`,[user.uid]);
      if(!me.rows[0]||!me.rows[0].profile_completed||!me.rows[0].is_visible)
        return {status:409,jsonBody:{ok:false,error:"PROFILE_NOT_READY"}};

      const m=me.rows[0];
      const candidates=await query(`
        SELECT u.id,p.*,EXTRACT(YEAR FROM age(CURRENT_DATE,p.date_of_birth))::int AS age,pp.min_age,pp.max_age,pp.preferred_gender,pp.countries,pp.cities,
               pp.preferred_marriage_timeline,pp.deal_breakers,pp.preferences
        FROM users u JOIN profiles p ON p.user_id=u.id
        LEFT JOIN partner_preferences pp ON pp.user_id=u.id
        WHERE u.status='active' AND p.profile_completed=true AND p.is_visible=true
          AND u.id<>$1
        LIMIT 500`,[m.id]);

      const matches=[];
      for(const c of candidates.rows){
        const myMin=Number(m.min_age||18),myMax=Number(m.max_age||100);
        const theirMin=Number(c.min_age||18),theirMax=Number(c.max_age||100);
        const ageOk=Number(c.age||0)>=myMin&&Number(c.age||0)<=myMax;
        const reciprocalAge=Number(m.age||0)>=theirMin&&Number(m.age||0)<=theirMax;
        if(!ageOk||!reciprocalAge)continue;
        if(!compatible(m.preferred_gender||"",c.gender)||!compatible(c.preferred_gender||"",m.gender))continue;

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
          userId:c.firebase_uid||null,displayName:c.display_name,age:c.age,gender:c.gender,
          country:c.country,city:c.city,marriageIntention:c.marriage_intention,
          marriageTimeline:c.preferred_marriage_timeline,readinessScore:c.readiness_score,
          compatibilityScore:score,whyWeMatched:reasons
        });
      }
      matches.sort((a,b)=>b.compatibilityScore-a.compatibilityScore);
      return {status:200,jsonBody:{ok:true,count:matches.length,matches:matches.slice(0,50)}};
    }catch(error){
      context.error("MATCHES_FAILED",error);
      return {status:500,jsonBody:{ok:false,error:"MATCHES_FAILED"}};
    }
  })
});
