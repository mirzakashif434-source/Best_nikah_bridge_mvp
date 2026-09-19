const { app } = require("@azure/functions");
const { checkDatabase } = require("./db");

app.http("databaseHealth", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "database/health",
  handler: async () => {
    try {
      const result = await checkDatabase();
      return {
        status: 200,
        jsonBody: {
          ok: true,
          service: "best-nikah-bridge-azure-backend",
          database: "postgresql",
          connected: true,
          latencyMs: result.latencyMs,
          serverTime: result.serverTime
        }
      };
    } catch (error) {
      console.error("DATABASE_HEALTH_FAILED:", error.message);
      return {
        status: 503,
        jsonBody: {
          ok: false,
          service: "best-nikah-bridge-azure-backend",
          database: "postgresql",
          connected: false
        }
      };
    }
  }
});
