const { onCall, HttpsError } = require('firebase-functions/v2/https');
const { onDocumentCreated } = require('firebase-functions/v2/firestore');
const admin = require('firebase-admin');
admin.initializeApp();
const db = admin.firestore();
const F = admin.firestore.FieldValue;
function auth(req){if(!req.auth)throw new HttpsError('unauthenticated','Sign-in required.');return req.auth.uid;}
function pair(a,b){if(!a||!b||a===b)throw new HttpsError('invalid-argument','Two different users are required.');return [a,b].sort().join('_');}
function isAdmin(req){return !!(req.auth&&req.auth.token&&req.auth.token.admin===true);}

exports.createMutualConnection=onCall(async req=>{
 const uid=auth(req), other=String(req.data?.otherUid||''); if(!other||other===uid)throw new HttpsError('invalid-argument','Invalid match.');
 const id=pair(uid,other), ref=db.collection('connections').doc(id);
 const [a,b]=await Promise.all([
  db.collection('interests').where('fromUid','==',uid).where('toUid','==',other).where('status','==','accepted').limit(1).get(),
  db.collection('interests').where('fromUid','==',other).where('toUid','==',uid).where('status','==','accepted').limit(1).get()
 ]);
 if(a.empty||b.empty)throw new HttpsError('failed-precondition','Both users must accept the interest.');
 await ref.set({uid1:id.split('_')[0],uid2:id.split('_')[1],status:'active',createdAt:F.serverTimestamp(),createdBy:uid},{merge:true});
 return {connectionId:id,status:'active'};
});

exports.requestWaliConnection=onCall(async req=>{
 const uid=auth(req), wali=String(req.data?.waliUid||''); if(!wali||wali===uid)throw new HttpsError('invalid-argument','Invalid Wali.');
 if(!(await db.collection('users').doc(wali).get()).exists)throw new HttpsError('not-found','Wali account not found.');
 await db.collection('waliConnections').doc(`${uid}_${wali}`).set({userUid:uid,waliUid:wali,status:'pending',requestedBy:uid,updatedAt:F.serverTimestamp()},{merge:true});
 return {status:'pending'};
});

exports.deleteMyAccount=onCall(async req=>{
 const uid=auth(req);
 const deleteQuery=async(q)=>{let snap=await q.limit(400).get();while(!snap.empty){await Promise.all(snap.docs.map(d=>db.recursiveDelete(d.ref)));snap=await q.limit(400).get();}};
 await db.recursiveDelete(db.collection('users').doc(uid));
 await deleteQuery(db.collection('interests').where('fromUid','==',uid));
 await deleteQuery(db.collection('interests').where('toUid','==',uid));
 await deleteQuery(db.collection('reports').where('reporterUid','==',uid));
 await deleteQuery(db.collection('reports').where('reportedUid','==',uid));
 await deleteQuery(db.collection('verifications').where('userUid','==',uid));
 await deleteQuery(db.collection('waliConnections').where('userUid','==',uid));
 await deleteQuery(db.collection('waliConnections').where('waliUid','==',uid));
 const c1=await db.collection('connections').where('uid1','==',uid).get(); const c2=await db.collection('connections').where('uid2','==',uid).get();
 await Promise.all([...c1.docs,...c2.docs].map(d=>db.recursiveDelete(d.ref)));
 await deleteQuery(db.collection('deletionRequests').where('userUid','==',uid));
 await admin.auth().deleteUser(uid);
 return {deleted:true};
});

exports.queueReport=onDocumentCreated('reports/{reportId}',async event=>{
 const r=event.data?.data();if(!r)return;
 await db.collection('moderationQueue').doc(event.params.reportId).set({reportId:event.params.reportId,reporterUid:r.reporterUid||null,reportedUid:r.reportedUid||null,reason:r.reason||'unspecified',status:'pending',createdAt:F.serverTimestamp()},{merge:true});
});
exports.setModerationDecision=onCall(async req=>{
 if(!isAdmin(req))throw new HttpsError('permission-denied','Admin access required.');
 const id=String(req.data?.reportId||''),decision=String(req.data?.decision||'');if(!id||!['reviewed','actioned','dismissed'].includes(decision))throw new HttpsError('invalid-argument','Invalid decision.');
 await db.collection('moderationQueue').doc(id).set({status:decision,moderatorUid:req.auth.uid,decidedAt:F.serverTimestamp()},{merge:true});return {status:decision};
});
exports.setVerificationStatus=onCall(async req=>{
 if(!isAdmin(req))throw new HttpsError('permission-denied','Admin access required.');
 const uid=String(req.data?.userUid||''),status=String(req.data?.status||'');if(!uid||!['verified','rejected','unverified'].includes(status))throw new HttpsError('invalid-argument','Invalid verification status.');
 await db.collection('users').doc(uid).update({verificationStatus:status,verificationUpdatedAt:F.serverTimestamp(),verificationBy:req.auth.uid});
 return {status};
});
