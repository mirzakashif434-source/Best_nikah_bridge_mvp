// Additive functions entrypoint: preserve every existing export from index.js.
const existing = require('./index');
const helpLine = require('./helpLineAI');
const helpLineAdmin = require('./helpLineAdmin');
Object.assign(existing, helpLine, helpLineAdmin);
module.exports = existing;
