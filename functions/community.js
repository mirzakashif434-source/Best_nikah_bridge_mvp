const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

const db = admin.firestore();
const F = admin.firestore.FieldValue;

const MAX_TEXT = 500;
const WINDOW_MS = 60 * 1000;
const MAX_PER_WINDOW = 5;

function uidOf(req) {
  if (!req.auth) throw new HttpsError('unauthenticated', 'Sign-in required.');
  return req.auth.uid;
}

async function activeUser(uid, req) {
  const d = await db.collection('users').doc(uid).get();
  if (!d.exists) throw new HttpsError('not-found', 'Profile not found.');
  const x = d.data() || {};
  if (x.profileActive !== true || x.discoverable !== true || x.termsAccepted !== true || x.intentConfirmed !== true) {
    throw new HttpsError('failed-precondition', 'An active Nikah profile is required.');
  }
  if (req && req.auth && uid === req.auth.uid && req.auth.token.email_verified !== true) {
    throw new HttpsError('failed-precondition', 'Email verification is required.');
  }
  return d;
}

function containsPrivateOrUnsafe(text) {
  const lower = text.toLowerCase();
  const patterns = [
    /(?:\+?\d[\d\s().-]{7,}\d)/,
    /(?:whatsapp|telegram|snapchat|instagram|t\.me|wa\.me)/i,
    /(?:otp|one[- ]time password|password|recovery code|verification code)/i,
    /(?:send money|transfer money|gift card|crypto|bitcoin|usdt|bank account|iban)/i,
    /(?:https?:\/\/|www\.)/i
  ];
  return patterns.some((p) => p.test(lower));
}

async function rateLimited(uid) {
  const ref = db.collection('communityRateLimits').doc(uid);
  const now = Date.now();
  return db.runTransaction(async (tx) => {
    const s = await tx.get(ref);
    const d = s.exists ? s.data() : {};
    const started = Number(d.windowStartedAt || 0);
    const count = Number(d.count || 0);
    if (!started || now - started >= WINDOW_MS) {
      tx.set(ref, { uid, windowStartedAt: now, count: 1, updatedAt: F.serverTimestamp() }, { merge: true });
      return false;
    }
    if (count >= MAX_PER_WINDOW) return true;
    tx.set(ref, { uid, windowStartedAt: started, count: count + 1, updatedAt: F.serverTimestamp() }, { merge: true });
    return false;
  });
}

exports.sendCommunityMessage = onCall(async (req) => {
  const uid = uidOf(req);
  const text = String(req.data?.text || '').trim();
  if (!text || text.length > MAX_TEXT) throw new HttpsError('invalid-argument', 'Message must be 1–500 characters.');
  if (containsPrivateOrUnsafe(text)) {
    throw new HttpsError('failed-precondition', 'Community Chat does not allow phone numbers, private contact details, passwords, OTPs, links, money requests or crypto promotion.');
  }
  const user = await activeUser(uid, req);
  if (await rateLimited(uid)) throw new HttpsError('resource-exhausted', 'Community rate limit reached. Please wait a moment before sending another message.');
  const data = user.data() || {};
  const ref = db.collection('communityMessages').doc();
  await ref.set({
    authorUid: uid,
    authorName: String(data.name || 'Member').slice(0, 80),
    country: String(data.country || '').slice(0, 80),
    text,
    type: 'community',
    createdAt: F.serverTimestamp()
  });
  return { sent: true, messageId: ref.id };
});

exports.muteCommunityUser = onCall(async (req) => {
  const uid = uidOf(req);
  const targetUid = String(req.data?.targetUid || '');
  if (!targetUid || targetUid === uid) throw new HttpsError('invalid-argument', 'Invalid member.');
  await activeUser(uid, req);
  await db.collection('users').doc(uid).collection('communityMutes').doc(targetUid).set({
    uid: targetUid,
    mutedAt: F.serverTimestamp()
  }, { merge: true });
  return { muted: true };
});

exports.unmuteCommunityUser = onCall(async (req) => {
  const uid = uidOf(req);
  const targetUid = String(req.data?.targetUid || '');
  if (!targetUid || targetUid === uid) throw new HttpsError('invalid-argument', 'Invalid member.');
  await activeUser(uid, req);
  await db.collection('users').doc(uid).collection('communityMutes').doc(targetUid).delete();
  return { muted: false };
});
