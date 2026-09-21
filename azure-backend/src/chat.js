const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

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
      const me=await ensureUser(user);
      const r=await query(
        "SELECT c.id,c.status,c.created_at,CASE WHEN c.user_a_id=$1 THEN COALESCE(ub.azure_subject,ub.firebase_uid) ELSE COALESCE(ua.azure_subject,ua.firebase_uid) END AS other_user_id,CASE WHEN c.user_a_id=$1 THEN pb.display_name ELSE pa.display_name END AS other_display_name,(SELECT m.body FROM messages m WHERE m.conversation_id=c.id ORDER BY m.created_at DESC LIMIT 1) AS last_message,(SELECT m.created_at FROM messages m WHERE m.conversation_id=c.id ORDER BY m.created_at DESC LIMIT 1) AS last_message_at FROM conversations c JOIN users ua ON ua.id=c.user_a_id JOIN users ub ON ub.id=c.user_b_id LEFT JOIN profiles pa ON pa.user_id=ua.id LEFT JOIN profiles pb ON pb.user_id=ub.id WHERE c.status='mutual' AND (c.user_a_id=$1 OR c.user_b_id=$1) ORDER BY COALESCE((SELECT m.created_at FROM messages m WHERE m.conversation_id=c.id ORDER BY m.created_at DESC LIMIT 1),c.created_at) DESC LIMIT 200",
        [me.id]);
      return {status:200,jsonBody:{ok:true,conversations:r.rows}};
    }catch(e){context.error("CONVERSATIONS_LIST_FAILED",e);return {status:500,jsonBody:{ok:false,error:"CONVERSATIONS_LIST_FAILED"}};}
  })
});

app.http("messagesList",{
  methods:["GET"],authLevel:"anonymous",route:"conversations/{conversationId}/messages",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
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
    try{
      const me=await ensureUser(user);
      const id=(request.params&&request.params.conversationId)||context.triggerMetadata?.conversationId;
      const c=await getConversationForUser(id,me.id);
      if(!c)return {status:404,jsonBody:{ok:false,error:"CONVERSATION_NOT_FOUND"}};
      if(c.status!=="mutual")return {status:403,jsonBody:{ok:false,error:"CHAT_NOT_AVAILABLE"}};
      const b=await request.json();
      const body=text(b.body,4000);
      if(!body)return {status:400,jsonBody:{ok:false,error:"MESSAGE_REQUIRED"}};
      const r=await query(
        "INSERT INTO messages(conversation_id,sender_user_id,body) VALUES($1,$2,$3) RETURNING id,conversation_id,sender_user_id,body,created_at,read_at",
        [id,me.id,body]);
      return {status:201,jsonBody:{ok:true,message:r.rows[0]}};
    }catch(e){context.error("MESSAGE_CREATE_FAILED",e);return {status:500,jsonBody:{ok:false,error:"MESSAGE_CREATE_FAILED"}};}
  })
});

app.http("messagesRead",{
  methods:["PATCH"],authLevel:"anonymous",route:"conversations/{conversationId}/messages/read",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
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
