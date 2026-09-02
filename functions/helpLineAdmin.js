const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');

function requireAdmin(request) {
  if (!request.auth?.token?.admin) throw new HttpsError('permission-denied', 'Admin access required.');
  return request.auth.uid;
}

exports.listHelpLineTickets = onCall(async (request) => {
  requireAdmin(request);
  const limit = Math.min(Math.max(Number(request.data?.limit || 100), 1), 200);
  const snap = await getFirestore().collection('helpLineTickets').orderBy('createdAt', 'desc').limit(limit).get();
  return { tickets: snap.docs.map(d => { const x = d.data() || {}; return {
    id: d.id, uid: String(x.uid || ''), question: String(x.question || ''),
    aiAnswer: x.aiAnswer || null, humanRequired: Boolean(x.humanRequired), status: String(x.status || ''),
    createdAt: x.createdAt?.toDate?.()?.toISOString() || null,
    humanReplyTargetAt: x.humanReplyTargetAt?.toDate?.()?.toISOString() || null,
    humanReply: x.humanReply || null,
    humanRepliedAt: x.humanRepliedAt?.toDate?.()?.toISOString() || null,
    repliedBy: x.repliedBy || null
  }; }) };
});

exports.replyHelpLineTicket = onCall(async (request) => {
  const adminUid = requireAdmin(request);
  const ticketId = String(request.data?.ticketId || '').trim();
  const reply = String(request.data?.reply || '').trim();
  if (!ticketId || !reply) throw new HttpsError('invalid-argument', 'Ticket ID and reply are required.');
  if (reply.length > 8000) throw new HttpsError('invalid-argument', 'Reply is too long.');
  const ref = getFirestore().collection('helpLineTickets').doc(ticketId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError('not-found', 'Help ticket not found.');
  await ref.update({ humanReply: reply, humanRepliedAt: FieldValue.serverTimestamp(), repliedBy: adminUid, status: 'human_replied', updatedAt: FieldValue.serverTimestamp() });
  return { replied: true, ticketId, status: 'human_replied' };
});
