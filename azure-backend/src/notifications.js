const { app } = require("@azure/functions");
const { query, getPool } = require("./db");
const { requireAuth } = require("./auth");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function currentUser(authUser){
  const subject=text(authUser?.azure_subject,300);
  if(!subject){const e=new Error("AUTH_IDENTITY_REQUIRED");e.statusCode=401;throw e;}
  const r=await query("SELECT id,status FROM users WHERE azure_subject=$1 LIMIT 1",[subject]);
  if(!r.rows[0]){const e=new Error("ACCOUNT_NOT_FOUND");e.statusCode=404;throw e;}
  return r.rows[0];
}

async function createNotification(userId,type,title,body,data={}){
  const r=await query(
    `INSERT INTO notifications(user_id,type,title,body,data)
     VALUES($1,$2,$3,$4,$5::jsonb)
     RETURNING id,user_id,type,title,body,data,read_at,created_at`,
    [userId,text(type,60)||"general",text(title,140)||"Best Nikah Bridge",text(body,500),JSON.stringify(data||{})]
  );
  return r.rows[0];
}

async function scheduleNotification(userId,type,title,body,data,dueAt,dedupeKey){
  const due=new Date(dueAt);
  if(!Number.isFinite(due.getTime()))throw new Error("INVALID_NOTIFICATION_DUE_AT");
  const key=text(dedupeKey,300);
  if(!key)throw new Error("NOTIFICATION_DEDUPE_KEY_REQUIRED");
  const r=await query(
    `INSERT INTO notification_schedules(user_id,type,title,body,data,due_at,dedupe_key)
     VALUES($1,$2,$3,$4,$5::jsonb,$6,$7)
     ON CONFLICT(dedupe_key) DO UPDATE SET
       due_at=EXCLUDED.due_at,
       title=EXCLUDED.title,
       body=EXCLUDED.body,
       data=EXCLUDED.data
     WHERE notification_schedules.dispatched_at IS NULL
     RETURNING id,due_at,dispatched_at`,
    [userId,text(type,60),text(title,140),text(body,500),JSON.stringify(data||{}),due.toISOString(),key]
  );
  return r.rows[0]||null;
}

app.http("notificationList",{
  methods:["GET"],authLevel:"anonymous",route:"notifications",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const user=await currentUser(authUser);
      const r=await query(
        `SELECT id,type,title,body,data,read_at,created_at
           FROM notifications
          WHERE user_id=$1
          ORDER BY created_at DESC
          LIMIT 100`,[user.id]
      );
      const unread=await query("SELECT count(*)::int AS count FROM notifications WHERE user_id=$1 AND read_at IS NULL",[user.id]);
      return {status:200,jsonBody:{ok:true,unread:Number(unread.rows[0]?.count||0),notifications:r.rows}};
    }catch(e){
      context.error("NOTIFICATION_LIST_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NOTIFICATION_LIST_FAILED"}};
    }
  })
});

app.http("notificationRead",{
  methods:["PATCH"],authLevel:"anonymous",route:"notifications/{notificationId}/read",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const user=await currentUser(authUser);
      const id=(request.params&&request.params.notificationId)||context.triggerMetadata?.notificationId;
      const r=await query(
        "UPDATE notifications SET read_at=COALESCE(read_at,now()) WHERE id=$1 AND user_id=$2 RETURNING id,read_at",
        [id,user.id]
      );
      if(!r.rows[0])return {status:404,jsonBody:{ok:false,error:"NOTIFICATION_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true,notification:r.rows[0]}};
    }catch(e){
      context.error("NOTIFICATION_READ_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NOTIFICATION_READ_FAILED"}};
    }
  })
});

app.http("notificationReadAll",{
  methods:["PATCH"],authLevel:"anonymous",route:"notifications/read-all",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const user=await currentUser(authUser);
      const r=await query("UPDATE notifications SET read_at=COALESCE(read_at,now()) WHERE user_id=$1 AND read_at IS NULL",[user.id]);
      return {status:200,jsonBody:{ok:true,markedRead:r.rowCount}};
    }catch(e){
      context.error("NOTIFICATION_READ_ALL_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NOTIFICATION_READ_ALL_FAILED"}};
    }
  })
});

app.timer("notificationScheduleDispatch",{
  schedule:"0 */5 * * * *",
  handler:async(_timer,context)=>{
    const client=await getPool().connect();
    try{
      await client.query("BEGIN");
      const due=await client.query(
        `SELECT id,user_id,type,title,body,data
           FROM notification_schedules
          WHERE dispatched_at IS NULL AND due_at<=now()
          ORDER BY due_at ASC
          FOR UPDATE SKIP LOCKED
          LIMIT 100`
      );
      for(const row of due.rows){
        await client.query(
          `INSERT INTO notifications(user_id,type,title,body,data)
           VALUES($1,$2,$3,$4,$5::jsonb)`,
          [row.user_id,row.type,row.title,row.body,JSON.stringify(row.data||{})]
        );
        await client.query("UPDATE notification_schedules SET dispatched_at=now() WHERE id=$1",[row.id]);
      }
      await client.query("COMMIT");
      if(due.rowCount)context.log("AZURE_NOTIFICATION_SCHEDULE_DISPATCHED",due.rowCount);
    }catch(e){
      await client.query("ROLLBACK").catch(()=>{});
      context.error("AZURE_NOTIFICATION_SCHEDULE_FAILED",e);
      throw e;
    }finally{client.release();}
  }
});

module.exports={createNotification,scheduleNotification};
