const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

const db = admin.firestore();
const TERMS_VERSION = '2026-09-07-v1';

function requireAuth(req) {
  if (!req.auth) throw new HttpsError('unauthenticated', 'Sign-in required.');
  return req.auth.uid;
}

exports.acceptTermsAndCommunityGuidelines = onCall(async (req) => {
  const uid = requireAuth(req);
  const acceptedVersion = String(req.data?.termsVersion || '');
  if (acceptedVersion !== TERMS_VERSION) {
    throw new HttpsError('failed-precondition', 'The current Terms & Community Guidelines must be accepted.');
  }

  const ref = db.collection('users').doc(uid);
  const snap = await ref.get();
  if (!snap.exists) throw new HttpsError('not-found', 'Profile not found.');
  const data = snap.data() || {};
  const age = data.age;
  if (!Number.isInteger(age) || age < 18) {
    throw new HttpsError('failed-precondition', 'This service is restricted to adults 18 and over.');
  }

  await ref.set({
    termsAccepted: true,
    termsVersion: TERMS_VERSION,
    termsAcceptedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });

  return { accepted: true, termsVersion: TERMS_VERSION };
});

exports.getTermsAcceptanceStatus = onCall(async (req) => {
  const uid = requireAuth(req);
  const snap = await db.collection('users').doc(uid).get();
  if (!snap.exists) throw new HttpsError('not-found', 'Profile not found.');
  const data = snap.data() || {};
  return {
    accepted: data.termsAccepted === true && data.termsVersion === TERMS_VERSION,
    termsVersion: TERMS_VERSION
  };
});
