const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');

admin.initializeApp();
const db = admin.firestore();
const FieldValue = admin.firestore.FieldValue;

function requireAuth(request) {
  if (!request.auth) throw new HttpsError('unauthenticated', 'Sign-in required.');
  return request.auth.uid;
}

function pairId(a, b) {
  if (!a || !b || a === b) throw new HttpsError('invalid-argument', 'Two different users are required.');
  return [a, b].sort().join('_');
}

function isAdmin(request) {
  return request.auth && request.auth.token && request.auth.token.admin === true;
}

// Creates an active connection only after both sides have an accepted interest.
exports.createMutualConnection = onCall(async (request) => {
  const uid = requireAuth(request);
  const otherUid = String(request.data?.otherUid || '');
  if (!otherUid || otherUid === uid) throw new HttpsError('invalid-argument', 'Invalid match.');

  const id = pairId(uid, otherUid);
  const connectionRef = db.collection('connections').doc(id);
  const [forward, reverse] = await Promise.all([
    db.collection('interests').where('fromUid', '==', uid).where('toUid', '==', otherUid).where('status', '==', 'accepted').limit(1).get(),
    db.collection('interests').where('fromUid', '==', otherUid).where('toUid', '==', uid).where('status', '==', 'accepted').limit(1).get()
  ]);

  if (forward.empty || reverse.empty) {
    throw new HttpsError('failed-precondition', 'A mutual accepted interest is required.');
  }

  await connectionRef.set({
    uid1: id.split('_')[0],
    uid2: id.split('_')[1],
    status: 'active',
    createdAt: FieldValue.serverTimestamp(),
    createdBy: uid
  }, { merge: false });

  return { connectionId: id, status: 'active' };
});

// Creates/updates a Wali relationship without exposing privileged verification fields.
exports.requestWaliConnection = onCall(async (request) => {
  const uid = requireAuth(request);
  const waliUid = String(request.data?.waliUid || '');
  if (!waliUid || waliUid === uid) throw new HttpsError('invalid-argument', 'Invalid Wali.');

  const ref = db.collection('waliConnections').doc(`${uid}_${waliUid}`);
  await ref.set({
    userUid: uid,
    waliUid,
    status: 'pending',
    requestedBy: uid,
    updatedAt: FieldValue.serverTimestamp()
  }, { merge: true });
  return { status: 'pending' };
});

// User requests deletion; trusted backend recursively removes owned data.
exports.deleteMyAccount = onCall(async (request) => {
  const uid = requireAuth(request);
  const collections = ['users', 'interests', 'connections', 'reports', 'verifications', 'privacy', 'blocks', 'waliConnections', 'aiConversations', 'deletionRequests'];
  const batchLimit = 400;

  for (const name of collections) {
    let snap = await db.collection(name).where('userUid', '==', uid).limit(batchLimit).get();
    if (name === 'users' || name === 'privacy') {
      const own = await db.collection(name).doc(uid).get();
      if (own.exists) {
        const b = db.batch();
        b.delete(own.ref);
        await b.commit();
      }
    }
    while (!snap.empty) {
      const b = db.batch();
      snap.docs.forEach((d) => b.delete(d.ref));
      await b.commit();
      snap = await db.collection(name).where('userUid', '==', uid).limit(batchLimit).get();
    }
  }

  await admin.auth().deleteUser(uid);
  return { deleted: true };
});

// Every user report is copied into a moderation queue; no client can mark it resolved.
exports.queueReport = onDocumentCreated('reports/{reportId}', async (event) => {
  const report = event.data?.data();
  if (!report) return;
  await db.collection('moderationQueue').doc(event.params.reportId).set({
    reportId: event.params.reportId,
    reporterUid: report.reporterUid || null,
    reportedUid: report.reportedUid || null,
    reason: report.reason || 'unspecified',
    status: 'pending',
    createdAt: FieldValue.serverTimestamp()
  }, { merge: true });
});

exports.setModerationDecision = onCall(async (request) => {
  if (!isAdmin(request)) throw new HttpsError('permission-denied', 'Admin access required.');
  const reportId = String(request.data?.reportId || '');
  const decision = String(request.data?.decision || '');
  if (!reportId || !['reviewed', 'actioned', 'dismissed'].includes(decision)) {
    throw new HttpsError('invalid-argument', 'Invalid moderation decision.');
  }
  await db.collection('moderationQueue').doc(reportId).set({
    status: decision,
    moderatorUid: request.auth.uid,
    decidedAt: FieldValue.serverTimestamp()
  }, { merge: true });
  return { status: decision };
});
