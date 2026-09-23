const { app } = require("@azure/functions");
const crypto = require("crypto");
const { query, getPool } = require("./db");
const { requireAuth } = require("./auth");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";
const ROLE_SET=new Set(["wali","parent","sibling","family","trusted"]);
const hashToken=(v)=>crypto.createHash("sha256").update(v,"utf8").digest("hex");

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!email) throw Object.assign(new Error("AUTH_IDENTITY_REQUIRED"),{statusCode:401});
  if(user.auth_provider==="azure_external_id" && user.azure_subject){
    const r=await query("SELECT id,status,email,azure_subject,firebase_uid FROM users WHERE azure_subject=$1 LIMIT 1",[String(user.azure_subject)]);
    if(!r.rows[0]) throw Object.assign(new Error("AZURE_USER_NOT_FOUND"),{statusCode:404});
    return r.rows[0];
  }
  if(!user.uid) throw Object.assign(new Error("AUTH_IDENTITY_REQUIRED"),{statusCode:401});
  const r=await query(
    "INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status,email,azure_subject,firebase_uid",
    [user.uid,email,Boolean(user.email_verified)]
  );
  return r.rows[0];
}

async function circleForUser(userId){
  const r=await query(
    `SELECT c.id,c.owner_user_id,c.name,c.status,c.created_at,
            m.role,m.can_suggest_matches,m.can_view_progress
     FROM family_circles c
     JOIN family_circle_members m ON m.circle_id=c.id AND m.user_id=$1 AND m.removed_at IS NULL
     WHERE c.status='active'
     ORDER BY c.created_at DESC LIMIT 1`,
    [userId]
  );
  return r.rows[0]||null;
}

async function requireCircleMember(circleId,userId){
  const r=await query(
    `SELECT c.id,c.owner_user_id,c.status,m.role,m.can_suggest_matches,m.can_view_progress
     FROM family_circles c
     JOIN family_circle_members m ON m.circle_id=c.id
     WHERE c.id=$1 AND m.user_id=$2 AND m.removed_at IS NULL AND c.status='active' LIMIT 1`,
    [circleId,userId]
  );
  if(!r.rows[0]) throw Object.assign(new Error("CIRCLE_ACCESS_DENIED"),{statusCode:403});
  return r.rows[0];
}

app.http("familyCircleGet",{
  methods:["GET"],authLevel:"anonymous",route:"family-circle",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      let circle=await circleForUser(me.id);
      if(!circle){
        const client=await getPool().connect();
        try{
          await client.query("BEGIN");
          const created=await client.query(
            "INSERT INTO family_circles(owner_user_id,name) VALUES($1,'My Nikah Circle') RETURNING id,owner_user_id,name,status,created_at",
            [me.id]
          );
          circle=created.rows[0];
          await client.query(
            "INSERT INTO family_circle_members(circle_id,user_id,role,can_suggest_matches,can_view_progress) VALUES($1,$2,'owner',true,true) ON CONFLICT(circle_id,user_id) DO NOTHING",
            [circle.id,me.id]
          );
          await client.query("COMMIT");
          circle={...circle,role:"owner",can_suggest_matches:true,can_view_progress:true};
        }catch(e){await client.query("ROLLBACK").catch(()=>{});throw e;}finally{client.release();}
      }
      const members=await query(
        `SELECT m.id,m.role,m.can_suggest_matches,m.can_view_progress,m.joined_at,
                COALESCE(p.display_name,u.email,'Family member') AS display_name,
                COALESCE(u.azure_subject,u.firebase_uid) AS user_identity
         FROM family_circle_members m
         JOIN users u ON u.id=m.user_id
         LEFT JOIN profiles p ON p.user_id=u.id
         WHERE m.circle_id=$1 AND m.removed_at IS NULL ORDER BY m.joined_at ASC`,
        [circle.id]
      );
      return {status:200,jsonBody:{ok:true,circle,members:members.rows}};
    }catch(e){context.error("FAMILY_CIRCLE_GET_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_GET_FAILED"}};}
  })
});

app.http("familyCircleInviteCreate",{
  methods:["POST"],authLevel:"anonymous",route:"family-circle/invites",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const circle=await circleForUser(me.id);
      if(!circle) return {status:409,jsonBody:{ok:false,error:"CIRCLE_NOT_READY"}};
      const membership=await requireCircleMember(circle.id,me.id);
      if(!["owner","wali","parent","sibling","family","trusted"].includes(membership.role))
        return {status:403,jsonBody:{ok:false,error:"CIRCLE_INVITE_NOT_ALLOWED"}};

      const active=await query(
        "SELECT count(*)::int AS count FROM family_circle_invites WHERE circle_id=$1 AND revoked_at IS NULL AND expires_at>now() AND use_count<max_uses",
        [circle.id]
      );
      if(Number(active.rows[0]?.count||0)>=10) return {status:429,jsonBody:{ok:false,error:"ACTIVE_INVITE_LIMIT_REACHED"}};

      const b=await request.json().catch(()=>({}));
      const role=text(b?.role,20).toLowerCase()||"family";
      if(!ROLE_SET.has(role)) return {status:400,jsonBody:{ok:false,error:"INVALID_CIRCLE_ROLE"}};
      const maxUses=Math.max(1,Math.min(20,Number(b?.maxUses)||5));
      const expiresDays=Math.max(1,Math.min(30,Number(b?.expiresDays)||7));

      const raw=crypto.randomBytes(24).toString("base64url");
      const tokenHash=hashToken(raw);
      const r=await query(
        `INSERT INTO family_circle_invites(circle_id,invited_by_user_id,token_hash,role,max_uses,expires_at)
         VALUES($1,$2,$3,$4,$5,now()+($6::text||' days')::interval)
         RETURNING id,role,max_uses,use_count,expires_at,created_at`,
        [circle.id,me.id,tokenHash,role,maxUses,String(expiresDays)]
      );
      const deepLink="bestnikahbredge://circle/join?token="+encodeURIComponent(raw);
      const origin=new URL(request.url).origin;
      const shareUrl=origin+"/api/family-circle/invite/"+encodeURIComponent(raw);
      return {status:201,jsonBody:{ok:true,invite:r.rows[0],deepLink,shareUrl}};
    }catch(e){context.error("FAMILY_CIRCLE_INVITE_CREATE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_INVITE_CREATE_FAILED"}};}
  })
});


app.http("familyCircleInviteLanding",{
  methods:["GET"],authLevel:"anonymous",route:"family-circle/invite/{token}",
  handler:async(request,context)=>{
    try{
      const token=text((request.params&&request.params.token)||context.triggerMetadata?.token,200);
      if(!token) return {status:400,headers:{"Content-Type":"text/plain; charset=utf-8"},body:"Invalid Family Circle invite."};
      const r=await query(
        `SELECT i.role,i.max_uses,i.use_count,i.expires_at,i.revoked_at,c.status
         FROM family_circle_invites i JOIN family_circles c ON c.id=i.circle_id
         WHERE i.token_hash=$1 LIMIT 1`,
        [hashToken(token)]
      );
      const i=r.rows[0];
      if(!i||i.status!=="active"||i.revoked_at||new Date(i.expires_at)<=new Date()||i.use_count>=i.max_uses)
        return {status:410,headers:{"Content-Type":"text/html; charset=utf-8","Cache-Control":"no-store"},body:"<!doctype html><html><body><h2>This Family Circle invite is no longer available.</h2></body></html>"};

      const encoded=encodeURIComponent(token);
      const openApp="bestnikahbredge://circle/join?token="+encoded;
      const play="https://play.google.com/store/apps/details?id=com.nikahbridge";
      const html=`<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Best Nikah Family Circle</title>
      <style>body{font-family:system-ui,sans-serif;background:#faf7ef;color:#1e3a30;margin:0;padding:24px}.card{max-width:560px;margin:40px auto;background:white;border-radius:24px;padding:28px;box-shadow:0 8px 30px #00000012}.btn{display:block;text-align:center;text-decoration:none;padding:16px;margin:14px 0;border-radius:14px;font-weight:700}.open{background:#126752;color:white}.play{border:2px solid #126752;color:#126752}small{color:#5b6d66}</style></head>
      <body><div class="card"><h1>Best Nikah Family Circle</h1><p>You have a secure invitation to join a private family-led Nikah circle.</p>
      <a class="btn open" href="${openApp}">Open Best Nikah Bredge</a>
      <a class="btn play" href="${play}">Get it on Google Play</a>
      <small>The invitation expires automatically and can be revoked by the circle owner.</small></div></body></html>`;
      return {status:200,headers:{"Content-Type":"text/html; charset=utf-8","Cache-Control":"no-store","X-Content-Type-Options":"nosniff","Referrer-Policy":"no-referrer"},body:html};
    }catch(e){
      context.error("FAMILY_CIRCLE_INVITE_LANDING_FAILED",e);
      return {status:500,headers:{"Content-Type":"text/plain; charset=utf-8"},body:"Family Circle invite is temporarily unavailable."};
    }
  }
});

app.http("familyCircleJoin",{
  methods:["POST"],authLevel:"anonymous",route:"family-circle/join",
  handler:requireAuth(async(request,context,user)=>{
    const client=await getPool().connect();
    try{
      const me=await ensureUser(user);
      const b=await request.json();
      const token=text(b?.token,200);
      if(!token) return {status:400,jsonBody:{ok:false,error:"INVITE_TOKEN_REQUIRED"}};
      const tokenHash=hashToken(token);

      await client.query("BEGIN");
      const inv=await client.query(
        `SELECT i.*,c.owner_user_id,c.status AS circle_status
         FROM family_circle_invites i JOIN family_circles c ON c.id=i.circle_id
         WHERE i.token_hash=$1 FOR UPDATE`,
        [tokenHash]
      );
      if(!inv.rows[0]){await client.query("ROLLBACK");return {status:404,jsonBody:{ok:false,error:"INVITE_NOT_FOUND"}};}
      const i=inv.rows[0];
      if(i.circle_status!=="active"||i.revoked_at||new Date(i.expires_at)<=new Date()||i.use_count>=i.max_uses){
        await client.query("ROLLBACK");return {status:410,jsonBody:{ok:false,error:"INVITE_EXPIRED_OR_USED"}};
      }
      if(i.owner_user_id===me.id){
        await client.query("ROLLBACK");return {status:409,jsonBody:{ok:false,error:"OWNER_ALREADY_IN_CIRCLE"}};
      }

      const existing=await client.query(
        "SELECT id,removed_at FROM family_circle_members WHERE circle_id=$1 AND user_id=$2 FOR UPDATE",
        [i.circle_id,me.id]
      );
      if(existing.rows[0]&&!existing.rows[0].removed_at){
        await client.query("ROLLBACK");return {status:409,jsonBody:{ok:false,error:"ALREADY_IN_CIRCLE"}};
      }
      if(existing.rows[0]){
        await client.query(
          "UPDATE family_circle_members SET role=$3,can_suggest_matches=true,can_view_progress=true,removed_at=NULL,joined_at=now() WHERE circle_id=$1 AND user_id=$2",
          [i.circle_id,me.id,i.role]
        );
      }else{
        await client.query(
          "INSERT INTO family_circle_members(circle_id,user_id,role) VALUES($1,$2,$3)",
          [i.circle_id,me.id,i.role]
        );
      }
      await client.query("UPDATE family_circle_invites SET use_count=use_count+1 WHERE id=$1",[i.id]);
      await client.query("COMMIT");
      return {status:200,jsonBody:{ok:true,circleId:i.circle_id,role:i.role}};
    }catch(e){
      await client.query("ROLLBACK").catch(()=>{});
      context.error("FAMILY_CIRCLE_JOIN_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_JOIN_FAILED"}};
    }finally{client.release();}
  })
});

app.http("familyCircleMemberRemove",{
  methods:["DELETE"],authLevel:"anonymous",route:"family-circle/members/{memberId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const circle=await circleForUser(me.id);
      if(!circle||circle.role!=="owner") return {status:403,jsonBody:{ok:false,error:"OWNER_REQUIRED"}};
      const memberId=(request.params&&request.params.memberId)||context.triggerMetadata?.memberId;
      const r=await query(
        "UPDATE family_circle_members SET removed_at=now() WHERE id=$1 AND circle_id=$2 AND user_id<>$3 AND removed_at IS NULL RETURNING id",
        [memberId,circle.id,me.id]
      );
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"CIRCLE_MEMBER_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true}};
    }catch(e){context.error("FAMILY_CIRCLE_MEMBER_REMOVE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_MEMBER_REMOVE_FAILED"}};}
  })
});

app.http("familyCircleSuggestionsList",{
  methods:["GET"],authLevel:"anonymous",route:"family-circle/suggestions",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const circle=await circleForUser(me.id);
      if(!circle) return {status:200,jsonBody:{ok:true,suggestions:[]}};
      await requireCircleMember(circle.id,me.id);
      const r=await query(
        `SELECT s.id,s.status,s.note,s.created_at,s.responded_at,
                COALESCE(sp.display_name,'Member') AS suggested_name,
                COALESCE(su.azure_subject,su.firebase_uid) AS suggested_identity,
                COALESCE(bp.display_name,'Family member') AS suggested_by_name
         FROM family_match_suggestions s
         JOIN users su ON su.id=s.suggested_user_id
         LEFT JOIN profiles sp ON sp.user_id=su.id
         JOIN users bu ON bu.id=s.suggested_by_user_id
         LEFT JOIN profiles bp ON bp.user_id=bu.id
         WHERE s.circle_id=$1 ORDER BY s.created_at DESC LIMIT 100`,
        [circle.id]
      );
      return {status:200,jsonBody:{ok:true,suggestions:r.rows}};
    }catch(e){context.error("FAMILY_CIRCLE_SUGGESTIONS_LIST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_SUGGESTIONS_LIST_FAILED"}};}
  })
});

app.http("familyCircleSuggestionCreate",{
  methods:["POST"],authLevel:"anonymous",route:"family-circle/suggestions",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const circle=await circleForUser(me.id);
      if(!circle) return {status:409,jsonBody:{ok:false,error:"CIRCLE_NOT_READY"}};
      const membership=await requireCircleMember(circle.id,me.id);
      if(!membership.can_suggest_matches) return {status:403,jsonBody:{ok:false,error:"MATCH_SUGGESTION_NOT_ALLOWED"}};

      const b=await request.json();
      const target=text(b?.suggestedUserId,300);
      const note=text(b?.note,500);
      if(!target) return {status:400,jsonBody:{ok:false,error:"SUGGESTED_USER_REQUIRED"}};
      const t=await query(
        "SELECT id,status FROM users WHERE (azure_subject=$1 OR firebase_uid=$1) LIMIT 1",
        [target]
      );
      if(!t.rows[0]||t.rows[0].status!=="active") return {status:404,jsonBody:{ok:false,error:"SUGGESTED_USER_NOT_FOUND"}};
      if(t.rows[0].id===circle.owner_user_id) return {status:400,jsonBody:{ok:false,error:"CANNOT_SUGGEST_OWNER"}};

      const blocked=await query(
        "SELECT 1 FROM blocked_users WHERE (blocker_user_id=$1 AND blocked_user_id=$2) OR (blocker_user_id=$2 AND blocked_user_id=$1) LIMIT 1",
        [circle.owner_user_id,t.rows[0].id]
      );
      if(blocked.rows[0]) return {status:403,jsonBody:{ok:false,error:"SUGGESTION_BLOCKED"}};

      const r=await query(
        `INSERT INTO family_match_suggestions(circle_id,suggested_by_user_id,suggested_user_id,note)
         VALUES($1,$2,$3,$4) RETURNING id,status,note,created_at`,
        [circle.id,me.id,t.rows[0].id,note||null]
      );
      return {status:201,jsonBody:{ok:true,suggestion:r.rows[0]}};
    }catch(e){context.error("FAMILY_CIRCLE_SUGGESTION_CREATE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_SUGGESTION_CREATE_FAILED"}};}
  })
});

app.http("familyCircleSuggestionRespond",{
  methods:["PATCH"],authLevel:"anonymous",route:"family-circle/suggestions/{suggestionId}",
  handler:requireAuth(async(request,context,user)=>{
    const client=await getPool().connect();
    try{
      const me=await ensureUser(user);
      const circle=await circleForUser(me.id);
      if(!circle||circle.owner_user_id!==me.id) return {status:403,jsonBody:{ok:false,error:"OWNER_REQUIRED"}};
      const id=(request.params&&request.params.suggestionId)||context.triggerMetadata?.suggestionId;
      const b=await request.json();
      const status=text(b?.status,20).toLowerCase();
      if(!["viewed","accepted","declined"].includes(status)) return {status:400,jsonBody:{ok:false,error:"INVALID_SUGGESTION_STATUS"}};

      await client.query("BEGIN");
      const current=await client.query(
        "SELECT * FROM family_match_suggestions WHERE id=$1 AND circle_id=$2 FOR UPDATE",
        [id,circle.id]
      );
      if(!current.rows[0]){await client.query("ROLLBACK");return {status:404,jsonBody:{ok:false,error:"SUGGESTION_NOT_FOUND"}};}
      const suggestion=current.rows[0];

      let interest=null;
      if(status==="accepted"){
        const blocked=await client.query(
          "SELECT 1 FROM blocked_users WHERE (blocker_user_id=$1 AND blocked_user_id=$2) OR (blocker_user_id=$2 AND blocked_user_id=$1) LIMIT 1",
          [circle.owner_user_id,suggestion.suggested_user_id]
        );
        if(blocked.rows[0]){await client.query("ROLLBACK");return {status:403,jsonBody:{ok:false,error:"SUGGESTION_BLOCKED"}};}
        const ir=await client.query(
          `INSERT INTO interests(sender_user_id,receiver_user_id,status)
           VALUES($1,$2,'pending')
           ON CONFLICT(sender_user_id,receiver_user_id) DO UPDATE SET
             status=CASE WHEN interests.status IN ('declined','cancelled') THEN 'pending' ELSE interests.status END,
             responded_at=CASE WHEN interests.status IN ('declined','cancelled') THEN NULL ELSE interests.responded_at END
           RETURNING id,status,created_at`,
          [circle.owner_user_id,suggestion.suggested_user_id]
        );
        interest=ir.rows[0]||null;
      }

      const r=await client.query(
        "UPDATE family_match_suggestions SET status=$3,responded_at=CASE WHEN $3 IN ('accepted','declined') THEN now() ELSE responded_at END WHERE id=$1 AND circle_id=$2 RETURNING id,status,responded_at",
        [id,circle.id,status]
      );
      await client.query("COMMIT");
      return {status:200,jsonBody:{ok:true,suggestion:r.rows[0],interest}};
    }catch(e){
      await client.query("ROLLBACK").catch(()=>{});
      context.error("FAMILY_CIRCLE_SUGGESTION_RESPOND_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_SUGGESTION_RESPOND_FAILED"}};
    }finally{client.release();}
  })
});

app.http("familyCircleInvitesList",{
  methods:["GET"],authLevel:"anonymous",route:"family-circle/invites",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const circle=await circleForUser(me.id);
      if(!circle) return {status:200,jsonBody:{ok:true,invites:[]}};
      await requireCircleMember(circle.id,me.id);
      const r=await query(
        `SELECT id,role,max_uses,use_count,expires_at,revoked_at,created_at
         FROM family_circle_invites WHERE circle_id=$1
         ORDER BY created_at DESC LIMIT 50`,
        [circle.id]
      );
      return {status:200,jsonBody:{ok:true,invites:r.rows}};
    }catch(e){context.error("FAMILY_CIRCLE_INVITES_LIST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_INVITES_LIST_FAILED"}};}
  })
});

app.http("familyCircleInviteRevoke",{
  methods:["DELETE"],authLevel:"anonymous",route:"family-circle/invites/{inviteId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const circle=await circleForUser(me.id);
      if(!circle||circle.owner_user_id!==me.id) return {status:403,jsonBody:{ok:false,error:"OWNER_REQUIRED"}};
      const id=(request.params&&request.params.inviteId)||context.triggerMetadata?.inviteId;
      const r=await query(
        "UPDATE family_circle_invites SET revoked_at=now() WHERE id=$1 AND circle_id=$2 AND revoked_at IS NULL RETURNING id",
        [id,circle.id]
      );
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"INVITE_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true}};
    }catch(e){context.error("FAMILY_CIRCLE_INVITE_REVOKE_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"FAMILY_CIRCLE_INVITE_REVOKE_FAILED"}};}
  })
});
