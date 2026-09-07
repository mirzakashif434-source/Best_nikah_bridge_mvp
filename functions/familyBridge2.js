const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

const db = admin.firestore();
const F = admin.firestore.FieldValue;

function uid(req) {
  if (!req.auth) throw new HttpsError('unauthenticated', 'Sign-in required.');
  return req.auth.uid;
}

function pair(a, b) {
  if (!a || !b || a === b) throw new HttpsError('invalid-argument', 'Two different users are required.');
  return [a, b].sort().join('_');
}

async function connected(a, b) {
  const id = pair(a, b);
  const c = await db.collection('connections').doc(id).get();
  if (!c.exists || c.data()?.status !== 'active') return false;
  const d = c.data() || {};
  return (d.uid1 === a && d.uid2 === b) || (d.uid1 === b && d.uid2 === a);
}

function bridgeParticipant(d, me) {
  return d && d.version === 2 && Array.isArray(d.participants) && d.participants.includes(me);
}

exports.createFamilyBridgeV2 = onCall(async req => {
  const me = uid(req);
  const connectionId = String(req.data?.connectionId || '').trim();
  const familyUid = String(req.data?.familyUid || '').trim();
  if (!connectionId || !familyUid || familyUid === me) {
    throw new HttpsError('invalid-argument', 'A mutual connection and a different family account are required.');
  }

  const parts = connectionId.split('_');
  if (parts.length !== 2 || !parts.includes(me) || parts.includes(familyUid) || !(await connected(parts[0], parts[1]))) {
    throw new HttpsError('permission-denied', 'An active mutual connection is required and the family account must be separate.');
  }

  const family = await db.collection('users').doc(familyUid).get();
  if (!family.exists || family.data()?.profileActive !== true || family.data()?.discoverable !== true || family.data()?.termsAccepted !== true || family.data()?.intentConfirmed !== true) {
    throw new HttpsError('failed-precondition', 'The invited family account is not active for Family Bridge.');
  }

  const existing = await db.collection('familyBridges')
    .where('version', '==', 2)
    .where('connectionId', '==', connectionId)
    .where('familyUid', '==', familyUid)
    .where('status', 'in', ['pending', 'active', 'paused'])
    .limit(1)
    .get();
  if (!existing.empty) throw new HttpsError('already-exists', 'A Family Bridge already exists for this connection and family account.');

  const ref = db.collection('familyBridges').doc();
  const now = admin.firestore.Timestamp.now();
  await ref.set({
    version: 2,
    connectionId,
    createdBy: me,
    primaryUserUids: parts,
    familyUid,
    status: 'pending',
    participants: [parts[0], parts[1], familyUid],
    consent: { [me]: true, [familyUid]: false },
    createdAt: now,
    updatedAt: now
  });
  return { bridgeId: ref.id, status: 'pending' };
});

exports.respondFamilyBridgeV2 = onCall(async req => {
  const me = uid(req);
  const bridgeId = String(req.data?.bridgeId || '').trim();
  const decision = String(req.data?.decision || '').trim();
  if (!bridgeId || !['accept', 'reject'].includes(decision)) {
    throw new HttpsError('invalid-argument', 'A valid bridge response is required.');
  }

  const ref = db.collection('familyBridges').doc(bridgeId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError('not-found', 'Family Bridge not found.');
  const d = snap.data() || {};
  if (d.version !== 2 || d.familyUid !== me) throw new HttpsError('permission-denied', 'Only the invited family account can respond.');
  if (d.status !== 'pending') throw new HttpsError('failed-precondition', 'This Family Bridge is no longer pending.');

  if (decision === 'reject') {
    await ref.update({ status: 'rejected', rejectedBy: me, rejectedAt: F.serverTimestamp(), updatedAt: F.serverTimestamp() });
    return { status: 'rejected' };
  }

  await ref.update({
    status: 'active',
    consent: { ...(d.consent || {}), [me]: true },
    acceptedBy: me,
    acceptedAt: F.serverTimestamp(),
    updatedAt: F.serverTimestamp()
  });
  return { status: 'active' };
});

exports.listMyFamilyBridgesV2 = onCall(async req => {
  const me = uid(req);
  const [participantSnap, invitedSnap, createdSnap] = await Promise.all([
    db.collection('familyBridges').where('participants', 'array-contains', me).limit(100).get(),
    db.collection('familyBridges').where('familyUid', '==', me).where('status', '==', 'pending').limit(100).get(),
    db.collection('familyBridges').where('createdBy', '==', me).limit(100).get()
  ]);

  const map = new Map();
  [...participantSnap.docs, ...invitedSnap.docs, ...createdSnap.docs].forEach(doc => {
    const d = doc.data() || {};
    map.set(doc.id, { id: doc.id, ...d });
  });

  const bridges = [...map.values()]
    .filter(d => d.version === 2 && Array.isArray(d.participants) && d.participants.includes(me))
    .sort((a, b) => (b.updatedAt?.toMillis?.() || 0) - (a.updatedAt?.toMillis?.() || 0))
    .slice(0, 100)
    .map(d => ({
      id: d.id,
      connectionId: d.connectionId,
      status: d.status,
      createdBy: d.createdBy,
      familyUid: d.familyUid,
      participants: d.participants || [],
      consent: d.consent || {},
      updatedAt: d.updatedAt?.toMillis?.() || null
    }));

  return { bridges };
});

exports.setFamilyBridgeConsentV2 = onCall(async req => {
  const me = uid(req);
  const bridgeId = String(req.data?.bridgeId || '').trim();
  const enabled = Boolean(req.data?.enabled);
  const ref = db.collection('familyBridges').doc(bridgeId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError('not-found', 'Family Bridge not found.');
  const d = snap.data() || {};
  if (!bridgeParticipant(d, me) || !['active', 'paused'].includes(d.status)) {
    throw new HttpsError('permission-denied', 'Active Family Bridge access is required.');
  }

  await ref.update({
    [`consent.${me}`]: enabled,
    updatedAt: F.serverTimestamp()
  });

  const fresh = await ref.get();
  const consent = fresh.data()?.consent || {};
  const anyConsent = Object.values(consent).some(Boolean);
  await ref.update({ status: anyConsent ? 'active' : 'paused', updatedAt: F.serverTimestamp() });
  return { enabled, status: anyConsent ? 'active' : 'paused' };
});

exports.sendFamilyQuestionV2 = onCall(async req => {
  const me = uid(req);
  const bridgeId = String(req.data?.bridgeId || '').trim();
  const text = String(req.data?.text || '').trim();
  if (!bridgeId || !text || text.length > 2000) throw new HttpsError('invalid-argument', 'Question is required and must be 1-2000 characters.');

  const ref = db.collection('familyBridges').doc(bridgeId);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError('not-found', 'Family Bridge not found.');
  const d = snap.data() || {};
  if (!bridgeParticipant(d, me) || d.status !== 'active' || d.consent?.[me] !== true) {
    throw new HttpsError('permission-denied', 'Active Family Bridge consent is required.');
  }

  const q = ref.collection('questions').doc();
  await q.set({ fromUid: me, text, status: 'open', createdAt: F.serverTimestamp(), updatedAt: F.serverTimestamp() });
  await ref.update({ updatedAt: F.serverTimestamp() });
  return { questionId: q.id };
});
