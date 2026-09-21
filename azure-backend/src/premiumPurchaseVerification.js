const { app } = require("@azure/functions");
const { SignJWT, importPKCS8 } = require("jose");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const PACKAGE_NAME = () => String(process.env.PLAY_PACKAGE_NAME || "com.nikahbridge").trim();

const PLAN_CONFIG = [
  { planKey: "premium_basic_20", productIdEnv: "PLAY_PREMIUM_BASIC_PRODUCT_ID", basePlanIdEnv: "PLAY_PREMIUM_BASIC_BASE_PLAN_ID", title: "Premium 20" },
  { planKey: "premium_plus_40", productIdEnv: "PLAY_PREMIUM_PLUS_PRODUCT_ID", basePlanIdEnv: "PLAY_PREMIUM_PLUS_BASE_PLAN_ID", title: "Premium 40" },
  { planKey: "premium_vip_60", productIdEnv: "PLAY_PREMIUM_VIP_PRODUCT_ID", basePlanIdEnv: "PLAY_PREMIUM_VIP_BASE_PLAN_ID", title: "Premium 60" }
];

function configuredPlans() {
  return PLAN_CONFIG.map(p => ({
    planKey: p.planKey,
    productId: String(process.env[p.productIdEnv] || "").trim(),
    basePlanId: String(process.env[p.basePlanIdEnv] || "").trim(),
    title: p.title
  }));
}

function requirePlayCredentials() {
  const email = String(process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_EMAIL || "").trim();
  const privateKey = String(process.env.GOOGLE_PLAY_SERVICE_ACCOUNT_PRIVATE_KEY || "").replace(/\\n/g, "\n").trim();
  if (!email || !privateKey) {
    const e = new Error("Google Play service account credentials are not configured.");
    e.statusCode = 503;
    throw e;
  }
  return { email, privateKey };
}

async function accessToken() {
  const { email, privateKey } = requirePlayCredentials();
  const key = await importPKCS8(privateKey, "RS256");
  const now = Math.floor(Date.now() / 1000);
  const assertion = await new SignJWT({
    iss: email,
    scope: "https://www.googleapis.com/auth/androidpublisher",
    aud: "https://oauth2.googleapis.com/token"
  })
    .setProtectedHeader({ alg: "RS256", typ: "JWT" })
    .setIssuedAt(now)
    .setExpirationTime(now + 3600)
    .sign(key);

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion
    })
  });
  const body = await response.json();
  if (!response.ok || !body.access_token) {
    throw new Error("Google Play OAuth token request failed.");
  }
  return body.access_token;
}

async function googleApi(path, options = {}) {
  const token = await accessToken();
  const response = await fetch("https://androidpublisher.googleapis.com/androidpublisher/v3/" + path, {
    ...options,
    headers: {
      Accept: "application/json",
      Authorization: "Bearer " + token,
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  let body = {};
  try { body = text ? JSON.parse(text) : {}; } catch {}
  if (!response.ok) {
    const e = new Error("Google Play API request failed.");
    e.statusCode = response.status;
    e.googleStatus = body?.error?.status || null;
    throw e;
  }
  return body;
}

function planFor(productId, basePlanId) {
  return configuredPlans().find(p => p.productId === productId && (!basePlanId || p.basePlanId === basePlanId));
}

function extractLineItem(purchase, productId) {
  return (purchase.lineItems || []).find(item => item.productId === productId && item.expiryTime);
}

async function currentUser(authUser) {
  const identityColumn = authUser.auth_provider === "azure_external_id" ? "azure_subject" : "firebase_uid";
  const identityValue = authUser.auth_provider === "azure_external_id" ? authUser.azure_subject : authUser.uid;
  const result = await query(
    `SELECT id, status FROM users WHERE ${identityColumn} = $1 LIMIT 1`,
    [identityValue]
  );
  const user = result.rows[0];
  if (!user || user.status !== "active") {
    const e = new Error("Active user account required.");
    e.statusCode = 403;
    throw e;
  }
  return user;
}

app.http("verifyPremiumPurchase", {
  methods: ["POST"],
  authLevel: "anonymous",
  route: "premium/purchases/verify",
  handler: requireAuth(async (request, context, firebaseUser) => {
    try {
      const user = await currentUser(firebaseUser);
      const body = await request.json();
      const productId = String(body?.productId || "").trim();
      const purchaseToken = String(body?.purchaseToken || "").trim();

      if (!productId || !purchaseToken) {
        return { status: 400, jsonBody: { ok: false, error: "productId and purchaseToken are required." } };
      }

      const plans = configuredPlans();
      const allowed = plans.find(p => p.productId === productId);
      if (!allowed) {
        return { status: 400, jsonBody: { ok: false, error: "Unknown premium product." } };
      }

      const purchase = await googleApi(
        "applications/" + encodeURIComponent(PACKAGE_NAME()) +
        "/purchases/subscriptionsv2/tokens/" + encodeURIComponent(purchaseToken)
      );

      if (purchase.packageName && purchase.packageName !== PACKAGE_NAME()) {
        return { status: 400, jsonBody: { ok: false, error: "Package name mismatch." } };
      }

      const item = extractLineItem(purchase, productId);
      const state = String(purchase.subscriptionState || "");
      const activeStates = new Set(["SUBSCRIPTION_STATE_ACTIVE", "SUBSCRIPTION_STATE_IN_GRACE_PERIOD"]);
      if (!item || !activeStates.has(state)) {
        return { status: 402, jsonBody: { ok: false, active: false, subscriptionState: state } };
      }

      const expiryTime = new Date(item.expiryTime);
      if (!Number.isFinite(expiryTime.getTime()) || expiryTime <= new Date()) {
        return { status: 402, jsonBody: { ok: false, active: false, error: "Subscription is expired." } };
      }

      const purchaseTokenHash = require("crypto").createHash("sha256").update(purchaseToken).digest("hex");
      const orderId = String(purchase.latestOrderId || item.latestSuccessfulOrderId || "").trim();

      await query("BEGIN");
      try {
        await query(
          `INSERT INTO premium_purchase_tokens
             (purchase_token_hash, user_id, product_id, base_plan_id, order_id, subscription_state, expiry_time, last_verified_at)
           VALUES ($1,$2,$3,$4,$5,$6,$7,now())
           ON CONFLICT (purchase_token_hash) DO UPDATE SET
             user_id=EXCLUDED.user_id, product_id=EXCLUDED.product_id, base_plan_id=EXCLUDED.base_plan_id,
             order_id=EXCLUDED.order_id, subscription_state=EXCLUDED.subscription_state,
             expiry_time=EXCLUDED.expiry_time, last_verified_at=now()`,
          [purchaseTokenHash, user.id, productId, allowed.basePlanId, orderId || null, state, expiryTime.toISOString()]
        );

        await query(
          `INSERT INTO premium_entitlements
             (user_id, plan_key, product_id, base_plan_id, purchase_token_hash, order_id, status, expires_at, updated_at)
           VALUES ($1,$2,$3,$4,$5,$6,'active',$7,now())
           ON CONFLICT (user_id) DO UPDATE SET
             plan_key=EXCLUDED.plan_key, product_id=EXCLUDED.product_id, base_plan_id=EXCLUDED.base_plan_id,
             purchase_token_hash=EXCLUDED.purchase_token_hash, order_id=EXCLUDED.order_id,
             status='active', expires_at=EXCLUDED.expires_at, updated_at=now()`,
          [user.id, allowed.planKey, productId, allowed.basePlanId, purchaseTokenHash, orderId || null, expiryTime.toISOString()]
        );

        await query(
          "UPDATE entitlements SET paid_tier = $2, updated_at = now() WHERE user_id = $1",
          [user.id, allowed.planKey]
        );

        await query("COMMIT");
      } catch (e) {
        await query("ROLLBACK");
        throw e;
      }

      if (String(purchase.acknowledgementState || "") !== "ACKNOWLEDGEMENT_STATE_ACKNOWLEDGED") {
        try {
          await googleApi(
            "applications/" + encodeURIComponent(PACKAGE_NAME()) +
            "/purchases/subscriptions/" + encodeURIComponent(productId) +
            "/tokens/" + encodeURIComponent(purchaseToken) + ":acknowledge",
            { method: "POST", headers: { "content-type": "application/json" }, body: "{}" }
          );
        } catch (ackError) {
          console.error("PREMIUM_ACKNOWLEDGE_FAILED:", ackError.message);
        }
      }

      return {
        status: 200,
        jsonBody: {
          ok: true,
          active: true,
          planKey: allowed.planKey,
          productId,
          basePlanId: allowed.basePlanId,
          expiresAt: expiryTime.toISOString(),
          subscriptionState: state
        }
      };
    } catch (error) {
      console.error("PREMIUM_PURCHASE_VERIFY_FAILED:", error.message);
      return { status: error.statusCode || 500, jsonBody: { ok: false, error: error.message } };
    }
  })
});

app.http("premiumEntitlement", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "premium/entitlement",
  handler: requireAuth(async (request, context, firebaseUser) => {
    const user = await currentUser(firebaseUser.uid);
    const result = await query(
      "SELECT plan_key, product_id, base_plan_id, status, expires_at FROM premium_entitlements WHERE user_id = $1 LIMIT 1",
      [user.id]
    );
    const entitlement = result.rows[0];
    const active = Boolean(entitlement && entitlement.status === "active" && new Date(entitlement.expires_at) > new Date());
    return { status: 200, jsonBody: { ok: true, active, entitlement: active ? entitlement : null } };
  })
});
