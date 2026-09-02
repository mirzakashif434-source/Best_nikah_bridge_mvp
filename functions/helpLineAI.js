const { onCall } = require('firebase-functions/v2/https');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const { getAI, getGenerativeModel } = require('firebase-admin/ai');

/**
 * Additive Help Line AI fallback.
 * AI handles ordinary app-help questions when human support is unavailable.
 * Human-support SLA is recorded as 24 hours; this is a target, not a guarantee.
 */
exports.helpLineAI = onCall(async (request) => {
  if (!request.auth) throw new Error('Sign-in required.');

  const uid = request.auth.uid;
  const question = String(request.data?.question || '').trim();
  if (!question) throw new Error('Please enter your help question.');
  if (question.length > 4000) throw new Error('Question is too long.');

  const db = getFirestore();
  const ticketRef = db.collection('helpLineTickets').doc();
  const now = FieldValue.serverTimestamp();

  const sensitive = /(password|otp|one[- ]time|bank|iban|card|refund|payment dispute|verification decision|ban|blocked|report|legal|police|harass|abuse|self[- ]harm|suicide)/i.test(question);

  let answer = '';
  if (!sensitive) {
    try {
      const ai = getAI();
      const model = getGenerativeModel(ai, { model: 'gemini-2.5-flash' });
      const result = await model.generateContent(
        'You are the Best Nikah Bridge Help Assistant. Answer only practical questions about using this Muslim matrimonial app. Be concise, respectful, halal, privacy-conscious, and never claim to be a human or admin. Never ask for passwords, OTPs, bank/card details, or private secrets. Do not make marriage, legal, financial, medical, or safety guarantees. If the question requires human support, say so clearly. User question:\n' + question
      );
      answer = result.response.text();
    } catch (e) {
      answer = '';
    }
  }

  const humanRequired = sensitive || !answer;
  const response = humanRequired
    ? 'Aap ki request human support ko bhej di gayi hai. Hamara target hai ke aapko 24 ghanton ke andar reply mile. Agar sawal urgent safety matter hai, local emergency services se rabta karein.'
    : answer + '\n\nAgar aap human support chahte hain, isi request ko support review ke liye bhej sakte hain.';

  await ticketRef.set({
    uid,
    question,
    aiAnswered: !humanRequired,
    humanRequired,
    status: humanRequired ? 'awaiting_human' : 'ai_answered',
    createdAt: now,
    humanReplyTargetAt: humanRequired ? new Date(Date.now() + 24 * 60 * 60 * 1000) : null
  });

  return {
    ticketId: ticketRef.id,
    answer: response,
    aiAnswered: !humanRequired,
    humanRequired,
    humanReplySlaHours: 24
  };
});
