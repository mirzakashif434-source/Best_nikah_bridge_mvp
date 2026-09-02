// Additive functions entrypoint: preserve every existing export from index.js.
const existing = require('./index');
const helpLine = require('./helpLineAI');
Object.assign(existing, helpLine);
module.exports = existing;
