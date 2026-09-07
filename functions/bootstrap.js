// Additive functions entrypoint: preserve every existing export from index.js.
// Production export verification: wallet + premium + owner dashboard + community remain additive.
// Backend redeploy trigger: keep all existing exports and logic unchanged.
// Production backend deployment verification: deploy the current real callable functions before app testing.
const existing = require('./index');
const helpLine = require('./helpLineAI');
const helpLineAdmin = require('./helpLineAdmin');
const premiumPlans = require('./premiumPlans');
const ownerDashboard = require('./ownerDashboard');
const community = require('./community');
Object.assign(existing, helpLine, helpLineAdmin, premiumPlans, ownerDashboard, community);
module.exports = existing;
