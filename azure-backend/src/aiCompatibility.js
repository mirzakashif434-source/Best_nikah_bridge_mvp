const { app } = require("@azure/functions");
const { DefaultAzureCredential } = require("@azure/identity");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");

const credential = new DefaultAzureCredential();
const MAX = 8000;
const LANGUAGE_NAMES={en:"English",ur:"Urdu",ar:"Arabic",bn:"Bengali",hi:"Hindi",tr:"Turkish",id:"Indonesian",ms:"Malay",pa:"Punjabi",fa:"Persian (Farsi)",fr:"French",de:"German",es:"Spanish",it:"Italian"};
function responseLanguage(req){const code=(req.headers.get("x-app-language")||"en").trim().toLowerCase();return LANGUAGE_NAMES[code]||"English";}

function clean(v){ return typeof v === "string" ? v.trim().slice(0, MAX) : ""; }

async function runAzureAI(body, instruction, targetLanguage){
  const endpoint=(process.env.AZURE_AI_ENDPOINT||"").trim().replace(/\/$/,"");
  const model=(process.env.AZURE_AI_MODEL||"").trim();
  if(!endpoint||!model) return {status:503,jsonBody:{ok:false,error:"AI_SERVICE_NOT_CONFIGURED"}};
  const token=await credential.getToken("https://cognitiveservices.azure.com/.default");
  const response=await fetch(endpoint+"/openai/v1/chat/completions",{method:"POST",headers:{"Authorization":"Bearer "+token.token,"Content-Type":"application/json"},body:JSON.stringify({model,messages:[{role:"system",content:instruction+" Respond in "+targetLanguage+" unless the user explicitly asks for another language."},{role:"user",content:clean(body?.prompt)}],temperature:0.2,max_tokens:1000})});
  const data=await response.json().catch(()=>({}));
  if(!response.ok) return {status:502,jsonBody:{ok:false,error:"AI_SERVICE_FAILED"}};
  const content=data?.choices?.[0]?.message?.content;
  if(typeof content!=="string"||!content.trim()) return {status:502,jsonBody:{ok:false,error:"AI_EMPTY_RESPONSE"}};
  return {status:200,jsonBody:{ok:true,assistant:{role:"assistant",content:content.trim()}}};
}

const mediatorInstruction="You are an impartial Nikah discussion assistant. Compare two perspectives fairly. Return: 1) neutral summary of each, 2) shared ground, 3) unresolved differences, 4) five practical questions, 5) a calm next step. Never declare a winner, issue binding religious/legal/medical advice, invent facts, or request secrets. For threats, abuse, coercion, fraud or immediate danger, prioritize human/family/professional safety support. Encourage consent, dignity, privacy and appropriate Wali/family involvement.";

const futureInstruction="You are a Nikah preparation comparison assistant. Compare two people's answers to marriage scenarios. Do not predict the future or calculate a fake compatibility percentage. Return: 1) clear agreement areas, 2) expectation differences, 3) issues to discuss before marriage, 4) five practical questions, 5) a neutral next step. Never issue binding religious/legal/medical advice or a marriage verdict. Do not invent facts. For threats, abuse, coercion, fraud or immediate danger, prioritize safety and appropriate human/family/professional help.";

app.http("aiNikahMediator",{methods:["POST"],authLevel:"anonymous",route:"ai/nikah-mediator",handler:requireAuth(async(req,context,user)=>{try{const a=await accessForAuth(user);if(!a.capabilities.advancedMatching)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};return await runAzureAI(await req.json(),mediatorInstruction,responseLanguage(req));}catch(e){return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"AI_MEDIATOR_FAILED"}};}})});
app.http("aiFutureLife",{methods:["POST"],authLevel:"anonymous",route:"ai/future-life",handler:requireAuth(async(req,context,user)=>{try{const a=await accessForAuth(user);if(!a.capabilities.advancedMatching)return {status:402,jsonBody:{ok:false,error:"PREMIUM_PLUS_REQUIRED",locked:true}};return await runAzureAI(await req.json(),futureInstruction,responseLanguage(req));}catch(e){return {status:e.statusCode||500,jsonBody:{ok:false,error:e.statusCode?e.message:"AI_FUTURE_LIFE_FAILED"}};}})});
