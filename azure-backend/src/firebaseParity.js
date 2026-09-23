const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

async function me(user){
  const key=user.azure_subject||user.uid;
  const r=await query("SELECT id,status FROM users WHERE azure_subject=$1 OR firebase_uid=$1 LIMIT 1",[key]);
  if(!r.rows[0]) throw Object.assign(new Error("USER_NOT_FOUND"),{statusCode:404});
  if(r.rows[0].status!=="active") throw Object.assign(new Error("USER_NOT_ACTIVE"),{statusCode:403});
  return r.rows[0];
}
function h(fn){return requireAuth(async(req,ctx,user)=>{try{return await fn(req,ctx,user)}catch(e){ctx.error("AZURE_PARITY_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"AZURE_PARITY_FAILED"}}}})}
app.http("sendLikeAzure",{methods:["POST"],authLevel:"anonymous",route:"likes",handler:h(async(req,ctx,user)=>{
 const u=await me(user),b=await req.json(),to=String(b?.toUid||""); if(!to||to===String(u.id))return {status:400,jsonBody:{ok:false,error:"INVALID_LIKE_TARGET"}};
 const t=await query("SELECT id FROM users WHERE (id::text=$1 OR azure_subject=$1 OR firebase_uid=$1) AND status='active' LIMIT 1",[to]); if(!t.rows[0])return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND"}};
 const day=new Date().toISOString().slice(0,10),c=await query("SELECT count FROM daily_likes WHERE user_id=$1 AND day=$2",[u.id,day]); if(Number(c.rows[0]?.count||0)>=20)return {status:429,jsonBody:{ok:false,error:"DAILY_LIKE_LIMIT_REACHED"}};
 await query("INSERT INTO likes(from_user_id,to_user_id,day) VALUES($1,$2,$3) ON CONFLICT(from_user_id,to_user_id) DO UPDATE SET active=true,day=EXCLUDED.day",[u.id,t.rows[0].id,day]);
 await query("INSERT INTO daily_likes(user_id,day,count) VALUES($1,$2,1) ON CONFLICT(user_id,day) DO UPDATE SET count=daily_likes.count+1,updated_at=now()",[u.id,day]);
 return {status:200,jsonBody:{ok:true,sent:true,remaining:19-Number(c.rows[0]?.count||0)}};
})});
app.http("claimFreeBoostAzure",{methods:["POST"],authLevel:"anonymous",route:"entitlements/free-boost",handler:h(async(req,ctx,user)=>{
 const u=await me(user),r=await query("SELECT claimed_at FROM free_boost_claims WHERE user_id=$1",[u.id]); if(r.rows[0]&&Date.now()-new Date(r.rows[0].claimed_at).getTime()<8*86400000)return {status:409,jsonBody:{ok:false,error:"FREE_BOOST_NOT_AVAILABLE"}};
 await query("INSERT INTO free_boost_claims(user_id,claimed_at) VALUES($1,now()) ON CONFLICT(user_id) DO UPDATE SET claimed_at=now()",[u.id]); return {status:200,jsonBody:{ok:true,claimed:true}};
})});
app.http("setNikahPromiseAzure",{methods:["POST"],authLevel:"anonymous",route:"connections/{connectionId}/nikah-promise",handler:h(async(req,ctx,user)=>{
 const u=await me(user),id=String(req.params.connectionId||""),b=await req.json();if(!id||!b?.stage)return {status:400,jsonBody:{ok:false,error:"INVALID_NIKAH_PROMISE"}};
 await query("INSERT INTO nikah_promise_paths(connection_id,stage,target_date,notes,updated_by) VALUES($1,$2,$3,$4,$5) ON CONFLICT(connection_id) DO UPDATE SET stage=EXCLUDED.stage,target_date=EXCLUDED.target_date,notes=EXCLUDED.notes,updated_by=EXCLUDED.updated_by,updated_at=now()",[id,String(b.stage).slice(0,80),String(b.targetDate||"").slice(0,100),String(b.notes||"").slice(0,2000),u.id]);return {status:200,jsonBody:{ok:true,saved:true}};
})});
app.http("livingCompatibilityAzure",{methods:["POST"],authLevel:"anonymous",route:"compatibility/living",handler:h(async(req,ctx,user)=>{
 const u=await me(user),b=await req.json(),v=k=>String(b?.[k]||"").slice(0,500);
 await query("INSERT INTO living_compatibility(user_id,country,city,marriage_timeline,family_involvement,children_expectation,career_plan,living_plan) VALUES($1,$2,$3,$4,$5,$6,$7,$8) ON CONFLICT(user_id) DO UPDATE SET country=EXCLUDED.country,city=EXCLUDED.city,marriage_timeline=EXCLUDED.marriage_timeline,family_involvement=EXCLUDED.family_involvement,children_expectation=EXCLUDED.children_expectation,career_plan=EXCLUDED.career_plan,living_plan=EXCLUDED.living_plan,updated_at=now()",[u.id,v("country"),v("city"),v("marriageTimeline"),v("familyInvolvement"),v("childrenExpectation"),v("careerPlan"),v("livingPlan")]);return {status:200,jsonBody:{ok:true,saved:true}};
})});
app.http("livingChangeAzure",{methods:["POST"],authLevel:"anonymous",route:"compatibility/living/share",handler:h(async(req,ctx,user)=>{
 const u=await me(user),b=await req.json(),r=await query("INSERT INTO living_change_alerts(from_user_id,to_user_id,field,value) SELECT $1,id,$3,$4 FROM users WHERE id::text=$2 OR azure_subject=$2 OR firebase_uid=$2 RETURNING id",[u.id,String(b.toUid||""),String(b.field||"").slice(0,80),String(b.value||"").slice(0,500)]);if(!r.rows[0])return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND"}};return {status:201,jsonBody:{ok:true,shared:true,alertId:r.rows[0].id}};
})});
app.http("healthCheckAzure",{methods:["POST"],authLevel:"anonymous",route:"connections/{connectionId}/health-check",handler:h(async(req,ctx,user)=>{
 const u=await me(user),b=await req.json(),id=String(req.params.connectionId||"");await query("INSERT INTO connection_health_checks(connection_id,user_id,communication,family_progress,unresolved_differences,timeline_aligned) VALUES($1,$2,$3,$4,$5,$6) ON CONFLICT(connection_id,user_id) DO UPDATE SET communication=EXCLUDED.communication,family_progress=EXCLUDED.family_progress,unresolved_differences=EXCLUDED.unresolved_differences,timeline_aligned=EXCLUDED.timeline_aligned,updated_at=now()",[id,u.id,String(b.communication||"").slice(0,500),String(b.familyProgress||"").slice(0,500),String(b.unresolvedDifferences||"").slice(0,500),String(b.timelineAligned||"").slice(0,500)]);return {status:200,jsonBody:{ok:true,saved:true}};
})});

app.http("livingCompatibilityGetAzure",{methods:["GET"],authLevel:"anonymous",route:"compatibility/living",handler:h(async(req,ctx,user)=>{
 const u=await me(user);
 const r=await query("SELECT country,city,marriage_timeline,family_involvement,children_expectation,career_plan,living_plan,updated_at FROM living_compatibility WHERE user_id=$1",[u.id]);
 return {status:200,jsonBody:{ok:true,living:r.rows[0]||{}}};
})});
