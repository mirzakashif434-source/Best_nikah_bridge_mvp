const { app } = require("@azure/functions");
const { requireAuth } = require("./auth");

app.http("authHealth", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "auth/health",
  handler: requireAuth(async (request, context, user) => ({
    status: 200,
    jsonBody: { ok: true, authenticated: true, uid: user.azure_subject, provider: "azure_external_id" }
  }))
});