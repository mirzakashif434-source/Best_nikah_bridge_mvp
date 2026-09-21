const { app } = require("@azure/functions");
const crypto = require("crypto");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const REWARDED_UNIT = () => String(process.env.ADMOB_REWARDED_AD_UNIT_ID || "").trim();

function numericAdUnit(id) {
  const value = String(id || "").trim();
  const i = value.lastIndexOf("/");
  return i >= 0 ? value.slice(i + 1) : value;
}

function requireAdMobUnit() {
  const unit = REWARDED_UNIT();
  if (!/^ca-app-pub-\d{16}\/\d+$/.test(unit)) {
    throw new Error("ADMOB_REWARDED_AD_UNIT_ID is not configured.");
  }
  return unit;
}

async function activeRewardedUser(authUser) {
  const result = await query(
    `SELECT u.id, u.email, u.email_verified_at, u.status, p.profile_completed, p.is_visible
       FROM users u
       LEFT JOIN profiles p ON p.user_id = u.id
      WHERE u.azure_subject = $1
      LIMIT 1`,
    [authUser.azure_subject]
  );
  const user = result.rows[0];
  if (!user || user.status !== "active" || user.profile_completed !== true || user.is_visible !== true) {
    const error = new Error("An active profile is required.");
    error.statusCode = 412;
    throw error;
  }
  return user;
}

app.http("getRewardedAdConfig", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "admob/rewarded/config",
  handler: requireAuth(async (request, context, authUser) => {
    if (authUser.auth_provider !== "azure_external_id" || !authUser.azure_subject) return { status: 401, jsonBody: { ok: false, error: "AZURE_AUTH_REQUIRED" } };
    await activeRewardedUser(authUser);
    const unit = requireAdMobUnit();
    return {
      status: 200,
      jsonBody: {
        ok: true,
        configured: true,
        rewardedAdUnitId: unit,
        rewardedAdUnitNumericId: numericAdUnit(unit),
        azureSubject: String(authUser.azure_subject),
        dailyRewardLimit: 2,
        rewardMessageCredits: 1
      }
    };
  })
});

async function verifierKeys() {
  const response = await fetch("https://www.gstatic.com/admob/reward/verifier-keys.json");
  if (!response.ok) throw new Error(`AdMob verifier key server returned ${response.status}`);
  return response.json();
}

async function verifySsvUrl(url) {
  const qIndex = url.indexOf("?");
  if (qIndex < 0) throw new Error("Missing query.");
  const queryString = url.slice(qIndex + 1);
  const sigMarker = "&signature=";
  const sigIndex = queryString.indexOf(sigMarker);
  if (sigIndex < 0) throw new Error("Missing signature.");
  const signedData = queryString.slice(0, sigIndex);
  const tail = queryString.slice(sigIndex + 1);
  const keyMarker = "&key_id=";
  const keyIndex = tail.indexOf(keyMarker);
  if (keyIndex < 0 || !tail.startsWith("signature=")) throw new Error("Invalid signature parameters.");
  const signatureText = tail.slice("signature=".length, keyIndex);
  const keyId = Number(tail.slice(keyIndex + keyMarker.length));
  if (!Number.isSafeInteger(keyId)) throw new Error("Invalid key id.");
  const signature = Buffer.from(signatureText.replace(/-/g, "+").replace(/_/g, "/"), "base64");
  const keys = await verifierKeys();
  const found = Array.isArray(keys.keys) ? keys.keys.find(k => Number(k.keyId) === keyId) : null;
  if (!found?.pem) throw new Error("Unknown AdMob verification key.");
  if (!crypto.verify("sha256", Buffer.from(signedData, "utf8"), found.pem, signature)) {
    throw new Error("Invalid AdMob SSV signature.");
  }
  return new URLSearchParams(queryString);
}

app.http("admobRewardedSsv", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "admob/rewarded/ssv",
  handler: async (request) => {
    try {
      const originalUrl = String(request.url || "");
      const params = await verifySsvUrl(originalUrl);
      const uid = String(params.get("user_id") || "").trim();
      const customUid = String(params.get("custom_data") || "").trim();
      const transactionId = String(params.get("transaction_id") || "").trim();
      const adUnit = String(params.get("ad_unit") || "").trim();
      const timestamp = Number(params.get("timestamp") || 0);

      if (!transactionId || !adUnit || !timestamp) {
        throw new Error("Missing required SSV fields.");
      }

      // AdMob's dashboard verification can omit user_id/custom_data. A valid
      // signed callback without an identity is acknowledged for URL verification
      // only; no reward is granted. Real app callbacks always include both
      // values because RewardedMessageActivity sets them before showing the ad.
      if (!uid || customUid !== uid) {
        return { status: 200, body: "OK" };
      }
      if (Math.abs(Date.now() - timestamp) > 24 * 60 * 60 * 1000) {
        throw new Error("Stale AdMob reward callback.");
      }

      const configuredUnit = requireAdMobUnit();
      if (numericAdUnit(configuredUnit) !== adUnit) {
        throw new Error("Unknown rewarded ad unit.");
      }

      const user = await activeRewardedUser({ azure_subject: uid });
      const rewardAmount = Number(params.get("reward_amount") || 0);
      const rewardItem = String(params.get("reward_item") || "message_credit");

      await query("BEGIN");
      try {
        const existing = await query(
          "SELECT transaction_id FROM rewarded_ad_transactions WHERE transaction_id = $1 FOR UPDATE",
          [transactionId]
        );
        if (existing.rowCount > 0) {
          await query("COMMIT");
          return { status: 200, body: "OK" };
        }

        const today = new Date().toISOString().slice(0, 10);
        const daily = await query(
          "SELECT claim_count FROM daily_reward_claims WHERE user_id = $1 AND claim_date = $2 FOR UPDATE",
          [user.id, today]
        );
        const used = Number(daily.rows[0]?.claim_count || 0);
        if (used >= 2) throw new Error("Daily rewarded limit reached.");

        await query(
          `INSERT INTO rewarded_ad_transactions
             (transaction_id, user_id, ad_unit, reward_amount, reward_item, verified_at)
           VALUES ($1,$2,$3,$4,$5,now())`,
          [transactionId, user.id, adUnit, rewardAmount, rewardItem]
        );

        await query(
          `INSERT INTO daily_reward_claims (user_id, claim_date, claim_count, updated_at)
           VALUES ($1,$2,1,now())
           ON CONFLICT (user_id, claim_date)
           DO UPDATE SET claim_count = daily_reward_claims.claim_count + 1, updated_at = now()`,
          [user.id, today]
        );

        await query(
          `INSERT INTO entitlements (user_id, message_credits, updated_at)
           VALUES ($1,1,now())
           ON CONFLICT (user_id)
           DO UPDATE SET message_credits = entitlements.message_credits + 1, updated_at = now()`,
          [user.id]
        );

        await query("COMMIT");
      } catch (error) {
        await query("ROLLBACK");
        throw error;
      }

      return { status: 200, body: "OK" };
    } catch (error) {
      console.error("ADMOB_SSV_REJECTED:", error.message);
      return { status: 400, body: "Invalid reward callback" };
    }
  }
});
