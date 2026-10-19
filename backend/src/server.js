'use strict';

// Entry point. Reads .env when one is present, then listens.

const path = require('path');
const fs = require('fs');

// Tiny .env reader so the project needs no extra dependency for local runs. Hosts such as
// Render set real environment variables, where no .env file exists and this does nothing.
(function loadDotEnv() {
  const file = path.join(__dirname, '..', '.env');
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, 'utf8').split('\n')) {
    const match = /^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$/.exec(line);
    if (!match) continue;
    const [, key, rawValue] = match;
    if (process.env[key] !== undefined) continue;
    process.env[key] = rawValue.replace(/^["'](.*)["']$/, '$1');
  }
})();

const { createApp } = require('./app');
const { initFirebase, testAuthAllowed } = require('./auth');
const db = require('./db');

const port = Number(process.env.PORT || 3000);
const app = createApp();

if (testAuthAllowed()) {
  console.warn('[server] MAHMS_AUTH_MODE=test: stub sign-in tokens are accepted. Never use this on a deployed server.');
} else if (!initFirebase()) {
  console.warn('[server] No Firebase credentials found, so /api routes will answer 503.');
  console.warn('[server] Set GOOGLE_APPLICATION_CREDENTIALS or FIREBASE_SERVICE_ACCOUNT.');
}

const server = app.listen(port, () => {
  console.log(`[server] MAHMS API listening on port ${port}`);
  console.log(`[server] Health check: http://localhost:${port}/health`);
});

// Finish in-flight requests, then close the database pool.
for (const signal of ['SIGTERM', 'SIGINT']) {
  process.on(signal, () => {
    console.log(`[server] ${signal} received, shutting down`);
    server.close(async () => {
      await db.pool.end().catch(() => {});
      process.exit(0);
    });
  });
}
