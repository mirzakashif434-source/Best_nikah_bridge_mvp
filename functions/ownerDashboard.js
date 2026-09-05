const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

const db = admin.firestore();
const PLAN_NAMES = {
  bnb_plus_20: '20 SAR',
  bnb_plus_40: '40 SAR',
  bnb_plus_60: '60 SAR',
};

function requireAdmin(req) {
  if (!req.auth?.token?.admin) throw new HttpsError('permission-denied', 'Admin access required.');
}

function isoDate(value) {
  return value?.toDate?.()?.toISOString?.() || null;
}

exports.getOwnerEarningsDashboard = onCall(async req => {
  requireAdmin(req);
  const snap = await db.collection('ownerEarnings').orderBy('createdAt', 'desc').limit(5000).get();
  const now = new Date();
  const currentMonth = `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, '0')}`;
  const monthly = new Map();
  const users = new Map();
  const planCounts = { bnb_plus_20: 0, bnb_plus_40: 0, bnb_plus_60: 0 };
  let totalSar = 0;
  let currentMonthUpgrades = 0;
  let currentMonthSar = 0;

  for (const doc of snap.docs) {
    const x = doc.data() || {};
    const productId = String(x.productId || '');
    const amount = Number(x.planValueSarMinor || 0) / 100;
    const date = x.createdAt?.toDate?.() || x.verifiedAt?.toDate?.() || null;
    const month = date ? `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}` : 'unknown';
    const uid = String(x.uid || '');

    totalSar += amount;
    if (planCounts[productId] !== undefined) planCounts[productId] += 1;

    if (!monthly.has(month)) monthly.set(month, { month, upgrades: 0, planValueSar: 0, plan20: 0, plan40: 0, plan60: 0 });
    const m = monthly.get(month);
    m.upgrades += 1;
    m.planValueSar += amount;
    if (productId === 'bnb_plus_20') m.plan20 += 1;
    if (productId === 'bnb_plus_40') m.plan40 += 1;
    if (productId === 'bnb_plus_60') m.plan60 += 1;

    if (month === currentMonth) {
      currentMonthUpgrades += 1;
      currentMonthSar += amount;
    }

    if (uid) {
      const old = users.get(uid) || { uid, upgrades: 0, planValueSar: 0, lastUpgradeAt: null, plans: [] };
      old.upgrades += 1;
      old.planValueSar += amount;
      if (!old.lastUpgradeAt && date) old.lastUpgradeAt = date.toISOString();
      old.plans.push(PLAN_NAMES[productId] || productId || 'Unknown');
      users.set(uid, old);
    }
  }

  const userIds = [...users.keys()].slice(0, 100);
  const userDocs = await Promise.all(userIds.map(uid => db.collection('users').doc(uid).get()));
  for (const d of userDocs) {
    if (!d.exists) continue;
    const uid = d.id;
    const u = d.data() || {};
    const row = users.get(uid);
    if (row) {
      row.name = String(u.name || u.fullName || u.displayName || '');
      row.email = String(u.email || '');
    }
  }

  return {
    currentMonth,
    currentMonthUpgrades,
    currentMonthPlanValueSar: Number(currentMonthSar.toFixed(2)),
    totalVerifiedUpgrades: snap.size,
    totalVerifiedPlanValueSar: Number(totalSar.toFixed(2)),
    planCounts,
    monthly: [...monthly.values()].sort((a, b) => b.month.localeCompare(a.month)),
    users: [...users.values()].sort((a, b) => b.planValueSar - a.planValueSar).slice(0, 100),
    note: 'Verified Google Play sales ledger values. Actual merchant settlement and payout remain controlled by Google Play.'
  };
});
