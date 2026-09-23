const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const cleanKey=(v)=>typeof v==="string"?v.trim().toLowerCase().replace(/[^a-z0-9_.-]/g,"").slice(0,80):"";

async function me(user){
  const key=user.azure_subject||user.uid;
  const r=await query("SELECT id,status FROM users WHERE azure_subject=$1 OR firebase_uid=$1 LIMIT 1",[key]);
  if(!r.rows[0]){const e=new Error("USER_NOT_FOUND");e.statusCode=404;throw e;}
  if(r.rows[0].status!=="active"){const e=new Error("USER_NOT_ACTIVE");e.statusCode=403;throw e;}
  return r.rows[0];
}

app.http("userSettingGet",{
  methods:["GET"],authLevel:"anonymous",route:"settings/{key}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const u=await me(user),key=cleanKey(request.params?.key||context.triggerMetadata?.key);
      if(!key)return {status:400,jsonBody:{ok:false,error:"SETTING_KEY_REQUIRED"}};
      const r=await query("SELECT setting_value,updated_at FROM user_settings WHERE user_id=$1 AND setting_key=$2",[u.id,key]);
      return {status:200,jsonBody:{ok:true,key,value:r.rows[0]?.setting_value||{},updatedAt:r.rows[0]?.updated_at||null}};
    }catch(e){context.error("SETTING_GET_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"SETTING_GET_FAILED"}};}
  })
});

app.http("userSettingPut",{
  methods:["PUT"],authLevel:"anonymous",route:"settings/{key}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const u=await me(user),key=cleanKey(request.params?.key||context.triggerMetadata?.key);
      if(!key)return {status:400,jsonBody:{ok:false,error:"SETTING_KEY_REQUIRED"}};
      const body=await request.json();
      if(!body || typeof body!=="object" || Array.isArray(body))return {status:400,jsonBody:{ok:false,error:"SETTING_VALUE_OBJECT_REQUIRED"}};
      const encoded=JSON.stringify(body);
      if(encoded.length>30000)return {status:413,jsonBody:{ok:false,error:"SETTING_VALUE_TOO_LARGE"}};
      const r=await query(
        "INSERT INTO user_settings(user_id,setting_key,setting_value) VALUES($1,$2,$3::jsonb) ON CONFLICT(user_id,setting_key) DO UPDATE SET setting_value=EXCLUDED.setting_value,updated_at=now() RETURNING setting_value,updated_at",
        [u.id,key,encoded]
      );
      return {status:200,jsonBody:{ok:true,key,value:r.rows[0].setting_value,updatedAt:r.rows[0].updated_at}};
    }catch(e){context.error("SETTING_PUT_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"SETTING_PUT_FAILED"}};}
  })
});

app.http("successMentorsList",{
  methods:["GET"],authLevel:"anonymous",route:"success-network/mentors",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const u=await me(user);
      const r=await query(
        `SELECT us.user_id,p.display_name,us.updated_at
         FROM user_settings us
         LEFT JOIN profiles p ON p.user_id=us.user_id
         WHERE us.setting_key='nikah_success_network'
           AND COALESCE((us.setting_value->>'nikahCompleted')::boolean,false)=true
           AND COALESCE((us.setting_value->>'mentorOptIn')::boolean,false)=true
           AND us.user_id<>$1
         ORDER BY us.updated_at DESC LIMIT 20`,[u.id]
      );
      return {status:200,jsonBody:{ok:true,mentors:r.rows.map(x=>({userId:x.user_id,displayName:x.display_name||"Peer mentor",updatedAt:x.updated_at}))}};
    }catch(e){context.error("SUCCESS_MENTORS_LIST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"SUCCESS_MENTORS_LIST_FAILED"}};}
  })
});

app.http("successMentorRequest",{
  methods:["POST"],authLevel:"anonymous",route:"success-network/mentor-requests",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const u=await me(user),body=await request.json(),mentorId=String(body?.mentorUserId||"").trim();
      if(!mentorId)return {status:400,jsonBody:{ok:false,error:"MENTOR_USER_ID_REQUIRED"}};
      const mentor=await query(
        `SELECT us.user_id FROM user_settings us
         WHERE us.user_id::text=$1 AND us.setting_key='nikah_success_network'
           AND COALESCE((us.setting_value->>'nikahCompleted')::boolean,false)=true
           AND COALESCE((us.setting_value->>'mentorOptIn')::boolean,false)=true`,[mentorId]
      );
      if(!mentor.rows[0])return {status:404,jsonBody:{ok:false,error:"MENTOR_NOT_AVAILABLE"}};
      const key="mentor_request_"+mentorId.replace(/[^a-zA-Z0-9-]/g,"").slice(0,40);
      const value=JSON.stringify({mentorUserId:mentorId,status:"pending",requestedAt:new Date().toISOString()});
      await query("INSERT INTO user_settings(user_id,setting_key,setting_value) VALUES($1,$2,$3::jsonb) ON CONFLICT(user_id,setting_key) DO UPDATE SET setting_value=EXCLUDED.setting_value,updated_at=now()",[u.id,key,value]);
      return {status:201,jsonBody:{ok:true,status:"pending"}};
    }catch(e){context.error("SUCCESS_MENTOR_REQUEST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"SUCCESS_MENTOR_REQUEST_FAILED"}};}
  })
});
