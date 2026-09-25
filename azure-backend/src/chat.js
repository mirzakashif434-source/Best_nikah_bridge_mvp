const { app } = require("@azure/functions");
const { query, getPool } = require("./db");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!email) throw new Error("AUTH_IDENTITY_REQUIRED");

  // Azure users are resolved by the stable Azure External ID subject.
  // Firebase UID remains supported only for legacy users during the migration.
  if(user.auth_provider==="azure_external_id" && user.azure_subject){
    const azure=await query(
      "SELECT id,status FROM users WHERE azure_subject=$1 LIMIT 1",
      [String(user.azure_subject)]
    );
    if(!azure.rows[0]) throw new Error("AZURE_USER_NOT_FOUND");
    return azure.rows[0];
  }

  if(!user.uid) throw new Error("AUTH_IDENTITY_REQUIRED");
  const r=await query(
    "INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status",
    [user.uid,email,Boolean(user.email_verified)]
  );
  return r.rows[0];
}

async function getConversationForUser(conversationId,userId){
  const r=await query(
    "SELECT c.id,c.user_a_id,c.user_b_id,c.status,c.created_at FROM conversations c WHERE c.id=$1 AND (c.user_a_id=$2 OR c.user_b_id=$2) AND NOT EXISTS (SELECT 1 FROM blocked_users b WHERE (b.blocker_user_id=$2 AND b.blocked_user_id=CASE WHEN c.user_a_id=$2 THEN c.user_b_id ELSE c.user_a_id END) OR (b.blocked_user_id=$2 AND b.blocker_user_id=CASE WHEN c.user_a_id=$2 THEN c.user_b_id ELSE c.user_a_id END))",
    [conversationId,userId]);
  return r.rows[0];
}

app.http("conversationsList",{
  methods:["GET"],authLevel:"anonymous",route:"conversations",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const access=await accessForAuth(user);if(!access.capabilities.paid40Features)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};const me=access.user;
      const r=await query(
        `SELECT c.id,c.status,c.created_at,
                CASE WHEN c.user_a_id=$1 THEN COALESCE(ub.azure_subject,ub.firebase_uid) ELSE COALESCE(ua.azure_subject,ua.firebase_uid) END AS other_user_id,
                CASE WHEN c.user_a_id=$1 THEN pb.display_name ELSE pa.display_name END AS other_display_name,
                (SELECT m.body FROM messages m WHERE m.conversation_id=c.id ORDER BY m.created_at DESC LIMIT 1) AS last_message,
                (SELECT m.created_at FROM messages m WHERE m.conversation_id=c.id ORDER BY m.created_at DESC LIMIT 1) AS last_message_at
           FROM conversations c
           JOIN users ua ON ua.id=c.user_a_id
           JOIN users ub ON ub.id=c.user_b_id
           LEFT JOIN profiles pa ON pa.user_id=ua.id
           LEFT JOIN profiles pb ON pb.user_id=ub.id
          WHERE c.status='mutual'
            AND (c.user_a_id=$1 OR c.user_b_id=$1)
            AND NOT EXISTS (
              SELECT 1 FROM blocked_users b
               WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=CASE WHEN c.user_a_id=$1 THEN c.user_b_id ELSE c.user_a_id END)
                  OR (b.blocked_user_id=$1 AND b.blocker_user_id=CASE WHEN c.user_a_id=$1 THEN c.user_b_id ELSE c.user_a_id END)
            )
          ORDER BY COALESCE((SELECT m.created_at FROM messages m WHERE m.conversation_id=c.id ORDER BY m.created_at DESC LIMIT 1),c.created_at) DESC
          LIMIT 200`,
        [me.id]);
      return {status:200,jsonBody:{ok:true,conversations:r.rows}};
    }catch(e){context.error("CONVERSATIONS_LIST_FAILED",e);return {status:500,jsonBody:{ok:false,error:"CONVERSATIONS_LIST_FAILED"}};}
  })
});

app.http("messagesList",{
  methods:["GET"],authLevel:"anonymous",route:"conversations/{conversationId}/messages",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const access=await accessForAuth(user);if(!access.capabilities.paid40Features)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};const me=access.user;
      const id=(request.params&&request.params.conversationId)||context.triggerMetadata?.conversationId;
      const c=await getConversationForUser(id,me.id);
      if(!c)return {status:404,jsonBody:{ok:false,error:"CONVERSATION_NOT_FOUND"}};
      if(c.status!=="mutual")return {status:403,jsonBody:{ok:false,error:"CHAT_NOT_AVAILABLE"}};
      const r=await query(
        "SELECT m.id,m.conversation_id,COALESCE(u.azure_subject,u.firebase_uid) AS sender_user_id,m.body,m.created_at,m.read_at FROM messages m JOIN users u ON u.id=m.sender_user_id WHERE m.conversation_id=$1 ORDER BY m.created_at ASC LIMIT 500",
        [id]);
      return {status:200,jsonBody:{ok:true,messages:r.rows}};
    }catch(e){context.error("MESSAGES_LIST_FAILED",e);return {status:500,jsonBody:{ok:false,error:"MESSAGES_LIST_FAILED"}};}
  })
});

app.http("messageCreate",{
  methods:["POST"],authLevel:"anonymous",route:"conversations/{conversationId}/messages",
  handler:requireAuth(async(request,context,user)=>{
    const client=await getPool().connect();
    try{
      const access=await accessForAuth(user);if(!access.capabilities.paid40Features)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};const me=access.user;
      const id=(request.params&&request.params.conversationId)||context.triggerMetadata?.conversationId;
      const b=await request.json();
      const body=text(b.body,4000);
      if(!body)return {status:400,jsonBody:{ok:false,error:"MESSAGE_REQUIRED"}};

      await client.query("BEGIN");
      const conv=await client.query(
        `SELECT c.id,c.user_a_id,c.user_b_id,c.status,
                pa.gender AS user_a_gender,pb.gender AS user_b_gender
           FROM conversations c
           LEFT JOIN profiles pa ON pa.user_id=c.user_a_id
           LEFT JOIN profiles pb ON pb.user_id=c.user_b_id
          WHERE c.id=$1 AND (c.user_a_id=$2 OR c.user_b_id=$2)
          FOR UPDATE`,
        [id,me.id]
      );
      const row=conv.rows[0];
      if(!row){await client.query("ROLLBACK");return {status:404,jsonBody:{ok:false,error:"CONVERSATION_NOT_FOUND"}};}
      if(row.status!=="mutual"){await client.query("ROLLBACK");return {status:403,jsonBody:{ok:false,error:"CHAT_NOT_AVAILABLE"}};}

      const otherId=row.user_a_id===me.id?row.user_b_id:row.user_a_id;
      const blocked=await client.query(
        "SELECT 1 FROM blocked_users WHERE (blocker_user_id=$1 AND blocked_user_id=$2) OR (blocker_user_id=$2 AND blocked_user_id=$1) LIMIT 1",
        [me.id,otherId]
      );
      if(blocked.rows[0]){await client.query("ROLLBACK");return {status:403,jsonBody:{ok:false,error:"USER_BLOCKED"}};}

      const myGender=String(row.user_a_id===me.id?row.user_a_gender:row.user_b_gender||"").toLowerCase();
      const otherGender=String(row.user_a_id===me.id?row.user_b_gender:row.user_a_gender||"").toLowerCase();
      let waitingForReply=false,remainingBeforeReply=null;

      if(myGender==="male"&&otherGender==="female"){
        const femaleReply=await client.query(
          "SELECT 1 FROM messages WHERE conversation_id=$1 AND sender_user_id=$2 LIMIT 1",
          [id,otherId]
        );
        if(!femaleReply.rows[0]){
          const sent=await client.query(
            "SELECT count(*)::int AS count FROM messages WHERE conversation_id=$1 AND sender_user_id=$2",
            [id,me.id]
          );
          const used=Number(sent.rows[0]?.count||0);
          if(used>=2){
            await client.query("ROLLBACK");
            return {status:429,jsonBody:{
              ok:false,
              error:"WAIT_FOR_HER_REPLY",
              warning:"You have already sent 2 messages. Please wait for her reply before sending another.",
              sentBeforeReply:used,
              remainingBeforeReply:0
            }};
          }
          remainingBeforeReply=Math.max(0,1-used);
          waitingForReply=used+1>=2;
        }
      }

      const r=await client.query(
        "INSERT INTO messages(conversation_id,sender_user_id,body) VALUES($1,$2,$3) RETURNING id,conversation_id,sender_user_id,body,created_at,read_at",
        [id,me.id,body]
      );
      await client.query("COMMIT");
      return {status:201,jsonBody:{
        ok:true,
        message:r.rows[0],
        waitingForReply,
        remainingBeforeReply,
        warning:waitingForReply?"You have sent 2 messages. Please wait for her reply before sending another.":null
      }};
    }catch(e){
      await client.query("ROLLBACK").catch(()=>{});
      context.error("MESSAGE_CREATE_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"MESSAGE_CREATE_FAILED"}};
    }finally{client.release();}
  })
});

app.http("messagesRead",{
  methods:["PATCH"],authLevel:"anonymous",route:"conversations/{conversationId}/messages/read",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const access=await accessForAuth(user);if(!access.capabilities.paid40Features)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};const me=access.user;
      const id=(request.params&&request.params.conversationId)||context.triggerMetadata?.conversationId;
      const c=await getConversationForUser(id,me.id);
      if(!c)return {status:404,jsonBody:{ok:false,error:"CONVERSATION_NOT_FOUND"}};
      if(c.status!=="mutual")return {status:403,jsonBody:{ok:false,error:"CHAT_NOT_AVAILABLE"}};
      const r=await query(
        "UPDATE messages SET read_at=now() WHERE conversation_id=$1 AND sender_user_id<>$2 AND read_at IS NULL RETURNING id",
        [id,me.id]);
      return {status:200,jsonBody:{ok:true,markedRead:r.rowCount}};
    }catch(e){context.error("MESSAGES_READ_FAILED",e);return {status:500,jsonBody:{ok:false,error:"MESSAGES_READ_FAILED"}};}
  })
});


app.http("conversationDeleteForBoth",{
  methods:["DELETE"],authLevel:"anonymous",route:"conversations/{conversationId}",
  handler:requireAuth(async(request,context,user)=>{
    const client=await getPool().connect();
    try{
      const access=await accessForAuth(user);if(!access.capabilities.paid40Features)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};const me=access.user;
      const id=(request.params&&request.params.conversationId)||context.triggerMetadata?.conversationId;
      await client.query("BEGIN");
      const conv=await client.query(
        "SELECT id,user_a_id,user_b_id,status FROM conversations WHERE id=$1 AND (user_a_id=$2 OR user_b_id=$2) FOR UPDATE",
        [id,me.id]
      );
      if(!conv.rows[0]){
        await client.query("ROLLBACK");
        return {status:404,jsonBody:{ok:false,error:"CONVERSATION_NOT_FOUND"}};
      }
      const deleted=await client.query(
        "DELETE FROM conversations WHERE id=$1 RETURNING id,user_a_id,user_b_id",
        [id]
      );
      await client.query("COMMIT");
      return {status:200,jsonBody:{ok:true,deletedForBoth:true,conversationId:deleted.rows[0].id}};
    }catch(e){
      await client.query("ROLLBACK").catch(()=>{});
      context.error("CONVERSATION_DELETE_FOR_BOTH_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"CONVERSATION_DELETE_FOR_BOTH_FAILED"}};
    }finally{client.release();}
  })
});
