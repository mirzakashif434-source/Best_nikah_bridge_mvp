const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

async function requireOwnerAdmin(user){
  const key=(user.azure_subject||user.uid||"").trim();
  const r=await query(
    "SELECT id,role,status FROM users WHERE (azure_subject=$1 OR firebase_uid=$1) AND status='active' LIMIT 1",
    [key]
  );
  const me=r.rows[0];
  if(!me||!["admin","moderator"].includes(me.role)){
    const e=new Error("ADMIN_REQUIRED");e.statusCode=403;throw e;
  }
  return me;
}

app.http("ownerLiveAnalyticsAzure",{
  methods:["GET"],authLevel:"anonymous",route:"owner/live-analytics",
  handler:requireAuth(async(request,context,user)=>{
    try{
      await requireOwnerAdmin(user);
      const [
        users,profiles,presence,likes,messages,conversations,
        premium,earnings,todayRevenue,monthRevenue,
        reports,photoPending,recentUsers
      ]=await Promise.all([
        query(`SELECT
          count(*)::int AS total_users,
          count(*) FILTER (WHERE created_at >= (now() AT TIME ZONE 'UTC')::date)::int AS new_today,
          count(*) FILTER (WHERE created_at >= now()-interval '7 days')::int AS new_7d,
          count(*) FILTER (WHERE status='active')::int AS active_accounts
          FROM users`),
        query(`SELECT
          count(*) FILTER (WHERE profile_completed=true)::int AS completed_profiles,
          count(*) FILTER (WHERE COALESCE(photo_verified,false)=true)::int AS verified_photo_profiles
          FROM profiles`),
        query(`SELECT
          count(*) FILTER (WHERE last_seen_at>=now()-interval '2 minutes')::int AS online_now,
          count(*) FILTER (WHERE last_seen_at>=now()-interval '24 hours')::int AS seen_24h
          FROM user_presence`),
        query(`SELECT
          count(*) FILTER (WHERE created_at>=now()-interval '24 hours')::int AS likes_24h,
          count(*)::int AS total_likes
          FROM like_events`),
        query(`SELECT
          count(*) FILTER (WHERE created_at>=now()-interval '24 hours')::int AS messages_24h,
          count(*)::int AS total_messages
          FROM messages`),
        query(`SELECT count(*) FILTER (WHERE status='mutual')::int AS mutual_chats FROM conversations`),
        query(`SELECT
          count(*) FILTER (WHERE status='active' AND expires_at>now())::int AS active_paid,
          count(*) FILTER (WHERE status='active' AND expires_at>now() AND plan_key='premium_basic_20')::int AS basic_20,
          count(*) FILTER (WHERE status='active' AND expires_at>now() AND plan_key='premium_plus_40')::int AS plus_40,
          count(*) FILTER (WHERE status='active' AND expires_at>now() AND plan_key='premium_vip_60')::int AS vip_60
          FROM premium_entitlements`),
        query(`SELECT * FROM owner_earnings_summary WHERE id=true`),
        query(`SELECT
          count(*)::int AS upgrades,
          COALESCE(sum(plan_value_sar_minor),0)::bigint AS sar_minor
          FROM owner_earnings
          WHERE status='verified_sale'
            AND created_at >= (now() AT TIME ZONE 'UTC')::date`),
        query(`SELECT
          count(*)::int AS upgrades,
          COALESCE(sum(plan_value_sar_minor),0)::bigint AS sar_minor
          FROM owner_earnings
          WHERE status='verified_sale'
            AND created_at >= date_trunc('month',now())`),
        query(`SELECT count(*) FILTER (WHERE status='open')::int AS open_reports FROM safety_reports`),
        query(`SELECT count(*) FILTER (WHERE status='pending')::int AS pending_photo_reviews FROM photo_verification_sets`),
        query(`SELECT
          u.id,COALESCE(u.azure_subject,u.firebase_uid) AS identity,
          u.email,u.created_at,
          p.display_name,p.gender,COALESCE(p.photo_verified,false) AS photo_verified,
          up.last_seen_at,
          COALESCE(up.last_seen_at>=now()-interval '2 minutes',false) AS is_online,
          pe.plan_key,pe.expires_at
          FROM users u
          LEFT JOIN profiles p ON p.user_id=u.id
          LEFT JOIN user_presence up ON up.user_id=u.id
          LEFT JOIN premium_entitlements pe ON pe.user_id=u.id AND pe.status='active' AND pe.expires_at>now()
          WHERE u.status='active'
          ORDER BY COALESCE(up.last_seen_at,u.created_at) DESC
          LIMIT 30`)
      ]);

      const u=users.rows[0]||{},p=profiles.rows[0]||{},pr=presence.rows[0]||{},
            l=likes.rows[0]||{},m=messages.rows[0]||{},c=conversations.rows[0]||{},
            pe=premium.rows[0]||{},e=earnings.rows[0]||{},
            tr=todayRevenue.rows[0]||{},mr=monthRevenue.rows[0]||{},
            sr=reports.rows[0]||{},pv=photoPending.rows[0]||{};

      return {status:200,jsonBody:{
        ok:true,
        generatedAt:new Date().toISOString(),
        users:{
          total:Number(u.total_users||0),
          newToday:Number(u.new_today||0),
          new7d:Number(u.new_7d||0),
          activeAccounts:Number(u.active_accounts||0),
          onlineNow:Number(pr.online_now||0),
          seen24h:Number(pr.seen_24h||0)
        },
        profiles:{
          completed:Number(p.completed_profiles||0),
          verifiedPhotos:Number(p.verified_photo_profiles||0),
          pendingPhotoReviews:Number(pv.pending_photo_reviews||0)
        },
        engagement:{
          likes24h:Number(l.likes_24h||0),
          totalLikes:Number(l.total_likes||0),
          messages24h:Number(m.messages_24h||0),
          totalMessages:Number(m.total_messages||0),
          mutualChats:Number(c.mutual_chats||0)
        },
        premium:{
          activePaid:Number(pe.active_paid||0),
          basic20:Number(pe.basic_20||0),
          plus40:Number(pe.plus_40||0),
          vip60:Number(pe.vip_60||0)
        },
        revenue:{
          todayUpgrades:Number(tr.upgrades||0),
          todayGrossSar:Number(tr.sar_minor||0)/100,
          monthUpgrades:Number(mr.upgrades||0),
          monthGrossSar:Number(mr.sar_minor||0)/100,
          totalVerifiedUpgrades:Number(e.verified_sales_count||0),
          totalGrossSar:Number(e.verified_plan_value_sar_minor||0)/100
        },
        payout:{
          availableUsd:Number(e.available_usd_minor||0)/100,
          pendingUsd:Number(e.pending_usd_minor||0)/100,
          settledUsd:Number(e.settled_usd_minor||0)/100,
          availableSar:Number(e.available_sar_minor||0)/100,
          pendingSar:Number(e.pending_sar_minor||0)/100,
          settledSar:Number(e.settled_sar_minor||0)/100
        },
        safety:{
          openReports:Number(sr.open_reports||0)
        },
        recentUsers:recentUsers.rows.map(r=>({
          id:r.id,
          identity:r.identity,
          email:r.email,
          displayName:r.display_name||"Member",
          gender:r.gender||null,
          joinedAt:r.created_at,
          lastSeenAt:r.last_seen_at||null,
          online:Boolean(r.is_online),
          photoVerified:Boolean(r.photo_verified),
          planKey:r.plan_key||null,
          planExpiresAt:r.expires_at||null
        })),
        privacyNote:"Private message bodies are not included in owner analytics. Only aggregate message counts are returned."
      }};
    }catch(e){
      context.error("OWNER_LIVE_ANALYTICS_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"OWNER_LIVE_ANALYTICS_FAILED"}};
    }
  })
});
