const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");

async function resolveActiveVisibleTarget(targetKey){
  const target=await query(
    `SELECT u.id
       FROM users u
       JOIN profiles p ON p.user_id=u.id
       LEFT JOIN privacy_settings ps ON ps.user_id=u.id
      WHERE (u.id::text=$1 OR u.azure_subject=$1 OR u.firebase_uid=$1)
        AND u.status='active'
        AND COALESCE(p.profile_completed,false)=true
        AND COALESCE(p.is_visible,false)=true
        AND COALESCE(ps.profile_discoverable,false)=true
      LIMIT 1`,
    [targetKey]
  );
  return target.rows[0]||null;
}

app.http("profileViewRecordAzure",{
  methods:["POST"],
  authLevel:"anonymous",
  route:"profile-views",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user}=await accessForAuth(authUser);
      const body=await request.json();
      const targetKey=String(body?.viewedUserId||"").trim();
      if(!targetKey) return {status:400,jsonBody:{ok:false,error:"VIEWED_USER_REQUIRED"}};

      const target=await resolveActiveVisibleTarget(targetKey);
      if(!target) return {status:404,jsonBody:{ok:false,error:"PROFILE_NOT_FOUND"}};
      if(String(target.id)===String(user.id)) return {status:400,jsonBody:{ok:false,error:"SELF_VIEW_NOT_RECORDED"}};

      const blocked=await query(
        `SELECT 1 FROM blocked_users
          WHERE (blocker_user_id=$1 AND blocked_user_id=$2)
             OR (blocker_user_id=$2 AND blocked_user_id=$1)
          LIMIT 1`,
        [user.id,target.id]
      );
      if(blocked.rows[0]) return {status:404,jsonBody:{ok:false,error:"PROFILE_NOT_FOUND"}};

      const saved=await query(
        `INSERT INTO profile_views(viewer_user_id,viewed_user_id,first_viewed_at,last_viewed_at,view_count)
         VALUES($1,$2,now(),now(),1)
         ON CONFLICT(viewer_user_id,viewed_user_id) DO UPDATE SET
           last_viewed_at=now(),
           view_count=profile_views.view_count+1
         RETURNING id,last_viewed_at,view_count`,
        [user.id,target.id]
      );

      return {status:200,jsonBody:{ok:true,recorded:true,view:saved.rows[0]}};
    }catch(e){
      context.error("PROFILE_VIEW_RECORD_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PROFILE_VIEW_RECORD_FAILED"}};
    }
  })
});

app.http("profileViewsReceivedAzure",{
  methods:["GET"],
  authLevel:"anonymous",
  route:"profile-views/received",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user}=await accessForAuth(authUser);
      const result=await query(
        `SELECT
            pv.id AS view_id,
            pv.first_viewed_at,
            pv.last_viewed_at,
            pv.view_count,
            COALESCE(u.azure_subject,u.firebase_uid,u.id::text) AS user_id,
            COALESCE(p.display_name,'Member') AS display_name,
            EXTRACT(YEAR FROM age(CURRENT_DATE,p.date_of_birth))::int AS age,
            p.gender,
            p.country,
            CASE WHEN COALESCE(ps.show_city,false) THEN p.city ELSE NULL END AS city,
            p.marriage_intention,
            p.education,
            p.family_involvement,
            COALESCE(p.photo_verified,false) AS photo_verified,
            EXISTS(
              SELECT 1 FROM verifications v
               WHERE v.user_id=u.id AND v.status='approved'
            ) AS identity_verified,
            COALESCE(up.last_seen_at >= now()-interval '2 minutes',false) AS is_online,
            EXISTS(
              SELECT 1 FROM likes l
               WHERE l.from_user_id=$1 AND l.to_user_id=pv.viewer_user_id AND l.active=true
            ) AS i_liked,
            EXISTS(
              SELECT 1 FROM likes l
               WHERE l.from_user_id=pv.viewer_user_id AND l.to_user_id=$1 AND l.active=true
            ) AS liked_me
         FROM profile_views pv
         JOIN users u ON u.id=pv.viewer_user_id
         LEFT JOIN profiles p ON p.user_id=u.id
         LEFT JOIN privacy_settings ps ON ps.user_id=u.id
         LEFT JOIN user_presence up ON up.user_id=u.id
        WHERE pv.viewed_user_id=$1
          AND u.status='active'
          AND COALESCE(p.profile_completed,false)=true
          AND COALESCE(p.is_visible,false)=true
          AND COALESCE(ps.profile_discoverable,false)=true
          AND NOT EXISTS (
            SELECT 1 FROM blocked_users b
             WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=pv.viewer_user_id)
                OR (b.blocked_user_id=$1 AND b.blocker_user_id=pv.viewer_user_id)
          )
        ORDER BY pv.last_viewed_at DESC
        LIMIT 200`,
        [user.id]
      );
      return {status:200,jsonBody:{ok:true,count:result.rows.length,viewers:result.rows}};
    }catch(e){
      context.error("PROFILE_VIEWS_RECEIVED_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"PROFILE_VIEWS_RECEIVED_FAILED"}};
    }
  })
});

module.exports={};
