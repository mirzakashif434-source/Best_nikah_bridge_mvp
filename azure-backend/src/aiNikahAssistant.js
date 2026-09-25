const { app } = require("@azure/functions");
const { DefaultAzureCredential } = require("@azure/identity");
const { requireAuth } = require("./auth");
const { accessForAuth } = require("./premiumAccess");
const { consumeAiQuota, quotaResponse } = require("./costGuard");

const credential = new DefaultAzureCredential();
const MAX_MESSAGE = Number(process.env.AI_MAX_INPUT_CHARS || 2500);
const MAX_MESSAGES = Number(process.env.AI_MAX_MESSAGES || 12);
const LANGUAGE_NAMES={en:"English",ur:"Urdu",ar:"Arabic",bn:"Bengali",hi:"Hindi",tr:"Turkish",id:"Indonesian",ms:"Malay",pa:"Punjabi",fa:"Persian (Farsi)",fr:"French",de:"German",es:"Spanish",it:"Italian"};
function responseLanguage(request){const code=(request.headers.get("x-app-language")||"en").trim().toLowerCase();return LANGUAGE_NAMES[code]||"English";}

function clean(v){
  return typeof v === "string" ? v.trim().slice(0, MAX_MESSAGE) : "";
}

app.http("aiNikahAssistant", {
  methods: ["POST"],
  authLevel: "anonymous",
  route: "ai/nikah-assistant",
  handler: requireAuth(async (request, context, user) => {
    try {
      const access=await accessForAuth(user);
      if(!access.capabilities.aiNikahAssistant) return {status:402,jsonBody:{ok:false,error:"PREMIUM_VIP_REQUIRED",locked:true}};
      const quota=await consumeAiQuota(access.user.id,access.premium.planKey,"nikah_assistant");
      if(!quota.allowed) return quotaResponse(quota);
      const endpoint = (process.env.AZURE_AI_ENDPOINT || "").trim().replace(/\/$/, "");
      const model = (process.env.AZURE_AI_MODEL || "").trim();
      if (!endpoint || !model) {
        return { status: 503, jsonBody: { ok: false, error: "AI_SERVICE_NOT_CONFIGURED" } };
      }

      const targetLanguage=responseLanguage(request);
      const body = await request.json();
      const input = Array.isArray(body?.messages) ? body.messages : [];
      if (!input.length || input.length > MAX_MESSAGES) {
        return { status: 400, jsonBody: { ok: false, error: "MESSAGES_INVALID" } };
      }

      const messages = input.map((m) => ({
        role: m?.role === "assistant" ? "assistant" : "user",
        content: clean(m?.content)
      })).filter((m) => m.content);

      if (!messages.length) {
        return { status: 400, jsonBody: { ok: false, error: "MESSAGE_CONTENT_REQUIRED" } };
      }

      const token = await credential.getToken("https://cognitiveservices.azure.com/.default");
      if (!token?.token) throw new Error("AZURE_AI_TOKEN_UNAVAILABLE");

      const response = await fetch(endpoint + "/openai/v1/chat/completions", {
        method: "POST",
        headers: {
          "Authorization": "Bearer " + token.token,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          model,
          messages: [
            {
              role: "system",
              content:
                "You are the Nikah Assistant for a serious Muslim matrimonial service. " +
                "Give respectful, practical, non-dating guidance about marriage preparation, " +
                "family or wali involvement, communication, profile safety, compatibility, " +
                "and general nikah planning. Do not make decisions for the user, do not claim " +
                "to be a scholar, lawyer, doctor, or imam, and advise the user to consult a " +
                "qualified local professional for religious, legal, medical, or safety matters. " +
                "Never ask for passwords, identity documents, payment card details, or other secrets. " +
                "Respond in " + targetLanguage + " unless the user explicitly asks for another language."
            },
            ...messages
          ],
          temperature: 0.2,
          max_tokens: Number(process.env.AI_NIKAH_MAX_TOKENS || 450)
        })
      });

      const data = await response.json().catch(() => ({}));
      if (!response.ok) {
        context.error("AZURE_AI_REQUEST_FAILED", response.status, data?.error || data);
        return { status: 502, jsonBody: { ok: false, error: "AI_SERVICE_FAILED" } };
      }

      const answer = data?.choices?.[0]?.message?.content;
      if (typeof answer !== "string" || !answer.trim()) {
        return { status: 502, jsonBody: { ok: false, error: "AI_EMPTY_RESPONSE" } };
      }

      return {
        status: 200,
        jsonBody: {
          ok: true,
          assistant: { role: "assistant", content: answer.trim() },
          aiQuota: { dailyLimit: quota.limit, remaining: quota.remaining }
        }
      };
    } catch (error) {
      context.error("AI_NIKAH_ASSISTANT_FAILED", error);
      return { status: 500, jsonBody: { ok: false, error: "AI_NIKAH_ASSISTANT_FAILED" } };
    }
  })
});
