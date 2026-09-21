# Azure Step 5 — AI Nikah Assistant Lock

Production migration checkpoint.

- Android Nikah Assistant calls Azure Function /api/ai/nikah-assistant.
- Azure External ID authentication is required.
- Azure OpenAI is the real AI provider; no demo/mock response is used.
- Azure Function uses managed identity (DefaultAzureCredential) to obtain the Cognitive Services token.
- Azure AI endpoint/model are production-configured by deployment workflow.
- Input size/message count are bounded and secrets are explicitly prohibited.
- Existing Firebase AI dependency/code is preserved; nothing from earlier work is deleted or replaced.
- Previous successful Azure deployment/health verification is retained as the deployment evidence for this checkpoint.
