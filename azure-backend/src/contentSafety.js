const { DefaultAzureCredential } = require("@azure/identity");

const credential = new DefaultAzureCredential();

async function analyzeImage(bytes) {
  const endpoint = process.env.AZURE_CONTENT_SAFETY_ENDPOINT;
  if (!endpoint) throw new Error("AZURE_CONTENT_SAFETY_ENDPOINT_NOT_CONFIGURED");
  const token = await credential.getToken("https://cognitiveservices.azure.com/.default");
  if (!token?.token) throw new Error("CONTENT_SAFETY_TOKEN_UNAVAILABLE");

  const response = await fetch(
    endpoint.replace(/\/$/,"") + "/contentsafety/image:analyze?api-version=2024-09-01",
    {
      method:"POST",
      headers:{
        "Authorization":"Bearer " + token.token,
        "Content-Type":"application/json"
      },
      body:JSON.stringify({
        image:{content:Buffer.from(bytes).toString("base64")},
        categories:["Hate","SelfHarm","Sexual","Violence"],
        outputType:"FourSeverityLevels"
      })
    }
  );
  const body=await response.json().catch(()=>({}));
  if(!response.ok) {
    const e=new Error("CONTENT_SAFETY_ANALYSIS_FAILED_" + response.status);
    e.details=body;
    throw e;
  }
  return body;
}

function shouldReject(result) {
  return (result?.categoriesAnalysis||[]).some(x =>
    ["Hate","SelfHarm","Sexual","Violence"].includes(x.category) &&
    Number(x.severity||0) >= 2
  );
}

module.exports={analyzeImage,shouldReject};
