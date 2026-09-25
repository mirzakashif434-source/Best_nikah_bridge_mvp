const { Pool } = require("pg");

let pool;

function getRequiredEnv(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
}

function getPool() {
  if (!pool) {
    const sslMode = process.env.AZURE_DB_SSLMODE || "require";
    pool = new Pool({
      host: getRequiredEnv("AZURE_DB_HOST"),
      port: Number(process.env.AZURE_DB_PORT || 5432),
      database: getRequiredEnv("AZURE_DB_NAME"),
      user: getRequiredEnv("AZURE_DB_USER"),
      password: getRequiredEnv("AZURE_DB_ADMIN_PASSWORD"),
      ssl: sslMode === "disable" ? false : { rejectUnauthorized: true },
      max: Number(process.env.AZURE_DB_POOL_MAX || 3),
      idleTimeoutMillis: Number(process.env.AZURE_DB_IDLE_TIMEOUT_MS || 30000),
      connectionTimeoutMillis: Number(process.env.AZURE_DB_CONNECTION_TIMEOUT_MS || 10000)
    });
    pool.on("error", (error) => {
      console.error("PostgreSQL pool error:", error.message);
    });
  }
  return pool;
}

async function query(text, params = []) {
  return getPool().query(text, params);
}

async function checkDatabase() {
  const startedAt = Date.now();
  const result = await query("SELECT current_database() AS database_name, NOW() AS server_time");
  return {
    database: result.rows[0].database_name,
    serverTime: result.rows[0].server_time,
    latencyMs: Date.now() - startedAt
  };
}

module.exports = { getPool, query, checkDatabase };
