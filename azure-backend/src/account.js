const { app } = require("@azure/functions");
const { query, getPool } = require("./db");
const { requireAuth, getFirebaseAdmin } = require("./auth");
const { getProfilePhotosContainer, getVerificationDocumentsContainer } = require("./storage");

async function deleteBlobs(container, keys) {
  for (const key of keys) {
    if (!key) continue;
    try { await container.getBlockBlobClient(key).deleteIfExists({ deleteSnapshots: "include" }); }
    catch (e) { throw new Error("ACCOUNT_STORAGE_DELETE_FAILED"); }
  }
}

app.http("accountDelete", {
  methods: ["DELETE"],
  authLevel: "anonymous",
  route: "account",
  handler: requireAuth(async (request, context, user) => {
    const pool = getPool();
    const client = await pool.connect();
    try {
      const userResult = await client.query(
        "SELECT id,status FROM users WHERE firebase_uid=$1 LIMIT 1",
        [user.uid]
      );
      if (!userResult.rows[0]) return { status: 404, jsonBody: { ok:false, error:"ACCOUNT_NOT_FOUND" } };
      const userId = userResult.rows[0].id;

      const photos = await client.query("SELECT blob_key FROM photos WHERE user_id=$1", [userId]);
      const documents = await client.query("SELECT document_blob_key FROM verifications WHERE user_id=$1 AND document_blob_key IS NOT NULL", [userId]);

      await deleteBlobs(getProfilePhotosContainer(), photos.rows.map(x => x.blob_key));
      await deleteBlobs(getVerificationDocumentsContainer(), documents.rows.map(x => x.document_blob_key));

      await client.query("BEGIN");
      await client.query("DELETE FROM users WHERE id=$1", [userId]);
      await client.query("COMMIT");

      // Azure External ID owns Azure-authenticated accounts. Do not call Firebase Admin
      // for those users; the database/storage deletion above is the authoritative cleanup.
      if (user.auth_provider === "firebase") {
        try {
          await getFirebaseAdmin().auth().deleteUser(user.uid);
        } catch (firebaseError) {
          context.error("FIREBASE_ACCOUNT_DELETE_FAILED", firebaseError);
          return {
            status: 202,
            jsonBody: {
              ok: true,
              deleted: true,
              authCleanupPending: true
            }
          };
        }
      }

      return { status:200, jsonBody:{ok:true,deleted:true,authCleanupPending:false} };
    } catch (e) {
      await client.query("ROLLBACK").catch(()=>{});
      context.error("ACCOUNT_DELETE_FAILED", e);
      return { status:e.statusCode||500, jsonBody:{ok:false,error:e.statusCode?e.message:"ACCOUNT_DELETE_FAILED"} };
    } finally {
      client.release();
    }
  })
});
