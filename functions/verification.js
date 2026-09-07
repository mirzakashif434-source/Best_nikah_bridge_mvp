const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');
const db = admin.firestore();

function uidOf(req) { if (!req.auth) throw new HttpsError('unauthenticated','Sign-in required.'); return req.auth.uid; }
async function active(uid, req) {
  const u = await db.collection('users').doc(uid).get(); const d = u.data() || {};
  if (!u.exists || d.profileActive !== true || d.termsAccepted !== true || d.intentConfirmed !== true) throw new HttpsError('failed-precondition','An active profile is required.');
  if (req.auth.token?.email_verified !== true) throw new HttpsError('failed-precondition','Email verification is required.');
  return d;
}
exports.submitIdentityVerification = onCall(async (req) => {
  const uid = uidOf(req); await active(uid, req);
  const type = String(req.data?.documentType || '').trim();
  const path = String(req.data?.storagePath || '').trim();
  if (!['national_id','passport','driving_license'].includes(type)) throw new HttpsError('invalid-argument','Choose a supported identity document.');
  const expected = `verificationDocuments/${uid}/`;
  if (!path.startsWith(expected) || path.length > expected.length + 180) throw new HttpsError('invalid-argument','Invalid verification document path.');
  const bucket = admin.storage().bucket();
  const file = bucket.file(path);
  const [exists] = await file.exists();
  if (!exists) throw new HttpsError('not-found','Verification document was not uploaded.');
  const [meta] = await file.getMetadata();
  const size = Number(meta.size || 0);
  const contentType = String(meta.contentType || '');
  if (size <= 0 || size > 10 * 1024 * 1024 || !(/^(image\/(jpeg|png|webp)|application\/pdf)$/i).test(contentType)) throw new HttpsError('invalid-argument','Unsupported verification file.');
  const ref = db.collection('verifications').doc(uid);
  const old = await ref.get();
  if (old.exists && ['pending','verified'].includes(String(old.data()?.status || ''))) throw new HttpsError('already-exists','A verification request is already pending or verified.');
  await ref.set({ userUid: uid, status: 'pending', documentType: type, storagePath: path, contentType, size, submittedAt: admin.firestore.FieldValue.serverTimestamp(), updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
  return { submitted: true, status: 'pending' };
});
exports.getMyVerificationStatus = onCall(async (req) => {
  const uid = uidOf(req); await active(uid, req); const s = await db.collection('verifications').doc(uid).get();
  if (!s.exists) return { status: 'not_submitted' };
  const d = s.data() || {}; return { status: String(d.status || 'not_submitted'), documentType: String(d.documentType || ''), submittedAt: d.submittedAt || null, reviewNote: String(d.reviewNote || '') };
});
