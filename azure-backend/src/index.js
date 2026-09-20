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

// Additive production readiness endpoints. Existing health endpoint remains unchanged.
require("./databaseHealth");
require("./storageHealth");

// Additive authenticated API foundation. Existing endpoints remain unchanged.
require("./authHealth");
require("./profile");
require("./matches");
require("./matchDetail");
require("./aiNikahAssistant");
require("./interests");
require("./chat");
require("./family");
require("./verification");
require("./photos");
require("./safety");
require("./blocks");
require("./privacy");
require("./account");
require("./moderation");
require("./accountDeletionWeb");

// Firebase migration #1: additive Azure Entra External ID authentication endpoints.
require("./azureExternalAuth");

require("./rewardedAds");

// Firebase migration #2: additive production Premium plan catalog.
require("./premiumPlans");


// Step 2: real Google Play subscription verification and paid entitlements.
require("./premiumPurchaseVerification");
