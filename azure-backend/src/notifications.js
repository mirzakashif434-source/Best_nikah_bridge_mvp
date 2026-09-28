const { app } = require("@azure/functions");
const crypto = require("crypto");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const text=(v,max)=>typeof v==="string"?v.trim().slice(0,max):"";

async function currentUser(authUser){
  const subject=text(authUser?.azure_subject,300);
  if(!subject){const e=new Error("AUTH_IDENTITY_REQUIRED");e.statusCode=401;throw e;}
  const r=await query("SELECT id,status FROM users WHERE azure_subject=$1 LIMIT 1",[subject]);
  if(!r.rows[0]){const e=new Error("ACCOUNT_NOT_FOUND");e.statusCode=404;throw e;}
  return r.rows[0];
}

function hubConfig(){
  return {
    namespace:text(process.env.AZURE_NOTIFICATION_HUB_NAMESPACE,200),
    hub:text(process.env.AZURE_NOTIFICATION_HUB_NAME,200),
    keyName:text(process.env.AZURE_NOTIFICATION_HUB_SAS_KEY_NAME,200),
    key:text(process.env.AZURE_NOTIFICATION_HUB_SAS_KEY,500)
  };
}

function sasToken(resourceUri,keyName,key){
  const expiry=Math.floor(Date.now()/1000)+3600;
  const encoded=encodeURIComponent(resourceUri.toLowerCase());
  const toSign=encoded+"\n"+expiry;
  const signature=encodeURIComponent(
    crypto.createHmac("sha256",key).update(toSign,"utf8").digest("base64")
  );
  return `SharedAccessSignature sr=${encoded}&sig=${signature}&se=${expiry}&skn=${encodeURIComponent(keyName)}`;
}

async function sendDirectFcmV1(deviceToken,notification){
  const cfg=hubConfig();
  if(!cfg.namespace||!cfg.hub||!cfg.keyName||!cfg.key) return {sent:false,reason:"HUB_NOT_CONFIGURED"};
  const resource=`https://${cfg.namespace}.servicebus.windows.net/${encodeURIComponent(cfg.hub)}`;
  const url=`${resource}/messages/?direct&api-version=2015-04`;
  const data={};
  for(const [k,v] of Object.entries(notification.data||{})) data[String(k)]=String(v??"");
  data.type=String(notification.type||"general");
  data.notificationId=String(notification.id||"");
  const payload={
    message:{
      notification:{title:notification.title,body:notification.body},
      data,
      android:{
        priority:"high",
        notification:{channel_id:"nikah_events",sound:"default"}
      }
    }
  };
  const response=await fetch(url,{
    method:"POST",
    headers:{
      Authorization:sasToken(resource,cfg.keyName,cfg.key),
      "Content-Type":"application/json;charset=utf-8",
      "ServiceBusNotification-DeviceHandle":deviceToken,
      "ServiceBusNotification-Format":"fcmV1",
      "x-ms-version":"2015-04"
    },
    body:JSON.stringify(payload)
  });
  if(!response.ok){
    const body=await response.text().catch(()=>"");
    throw new Error(`AZURE_NOTIFICATION_HUB_${response.status}: ${body.slice(0,300)}`);
  }
  return {sent:true};
}

async function createNotification(userId,type,title,body,data={}){
  const saved=await query(
    `INSERT INTO notifications(user_id,type,title,body,data)
     VALUES($1,$2,$3,$4,$5::jsonb)
     RETURNING id,user_id,type,title,body,data,created_at`,
    [userId,text(type,60)||"general",text(title,140)||"Best Nikah Bridge",text(body,500),JSON.stringify(data||{})]
  );
  const n=saved.rows[0];
  const devices=await query(
    "SELECT device_token FROM notification_devices WHERE user_id=$1 AND active=true ORDER BY last_seen_at DESC LIMIT 10",
    [userId]
  );
  for(const d of devices.rows){
    try{await sendDirectFcmV1(d.device_token,n);}
    catch(e){console.warn("PUSH_DELIVERY_FAILED",{notificationId:n.id,message:e.message});}
  }
  return n;
}

app.http("notificationDeviceRegister",{
  methods:["POST"],authLevel:"anonymous",route:"notifications/devices",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const user=await currentUser(authUser);
      if(user.status!=="active")return {status:403,jsonBody:{ok:false,error:"ACCOUNT_NOT_ACTIVE"}};
      const b=await request.json();
      const token=text(b.token,4096);
      const platform=text(b.platform,30).toLowerCase()||"android";
      if(!token)return {status:400,jsonBody:{ok:false,error:"DEVICE_TOKEN_REQUIRED"}};
      if(platform!=="android")return {status:400,jsonBody:{ok:false,error:"UNSUPPORTED_PLATFORM"}};
      await query(
        `INSERT INTO notification_devices(user_id,platform,device_token,active,last_seen_at)
         VALUES($1,$2,$3,true,now())
         ON CONFLICT(device_token) DO UPDATE SET
           user_id=EXCLUDED.user_id,platform=EXCLUDED.platform,active=true,last_seen_at=now(),updated_at=now()`,
        [user.id,platform,token]
      );
      return {status:200,jsonBody:{ok:true,registered:true}};
    }catch(e){
      context.error("NOTIFICATION_DEVICE_REGISTER_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NOTIFICATION_DEVICE_REGISTER_FAILED"}};
    }
  })
});

app.http("notificationDeviceUnregister",{
  methods:["DELETE"],authLevel:"anonymous",route:"notifications/devices/current",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const user=await currentUser(authUser);
      const b=await request.json().catch(()=>({}));
      const token=text(b.token,4096);
      if(!token)return {status:400,jsonBody:{ok:false,error:"DEVICE_TOKEN_REQUIRED"}};
      await query(
        "UPDATE notification_devices SET active=false,updated_at=now() WHERE user_id=$1 AND device_token=$2",
        [user.id,token]
      );
      return {status:200,jsonBody:{ok:true,unregistered:true}};
    }catch(e){
      context.error("NOTIFICATION_DEVICE_UNREGISTER_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NOTIFICATION_DEVICE_UNREGISTER_FAILED"}};
    }
  })
});

app.http("notificationList",{
  methods:["GET"],authLevel:"anonymous",route:"notifications",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const user=await currentUser(authUser);
      const r=await query(
        `SELECT id,type,title,body,data,read_at,created_at
           FROM notifications
          WHERE user_id=$1
          ORDER BY created_at DESC
          LIMIT 100`,[user.id]
      );
      const unread=await query("SELECT count(*)::int AS count FROM notifications WHERE user_id=$1 AND read_at IS NULL",[user.id]);
      return {status:200,jsonBody:{ok:true,unread:Number(unread.rows[0]?.count||0),notifications:r.rows}};
    }catch(e){
      context.error("NOTIFICATION_LIST_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NOTIFICATION_LIST_FAILED"}};
    }
  })
});

app.http("notificationRead",{
  methods:["PATCH"],authLevel:"anonymous",route:"notifications/{notificationId}/read",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const user=await currentUser(authUser);
      const id=(request.params&&request.params.notificationId)||context.triggerMetadata?.notificationId;
      const r=await query(
        "UPDATE notifications SET read_at=COALESCE(read_at,now()) WHERE id=$1 AND user_id=$2 RETURNING id,read_at",
        [id,user.id]
      );
      if(!r.rows[0])return {status:404,jsonBody:{ok:false,error:"NOTIFICATION_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true,notification:r.rows[0]}};
    }catch(e){
      context.error("NOTIFICATION_READ_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NOTIFICATION_READ_FAILED"}};
    }
  })
});

app.http("notificationReadAll",{
  methods:["PATCH"],authLevel:"anonymous",route:"notifications/read-all",
  handler:requireAuth(async(request,context,authUser)=>{
    try{
      const user=await currentUser(authUser);
      const r=await query("UPDATE notifications SET read_at=COALESCE(read_at,now()) WHERE user_id=$1 AND read_at IS NULL",[user.id]);
      return {status:200,jsonBody:{ok:true,markedRead:r.rowCount}};
    }catch(e){
      context.error("NOTIFICATION_READ_ALL_FAILED",e);
      return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"NOTIFICATION_READ_ALL_FAILED"}};
    }
  })
});

module.exports={createNotification,sendDirectFcmV1};
