const { onCall } = require('firebase-functions/v2/https');
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');

const db = admin.firestore();
const F = admin.firestore.FieldValue;
const SUMMARY = db.collection('ownerEarningsSummary').doc('main');

const PLAN_VALUE_SAR_MINOR = {
  bnb_plus_20: 2000,
  bnb_plus_40: 4000,
  bnb_plus_60: 6000,
};

function requireAdmin(req) {
  if (!req.auth?.token?.admin) throw new (require('firebase-functions/v2/https').HttpsError)('permission-denied', 'Admin access required.');
  return req.auth.uid;
}

function currency(value) {
  const c = String(value || '').trim().toUpperCase();
  if (!['SAR', 'PKR', 'USDT'].includes(c)) {
    throw new (require('firebase-functions/v2/https').HttpsError)('invalid-argument', 'Supported settlement currencies are SAR, PKR and USDT.');
  }
  return c;
}

function amountMinor(value) {
  const n = Number(value);
  if (!Number.isFinite(n) || n <= 0 || n > 1000000000) {
    throw new (require('firebase-functions/v2/https').HttpsError)('invalid-argument', 'Invalid amount.');
  }
  return Math.round(n * 100);
}

function validDestination(c, value) {
  const d = String(value || '').trim();
  if (d.length < 6 || d.length > 300) return false;
  if (c === 'USDT' && !/^(T[1-9A-HJ-NP-Za-km-z]{33}|0x[a-fA-F0-9]{40})$/.test(d)) return false;
  return true;
}

exports.recordVerifiedPlaySale = onDocumentCreated('playPurchases/{purchaseId}', async event => {
  const purchase = event.data?.data();
  if (!purchase) return;
  const productId = String(purchase.productId || '');
  const planValue = PLAN_VALUE_SAR_MINOR[productId];
  if (!planValue) return;

  const saleRef = db.collection('ownerEarnings').doc(event.params.purchaseId);
  await db.runTransaction(async tx => {
    const existing = await tx.get(saleRef);
    if (existing.exists) return;
    tx.set(saleRef, {
      purchaseId: event.params.purchaseId,
      productId,
      uid: String(purchase.uid || ''),
      orderId: purchase.orderId || null,
      status: 'verified_sale',
      planValueSarMinor: planValue,
      actualSettlementAmountMinor: null,
      actualSettlementCurrency: null,
      createdAt: F.serverTimestamp(),
      verifiedAt: purchase.verifiedAt || F.serverTimestamp(),
      note: 'Verified Google Play purchase. Plan value is a reference value; actual cash settlement is determined by Google Play earnings reports and payout.',
    });
    tx.set(SUMMARY, {
      verifiedSalesCount: F.increment(1),
      verifiedPlanValueSarMinor: F.increment(planValue),
      updatedAt: F.serverTimestamp(),
    }, { merge: true });
  });
});

exports.getOwnerEarnings = onCall(async req => {
  requireAdmin(req);
  const [summarySnap, settingsSnap] = await Promise.all([
    SUMMARY.get(),
    db.collection('ownerSettings').doc('settlement').get(),
  ]);
  const s = summarySnap.exists ? summarySnap.data() : {};
  const settings = settingsSnap.exists ? settingsSnap.data() : {};
  return {
    verifiedSalesCount: Number(s.verifiedSalesCount || 0),
    verifiedPlanValueSar: Number(s.verifiedPlanValueSarMinor || 0) / 100,
    availableSar: Number(s.availableSarMinor || 0) / 100,
    availablePkr: Number(s.availablePkrMinor || 0) / 100,
    availableUsdt: Number(s.availableUsdtMinor || 0) / 100,
    pendingSar: Number(s.pendingSarMinor || 0) / 100,
    pendingPkr: Number(s.pendingPkrMinor || 0) / 100,
    pendingUsdt: Number(s.pendingUsdtMinor || 0) / 100,
    settledSar: Number(s.settledSarMinor || 0) / 100,
    settledPkr: Number(s.settledPkrMinor || 0) / 100,
    settledUsdt: Number(s.settledUsdtMinor || 0) / 100,
    settlementProfile: {
      country: String(settings.country || ''),
      currency: String(settings.currency || ''),
      destination: String(settings.destination || ''),
      label: String(settings.label || ''),
    },
    providerMode: String(process.env.OWNER_PAYOUT_PROVIDER || 'not_configured'),
    realCashNote: 'Verified sales are not treated as withdrawable cash until a real provider/Google Play settlement is reconciled.',
  };
});

exports.saveOwnerSettlementProfile = onCall(async req => {
  const adminUid = requireAdmin(req);
  const c = currency(req.data?.currency);
  const destination = String(req.data?.destination || '').trim();
  const country = String(req.data?.country || '').trim();
  const label = String(req.data?.label || '').trim();
  if (!validDestination(c, destination) || country.length < 2 || country.length > 100 || label.length > 80) {
    throw new (require('firebase-functions/v2/https').HttpsError)('invalid-argument', 'Invalid settlement destination.');
  }
  await db.collection('ownerSettings').doc('settlement').set({
    country,
    currency: c,
    destination,
    label,
    updatedBy: adminUid,
    updatedAt: F.serverTimestamp(),
  }, { merge: true });
  return { saved: true, currency: c, country, label };
});

exports.recordOwnerProviderSettlement = onCall(async req => {
  const adminUid = requireAdmin(req);
  const c = currency(req.data?.currency);
  const amount = amountMinor(req.data?.amount);
  const provider = String(req.data?.provider || '').trim();
  const reference = String(req.data?.providerReference || '').trim();
  if (provider.length < 2 || provider.length > 80 || reference.length < 3 || reference.length > 200) {
    throw new (require('firebase-functions/v2/https').HttpsError)('invalid-argument', 'A real provider and provider reference are required.');
  }
  const ref = db.collection('ownerProviderSettlements').doc(reference.replace(/[^a-zA-Z0-9_-]/g, '_').slice(0, 100));
  await db.runTransaction(async tx => {
    const old = await tx.get(ref);
    if (old.exists) return;
    const availableKey = c === 'SAR' ? 'availableSarMinor' : c === 'PKR' ? 'availablePkrMinor' : 'availableUsdtMinor';
    tx.set(ref, { currency: c, amountMinor: amount, provider, providerReference: reference, recordedBy: adminUid, createdAt: F.serverTimestamp() });
    tx.set(SUMMARY, { [availableKey]: F.increment(amount), updatedAt: F.serverTimestamp() }, { merge: true });
  });
  return { recorded: true, currency: c, amount: amount / 100, providerReference: reference };
});

exports.requestOwnerSettlement = onCall(async req => {
  const adminUid = requireAdmin(req);
  const c = currency(req.data?.currency);
  const amount = amountMinor(req.data?.amount);
  const destination = String(req.data?.destination || '').trim();
  const country = String(req.data?.country || '').trim();
  if (!validDestination(c, destination) || country.length < 2) {
    throw new (require('firebase-functions/v2/https').HttpsError)('invalid-argument', 'Valid settlement destination and country are required.');
  }
  const availableKey = c === 'SAR' ? 'availableSarMinor' : c === 'PKR' ? 'availablePkrMinor' : 'availableUsdtMinor';
  const pendingKey = c === 'SAR' ? 'pendingSarMinor' : c === 'PKR' ? 'pendingPkrMinor' : 'pendingUsdtMinor';
  const ref = db.collection('ownerSettlements').doc();
  await db.runTransaction(async tx => {
    const s = await tx.get(SUMMARY);
    const d = s.exists ? s.data() : {};
    const available = Number(d[availableKey] || 0);
    if (amount > available) throw new (require('firebase-functions/v2/https').HttpsError)('failed-precondition', 'Insufficient settled/available balance.');
    tx.set(SUMMARY, { [availableKey]: available - amount, [pendingKey]: Number(d[pendingKey] || 0) + amount, updatedAt: F.serverTimestamp() }, { merge: true });
    tx.set(ref, { currency: c, amountMinor: amount, country, destination, status: 'pending_provider', requestedBy: adminUid, createdAt: F.serverTimestamp(), updatedAt: F.serverTimestamp() });
  });
  return { submitted: true, settlementId: ref.id, status: 'pending_provider' };
});

exports.markOwnerSettlementPaid = onCall(async req => {
  const adminUid = requireAdmin(req);
  const settlementId = String(req.data?.settlementId || '');
  const providerReference = String(req.data?.providerReference || '').trim();
  if (!settlementId || providerReference.length < 3) throw new (require('firebase-functions/v2/https').HttpsError)('invalid-argument', 'Settlement ID and real provider reference are required.');
  const ref = db.collection('ownerSettlements').doc(settlementId);
  await db.runTransaction(async tx => {
    const s = await tx.get(ref);
    if (!s.exists) throw new (require('firebase-functions/v2/https').HttpsError)('not-found', 'Settlement not found.');
    const d = s.data() || {};
    if (d.status !== 'pending_provider') throw new (require('firebase-functions/v2/https').HttpsError)('failed-precondition', 'Settlement is not pending.');
    const c = currency(d.currency);
    const amount = Number(d.amountMinor || 0);
    const pendingKey = c === 'SAR' ? 'pendingSarMinor' : c === 'PKR' ? 'pendingPkrMinor' : 'pendingUsdtMinor';
    const settledKey = c === 'SAR' ? 'settledSarMinor' : c === 'PKR' ? 'settledPkrMinor' : 'settledUsdtMinor';
    tx.update(ref, { status: 'paid', providerReference, paidBy: adminUid, paidAt: F.serverTimestamp(), updatedAt: F.serverTimestamp() });
    tx.set(SUMMARY, { [pendingKey]: F.increment(-amount), [settledKey]: F.increment(amount), updatedAt: F.serverTimestamp() }, { merge: true });
  });
  return { paid: true, settlementId, providerReference };
});

exports.listOwnerEarnings = onCall(async req => {
  requireAdmin(req);
  const [sales, settlements] = await Promise.all([
    db.collection('ownerEarnings').orderBy('createdAt', 'desc').limit(50).get(),
    db.collection('ownerSettlements').orderBy('createdAt', 'desc').limit(50).get(),
  ]);
  return {
    sales: sales.docs.map(d => { const x = d.data() || {}; return { id: d.id, productId: x.productId || '', status: x.status || '', planValueSar: Number(x.planValueSarMinor || 0) / 100, orderId: x.orderId || null, createdAt: x.createdAt?.toDate?.()?.toISOString() || null }; }),
    settlements: settlements.docs.map(d => { const x = d.data() || {}; return { id: d.id, currency: x.currency || '', amount: Number(x.amountMinor || 0) / 100, country: x.country || '', status: x.status || '', providerReference: x.providerReference || null, createdAt: x.createdAt?.toDate?.()?.toISOString() || null }; }),
  };
});
