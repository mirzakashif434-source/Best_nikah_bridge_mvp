const { app } = require("@azure/functions");
const { getPool, query } = require("./db");
const { requireAuth } = require("./auth");

const text = (v, max) => typeof v === "string" ? v.trim().slice(0, max) : "";

function exactAgeFromIsoDate(dob) {
  const m = /^(\d{4})-(\d{2})-(\d{2})$/.exec(dob);
  if (!m) return null;
  const year = Number(m[1]), month = Number(m[2]), day = Number(m[3]);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (date.getUTCFullYear() !== year || date.getUTCMonth() !== month - 1 || date.getUTCDate() !== day) return null;
  const now = new Date();
  let age = now.getUTCFullYear() - year;
  const monthDelta = now.getUTCMonth() - (month - 1);
  if (monthDelta < 0 || (monthDelta === 0 && now.getUTCDate() < day)) age--;
  return age;
}

async function ensureUser(user) {
  const email = text(user.email, 320).toLowerCase();
  if (!email) { const e=new Error("AUTH_EMAIL_REQUIRED"); e.statusCode=401; throw e; }
  const azureSubject = typeof user.azure_subject==="string" && user.azure_subject.trim() ? user.azure_subject.trim() : null;
  let row=null;
  if (azureSubject) {
    const existing=await query(
      "SELECT id,email,status FROM users WHERE azure_subject=$1 OR firebase_uid=$2 LIMIT 1",
      [azureSubject,user.uid||azureSubject]
    );
    if(existing.rows[0]){
      await query(
        "UPDATE users SET azure_subject=COALESCE(azure_subject,$1),email=$2,email_verified_at=COALESCE(email_verified_at,CASE WHEN $3 THEN now() ELSE NULL END),updated_at=now() WHERE id=$4",
        [azureSubject,email,Boolean(user.email_verified),existing.rows[0].id]
      );
      row={...existing.rows[0],email};
    }else{
      const created=await query(
        "INSERT INTO users(firebase_uid,azure_subject,email,email_verified_at) VALUES($1,$2,$3,CASE WHEN $4 THEN now() ELSE NULL END) RETURNING id,email,status",
        [user.uid||azureSubject,azureSubject,email,Boolean(user.email_verified)]
      );
      row=created.rows[0];
    }
  } else {
    if(!user.uid){ const e=new Error("AUTH_IDENTITY_REQUIRED"); e.statusCode=401; throw e; }
    const r = await query(
      `INSERT INTO users (firebase_uid, email, email_verified_at)
       VALUES ($1,$2,CASE WHEN $3 THEN now() ELSE NULL END)
       ON CONFLICT (firebase_uid) DO UPDATE SET
         email=EXCLUDED.email,
         email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),
         updated_at=now()
       RETURNING id,email,status`,
      [user.uid,email,Boolean(user.email_verified)]
    );
    row=r.rows[0];
  }
  return row;
}

app.http("profileGet", {
  methods:["GET"], authLevel:"anonymous", route:"profile",
  handler: requireAuth(async (request, context, user) => {
    try {
      const u=await ensureUser(user);
      if(u.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      const r=await query(
        `SELECT u.id,u.email,u.status,p.display_name,p.date_of_birth,p.gender,p.country,p.city,p.bio,
                p.marriage_intention,p.education,p.family_involvement,p.readiness_score,p.profile_completed,p.is_visible,
                pp.min_age,pp.max_age,pp.preferred_gender,pp.countries,pp.cities,pp.preferred_marriage_timeline,
                pp.deal_breakers,pp.preferences
         FROM users u LEFT JOIN profiles p ON p.user_id=u.id
         LEFT JOIN partner_preferences pp ON pp.user_id=u.id WHERE u.id=$1`,[u.id]);
      return {status:200,jsonBody:{ok:true,profile:r.rows[0]||null}};
    } catch(e) {
      context.error("PROFILE_READ_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PROFILE_READ_FAILED"}};
    }
  })
});

app.http("profilePut", {
  methods:["PUT"], authLevel:"anonymous", route:"profile",
  handler: requireAuth(async (request, context, user) => {
    const pool=getPool(); const client=await pool.connect();
    try {
      const u=await ensureUser(user);
      if(u.status!=="active") return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      const b=await request.json();
      const displayName=text(b.displayName,120);
      const dob=text(b.dateOfBirth,10);
      const gender=text(b.gender,20).toLowerCase();
      if(displayName.length<2) return {status:400,jsonBody:{ok:false,error:"DISPLAY_NAME_REQUIRED"}};
      if(!/^\d{4}-\d{2}-\d{2}$/.test(dob)) return {status:400,jsonBody:{ok:false,error:"DATE_OF_BIRTH_REQUIRED"}};
      const age=exactAgeFromIsoDate(dob);
      if(age===null) return {status:400,jsonBody:{ok:false,error:"DATE_OF_BIRTH_INVALID"}};
      if(age<18||age>100) return {status:400,jsonBody:{ok:false,error:"AGE_MUST_BE_18_TO_100"}};
      if(!["male","female"].includes(gender)) return {status:400,jsonBody:{ok:false,error:"GENDER_INVALID"}};
      const preferredGender=text(b.preferredGender,20).toLowerCase();
      if(!["male","female","any"].includes(preferredGender)) return {status:400,jsonBody:{ok:false,error:"PREFERRED_GENDER_REQUIRED"}};
      const min=b.minAge==null?null:Number(b.minAge), max=b.maxAge==null?null:Number(b.maxAge);
      if((min!=null&&(!Number.isInteger(min)||min<18||min>100))||(max!=null&&(!Number.isInteger(max)||max<18||max>100))||(min!=null&&max!=null&&min>max))
        return {status:400,jsonBody:{ok:false,error:"PARTNER_AGE_RANGE_INVALID"}};
      const countries=Array.isArray(b.countries)?b.countries.map(x=>text(x,120)).filter(Boolean).slice(0,50):[];
      const cities=Array.isArray(b.cities)?b.cities.map(x=>text(x,120)).filter(Boolean).slice(0,50):[];
      await client.query("BEGIN");
      await client.query(
        `INSERT INTO profiles(user_id,display_name,date_of_birth,gender,country,city,bio,marriage_intention,education,family_involvement,readiness_score,profile_completed,is_visible)
         VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13)
         ON CONFLICT(user_id) DO UPDATE SET display_name=EXCLUDED.display_name,date_of_birth=EXCLUDED.date_of_birth,
         gender=EXCLUDED.gender,country=EXCLUDED.country,city=EXCLUDED.city,bio=EXCLUDED.bio,
         marriage_intention=EXCLUDED.marriage_intention,education=EXCLUDED.education,
         family_involvement=EXCLUDED.family_involvement,readiness_score=EXCLUDED.readiness_score,
         profile_completed=EXCLUDED.profile_completed,is_visible=EXCLUDED.is_visible,updated_at=now()`,
        [u.id,displayName,dob,gender,text(b.country,120)||null,text(b.city,120)||null,text(b.bio,5000)||null,
         text(b.marriageIntention,120)||null,text(b.education,120)||null,text(b.familyInvolvement,120)||null,
         b.readinessScore==null?null:Math.max(0,Math.min(100,Number(b.readinessScore))),
         Boolean(b.profileCompleted),Boolean(b.isVisible??true)]);
      await client.query(
        `INSERT INTO partner_preferences(user_id,min_age,max_age,preferred_gender,countries,cities,preferred_marriage_timeline,deal_breakers,preferences)
         VALUES($1,$2,$3,$4,$5,$6,$7,$8::jsonb,$9::jsonb)
         ON CONFLICT(user_id) DO UPDATE SET min_age=EXCLUDED.min_age,max_age=EXCLUDED.max_age,preferred_gender=EXCLUDED.preferred_gender,
         countries=EXCLUDED.countries,cities=EXCLUDED.cities,preferred_marriage_timeline=EXCLUDED.preferred_marriage_timeline,
         deal_breakers=EXCLUDED.deal_breakers,preferences=EXCLUDED.preferences,updated_at=now()`,
        [u.id,min,max,preferredGender,countries,cities,text(b.preferredMarriageTimeline,120)||null,
         JSON.stringify(b.dealBreakers&&typeof b.dealBreakers==="object"?b.dealBreakers:{}),
         JSON.stringify(b.preferences&&typeof b.preferences==="object"?b.preferences:{})]);
      await client.query("COMMIT");
      return {status:200,jsonBody:{ok:true,profileSaved:true}};
    } catch(e) {
      await client.query("ROLLBACK").catch(()=>{});
      context.error("PROFILE_WRITE_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PROFILE_WRITE_FAILED"}};
    } finally { client.release(); }
  })
});
