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
require("./aiCompatibility");
require("./interests");
require("./chat");
require("./family");
require("./familyCircle");
require("./verification");
require("./photos");
require("./community");
require("./terms");
require("./safety");
require("./blocks");
require("./privacy");
require("./account");
require("./moderation");
require("./accountDeletionWeb");

require("./azureExternalAuth");

require("./rewardedAds");

require("./premiumPlans");
require("./seriousNikahPlus");

// Step 2: real Google Play subscription verification and paid entitlements.
require("./premiumPurchaseVerification");

require("./wallet");

require("./helpLine");

require("./ownerEarnings");
require("./featureParity");

require("./settings");

// Production 14-language UI translation service.
require("./localization");
