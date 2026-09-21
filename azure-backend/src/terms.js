const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const TERMS_VERSION = "2026-09-07-v1";

async function ensureUser(user) {
  const email = String(user.email || "").trim().toLowerCase();
  if (!user.uid || !email) throw new Error("AUTH_IDENTITY_REQUIRED");
  const r = await query(
    `INSERT INTO users (firebase_uid,email,email_verified_at)
     VALUES ($1,$2,CASE WHEN $3 THEN now() ELSE NULL END)
     ON CONFLICT (firebase_uid) DO UPDATE SET
       email=EXCLUDED.email,
       email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),
       updated_at=now()
     RETURNING id,status,terms_accepted,terms_version`,
    [user.uid,email,Boolean(user.email_verified)]
  );
  return r.rows[0];
}

app.http("termsStatus", {
  methods:["GET"], authLevel:"anonymous", route:"terms/status",
  handler: requireAuth(async (request, context, user) => {
    try {
      const me = await ensureUser(user);
      const r = await query(
        `SELECT u.terms_accepted,u.terms_version,
                p.date_of_birth,
                CASE WHEN p.date_of_birth IS NULL THEN 0
                     ELSE EXTRACT(YEAR FROM age(current_date,p.date_of_birth))::int END AS age
         FROM users u LEFT JOIN profiles p ON p.user_id=u.id WHERE u.id=$1`, [me.id]);
      const row=r.rows[0]||{};
      return {status:200,jsonBody:{ok:true,termsAccepted:Boolean(row.terms_accepted),
        termsVersion:row.terms_version||null,age:Number(row.age||0),currentTermsVersion:TERMS_VERSION}};
    } catch(e) {
      context.error("TERMS_STATUS_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"TERMS_STATUS_FAILED"}};
    }
  })
});

app.http("termsAccept", {
  methods:["POST"], authLevel:"anonymous", route:"terms/accept",
  handler: requireAuth(async (request, context, user) => {
    try {
      const me=await ensureUser(user);
      const r=await query(
        `SELECT date_of_birth,
                CASE WHEN date_of_birth IS NULL THEN 0
                     ELSE EXTRACT(YEAR FROM age(current_date,date_of_birth))::int END AS age
         FROM profiles WHERE user_id=$1`,[me.id]);
      const age=Number(r.rows[0]?.age||0);
      if(age<18 || age>100) return {status:400,jsonBody:{ok:false,error:"AGE_MUST_BE_18_TO_100"}};
      const updated=await query(
        `UPDATE users SET terms_accepted=true,terms_version=$1,terms_accepted_at=now(),updated_at=now()
         WHERE id=$2 RETURNING terms_accepted,terms_version,terms_accepted_at`,[TERMS_VERSION,me.id]);
      return {status:200,jsonBody:{ok:true,...updated.rows[0]}};
    } catch(e) {
      context.error("TERMS_ACCEPT_FAILED",e);
      return {status:500,jsonBody:{ok:false,error:"TERMS_ACCEPT_FAILED"}};
    }
  })
});
