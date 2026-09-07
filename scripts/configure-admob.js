const fs = require('fs');
const admin = require('../functions/node_modules/firebase-admin');

const config = JSON.parse(fs.readFileSync('config/admob-production.json', 'utf8'));
const unit = String(config.rewardedAdUnitId || '').trim();
if (!/^ca-app-pub-\d{16}\/\d+$/.test(unit)) throw new Error('Invalid production AdMob rewarded ad unit ID.');

const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
if (!raw) throw new Error('FIREBASE_SERVICE_ACCOUNT is not configured.');
const credentials = JSON.parse(raw);
admin.initializeApp({ credential: admin.credential.cert(credentials), projectId: 'best-nikah-bridge' });

(async () => {
  await admin.firestore().collection('publicConfig').doc('admob').set({
    rewardedAdUnitId: unit,
    rewardItem: String(config.rewardItem || 'Message Credit'),
    rewardAmount: Number(config.rewardAmount || 1),
    dailyLimit: Number(config.dailyLimit || 2),
    updatedAt: admin.firestore.FieldValue.serverTimestamp()
  }, { merge: true });
  console.log('PRODUCTION ADMOB CONFIGURED:', unit);
  process.exit(0);
})().catch(err => { console.error(err); process.exit(1); });
