// Read-only Owner Dashboard access. Does not grant admin/moderator or payout write access.
const { query } = require("./db");

const REPORT_VIEWER_EMAIL = "mirzakashif121212@gmail.com";

async function requireOwnerReportAccess(user) {
  const subject = String(user.azure_subject || "").trim();
  if (!subject) {
    const error = new Error("ADMIN_REQUIRED"); error.statusCode = 403; throw error;
  }
  const result = await query(
    "SELECT id,email,role,status FROM users WHERE azure_subject=$1 AND status='active' LIMIT 1",
    [subject]
  );
  const account = result.rows[0];
  const role = account?.role;
  const existingAdmin = role === "admin" || role === "moderator";
  // Verified Azure subject must resolve to this exact active database account.
  // This exception authorizes only report GET routes using this function.
  const reportViewer = role === "user" &&
    String(account?.email || "").trim().toLowerCase() === REPORT_VIEWER_EMAIL &&
    String(user.email || "").trim().toLowerCase() === REPORT_VIEWER_EMAIL;
  if (!existingAdmin && !reportViewer) {
    const error = new Error("ADMIN_REQUIRED"); error.statusCode = 403; throw error;
  }
  return account;
}

module.exports = { requireOwnerReportAccess };
