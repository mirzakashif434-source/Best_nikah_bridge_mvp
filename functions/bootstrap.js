// Additive functions entrypoint: preserve every existing export from index.js.
const existing = require('./index');
const helpLine = require('./helpLineAI');
const helpLineAdmin = require('./helpLineAdmin');
const premiumPlans = require('./premiumPlans');
Object.assign(existing, helpLine, helpLineAdmin, premiumPlans);
module.exports = existing;
