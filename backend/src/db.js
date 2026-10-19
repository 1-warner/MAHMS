'use strict';

// One shared PostgreSQL connection pool for the whole API.
//
// DATABASE_URL is the only setting needed, for example
//   postgres://mahms:mahms@localhost:5432/mahms
// Managed hosts (Render, Railway, Fly.io, Supabase) hand out a URL that needs TLS, so
// PGSSL=require turns that on.

const { Pool } = require('pg');

const connectionString = process.env.DATABASE_URL;

if (!connectionString) {
  throw new Error('DATABASE_URL is not set. Copy .env.example and fill it in.');
}

const pool = new Pool({
  connectionString,
  ssl: process.env.PGSSL === 'require' ? { rejectUnauthorized: false } : undefined,
  max: Number(process.env.PGPOOL_MAX || 10),
  idleTimeoutMillis: 30_000,
  connectionTimeoutMillis: 10_000,
});

pool.on('error', (err) => {
  // A pooled client dropped while idle. The pool replaces it, so this is logged, not fatal.
  console.error('[db] idle client error:', err.message);
});

/** Runs one statement and returns the pg result. */
function query(text, params) {
  return pool.query(text, params);
}

/** Returns the first row, or null. */
async function one(text, params) {
  const { rows } = await pool.query(text, params);
  return rows[0] || null;
}

/** Returns every row. */
async function many(text, params) {
  const { rows } = await pool.query(text, params);
  return rows;
}

/** Runs [fn] inside a transaction, passing it a dedicated client. */
async function transaction(fn) {
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (err) {
    await client.query('ROLLBACK');
    throw err;
  } finally {
    client.release();
  }
}

module.exports = { pool, query, one, many, transaction };
