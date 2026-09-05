// Additive functions entrypoint: preserve every existing export from index.js.
const existing = require('./index');
const helpLine = require('./helpLineAI');
const helpLineAdmin = require('./helpLineAdmin');
const premiumPlans = require('./premiumPlans');
const ownerDashboard = require('./ownerDashboard');
Object.assign(existing, helpLine, helpLineAdmin, premiumPlans, ownerDashboard);
module.exports = existing;
