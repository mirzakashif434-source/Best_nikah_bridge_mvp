// node --test azure-backend/tests/owner-report-only.test.js
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const root = path.resolve(__dirname, "..");
const read = (name) => fs.readFileSync(path.join(root, "src", name), "utf8");
const helper = read("ownerReportAccess.js");
function authFor(dbUser) {
  const mod = { exports: {} };
  const sandbox = {
    module: mod, exports: mod.exports,
    require(id) { assert.equal(id, "./db"); return { query: async (_sql, params) => {
      assert.match(_sql, /azure_subject=\\$1/);
      assert.match(_sql, /status='active'/);
      return { rows: dbUser && dbUser.subject === params[0] ? [dbUser] : [] };
    } }; }
  };
  vm.runInNewContext(helper, sandbox);
  return mod.exports.requireOwnerReportAccess;
}
function caller(email, subject="sub-121212") { return { email, azure_subject: subject }; }
const email = "mirzakashif121212@gmail.com";
test("active user with verified DB subject and exact viewer email may read reports", async () => {
  const fn = authFor({subject:"sub-121212",email,role:"user",status:"active"});
  assert.equal((await fn(caller(email))).role, "user");
});
test("wrong Gmail, subject, missing subject, deactivated and other users fail closed", async () => {
  const fn = authFor({subject:"sub-121212",email,role:"user",status:"active"});
  for (const identity of [caller("other@gmail.com"),caller(email,"other-sub"),caller(email,"")]) {
    await assert.rejects(fn(identity), e => e.statusCode === 403);
  }
  await assert.rejects(authFor(null)(caller(email)), e => e.statusCode === 403);
  await assert.rejects(authFor({subject:"sub-121212",email,role:"blocked",status:"active"})(caller(email)), e => e.statusCode === 403);
});
test("existing admin and moderator report access remains", async () => {
  for (const role of ["admin","moderator"]) {
    const fn = authFor({subject:"sub-121212",email:"admin@example.com",role,status:"active"});
    assert.equal((await fn(caller("admin@example.com"))).role, role);
  }
});
test("only GET report handlers use read-only check; payout mutation handlers remain admin guarded", () => {
  const analytics = read("ownerAnalytics.js");
  const earnings = read("ownerEarnings.js");
  assert.match(analytics, /methods:\\["GET"\\],authLevel:"anonymous",route:"owner\\/live-analytics"/);
  assert.match(earnings, /methods:\\["GET"\\],authLevel:"anonymous",route:"owner\\/earnings"/);
  assert.match(analytics, /await requireOwnerReportAccess\\(user\\)/);
  assert.match(earnings, /try\\{await requireOwnerReportAccess\\(user\\);const \\[s,rows/);
  assert.equal((earnings.match(/await admin\\(user\\)/g)||[]).length, 4);
  for (const route of ["admin/owner/settlement-profile","admin/owner/provider-settlement","admin/owner/settlements"]) {
    assert.ok(earnings.includes(route));
  }
});
