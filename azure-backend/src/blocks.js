const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!user.uid||!email){const e=new Error("AUTH_IDENTITY_REQUIRED");e.statusCode=401;throw e;}
  const r=await query(
    "INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status",
    [user.uid,email,Boolean(user.email_verified)]
  );
  if(r.rows[0].status!=="active"){const e=new Error("USER_NOT_ACTIVE");e.statusCode=403;throw e;}
  return r.rows[0];
}

async function resolveUser(identifier){
  const value=text(identifier,128);
  if(!value)return null;
  const r=await query("SELECT id,firebase_uid FROM users WHERE firebase_uid=$1 OR id::text=$1 LIMIT 1",[value]);
  return r.rows[0]||null;
}

app.http("blockUserCreate",{
  methods:["POST"],authLevel:"anonymous",route:"blocks",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const b=await request.json();
      const target=await resolveUser(b.userId||b.userUid);
      if(!target)return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND"}};
      if(target.id===me.id)return {status:400,jsonBody:{ok:false,error:"CANNOT_BLOCK_SELF"}};
      const r=await query(
        "INSERT INTO blocked_users(blocker_user_id,blocked_user_id) VALUES($1,$2) ON CONFLICT(blocker_user_id,blocked_user_id) DO NOTHING RETURNING id,blocked_user_id,created_at",
        [me.id,target.id]
      );
      return {status:201,jsonBody:{ok:true,blocked:r.rows[0]||{blocked_user_id:target.id}}};
    }catch(e){context.error("BLOCK_CREATE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"BLOCK_CREATE_FAILED"}};}
  })
});

app.http("blockUserList",{
  methods:["GET"],authLevel:"anonymous",route:"blocks",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const r=await query(
        "SELECT b.id,u.firebase_uid AS user_id,b.blocked_user_id,p.display_name,b.created_at FROM blocked_users b JOIN users u ON u.id=b.blocked_user_id LEFT JOIN profiles p ON p.user_id=u.id WHERE b.blocker_user_id=$1 ORDER BY b.created_at DESC LIMIT 500",
        [me.id]
      );
      return {status:200,jsonBody:{ok:true,blocks:r.rows}};
    }catch(e){context.error("BLOCK_LIST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"BLOCK_LIST_FAILED"}};}
  })
});

app.http("blockUserDelete",{
  methods:["DELETE"],authLevel:"anonymous",route:"blocks/{userId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const target=await resolveUser(request.params?.userId||context.triggerMetadata?.userId);
      if(!target)return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND"}};
      const r=await query("DELETE FROM blocked_users WHERE blocker_user_id=$1 AND blocked_user_id=$2 RETURNING id",[me.id,target.id]);
      if(!r.rowCount)return {status:404,jsonBody:{ok:false,error:"BLOCK_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true,unblockedUserId:target.firebase_uid}};
    }catch(e){context.error("BLOCK_DELETE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"BLOCK_DELETE_FAILED"}};}
  })
});
