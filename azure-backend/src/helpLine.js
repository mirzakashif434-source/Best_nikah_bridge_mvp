const { app } = require("@azure/functions");
const { DefaultAzureCredential } = require("@azure/identity");
const { query } = require("./db");
const { requireAuth } = require("./auth");

const credential = new DefaultAzureCredential();
const MAX_Q = 4000;
const MAX_A = 8000;
const clean = (v,max) => typeof v === "string" ? v.trim().slice(0,max) : "";

const sensitivePattern = /(password|otp|one[- ]time|bank|iban|card|refund|payment dispute|verification decision|ban|blocked|report|legal|police|harass|abuse|self[- ]harm|suicide)/i;

async function ensureUser(user) {
  const email = clean(user.email,320).toLowerCase();
  if (!user.uid || !email) { const e=new Error("AUTH_IDENTITY_REQUIRED"); e.statusCode=401; throw e; }
  const r=await query(
    "INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status",
    [user.uid,email,Boolean(user.email_verified)]
  );
  if(r.rows[0].status!=="active"){const e=new Error("USER_NOT_ACTIVE");e.statusCode=403;throw e;}
  return r.rows[0];
}

async function requireAdmin(user) {
  const me=await ensureUser(user);
  const r=await query("SELECT id,role FROM users WHERE id=$1 AND status='active'",[me.id]);
  if(!r.rows[0] || !["admin","moderator"].includes(r.rows[0].role)){const e=new Error("ADMIN_REQUIRED");e.statusCode=403;throw e;}
  return r.rows[0];
}

async function azureAnswer(question, context) {
  const endpoint=(process.env.AZURE_AI_ENDPOINT||"").trim().replace(/\/$/,"");
  const model=(process.env.AZURE_AI_MODEL||"").trim();
  if(!endpoint || !model) throw new Error("AI_SERVICE_NOT_CONFIGURED");
  const token=await credential.getToken("https://cognitiveservices.azure.com/.default");
  if(!token?.token) throw new Error("AZURE_AI_TOKEN_UNAVAILABLE");
  const response=await fetch(endpoint+"/openai/v1/chat/completions",{
    method:"POST",
    headers:{"Authorization":"Bearer "+token.token,"Content-Type":"application/json"},
    body:JSON.stringify({
      model,
      messages:[
        {role:"system",content:"You are the production Help Assistant for a serious Muslim matrimonial app. Answer concise practical questions about using the app. Be respectful and privacy-conscious. Never ask for passwords, OTPs, bank/card details, identity documents, or secrets. Do not provide binding religious, legal, medical, financial, or safety guarantees. For sensitive or human-review matters, do not answer; the server will route them to human support."},
        {role:"user",content:question}
      ],
      temperature:0.2,max_tokens:700
    })
  });
  const data=await response.json().catch(()=>({}));
  if(!response.ok) { context.error("AZURE_HELP_AI_FAILED",response.status,data?.error||data); throw new Error("AZURE_HELP_AI_FAILED"); }
  const answer=data?.choices?.[0]?.message?.content;
  if(typeof answer!=="string" || !answer.trim()) throw new Error("AZURE_HELP_AI_EMPTY");
  return answer.trim();
}

app.http("helpLineAsk",{
  methods:["POST"],authLevel:"anonymous",route:"help/ask",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const me=await ensureUser(user);
      const body=await request.json();
      const question=clean(body?.question,MAX_Q);
      if(!question)return {status:400,jsonBody:{ok:false,error:"QUESTION_REQUIRED"}};
      if(sensitivePattern.test(question)){
        const r=await query("INSERT INTO help_line_tickets(user_id,question,human_required,status,human_reply_target_at) VALUES($1,$2,true,'awaiting_human',now()+interval '24 hours') RETURNING id,status,human_reply_target_at,created_at",[me.id,question]);
        return {status:201,jsonBody:{ok:true,ticketId:r.rows[0].id,answer:"Aap ki request human support ko bhej di gayi hai. Hamara target hai ke aapko 24 ghanton ke andar reply mile.",aiAnswered:false,humanRequired:true,humanReplySlaHours:24,ticket:r.rows[0]}};
      }
      try{
        const answer=await azureAnswer(question,context);
        const r=await query("INSERT INTO help_line_tickets(user_id,question,ai_answer,ai_answered,human_required,status,created_at) VALUES($1,$2,$3,true,false,'ai_answered',now()) RETURNING id,status,created_at",[me.id,question,answer]);
        return {status:201,jsonBody:{ok:true,ticketId:r.rows[0].id,answer,aiAnswered:true,humanRequired:false,ticket:r.rows[0]}};
      }catch(e){
        context.error("HELP_AI_FALLBACK_TO_HUMAN",e);
        const r=await query("INSERT INTO help_line_tickets(user_id,question,human_required,status,human_reply_target_at) VALUES($1,$2,true,'awaiting_human',now()+interval '24 hours') RETURNING id,status,human_reply_target_at,created_at",[me.id,question]);
        return {status:201,jsonBody:{ok:true,ticketId:r.rows[0].id,answer:"Aap ki request human support ko bhej di gayi hai. Hamara target hai ke aapko 24 ghanton ke andar reply mile.",aiAnswered:false,humanRequired:true,humanReplySlaHours:24,ticket:r.rows[0]}};
      }
    }catch(e){context.error("HELP_LINE_ASK_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"HELP_LINE_ASK_FAILED"}};}
  })
});

app.http("helpLineAdminList",{
  methods:["GET"],authLevel:"anonymous",route:"admin/help/tickets",
  handler:requireAuth(async(request,context,user)=>{
    try{
      await requireAdmin(user);
      const r=await query("SELECT t.id,u.firebase_uid AS uid,t.question,t.ai_answer,t.ai_answered,t.human_required,t.status,t.created_at,t.human_reply_target_at,t.human_reply,t.human_replied_at,t.replied_by FROM help_line_tickets t JOIN users u ON u.id=t.user_id ORDER BY t.created_at DESC LIMIT 200");
      return {status:200,jsonBody:{ok:true,tickets:r.rows}};
    }catch(e){context.error("HELP_ADMIN_LIST_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"HELP_ADMIN_LIST_FAILED"}};}
  })
});

app.http("helpLineAdminReply",{
  methods:["PATCH"],authLevel:"anonymous",route:"admin/help/tickets/{ticketId}",
  handler:requireAuth(async(request,context,user)=>{
    try{
      const admin=await requireAdmin(user);
      const body=await request.json();
      const reply=clean(body?.reply,MAX_A);
      if(!reply)return {status:400,jsonBody:{ok:false,error:"REPLY_REQUIRED"}};
      const r=await query("UPDATE help_line_tickets SET human_reply=$2,human_replied_at=now(),replied_by=$3,status='human_replied',updated_at=now() WHERE id=$1 RETURNING id,status,human_replied_at",[request.params.ticketId,reply,admin.id]);
      if(!r.rows[0])return {status:404,jsonBody:{ok:false,error:"HELP_TICKET_NOT_FOUND"}};
      return {status:200,jsonBody:{ok:true,ticket:r.rows[0]}};
    }catch(e){context.error("HELP_ADMIN_REPLY_FAILED",e);return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"HELP_ADMIN_REPLY_FAILED"}};}
  })
});
