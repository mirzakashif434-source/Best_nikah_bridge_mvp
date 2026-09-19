const { app } = require("@azure/functions");
const { checkBlobStorage } = require("./storage");

app.http("storageHealth", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "storage/health",
  handler: async () => {
    try {
      const result = await checkBlobStorage();
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
