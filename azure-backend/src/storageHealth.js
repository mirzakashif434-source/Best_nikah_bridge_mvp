const { app } = require("@azure/functions");
const { checkBlobStorage } = require("./storage");

// Coalesce overlapping health probes and reuse only successful checks briefly.
// This avoids repeated billable container-existence requests from health monitors.
const STORAGE_HEALTH_CACHE_MS = 60 * 1000;
let lastHealthyResult = null;
let lastHealthyAt = 0;
let healthCheckInFlight = null;

async function getCachedStorageStatus() {
  const now = Date.now();
  if (lastHealthyResult && now - lastHealthyAt < STORAGE_HEALTH_CACHE_MS) {
    return lastHealthyResult;
  }
  if (!healthCheckInFlight) {
    healthCheckInFlight = checkBlobStorage()
      .then(result => {
        // Never cache unhealthy checks; allow immediate recovery on the next probe.
        if (result.profilePhotosExists && result.verificationDocumentsExists) {
          lastHealthyResult = result;
          lastHealthyAt = Date.now();
        } else {
          lastHealthyResult = null;
          lastHealthyAt = 0;
        }
        return result;
      })
      .finally(() => { healthCheckInFlight = null; });
  }
  return healthCheckInFlight;
}

app.http("storageHealth", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "storage/health",
  handler: async () => {
    try {
      const result = await getCachedStorageStatus();
      const ready = result.profilePhotosExists && result.verificationDocumentsExists;
      return {
        status: ready ? 200 : 503,
        jsonBody: {
          ok: ready,
          service: "best-nikah-bridge-azure-backend",
          storage: "azure-blob",
          connected: true,
          containersReady: ready,
          profilePhotosContainer: result.profilePhotosContainer,
          profilePhotosExists: result.profilePhotosExists,
          verificationDocumentsContainer: result.verificationDocumentsContainer,
          verificationDocumentsExists: result.verificationDocumentsExists
        }
      };
    } catch (error) {
      console.error("STORAGE_HEALTH_FAILED:", error.message);
      return {
        status: 503,
        jsonBody: {
          ok: false,
          service: "best-nikah-bridge-azure-backend",
          storage: "azure-blob",
          connected: false,
          containersReady: false
        }
      };
    }
  }
});
