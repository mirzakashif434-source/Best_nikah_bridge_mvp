const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");

/**
 * Real like-history APIs.
 * Additive only: existing /likes send flow remains authoritative and unchanged.
 */
app.http("likesReceivedAzure", {
  methods:["GET"],
  authLevel:"anonymous",
  route:"likes/received",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user,capabilities}=await accessForAuth(authUser);

      const countResult=await query(
        `SELECT count(*)::int AS count
           FROM likes l
          WHERE l.to_user_id=$1 AND l.active=true
            AND NOT EXISTS (
              SELECT 1 FROM blocked_users b
               WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=l.from_user_id)
                  OR (b.blocked_user_id=$1 AND b.blocker_user_id=l.from_user_id)
            )`,
        [user.id]
      );
      const incomingCount=Number(countResult.rows[0]?.count||0);

      if(!capabilities.whoLikedYou){
        return {
          status:402,
          jsonBody:{
            ok:false,
            error:"PREMIUM_BASIC_REQUIRED",
            locked:true,
            incomingLikeCount:incomingCount
          }
        };
      }

      const result=await query(
        `SELECT
            l.id AS like_id,
            l.created_at,
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
            up.last_seen_at,
            EXISTS(
              SELECT 1 FROM likes back
               WHERE back.from_user_id=$1
                 AND back.to_user_id=l.from_user_id
                 AND back.active=true
            ) AS liked_back
         FROM likes l
         JOIN users u ON u.id=l.from_user_id
         LEFT JOIN profiles p ON p.user_id=u.id
         LEFT JOIN privacy_settings ps ON ps.user_id=u.id
         LEFT JOIN user_presence up ON up.user_id=u.id
        WHERE l.to_user_id=$1
          AND l.active=true
          AND u.status='active'
          AND COALESCE(p.profile_completed,false)=true
          AND COALESCE(p.is_visible,false)=true
          AND COALESCE(ps.profile_discoverable,false)=true
          AND NOT EXISTS (
            SELECT 1 FROM blocked_users b
             WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=l.from_user_id)
                OR (b.blocked_user_id=$1 AND b.blocker_user_id=l.from_user_id)
          )
        ORDER BY l.created_at DESC
        LIMIT 200`,
        [user.id]
      );

      return {
        status:200,
        jsonBody:{
          ok:true,
          locked:false,
          incomingLikeCount:incomingCount,
          likes:result.rows
        }
      };
    }catch(e){
      context.error("LIKES_RECEIVED_FAILED",e);
      return {
        status:e.statusCode||500,
        jsonBody:{ok:false,error:e.statusCode?e.message:"LIKES_RECEIVED_FAILED"}
      };
    }
  })
});


app.http("likesSentAzure", {
  methods:["GET"],
  authLevel:"anonymous",
  route:"likes/sent",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user}=await accessForAuth(authUser);
      const result=await query(
        `SELECT
            l.id AS like_id,
            l.created_at,
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
            up.last_seen_at,
            EXISTS(
              SELECT 1 FROM likes back
               WHERE back.from_user_id=l.to_user_id
                 AND back.to_user_id=$1
                 AND back.active=true
            ) AS mutual_like
         FROM likes l
         JOIN users u ON u.id=l.to_user_id
         LEFT JOIN profiles p ON p.user_id=u.id
         LEFT JOIN privacy_settings ps ON ps.user_id=u.id
         LEFT JOIN user_presence up ON up.user_id=u.id
        WHERE l.from_user_id=$1
          AND l.active=true
          AND u.status='active'
          AND COALESCE(p.profile_completed,false)=true
          AND COALESCE(p.is_visible,false)=true
          AND COALESCE(ps.profile_discoverable,false)=true
          AND NOT EXISTS (
            SELECT 1 FROM blocked_users b
             WHERE (b.blocker_user_id=$1 AND b.blocked_user_id=l.to_user_id)
                OR (b.blocked_user_id=$1 AND b.blocker_user_id=l.to_user_id)
          )
        ORDER BY l.created_at DESC
        LIMIT 200`,
        [user.id]
      );
      return {status:200,jsonBody:{ok:true,count:result.rows.length,likes:result.rows}};
    }catch(e){
      context.error("LIKES_SENT_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"LIKES_SENT_FAILED"}};
    }
  })
});

app.http("likeCancelAzure", {
  methods:["DELETE"],
  authLevel:"anonymous",
  route:"likes/{targetUserId}",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const {user}=await accessForAuth(authUser);
      const targetKey=String(request.params?.targetUserId||context.triggerMetadata?.targetUserId||"").trim();
      if(!targetKey) return {status:400,jsonBody:{ok:false,error:"LIKE_TARGET_REQUIRED"}};
      const target=await query(
        "SELECT id FROM users WHERE id::text=$1 OR azure_subject=$1 OR firebase_uid=$1 LIMIT 1",
        [targetKey]
      );
      if(!target.rows[0]) return {status:404,jsonBody:{ok:false,error:"USER_NOT_FOUND"}};
      const result=await query(
        `UPDATE likes
            SET active=false
          WHERE from_user_id=$1 AND to_user_id=$2 AND active=true
          RETURNING id`,
        [user.id,target.rows[0].id]
      );
      if(!result.rows[0]) return {status:404,jsonBody:{ok:false,error:"ACTIVE_LIKE_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true,cancelled:true}};
    }catch(e){
      context.error("LIKE_CANCEL_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"LIKE_CANCEL_FAILED"}};
    }
  })
});

module.exports={};
