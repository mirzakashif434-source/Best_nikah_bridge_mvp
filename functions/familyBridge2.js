const { onCall, HttpsError } = require('firebase-functions/v2/https');
const admin = require('firebase-admin');

const db = admin.firestore();
const F = admin.firestore.FieldValue;

function uid(req){ if(!req.auth) throw new HttpsError('unauthenticated','Sign-in required.'); return req.auth.uid; }
function pair(a,b){ if(!a||!b||a===b) throw new HttpsError('invalid-argument','Two different users are required.'); return [a,b].sort().join('_'); }
async function connected(a,b){ const id=pair(a,b); const c=await db.collection('connections').doc(id).get(); return c.exists && c.data()?.status==='active'; }

// Additive v2 flow. Existing Family Bridge functions remain untouched.
exports.createFamilyBridgeV2=onCall(async req=>{
  const me=uid(req), connectionId=String(req.data?.connectionId||''), familyUid=String(req.data?.familyUid||'');
  if(!connectionId||!familyUid||familyUid===me) throw new HttpsError('invalid-argument','A mutual connection and a different family account are required.');
  const parts=connectionId.split('_');
  if(parts.length!==2||!parts.includes(me)||!(await connected(parts[0],parts[1]))) throw new HttpsError('permission-denied','Active mutual connection required.');
  const family=await db.collection('users').doc(familyUid).get();
  if(!family.exists||family.data()?.profileActive!==true) throw new HttpsError('failed-precondition','Family account is not active.');
  const ref=db.collection('familyBridges').doc();
  const now=admin.firestore.Timestamp.now();
  await ref.set({version:2,connectionId,createdBy:me,primaryUserUids:parts,familyUid,status:'pending',participants:[parts[0],parts[1],familyUid],consent:{[me]:true,[familyUid]:false},createdAt:now,updatedAt:now});
  return {bridgeId:ref.id,status:'pending'};
});

exports.respondFamilyBridgeV2=onCall(async req=>{
  const me=uid(req), bridgeId=String(req.data?.bridgeId||''), decision=String(req.data?.decision||'');
  if(!bridgeId||!['accept','reject'].includes(decision)) throw new HttpsError('invalid-argument','Valid bridge response is required.');
  const ref=db.collection('familyBridges').doc(bridgeId), snap=await ref.get();
  if(!snap.exists) throw new HttpsError('not-found','Family Bridge not found.');
  const d=snap.data()||{};
  if(d.version!==2||d.familyUid!==me) throw new HttpsError('permission-denied','Only the invited family account can respond.');
  if(d.status!=='pending') throw new HttpsError('failed-precondition','This Family Bridge is no longer pending.');
  if(decision==='reject'){ await ref.update({status:'rejected',rejectedBy:me,rejectedAt:F.serverTimestamp(),updatedAt:F.serverTimestamp()}); return {status:'rejected'}; }
  await ref.update({status:'active',consent:{...(d.consent||{}),[me]:true},acceptedBy:me,acceptedAt:F.serverTimestamp(),updatedAt:F.serverTimestamp()});
  return {status:'active'};
});

exports.listMyFamilyBridgesV2=onCall(async req=>{
  const me=uid(req);
  const [a,b,c]=await Promise.all([
    db.collection('familyBridges').where('participants','array-contains',me).orderBy('updatedAt','desc').limit(50).get(),
    db.collection('familyBridges').where('familyUid','==',me).where('status','==','pending').limit(50).get(),
    db.collection('familyBridges').where('createdBy','==',me).orderBy('updatedAt','desc').limit(50).get()
  ]);
  const map=new Map(); [...a.docs,...b.docs,...c.docs].forEach(x=>map.set(x.id,{id:x.id,...x.data()}));
  return {bridges:[...map.values()].map(x=>({id:x.id,connectionId:x.connectionId,status:x.status,createdBy:x.createdBy,familyUid:x.familyUid,participants:x.participants||[],consent:x.consent||{},updatedAt:x.updatedAt?.toMillis?.()||null}))};
});

exports.setFamilyBridgeConsentV2=onCall(async req=>{
  const me=uid(req), bridgeId=String(req.data?.bridgeId||''), enabled=Boolean(req.data?.enabled);
  const ref=db.collection('familyBridges').doc(bridgeId), snap=await ref.get();
  if(!snap.exists) throw new HttpsError('not-found','Family Bridge not found.');
  const d=snap.data()||{};
  if(d.version!==2||!Array.isArray(d.participants)||!d.participants.includes(me)||d.status!=='active') throw new HttpsError('permission-denied','Active Family Bridge access required.');
  await ref.update({[`consent.${me}`]:enabled,updatedAt:F.serverTimestamp(),status:enabled?'active':'paused'});
  return {enabled,status:enabled?'active':'paused'};
});

exports.sendFamilyQuestionV2=onCall(async req=>{
  const me=uid(req), bridgeId=String(req.data?.bridgeId||''), text=String(req.data?.text||'').trim();
  if(!bridgeId||!text||text.length>2000) throw new HttpsError('invalid-argument','Question is required.');
  const ref=db.collection('familyBridges').doc(bridgeId), snap=await ref.get();
  if(!snap.exists) throw new HttpsError('not-found','Family Bridge not found.');
  const d=snap.data()||{};
  if(d.version!==2||!Array.isArray(d.participants)||!d.participants.includes(me)||d.status!=='active'||d.consent?.[me]!==true) throw new HttpsError('permission-denied','Active Family Bridge consent is required.');
  const q=ref.collection('questions').doc();
  await q.set({fromUid:me,text,status:'open',createdAt:F.serverTimestamp(),updatedAt:F.serverTimestamp()});
  await ref.update({updatedAt:F.serverTimestamp()});
  return {questionId:q.id};
});
