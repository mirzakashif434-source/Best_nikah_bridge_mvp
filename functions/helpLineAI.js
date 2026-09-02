const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

/**
 * Help Line ticket backend. AI generation stays in the Android Firebase AI Logic
 * layer already used by the app; this callable securely records the request and
 * the AI answer, or queues it for human support with a 24-hour target.
 */
exports.helpLineAI = onCall(async (request) => {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign-in required.');

  const uid = request.auth.uid;
  const question = String(request.data?.question || '').trim();
  const aiAnswer = String(request.data?.aiAnswer || '').trim();
  const humanRequired = Boolean(request.data?.humanRequired) || !aiAnswer;
  if (!question) throw new HttpsError('invalid-argument', 'Please enter your help question.');
  if (question.length > 4000) throw new HttpsError('invalid-argument', 'Question is too long.');
  if (aiAnswer.length > 8000) throw new HttpsError('invalid-argument', 'Answer is too long.');

  const db = getFirestore();
  const ref = db.collection('helpLineTickets').doc();
  await ref.set({
    uid,
    question,
    aiAnswer: humanRequired ? null : aiAnswer,
    aiAnswered: !humanRequired,
    humanRequired,
    status: humanRequired ? 'awaiting_human' : 'ai_answered',
    createdAt: FieldValue.serverTimestamp(),
    humanReplyTargetAt: humanRequired ? new Date(Date.now() + 24 * 60 * 60 * 1000) : null
  });

  return {
    ticketId: ref.id,
    answer: humanRequired
      ? 'Aap ki request human support ko bhej di gayi hai. Hamara target hai ke aapko 24 ghanton ke andar reply mile.'
      : aiAnswer,
    aiAnswered: !humanRequired,
    humanRequired,
    humanReplySlaHours: 24
  };
});
