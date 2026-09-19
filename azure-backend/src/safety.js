const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const text = (v, max) => typeof v === "string" ? v.trim().slice(0, max) : "";

async function ensureUser(user) {
  const email = text(user.email, 320).toLowerCase();
  if (!user.uid || !email) {
    const e = new Error("AUTH_IDENTITY_REQUIRED");
    e.statusCode = 401;
    throw e;
  }
  const r = await query(
    "INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status",
    [user.uid, email, Boolean(user.email_verified)]
  );
  if (r.rows[0].status !== "active") {
    const e = new Error("USER_NOT_ACTIVE");
    e.statusCode = 403;
    throw e;
  }
  return r.rows[0];
}

async function resolveUserId(identifier) {
  const value = text(identifier, 128);
  if (!value) return null;
  const r = await query(
    "SELECT id FROM users WHERE firebase_uid=$1 OR id::text=$1 LIMIT 1",
    [value]
  );
  return r.rows[0]?.id || null;
}

app.http("safetyReportCreate", {
  methods: ["POST"],
  authLevel: "anonymous",
  route: "safety/reports",
  handler: requireAuth(async (request, context, user) => {
    try {
      const me = await ensureUser(user);
      const body = await request.json();
      const reportedUserId = await resolveUserId(body.reportedUserId || body.reportedUserUid);
      if (!reportedUserId) return { status: 404, jsonBody: { ok: false, error: "REPORTED_USER_NOT_FOUND" } };
      if (reportedUserId === me.id) return { status: 400, jsonBody: { ok: false, error: "CANNOT_REPORT_SELF" } };

      const reason = text(body.reason, 80).toLowerCase();
      const allowed = new Set(["harassment", "scam", "impersonation", "inappropriate_content", "unsafe_request", "other"]);
      if (!allowed.has(reason)) {
        return { status: 400, jsonBody: { ok: false, error: "INVALID_REPORT_REASON" } };
      }
      const details = text(body.details, 2000);
      const r = await query(
        "INSERT INTO safety_reports(reporter_user_id,reported_user_id,reason,details) VALUES($1,$2,$3,$4) RETURNING id,reported_user_id,reason,details,status,created_at",
        [me.id, reportedUserId, reason, details || null]
      );
      return { status: 201, jsonBody: { ok: true, report: r.rows[0] } };
    } catch (e) {
      context.error("SAFETY_REPORT_CREATE_FAILED", e);
      return { status: e.statusCode || 500, jsonBody: { ok: false, error: e.statusCode ? e.message : "SAFETY_REPORT_CREATE_FAILED" } };
    }
  })
});

app.http("safetyReportsList", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "safety/reports/mine",
  handler: requireAuth(async (request, context, user) => {
    try {
      const me = await ensureUser(user);
      const r = await query(
        "SELECT id,reported_user_id,reason,details,status,created_at,resolved_at FROM safety_reports WHERE reporter_user_id=$1 ORDER BY created_at DESC LIMIT 100",
        [me.id]
      );
      return { status: 200, jsonBody: { ok: true, reports: r.rows } };
    } catch (e) {
      context.error("SAFETY_REPORTS_LIST_FAILED", e);
      return { status: e.statusCode || 500, jsonBody: { ok: false, error: e.statusCode ? e.message : "SAFETY_REPORTS_LIST_FAILED" } };
    }
  })
});
