const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

async function requireAdmin(user) {
  const r = await query("SELECT id,role,status FROM users WHERE firebase_uid=$1 LIMIT 1",[user.uid]);
  if (!r.rows[0] || r.rows[0].status !== "active") {
    const e=new Error("USER_NOT_ACTIVE"); e.statusCode=403; throw e;
  }
  if (!["admin","moderator"].includes(r.rows[0].role)) {
    const e=new Error("MODERATION_ROLE_REQUIRED"); e.statusCode=403; throw e;
  }
  return r.rows[0];
}

const clean=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

app.http("moderationReportsQueue",{
  methods:["GET"],authLevel:"anonymous",route:"admin/moderation/reports",
  handler:requireAuth(async(request,context,user)=>{
    try{
      await requireAdmin(user);
      const status=clean(request.query.get("status"),30);
      const allowed=["open","reviewing","resolved","dismissed"];
      const values=allowed.includes(status)?[status]:[];
      const r=values.length
        ? await query("SELECT r.id,r.reason,r.details,r.status,r.created_at,r.resolved_at,ru.email AS reporter_email,pu.email AS reported_email FROM safety_reports r JOIN users ru ON ru.id=r.reporter_user_id LEFT JOIN users pu ON pu.id=r.reported_user_id WHERE r.status=$1 ORDER BY r.created_at ASC LIMIT 200",values)
        : await query("SELECT r.id,r.reason,r.details,r.status,r.created_at,r.resolved_at,ru.email AS reporter_email,pu.email AS reported_email FROM safety_reports r JOIN users ru ON ru.id=r.reporter_user_id LEFT JOIN users pu ON pu.id=r.reported_user_id WHERE r.status IN ('open','reviewing') ORDER BY r.created_at ASC LIMIT 200");
      return {status:200,jsonBody:{ok:true,reports:r.rows}};
    }catch(e){context.error("MODERATION_QUEUE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"MODERATION_QUEUE_FAILED"}};}
  })
});

app.http("moderationReportUpdate",{
  methods:["PATCH"],authLevel:"anonymous",route:"admin/moderation/reports/{reportId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      await requireAdmin(user);
      const body=await request.json();
      const status=clean(body.status,30);
      if(!["open","reviewing","resolved","dismissed"].includes(status)) return {status:400,jsonBody:{ok:false,error:"INVALID_MODERATION_STATUS"}};
      const r=await query("UPDATE safety_reports SET status=$2,resolved_at=CASE WHEN $2 IN ('resolved','dismissed') THEN now() ELSE NULL END WHERE id=$1 RETURNING id,status,resolved_at",[request.params.reportId,status]);
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"REPORT_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true,report:r.rows[0]}};
    }catch(e){context.error("MODERATION_REPORT_UPDATE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"MODERATION_REPORT_UPDATE_FAILED"}};}
  })
});

app.http("moderationUserSuspend",{
  methods:["POST"],authLevel:"anonymous",route:"admin/moderation/users/{userId}/suspend",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const admin=await requireAdmin(user);
      if(admin.id===request.params.userId) return {status:400,jsonBody:{ok:false,error:"CANNOT_SUSPEND_SELF"}};
      const r=await query("UPDATE users SET status='suspended',updated_at=now() WHERE id=$1 AND role<>'admin' RETURNING id,status",[request.params.userId]);
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND_OR_PROTECTED"}};
      await query("UPDATE profiles SET is_visible=false,updated_at=now() WHERE user_id=$1",[request.params.userId]);
      return {status:200,jsonBody:{ok:true,user:r.rows[0]}};
    }catch(e){context.error("MODERATION_USER_SUSPEND_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"MODERATION_USER_SUSPEND_FAILED"}};}
  })
});

app.http("moderationUserRestore",{
  methods:["POST"],authLevel:"anonymous",route:"admin/moderation/users/{userId}/restore",
  handler:requireAuth(async(request,context,user)=>{
    try{
      await requireAdmin(user);
      const r=await query("UPDATE users SET status='active',updated_at=now() WHERE id=$1 AND role<>'admin' RETURNING id,status",[request.params.userId]);
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND_OR_PROTECTED"}};
      return {status:200,jsonBody:{ok:true,user:r.rows[0]}};
    }catch(e){context.error("MODERATION_USER_RESTORE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"MODERATION_USER_RESTORE_FAILED"}};}
  })
});
