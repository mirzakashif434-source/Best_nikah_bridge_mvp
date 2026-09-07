// Additive functions entrypoint: preserve every existing export from index.js.
// Production export verification: wallet + premium + owner dashboard + community + safety + verification + verification admin + AdMob remain additive.
// No existing index.js function is deleted or replaced.
const existing = require('./index');
const helpLine = require('./helpLineAI');
const helpLineAdmin = require('./helpLineAdmin');
const premiumPlans = require('./premiumPlans');
const ownerDashboard = require('./ownerDashboard');
const community = require('./community');
const communitySafety = require('./communitySafety');
const verification = require('./verification');
const verificationAdmin = require('./verificationAdmin');
const admobRewarded = require('./admobRewarded');
Object.assign(existing, helpLine, helpLineAdmin, premiumPlans, ownerDashboard, community, communitySafety, verification, verificationAdmin, admobRewarded);
module.exports = existing;
