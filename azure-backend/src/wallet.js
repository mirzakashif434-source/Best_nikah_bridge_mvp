const { app } = require("@azure/functions");
const { getPool, query } = require("./db");
const { requireAuth } = require("./auth");

const text = (value, max) => typeof value === "string" ? value.trim().slice(0, max) : "";

async function ensureUser(user) {
  const email = text(user.email, 320).toLowerCase();
  if (!user.uid || !email) throw new Error("AUTH_IDENTITY_REQUIRED");
  const result = await query(
    "INSERT INTO users(firebase_uid,email,email_verified_at) VALUES($1,$2,CASE WHEN $3 THEN now() ELSE NULL END) ON CONFLICT(firebase_uid) DO UPDATE SET email=EXCLUDED.email,email_verified_at=COALESCE(EXCLUDED.email_verified_at,users.email_verified_at),updated_at=now() RETURNING id,status,email",
    [user.uid, email, Boolean(user.email_verified)]
  );
  return result.rows[0];
}

app.http("walletGet", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "wallet",
  handler: requireAuth(async (request, context, user) => {
    try {
      const me = await ensureUser(user);
      await query("INSERT INTO wallet_accounts(user_id) VALUES($1) ON CONFLICT(user_id) DO NOTHING", [me.id]);
      const result = await query(
        "SELECT user_id,balance,currency,created_at,updated_at FROM wallet_accounts WHERE user_id=$1",
        [me.id]
      );
      return { status: 200, jsonBody: { ok: true, wallet: result.rows[0] } };
    } catch (error) {
      context.error("WALLET_GET_FAILED", error);
      return { status: 500, jsonBody: { ok: false, error: "WALLET_GET_FAILED" } };
    }
  })
});

app.http("walletTransactions", {
  methods: ["GET"],
  authLevel: "anonymous",
  route: "wallet/transactions",
  handler: requireAuth(async (request, context, user) => {
    try {
      const me = await ensureUser(user);
      const result = await query(
        "SELECT id,entry_type,amount,currency,reference_id,description,created_at FROM wallet_ledger WHERE user_id=$1 ORDER BY created_at DESC LIMIT 100",
        [me.id]
      );
      return { status: 200, jsonBody: { ok: true, transactions: result.rows } };
    } catch (error) {
      context.error("WALLET_TRANSACTIONS_FAILED", error);
      return { status: 500, jsonBody: { ok: false, error: "WALLET_TRANSACTIONS_FAILED" } };
    }
  })
});

app.http("walletWithdrawalCreate", {
  methods: ["POST"],
  authLevel: "anonymous",
  route: "wallet/withdrawals",
  handler: requireAuth(async (request, context, user) => {
    const pool = getPool();
    let client;
    try {
      const me = await ensureUser(user);
      if (me.status !== "active") {
        return { status: 403, jsonBody: { ok: false, error: "ACCOUNT_NOT_ACTIVE" } };
      }

      const body = await request.json();
      const amount = Number(body.amount);
      const currency = text(body.currency, 10).toUpperCase();
      const country = text(body.country, 80);
      const destination = text(body.destination, 500);

      if (!Number.isFinite(amount) || amount < 10) {
        return { status: 400, jsonBody: { ok: false, error: "MINIMUM_WITHDRAWAL_IS_10" } };
      }
      if (!["SAR", "USDT"].includes(currency)) {
        return { status: 400, jsonBody: { ok: false, error: "UNSUPPORTED_CURRENCY" } };
      }
      if (!country || !destination) {
        return { status: 400, jsonBody: { ok: false, error: "PAYOUT_DETAILS_REQUIRED" } };
      }

      client = await pool.connect();
      await client.query("BEGIN");
      await client.query(
        "INSERT INTO wallet_accounts(user_id,currency) VALUES($1,$2) ON CONFLICT(user_id) DO NOTHING",
        [me.id, currency]
      );

      const walletResult = await client.query(
        "SELECT balance,currency FROM wallet_accounts WHERE user_id=$1 FOR UPDATE",
        [me.id]
      );
      const wallet = walletResult.rows[0];

      if (!wallet || wallet.currency !== currency) {
        await client.query("ROLLBACK");
        return { status: 409, jsonBody: { ok: false, error: "WALLET_CURRENCY_MISMATCH" } };
      }
      if (Number(wallet.balance) < amount) {
        await client.query("ROLLBACK");
        return { status: 409, jsonBody: { ok: false, error: "INSUFFICIENT_BALANCE" } };
      }

      const withdrawal = await client.query(
        "INSERT INTO wallet_withdrawals(user_id,amount,currency,country,destination,status) VALUES($1,$2,$3,$4,$5,'pending') RETURNING id,amount,currency,country,status,created_at",
        [me.id, amount, currency, country, destination]
      );

      await client.query(
        "UPDATE wallet_accounts SET balance=balance-$1,updated_at=now() WHERE user_id=$2",
        [amount, me.id]
      );
      await client.query(
        "INSERT INTO wallet_ledger(user_id,entry_type,amount,currency,reference_id,description) VALUES($1,'withdrawal_hold',$2,$3,$4,'Withdrawal request held for review')",
        [me.id, amount, currency, String(withdrawal.rows[0].id)]
      );
      await client.query("COMMIT");

      return { status: 201, jsonBody: { ok: true, withdrawal: withdrawal.rows[0] } };
    } catch (error) {
      if (client) await client.query("ROLLBACK").catch(() => {});
      context.error("WALLET_WITHDRAWAL_FAILED", error);
      return { status: 500, jsonBody: { ok: false, error: "WALLET_WITHDRAWAL_FAILED" } };
    } finally {
      if (client) client.release();
    }
  })
});
