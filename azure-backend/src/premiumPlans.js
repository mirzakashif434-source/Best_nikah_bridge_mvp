const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const PLAN_CONFIG = [
  {
    planKey: "premium_monthly",
    productIdEnv: "PLAY_PREMIUM_MONTHLY_PRODUCT_ID",
    basePlanIdEnv: "PLAY_PREMIUM_MONTHLY_BASE_PLAN_ID",
    billingPeriod: "monthly",
    title: "Premium Monthly"
  },
  {
    planKey: "premium_yearly",
    productIdEnv: "PLAY_PREMIUM_YEARLY_PRODUCT_ID",
    basePlanIdEnv: "PLAY_PREMIUM_YEARLY_BASE_PLAN_ID",
    billingPeriod: "yearly",
    title: "Premium Yearly"
  }
];

function configuredPlans() {
  return PLAN_CONFIG.map((config) => ({
    planKey: config.planKey,
    productId: String(process.env[config.productIdEnv] || "").trim(),
    basePlanId: String(process.env[config.basePlanIdEnv] || "").trim(),
    billingPeriod: config.billingPeriod,
    title: config.title
  }));
}

function requireConfiguredPlans() {
  const plans = configuredPlans();
  if (plans.some((plan) => !/^[a-z0-9_.-]{1,40}$/.test(plan.productId) || !/^[a-z0-9-]{1,40}$/.test(plan.basePlanId))) {
    const error = new Error("Google Play premium product configuration is incomplete.");
    error.statusCode = 503;
    throw error;
  }
  return plans;
}

app.http("getPremiumPlans", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "premium/plans",
  handler: requireAuth(async () => {
    const plans = requireConfiguredPlans();
    const result = await query(
      `SELECT plan_key, display_name, billing_period, features
         FROM premium_plans
        WHERE active = TRUE
        ORDER BY sort_order ASC`
    );

    return {
      status: 200,
      jsonBody: {
        ok: true,
        plans: result.rows.map((row) => {
          const config = plans.find((plan) => plan.planKey === row.plan_key);
          return {
            planKey: row.plan_key,
            productId: config.productId,
            basePlanId: config.basePlanId,
            displayName: row.display_name,
            billingPeriod: row.billing_period,
            features: row.features
          };
        })
      }
    };
  })
});

app.http("getPremiumCatalogHealth", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "premium/catalog-health",
  handler: async () => {
    try {
      const plans = requireConfiguredPlans();
      const result = await query(
        "SELECT plan_key FROM premium_plans WHERE active = TRUE ORDER BY sort_order ASC"
      );
      const activeKeys = new Set(result.rows.map((row) => row.plan_key));
      const missingDbPlans = plans.filter((plan) => !activeKeys.has(plan.planKey)).map((plan) => plan.planKey);

      if (missingDbPlans.length > 0) {
        return {
          status: 503,
          jsonBody: { ok: false, configured: true, missingDbPlans }
        };
      }

      return {
        status: 200,
        jsonBody: {
          ok: true,
          configured: true,
          planCount: plans.length
        }
      };
    } catch (error) {
      return {
        status: error.statusCode || 503,
        jsonBody: {
          ok: false,
          configured: false,
          error: error.message
        }
      };
    }
  }
});
