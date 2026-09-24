const { app } = require("@azure/functions");
const { query } = require("./db");

const REQUIRED_TABLES = [
  "users","profiles","privacy_settings","verifications","conversations","family_links",
  "premium_plans","premium_entitlements","owner_earnings","owner_earnings_summary",
  "owner_provider_settlements","wallet_accounts","wallet_ledger"
];

async function tableExists(name){
  const r=await query(
    "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name=$1) AS ok",
    [name]
  );
  return Boolean(r.rows[0]?.ok);
}
async function columnExists(table,column){
  const r=await query(
    "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name=$1 AND column_name=$2) AS ok",
    [table,column]
  );
  return Boolean(r.rows[0]?.ok);
}

app.http("releaseReadiness",{
  methods:["GET"],authLevel:"anonymous",route:"release/readiness",
  handler:async(request,context)=>{
    try{
      const checks={};
      checks.tables={};
      for(const t of REQUIRED_TABLES) checks.tables[t]=await tableExists(t);

      checks.ownerUsdColumns={
        available:await columnExists("owner_earnings_summary","available_usd_minor"),
        pending:await columnExists("owner_earnings_summary","pending_usd_minor"),
        settled:await columnExists("owner_earnings_summary","settled_usd_minor")
      };

      const plans=await query(
        `SELECT plan_key FROM premium_plans
          WHERE active=true
            AND plan_key = ANY($1::text[])
          ORDER BY plan_key`,
        [["premium_basic_20","premium_plus_40","premium_vip_60"]]
      );
      checks.premiumPlans=plans.rows.map(r=>r.plan_key);
      checks.premiumPlanCount=checks.premiumPlans.length;

      const envNames=[
        "PLAY_PREMIUM_BASIC_PRODUCT_ID","PLAY_PREMIUM_BASIC_BASE_PLAN_ID",
        "PLAY_PREMIUM_PLUS_PRODUCT_ID","PLAY_PREMIUM_PLUS_BASE_PLAN_ID",
        "PLAY_PREMIUM_VIP_PRODUCT_ID","PLAY_PREMIUM_VIP_BASE_PLAN_ID"
      ];
      checks.premiumEnvironment=envNames.every(n=>String(process.env[n]||"").trim().length>0);
      checks.rewardedAdConfigured=/^ca-app-pub-\d{16}\/\d+$/.test(String(process.env.ADMOB_REWARDED_AD_UNIT_ID||"").trim());

      const allTables=Object.values(checks.tables).every(Boolean);
      const allOwnerCols=Object.values(checks.ownerUsdColumns).every(Boolean);
      const ready=allTables && allOwnerCols && checks.premiumPlanCount===3 && checks.premiumEnvironment && checks.rewardedAdConfigured;

      return {status:ready?200:503,jsonBody:{
        ok:ready,
        ready,
        checks,
        releaseGate:"2026-09-24-18-issue-final"
      }};
    }catch(e){
      context.error("RELEASE_READINESS_FAILED",e);
      return {status:503,jsonBody:{ok:false,ready:false,error:"RELEASE_READINESS_FAILED"}};
    }
  }
});
