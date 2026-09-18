const { app } = require("@azure/functions");

app.http("health", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "health",
  handler: async () => ({
    status: 200,
    jsonBody: {
      ok: true,
      service: "best-nikah-bridge-azure-backend",
      environment: "azure",
      version: "1.0.0"
    }
  })
});
