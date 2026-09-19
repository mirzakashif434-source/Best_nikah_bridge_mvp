const { app } = require("@azure/functions");
const { query } = require("./db");
const { requireAuth } = require("./auth");
const { getProfilePhotosContainer, getVerificationDocumentsContainer } = require("./storage");
const { getFirebaseAdmin } = require("./auth");

async function deleteUserData(user, context) {
  const r = await query("SELECT id FROM users WHERE firebase_uid=$1 LIMIT 1",[user.uid]);
  if(!r.rows[0]) return {status:404,jsonBody:{ok:false,error:"ACCOUNT_NOT_FOUND"}};
  const userId=r.rows[0].id;
  const photos=await query("SELECT blob_key FROM photos WHERE user_id=$1",[userId]);
  const docs=await query("SELECT document_blob_key FROM verifications WHERE user_id=$1 AND document_blob_key IS NOT NULL",[userId]);
  for(const x of photos.rows) if(x.blob_key) await getProfilePhotosContainer().getBlockBlobClient(x.blob_key).deleteIfExists({deleteSnapshots:"include"});
  for(const x of docs.rows) if(x.document_blob_key) await getVerificationDocumentsContainer().getBlockBlobClient(x.document_blob_key).deleteIfExists({deleteSnapshots:"include"});
  await query("DELETE FROM users WHERE id=$1",[userId]);
  try { await getFirebaseAdmin().auth().deleteUser(user.uid); } catch(e){ context.error("EXTERNAL_ACCOUNT_DELETE_AUTH_CLEANUP",e); }
  return {status:200,jsonBody:{ok:true,deleted:true}};
}

app.http("accountDeletionWeb",{
  methods:["GET"],authLevel:"anonymous",route:"account-deletion",
  handler:async()=>{
    const html='<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Account Deletion</title></head><body><main><h1>Delete your account</h1><p>For security, open the app and use Settings → Delete Account. This page is the official deletion resource for the app.</p><p>Your profile, matches, messages, family links, verification records and stored photos are permanently deleted when the authenticated deletion request is completed.</p><p>If you cannot access the app, contact the support address published on the Google Play listing.</p></main></body></html>';
    return {status:200,headers:{"Content-Type":"text/html; charset=utf-8","Cache-Control":"no-store"},body:html};
  }
});

app.http("accountDeletionAuthenticatedWeb",{
  methods:["POST"],authLevel:"anonymous",route:"account-deletion",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const body=await request.json().catch(()=>({}));
      if(body.confirm!=="DELETE_MY_ACCOUNT") return {status:400,jsonBody:{ok:false,error:"CONFIRMATION_REQUIRED"}};
      return await deleteUserData(user,context);
    }catch(e){context.error("EXTERNAL_ACCOUNT_DELETE_FAILED",e);return {status:500,jsonBody:{ok:false,error:"ACCOUNT_DELETE_FAILED"}};
    }
  })
});
