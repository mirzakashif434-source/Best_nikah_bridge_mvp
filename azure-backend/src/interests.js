const { app } = require("@azure/functions");
const { getPool, query } = require("./db");
const { requireAuth } = require("./auth");
const { entitlementForUser } = require("./premiumAccess");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!user.uid||!email) throw new Error("AUTH_IDENTITY_REQUIRED");
  const azureSubject=typeof user.azure_subject==="string"&&user.azure_subject.trim()?user.azure_subject.trim():null;
  if(azureSubject){
    const existing=await query("SELECT id,email,status FROM users WHERE azure_subject=$1 OR firebase_uid=$2 LIMIT 1",[azureSubject,user.uid]);
    if(existing.rows[0]) return existing.rows[0];
    const created=await query("INSERT INTO users(firebase_uid,azure_subject,email,email_verified_at) VALUES($1,$2,$3,CASE WHEN $4 THEN now() ELSE NULL END) RETURNING id,email,status",[user.uid,azureSubject,email,Boolean(user.email_verified)]);
    return created.rows[0];
  }
  const r=await query(
    `INSERT INTO users(firebase_uid,email,email_verified_at)
     VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END)
     ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,
       email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now()
     RETURNING id,email,status`,
    [user.uid,email,Boolean(user.email_verified)]
  );
  return r.rows[0];
}

app.http("interestCreate",{
  methods:["POST"],authLevel:"anonymous",route:"interests",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      if(me.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      const b=await request.json();
      const receiverUid=text(b.receiverUserId,300);
      const myIdentity=(user.azure_subject||user.uid||"").trim();
      if(!receiverUid||receiverUid===myIdentity||receiverUid===user.uid) return {status:400,jsonBody:{ok:false,error:"INVALID_RECEIVER"}};
      const receiver=await query("SELECT id,status FROM users WHERE azure_subject=$1 OR firebase_uid=$1 LIMIT 1",[receiverUid]);
      if(!receiver.rows[0]||receiver.rows[0].status!=="active") return {status:404,jsonBody:{ok:false,error:"RECEIVER_NOT_FOUND"}};
      const blocked=await query("SELECT 1 FROM blocked_users WHERE (blocker_user_id=$1 AND blocked_user_id=$2) OR (blocker_user_id=$2 AND blocked_user_id=$1) LIMIT 1",[me.id,receiver.rows[0].id]);
      if(blocked.rows[0]) return {status:403,jsonBody:{ok:false,error:"USER_BLOCKED"}};

      const existingInterest=await query(
        "SELECT id,status,created_at FROM interests WHERE sender_user_id=$1 AND receiver_user_id=$2 LIMIT 1",
        [me.id,receiver.rows[0].id]
      );
      const premium=await entitlementForUser(me.id);
      if(!premium.active && !existingInterest.rows[0]){
        const today=await query(
          "SELECT count(*)::int AS count FROM interests WHERE sender_user_id=$1 AND created_at::date=(now() AT TIME ZONE 'UTC')::date",
          [me.id]
        );
        const used=Number(today.rows[0]?.count||0),limit=3;
        if(used>=limit) return {status:402,jsonBody:{ok:false,error:"FREE_DAILY_INTEREST_LIMIT",premiumRequired:true,limit,used,remaining:0}};
      }
      const r=await query(
        `INSERT INTO interests(sender_user_id,receiver_user_id,status)
         VALUES($1,$2,'pending')
         ON CONFLICT(sender_user_id,receiver_user_id) DO UPDATE SET
           status=CASE WHEN interests.status='declined' THEN 'pending' ELSE interests.status END,
           responded_at=CASE WHEN interests.status='declined' THEN NULL ELSE interests.responded_at END
         RETURNING id,status,created_at`,
        [me.id,receiver.rows[0].id]
      );
      return {status:200,jsonBody:{ok:true,interest:r.rows[0]}};
    }catch(e){context.error("INTEREST_CREATE_FAILED",e);return {status:500,jsonBody:{ok:false,error:"INTEREST_CREATE_FAILED"}};}
  })
});

app.http("interestList",{
  methods:["GET"],authLevel:"anonymous",route:"interests",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const r=await query(
        `SELECT i.id,i.status,i.created_at,i.responded_at,
                CASE WHEN i.sender_user_id=$1 THEN 'sent' ELSE 'received' END AS direction,
                COALESCE(u.azure_subject,u.firebase_uid) AS user_id,p.display_name,p.gender,p.country,p.city
         FROM interests i
         JOIN users u ON u.id=CASE WHEN i.sender_user_id=$1 THEN i.receiver_user_id ELSE i.sender_user_id END
         LEFT JOIN profiles p ON p.user_id=u.id
         WHERE (i.sender_user_id=$1 OR i.receiver_user_id=$1)
           AND NOT EXISTS (
             SELECT 1 FROM blocked_users b
             WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=CASE WHEN i.sender_user_id=$1 THEN i.receiver_user_id ELSE i.sender_user_id END)
                OR (b.blocked_user_id=$1 AND b.blocker_user_id=CASE WHEN i.sender_user_id=$1 THEN i.receiver_user_id ELSE i.sender_user_id END)
           )
         ORDER BY i.created_at DESC LIMIT 200`,[me.id]
      );
      return {status:200,jsonBody:{ok:true,interests:r.rows}};
    }catch(e){context.error("INTEREST_LIST_FAILED",e);return {status:500,jsonBody:{ok:false,error:"INTEREST_LIST_FAILED"}};}
  })
});

app.http("interestRespond",{
  methods:["PATCH"],authLevel:"anonymous",route:"interests/{interestId}",
  handler:requireAuth(async(request,context,user)=>{
    const client=await getPool().connect();
    try{
      const me=await ensureUser(user);
      const id=context.triggerMetadata?.interestId;
      const b=await request.json();
      const status=text(b.status,20).toLowerCase();
      if(!["accepted","declined","cancelled"].includes(status)) return {status:400,jsonBody:{ok:false,error:"INVALID_STATUS"}};
      await client.query("BEGIN");
      const current=await client.query(
        `SELECT i.*,COALESCE(s.azure_subject,s.firebase_uid) AS sender_uid,COALESCE(r.azure_subject,r.firebase_uid) AS receiver_uid
         FROM interests i JOIN users s ON s.id=i.sender_user_id JOIN users r ON r.id=i.receiver_user_id
         WHERE i.id=$1 FOR UPDATE`,[id]
      );
      if(!current.rows[0]){await client.query("ROLLBACK");return {status:404,jsonBody:{ok:false,error:"INTEREST_NOT_FOUND"}};}
      const i=current.rows[0];
      const blocked=await client.query("SELECT 1 FROM blocked_users WHERE (blocker_user_id=$1 AND blocked_user_id=$2) OR (blocker_user_id=$2 AND blocked_user_id=$1) LIMIT 1",[me.id,i.sender_user_id===me.id?i.receiver_user_id:i.sender_user_id]);
      if(blocked.rows[0]){await client.query("ROLLBACK");return {status:403,jsonBody:{ok:false,error:"USER_BLOCKED"}};}
      if(status==="cancelled"){
        if(i.sender_user_id!==me.id){await client.query("ROLLBACK");return {status:403,jsonBody:{ok:false,error:"ONLY_SENDER_CAN_CANCEL"}};}
      }else if(i.receiver_user_id!==me.id){
        await client.query("ROLLBACK");return {status:403,jsonBody:{ok:false,error:"ONLY_RECEIVER_CAN_RESPOND"}};
      }
      if(i.status!=="pending"){await client.query("ROLLBACK");return {status:409,jsonBody:{ok:false,error:"INTEREST_ALREADY_RESPONDED"}};}
      const updated=await client.query(
        `UPDATE interests SET status=$1,responded_at=now() WHERE id=$2 RETURNING id,status,responded_at`,
        [status,id]
      );
      let mutual=false,conversationId=null;
      if(status==="accepted"){
        // Accepting a pending interest is the mutual-consent event.
        // Create (or reuse) the canonical mutual conversation immediately;
        // a second reverse interest is not required.
        const conv=await client.query(
          `INSERT INTO conversations(user_a_id,user_b_id,status)
           VALUES($1,$2,'mutual')
           ON CONFLICT DO NOTHING
           RETURNING id`,[i.sender_user_id,i.receiver_user_id]
        );
        if(conv.rows[0]) conversationId=conv.rows[0].id;
        else {
          const existing=await client.query(
            `SELECT id FROM conversations
             WHERE LEAST(user_a_id,user_b_id)=LEAST($1::uuid,$2::uuid)
               AND GREATEST(user_a_id,user_b_id)=GREATEST($1::uuid,$2::uuid)
             LIMIT 1`,[i.sender_user_id,i.receiver_user_id]
          );
          conversationId=existing.rows[0]?.id||null;
        }
        mutual=Boolean(conversationId);
      }
      await client.query("COMMIT");
      return {status:200,jsonBody:{ok:true,interest:updated.rows[0],mutual,conversationId}};
    }catch(e){
      await client.query("ROLLBACK").catch(()=>{});
      context.error("INTEREST_RESPOND_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"INTEREST_RESPOND_FAILED"}};
    }finally{client.release();}
  })
});
