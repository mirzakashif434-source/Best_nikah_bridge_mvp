const { app } = require("@azure/functions");
const { DefaultAzureCredential } = require("@azure/identity");
const { requireAuth } = require("./auth");

const credential = new DefaultAzureCredential();
const TARGETS = {
  ur:"Urdu", ar:"Arabic", bn:"Bengali", hi:"Hindi", tr:"Turkish",
  id:"Indonesian", ms:"Malay", pa:"Punjabi (Gurmukhi)", fa:"Persian (Farsi)",
  fr:"French", de:"German", es:"Spanish", it:"Italian"
};
const CACHE = new Map();
const MAX_ITEMS = 30, MAX_ITEM = 500, MAX_TOTAL = 8000, MAX_CACHE = 4000;

function clean(v){return typeof v==="string"?v.trim().slice(0,MAX_ITEM):"";}
function cacheKey(target,text){return target+"\u0001"+text;}

app.http("localizationTranslate",{
  methods:["POST"],authLevel:"anonymous",route:"localization/translate",
  handler:requireAuth(async(request,context)=>{
    try{
      const body=await request.json();
      const target=typeof body?.target==="string"?body.target.trim().toLowerCase():"";
      if(!TARGETS[target]) return {status:400,jsonBody:{ok:false,error:"TARGET_LANGUAGE_NOT_SUPPORTED"}};
      const raw=Array.isArray(body?.texts)?body.texts:[];
      if(!raw.length||raw.length>MAX_ITEMS) return {status:400,jsonBody:{ok:false,error:"TEXT_BATCH_INVALID"}};
      const texts=raw.map(clean);
      if(texts.some(x=>!x)) return {status:400,jsonBody:{ok:false,error:"TEXT_INVALID"}};
      if(texts.reduce((n,x)=>n+x.length,0)>MAX_TOTAL) return {status:400,jsonBody:{ok:false,error:"TEXT_BATCH_TOO_LARGE"}};

      const translations=new Array(texts.length);
      const missing=[], missingIndexes=[];
      texts.forEach((t,i)=>{
        const hit=CACHE.get(cacheKey(target,t));
        if(hit){translations[i]=hit;}else{missing.push(t);missingIndexes.push(i);}
      });

      if(missing.length){
        const endpoint=(process.env.AZURE_AI_ENDPOINT||"").trim().replace(/\/$/,"");
        const model=(process.env.AZURE_AI_MODEL||"").trim();
        if(!endpoint||!model) return {status:503,jsonBody:{ok:false,error:"AI_SERVICE_NOT_CONFIGURED"}};
        const token=await credential.getToken("https://cognitiveservices.azure.com/.default");
        if(!token?.token) throw new Error("AZURE_AI_TOKEN_UNAVAILABLE");

        const instruction =
          "Translate each UI string into "+TARGETS[target]+". Return ONLY valid JSON in this exact shape: "+
          "{\"translations\":[\"...\"]}. Preserve array order and item count. Preserve brand/product terms "+
          "Best Nikah Bredge, Azure, Microsoft, Google Play, SAR, IDs, numbers, emoji and placeholders. "+
          "Keep Nikah and Wali recognizable and culturally appropriate. Do not add explanations, advice, "+
          "religious rulings, or extra text. Translate interface wording naturally and concisely.";
        const response=await fetch(endpoint+"/openai/v1/chat/completions",{
          method:"POST",
          headers:{"Authorization":"Bearer "+token.token,"Content-Type":"application/json"},
          body:JSON.stringify({
            model,
            messages:[{role:"system",content:instruction},{role:"user",content:JSON.stringify({texts:missing})}],
            temperature:0,
            max_tokens:2200
          })
        });
        const data=await response.json().catch(()=>({}));
        if(!response.ok){
          context.error("LOCALIZATION_AI_REQUEST_FAILED",response.status);
          return {status:502,jsonBody:{ok:false,error:"LOCALIZATION_SERVICE_FAILED"}};
        }
        let content=data?.choices?.[0]?.message?.content;
        if(typeof content!=="string"||!content.trim()) return {status:502,jsonBody:{ok:false,error:"LOCALIZATION_EMPTY_RESPONSE"}};
        content=content.trim().replace(/^\`\`\`(?:json)?\s*/i,"").replace(/\s*\`\`\`$/,"");
        let parsed; try{parsed=JSON.parse(content);}catch(e){return {status:502,jsonBody:{ok:false,error:"LOCALIZATION_INVALID_RESPONSE"}};}
        const out=Array.isArray(parsed?.translations)?parsed.translations:[];
        if(out.length!==missing.length||out.some(x=>typeof x!=="string"||!x.trim()))
          return {status:502,jsonBody:{ok:false,error:"LOCALIZATION_COUNT_MISMATCH"}};
        out.forEach((value,j)=>{
          const translated=value.trim().slice(0,800);
          const source=missing[j], index=missingIndexes[j];
          translations[index]=translated;
          CACHE.set(cacheKey(target,source),translated);
        });
        if(CACHE.size>MAX_CACHE){
          const remove=CACHE.size-MAX_CACHE;
          let n=0; for(const k of CACHE.keys()){CACHE.delete(k);if(++n>=remove)break;}
        }
      }
      return {status:200,jsonBody:{ok:true,target,translations}};
    }catch(error){
      context.error("LOCALIZATION_TRANSLATE_FAILED",error?.message||error);
      return {status:500,jsonBody:{ok:false,error:"LOCALIZATION_TRANSLATE_FAILED"}};
    }
  })
});
