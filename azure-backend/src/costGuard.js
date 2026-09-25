const { query } = require("./db");

let schemaReady = false;

function positiveInt(name, fallback) {
  const n = Number(process.env[name] || fallback);
  return Number.isFinite(n) && n > 0 ? Math.floor(n) : fallback;
}

function limitFor(planKey, bucket) {
  if (planKey === "premium_vip_60") {
    return positiveInt("AI_DAILY_LIMIT_VIP", 12);
  }
  if (planKey === "premium_plus_40" && bucket === "advanced_matching") {
    return positiveInt("AI_DAILY_LIMIT_PLUS", 5);
  }
  return 0;
}

async function ensureSchema() {
  if (schemaReady) return;
  await query(`
    CREATE TABLE IF NOT EXISTS ai_usage_daily (
      user_id TEXT NOT NULL,
      usage_day DATE NOT NULL DEFAULT CURRENT_DATE,
      bucket TEXT NOT NULL,
      request_count INTEGER NOT NULL DEFAULT 0,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
      PRIMARY KEY (user_id, usage_day, bucket)
    )
  `);
  schemaReady = true;
}

async function consumeAiQuota(userId, planKey, bucket) {
  const limit = limitFor(planKey, bucket);
  if (!limit) return { allowed: false, limit: 0, remaining: 0 };

  await ensureSchema();
  const result = await query(
    `INSERT INTO ai_usage_daily(user_id, usage_day, bucket, request_count)
     VALUES ($1, CURRENT_DATE, $2, 1)
     ON CONFLICT (user_id, usage_day, bucket)
     DO UPDATE SET
       request_count = ai_usage_daily.request_count + 1,
       updated_at = now()
     WHERE ai_usage_daily.request_count < $3
     RETURNING request_count`,
    [String(userId), bucket, limit]
  );

  if (!result.rows.length) return { allowed: false, limit, remaining: 0 };
  const used = Number(result.rows[0].request_count || 0);
  return { allowed: true, limit, remaining: Math.max(0, limit - used) };
}

function quotaResponse(quota) {
  return {
    status: 429,
    headers: { "Retry-After": "3600" },
    jsonBody: {
      ok: false,
      error: "DAILY_AI_LIMIT_REACHED",
      dailyLimit: quota.limit,
      remaining: 0
    }
  };
}

module.exports = { consumeAiQuota, quotaResponse };
