const { onCall, onRequest, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');
const crypto = require('crypto');
const db = admin.firestore();

async function activeUser(req) {
  if (!req.auth) throw new HttpsError('unauthenticated', 'Sign-in required.');
  const snap = await db.collection('users').doc(req.auth.uid).get();
  const d = snap.data() || {};
  if (!snap.exists || d.profileActive !== true || d.discoverable !== true || d.termsAccepted !== true || d.intentConfirmed !== true) {
    throw new HttpsError('failed-precondition', 'An active profile is required.');
  }
  if (req.auth.token?.email_verified !== true) throw new HttpsError('failed-precondition', 'Email verification is required.');
  return d;
}

function numericAdUnit(id) { const s = String(id || '').trim(); const i = s.lastIndexOf('/'); return i >= 0 ? s.slice(i + 1) : s; }

exports.getRewardedAdConfig = onCall(async req => {
  await activeUser(req);
  const snap = await db.collection('publicConfig').doc('admob').get();
  const d = snap.data() || {};
  const unit = String(d.rewardedAdUnitId || '').trim();
  if (!/^ca-app-pub-\d{16}\/\d+$/.test(unit)) return { configured: false };
  return { configured: true, rewardedAdUnitId: unit, rewardedAdUnitNumericId: numericAdUnit(unit) };
});

async function verifierKeys() {
  const r = await fetch('https://www.gstatic.com/admob/reward/verifier-keys.json');
  if (!r.ok) throw new Error(`AdMob key server returned ${r.status}`);
  return r.json();
}

async function verifySsvUrl(url) {
  const qIndex = url.indexOf('?');
  if (qIndex < 0) throw new Error('Missing query.');
  const query = url.slice(qIndex + 1);
  const sigMarker = '&signature=';
  const sigIndex = query.indexOf(sigMarker);
  if (sigIndex < 0) throw new Error('Missing signature.');
  const signedData = query.slice(0, sigIndex);
  const tail = query.slice(sigIndex + 1);
  const keyMarker = '&key_id=';
  const keyIndex = tail.indexOf(keyMarker);
  if (keyIndex < 0 || !tail.startsWith('signature=')) throw new Error('Invalid signature parameters.');
  const signatureText = tail.slice('signature='.length, keyIndex);
  const keyId = Number(tail.slice(keyIndex + keyMarker.length));
  if (!Number.isSafeInteger(keyId)) throw new Error('Invalid key id.');
  const sig = Buffer.from(signatureText.replace(/-/g, '+').replace(/_/g, '/'), 'base64');
  const keys = await verifierKeys();
  const found = Array.isArray(keys.keys) ? keys.keys.find(k => Number(k.keyId) === keyId) : null;
  if (!found?.pem) throw new Error('Unknown AdMob verification key.');
  const ok = crypto.verify('sha256', Buffer.from(signedData, 'utf8'), found.pem, sig);
  if (!ok) throw new Error('Invalid AdMob SSV signature.');
  return new URLSearchParams(query);
}

exports.admobRewardedSsv = onRequest(async (req, res) => {
  try {
    if (req.method !== 'GET') return res.status(405).send('Method Not Allowed');
    const original = String(req.originalUrl || req.url || '');
    const params = await verifySsvUrl(original);
    const uid = String(params.get('user_id') || '').trim();
    const customUid = String(params.get('custom_data') || '').trim();
    const transactionId = String(params.get('transaction_id') || '').trim();
    const adUnit = String(params.get('ad_unit') || '').trim();
    const timestamp = Number(params.get('timestamp') || 0);
    if (!uid || customUid !== uid || !transactionId || !adUnit || !timestamp) throw new Error('Missing required SSV fields.');
    if (Math.abs(Date.now() - timestamp) > 24 * 60 * 60 * 1000) throw new Error('Stale AdMob reward callback.');
    const configSnap = await db.collection('publicConfig').doc('admob').get();
    const configuredUnit = String(configSnap.data()?.rewardedAdUnitId || '').trim();
    if (!configuredUnit || numericAdUnit(configuredUnit) !== adUnit) throw new Error('Unknown rewarded ad unit.');
    const txRef = db.collection('rewardedAdTransactions').doc(transactionId);
    const day = new Date().toISOString().slice(0, 10);
    const dailyRef = db.collection('dailyRewardClaims').doc(`${uid}_${day}`);
    const entitlementRef = db.collection('entitlements').doc(uid);
    await db.runTransaction(async tx => {
      const [txSnap, dailySnap] = await Promise.all([tx.get(txRef), tx.get(dailyRef)]);
      if (txSnap.exists) return;
      const used = Number(dailySnap.exists ? dailySnap.data()?.count || 0 : 0);
      if (used >= 2) throw new Error('Daily rewarded limit reached.');
      tx.create(txRef, { uid, transactionId, adUnit, rewardAmount: Number(params.get('reward_amount') || 0), rewardItem: String(params.get('reward_item') || ''), verifiedAt: admin.firestore.FieldValue.serverTimestamp() });
      tx.set(dailyRef, { uid, day, count: used + 1, updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
      tx.set(entitlementRef, { uid, messageCredits: admin.firestore.FieldValue.increment(1), lastRewardedClaimAt: admin.firestore.FieldValue.serverTimestamp(), updatedAt: admin.firestore.FieldValue.serverTimestamp() }, { merge: true });
    });
    return res.status(200).send('OK');
  } catch (e) {
    console.error('AdMob SSV rejected:', e?.message || e);
    return res.status(400).send('Invalid reward callback');
  }
});
