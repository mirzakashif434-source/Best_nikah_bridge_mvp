const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");

app.http("seriousNikahPlusSummary",{
  methods:["GET"],authLevel:"anonymous",route:"premium/serious-plus-summary",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user,premium,capabilities}=await accessForAuth(authUser);
      const [likes,today,circle]=await Promise.all([
        query(
          `SELECT count(*)::int AS count
             FROM interests i
            WHERE i.receiver_user_id=$1 AND i.status='pending'
              AND NOT EXISTS (
                SELECT 1 FROM blocked_users b
                 WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=i.sender_user_id)
                    OR (b.blocked_user_id=$1 AND b.blocker_user_id=i.sender_user_id)
              )`,[user.id]),
        query(
          `SELECT count(*)::int AS count
             FROM interests
            WHERE sender_user_id=$1 AND created_at::date=(now() AT TIME ZONE 'UTC')::date`,[user.id]),
        query(
          `SELECT count(*)::int AS count
             FROM family_circle_members m
             JOIN family_circles c ON c.id=m.circle_id
            WHERE c.owner_user_id=$1 AND c.status='active'
              AND m.removed_at IS NULL AND m.role<>'owner'`,[user.id])
      ]);
      const freeLimit=3, used=Number(today.rows[0]?.count||0);
      return {status:200,jsonBody:{
        ok:true,
        premium,
        incomingInterestCount:Number(likes.rows[0]?.count||0),
        interestsToday:used,
        freeInterestDailyLimit:freeLimit,
        freeInterestsRemaining:premium.active?null:Math.max(0,freeLimit-used),
        familyCircleMembers:Number(circle.rows[0]?.count||0),
        familyCircleLimit:capabilities.familyCircleLimit,
        features:capabilities
      }};
    }catch(e){
      context.error("SERIOUS_PLUS_SUMMARY_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"SERIOUS_PLUS_SUMMARY_FAILED"}};
    }
  })
});

app.http("whoLikedYou",{
  methods:["GET"],authLevel:"anonymous",route:"premium/who-liked-you",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user,premium,capabilities}=await accessForAuth(authUser);
      const count=await query(
        `SELECT count(*)::int AS count
           FROM interests i
          WHERE i.receiver_user_id=$1 AND i.status='pending'
            AND NOT EXISTS (
              SELECT 1 FROM blocked_users b
               WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=i.sender_user_id)
                  OR (b.blocked_user_id=$1 AND b.blocker_user_id=i.sender_user_id)
            )`,[user.id]);
      const incomingCount=Number(count.rows[0]?.count||0);
      if(!capabilities.whoLikedYou) return {status:402,jsonBody:{ok:false,error:"PREMIUM_BASIC_REQUIRED",locked:true,incomingInterestCount}};
      const r=await query(
        `SELECT i.id AS interest_id,i.created_at,
                COALESCE(u.azure_subject,u.firebase_uid) AS user_id,
                COALESCE(p.display_name,'Member') AS display_name,
                p.gender,p.country,p.city,
                EXTRACT(YEAR FROM age(CURRENT_DATE,p.date_of_birth))::int AS age
           FROM interests i
           JOIN users u ON u.id=i.sender_user_id
           LEFT JOIN profiles p ON p.user_id=u.id
          WHERE i.receiver_user_id=$1 AND i.status='pending'
            AND u.status='active'
            AND NOT EXISTS (
              SELECT 1 FROM blocked_users b
               WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=i.sender_user_id)
                  OR (b.blocked_user_id=$1 AND b.blocker_user_id=i.sender_user_id)
            )
          ORDER BY i.created_at DESC LIMIT 200`,[user.id]);
      return {status:200,jsonBody:{ok:true,locked:false,incomingInterestCount,people:r.rows}};
    }catch(e){
      context.error("WHO_LIKED_YOU_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"WHO_LIKED_YOU_FAILED"}};
    }
  })
});


app.http("premiumAdvancedFiltersGet",{
  methods:["GET"],authLevel:"anonymous",route:"premium/advanced-filters",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user,capabilities}=await accessForAuth(authUser);
      if(!capabilities.advancedMatching) return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};
      const r=await query(
        "SELECT countries,cities,education_levels,family_involvement FROM partner_preferences WHERE user_id=$1 LIMIT 1",
        [user.id]
      );
      return {status:200,jsonBody:{ok:true,filters:r.rows[0]||{countries:[],cities:[],education_levels:[],family_involvement:null}}};
    }catch(e){
      context.error("PREMIUM_ADVANCED_FILTERS_GET_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PREMIUM_ADVANCED_FILTERS_GET_FAILED"}};
    }
  })
});

app.http("premiumAdvancedFiltersSave",{
  methods:["PATCH"],authLevel:"anonymous",route:"premium/advanced-filters",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user,capabilities}=await accessForAuth(authUser);
      if(!capabilities.advancedMatching) return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};
      const b=await request.json();
      const cleanList=(v)=>Array.isArray(v)?v.map(x=>String(x||"").trim().slice(0,120)).filter(Boolean).slice(0,30):[];
      const countries=cleanList(b?.countries);
      const cities=cleanList(b?.cities);
      const education=cleanList(b?.educationLevels);
      const family=String(b?.familyInvolvement||"").trim().slice(0,120)||null;
      const r=await query(
        `INSERT INTO partner_preferences(user_id,countries,cities,education_levels,family_involvement)
         VALUES($1,$2,$3,$4,$5)
         ON CONFLICT(user_id) DO UPDATE SET
           countries=EXCLUDED.countries,
           cities=EXCLUDED.cities,
           education_levels=EXCLUDED.education_levels,
           family_involvement=EXCLUDED.family_involvement,
           updated_at=now()
         RETURNING countries,cities,education_levels,family_involvement`,
        [user.id,countries,cities,education,family]
      );
      return {status:200,jsonBody:{ok:true,filters:r.rows[0]}};
    }catch(e){
      context.error("PREMIUM_ADVANCED_FILTERS_SAVE_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PREMIUM_ADVANCED_FILTERS_SAVE_FAILED"}};
    }
  })
});
