const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');
const { google } = require('googleapis');
const crypto = require('crypto');

const db = admin.firestore();
const PACKAGE_NAME = 'com.nikahbridge';
const PLANS = {
  bnb_plus_20: { tier: 'plus20', sar: 20, messageCredits: 10 },
  bnb_plus_40: { tier: 'plus40', sar: 40, messageCredits: 30 },
  bnb_plus_60: { tier: 'plus60', sar: 60, messageCredits: 60 },
};
const TIER_RANK = { free: 0, plus20: 20, plus40: 40, plus60: 60 };

let publisherPromise = null;
async function publisher() {
  if (!publisherPromise) {
    const raw = process.env.PLAY_SERVICE_ACCOUNT_JSON;
    if (!raw) throw new Error('PLAY_SERVICE_ACCOUNT_JSON is not configured.');
    const credentials = JSON.parse(raw);
    const auth = new google.auth.GoogleAuth({
      credentials,
      scopes: ['https://www.googleapis.com/auth/androidpublisher'],
    });
    publisherPromise = auth.getClient().then(client => google.androidpublisher({ version: 'v3', auth: client }));
  }
  return publisherPromise;
}

function uidFrom(req) {
  if (!req.auth?.uid) throw new HttpsError('unauthenticated', 'Sign-in required.');
  return req.auth.uid;
}

function tokenHash(token) {
  return crypto.createHash('sha256').update(token).digest('hex');
}

exports.getPremiumEntitlement = onCall(async req => {
  const uid = uidFrom(req);
  const snap = await db.collection('entitlements').doc(uid).get();
  const d = snap.exists ? snap.data() : {};
  return {
    tier: String(d.tier || 'free'),
    messageCredits: Number(d.messageCredits || 0),
    premiumActive: d.premiumActive === true,
    productId: String(d.productId || ''),
    updatedAt: d.updatedAt?.toDate?.()?.toISOString() || null,
  };
});

exports.verifyPremiumPurchase = onCall(async req => {
  const uid = uidFrom(req);
  const productId = String(req.data?.productId || '');
  const purchaseToken = String(req.data?.purchaseToken || '');
  if (!PLANS[productId] || purchaseToken.length < 20 || purchaseToken.length > 1000) {
    throw new HttpsError('invalid-argument', 'Invalid premium purchase data.');
  }

  let playPurchase;
  try {
    const api = await publisher();
    const result = await api.purchases.products.get({
      packageName: PACKAGE_NAME,
      productId,
      token: purchaseToken,
    });
    playPurchase = result.data || {};
  } catch (err) {
    console.error('Google Play purchase verification failed', err?.message || err);
    throw new HttpsError('failed-precondition', 'Google Play could not verify this purchase.');
  }

  if (Number(playPurchase.purchaseState) !== 0) {
    throw new HttpsError('failed-precondition', 'This Google Play purchase is not completed.');
  }

  const plan = PLANS[productId];
  const purchaseId = tokenHash(purchaseToken);
  const purchaseRef = db.collection('playPurchases').doc(purchaseId);
  const entitlementRef = db.collection('entitlements').doc(uid);
  const existing = await purchaseRef.get();
  if (existing.exists && existing.data()?.uid !== uid) {
    throw new HttpsError('permission-denied', 'This purchase token is already linked to another account.');
  }

  await db.runTransaction(async tx => {
    const [purchaseSnap, entitlementSnap] = await Promise.all([tx.get(purchaseRef), tx.get(entitlementRef)]);
    if (purchaseSnap.exists && purchaseSnap.data()?.processed === true) return;
    const current = entitlementSnap.exists ? entitlementSnap.data() : {};
    const currentTier = String(current.tier || 'free');
    const nextTier = (TIER_RANK[plan.tier] || 0) >= (TIER_RANK[currentTier] || 0) ? plan.tier : currentTier;
    tx.set(entitlementRef, {
      uid,
      tier: nextTier,
      premiumActive: true,
      messageCredits: Number(current.messageCredits || 0) + plan.messageCredits,
      productId,
      planSar: plan.sar,
      lastPurchaseTokenHash: purchaseId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    tx.set(purchaseRef, {
      uid,
      productId,
      purchaseToken,
      orderId: playPurchase.orderId || null,
      purchaseTimeMillis: playPurchase.purchaseTimeMillis || null,
      purchaseState: Number(playPurchase.purchaseState),
      acknowledgementState: Number(playPurchase.acknowledgementState || 0),
      consumptionState: Number(playPurchase.consumptionState || 0),
      planSar: plan.sar,
      processed: true,
      verifiedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
  });

  try {
    const api = await publisher();
    const refreshed = await api.purchases.products.get({
      packageName: PACKAGE_NAME,
      productId,
      token: purchaseToken,
    });
    const state = refreshed.data || {};
    if (Number(state.consumptionState) !== 1) {
      await api.purchases.products.consume({ packageName: PACKAGE_NAME, productId, token: purchaseToken });
    }
  } catch (err) {
    console.error('Google Play purchase consumption failed', err?.message || err);
    throw new HttpsError('internal', 'Purchase was verified but Google Play did not confirm final processing. Please reopen the app.');
  }

  return {
    verified: true,
    productId,
    tier: plan.tier,
    planSar: plan.sar,
    messageCreditsAdded: plan.messageCredits,
    orderId: playPurchase.orderId || null,
  };
});
