const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function ensureUser(user){
  const email=text(user.email,320).toLowerCase();
  if(!email) throw new Error("AUTH_IDENTITY_REQUIRED");

  // Azure External ID is the primary identity for Azure users.
  // Firebase UID remains available only for legacy users during the migration.
  if(user.auth_provider==="azure_external_id" && user.azure_subject){
    const azure=await query(
      "SELECT id,status,email FROM users WHERE azure_subject=$1 LIMIT 1",
      [String(user.azure_subject)]
    );
    if(!azure.rows[0]) throw new Error("AZURE_USER_NOT_FOUND");
    return azure.rows[0];
  }

  if(!user.uid) throw new Error("AUTH_IDENTITY_REQUIRED");
  const r=await query(
    "INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status,email",
    [user.uid,email,Boolean(user.email_verified)]
  );
  return r.rows[0];
}

app.http("familyLinksList",{
  methods:["GET"],authLevel:"anonymous",route:"family-links",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const r=await query(
        "SELECT id,wali_name,wali_email,wali_phone_e164,status,created_at,verified_at FROM family_links WHERE user_id=$1 AND status<>'revoked' ORDER BY created_at DESC",
        [me.id]);
      return {status:200,jsonBody:{ok:true,familyLinks:r.rows}};
    }catch(e){context.error("FAMILY_LINKS_LIST_FAILED",e);return {status:500,jsonBody:{ok:false,error:"FAMILY_LINKS_LIST_FAILED"}};}
  })
});

app.http("familyLinkCreate",{
  methods:["POST"],authLevel:"anonymous",route:"family-links",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      if(me.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      const b=await request.json();
      const name=text(b.waliName,120);
      const email=text(b.waliEmail,320).toLowerCase();
      const phone=text(b.waliPhoneE164,30);
      if(!name || (!email && !phone)) return {status:400,jsonBody:{ok:false,error:"WALI_NAME_AND_CONTACT_REQUIRED"}};
      if(email===me.email) return {status:400,jsonBody:{ok:false,error:"WALI_MUST_BE_SEPARATE_ACCOUNT"}};
      await query("UPDATE family_links SET status='revoked' WHERE user_id=$1 AND status IN ('pending','verified')",[me.id]);
      const r=await query(
        "INSERT INTO family_links(user_id,wali_email,wali_phone_e164,wali_name,status) VALUES($1,$2,$3,$4,'pending') RETURNING id,wali_name,wali_email,wali_phone_e164,status,created_at",
        [me.id,email||null,phone||null,name]
      );
      return {status:201,jsonBody:{ok:true,familyLink:r.rows[0]}};
    }catch(e){context.error("FAMILY_LINK_CREATE_FAILED",e);return {status:500,jsonBody:{ok:false,error:"FAMILY_LINK_CREATE_FAILED"}};}
  })
});

app.http("familyLinkVerify",{
  methods:["POST"],authLevel:"anonymous",route:"family-links/{familyLinkId}/verify",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const id=(request.params&&request.params.familyLinkId)||context.triggerMetadata?.familyLinkId;
      const r=await query(
        "SELECT fl.*,u.email AS owner_email FROM family_links fl JOIN users u ON u.id=fl.user_id WHERE fl.id=$1",
        [id]);
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"FAMILY_LINK_NOT_FOUND"}};
      const link=r.rows[0];
      if(link.status!=="pending") return {status:409,jsonBody:{ok:false,error:"FAMILY_LINK_NOT_PENDING"}};
      const normalizedEmail=text(user.email,320).toLowerCase();
      const phone=text(user.phone_number,30);
      const emailMatch=Boolean(link.wali_email&&normalizedEmail&&link.wali_email===normalizedEmail);
      const phoneMatch=Boolean(link.wali_phone_e164&&phone&&link.wali_phone_e164===phone);
      if(!emailMatch&&!phoneMatch) return {status:403,jsonBody:{ok:false,error:"WALI_IDENTITY_MISMATCH"}};
      const updated=await query(
        "UPDATE family_links SET status='verified',verified_at=now() WHERE id=$1 AND status='pending' RETURNING id,status,verified_at",
        [id]);
      return {status:200,jsonBody:{ok:true,familyLink:updated.rows[0]}};
    }catch(e){context.error("FAMILY_LINK_VERIFY_FAILED",e);return {status:500,jsonBody:{ok:false,error:"FAMILY_LINK_VERIFY_FAILED"}};}
  })
});

app.http("familyLinkRevoke",{
  methods:["DELETE"],authLevel:"anonymous",route:"family-links/{familyLinkId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const id=(request.params&&request.params.familyLinkId)||context.triggerMetadata?.familyLinkId;
      const r=await query(
        "UPDATE family_links SET status='revoked' WHERE id=$1 AND user_id=$2 AND status<>'revoked' RETURNING id,status",
        [id,me.id]);
      if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"FAMILY_LINK_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true,familyLink:r.rows[0]}};
    }catch(e){context.error("FAMILY_LINK_REVOKE_FAILED",e);return {status:500,jsonBody:{ok:false,error:"FAMILY_LINK_REVOKE_FAILED"}};}
  })
});
