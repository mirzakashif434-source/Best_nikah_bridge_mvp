const { app } = require("@azure/functions");
const { getPool, query } = require("./db");
const { requireAuth } = require("./auth");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!user.uid||!email) throw new Error("AUTH_IDENTITY_REQUIRED");
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
      const receiverUid=text(b.receiverUserId,200);
      if(!receiverUid||receiverUid===user.uid) return {status:400,jsonBody:{ok:false,error:"INVALID_RECEIVER"}};
      const receiver=await query("SELECT id,status FROM users WHERE firebase_uid=$1",[receiverUid]);
      if(!receiver.rows[0]||receiver.rows[0].status!=="active") return {status:404,jsonBody:{ok:false,error:"RECEIVER_NOT_FOUND"}};
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
                u.firebase_uid AS user_id,p.display_name,p.gender,p.country,p.city
         FROM interests i
         JOIN users u ON u.id=CASE WHEN i.sender_user_id=$1 THEN i.receiver_user_id ELSE i.sender_user_id END
         LEFT JOIN profiles p ON p.user_id=u.id
         WHERE i.sender_user_id=$1 OR i.receiver_user_id=$1
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
        `SELECT i.*,s.firebase_uid AS sender_uid,r.firebase_uid AS receiver_uid
         FROM interests i JOIN users s ON s.id=i.sender_user_id JOIN users r ON r.id=i.receiver_user_id
         WHERE i.id=$1 FOR UPDATE`,[id]
      );
      if(!current.rows[0]){await client.query("ROLLBACK");return {status:404,jsonBody:{ok:false,error:"INTEREST_NOT_FOUND"}};}
      const i=current.rows[0];
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
        const reverse=await client.query(
          `SELECT id,status FROM interests
           WHERE sender_user_id=$1 AND receiver_user_id=$2
           FOR UPDATE`,[i.receiver_user_id,i.sender_user_id]
        );
        if(reverse.rows[0]?.status==="accepted"){
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
