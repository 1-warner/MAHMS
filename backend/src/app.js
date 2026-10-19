'use strict';

// Builds the Express application.
//
// Kept separate from server.js so the automated tests can mount the same app with supertest
// without opening a port.

const express = require('express');
const db = require('./db');
const { authenticate } = require('./auth');

const meRoutes = require('./routes/me');
const appointmentRoutes = require('./routes/appointments');
const doctorRoutes = require('./routes/doctors');
const prescriptionRoutes = require('./routes/prescriptions');
const paymentRoutes = require('./routes/payments');
const emergencyRoutes = require('./routes/emergencies');

function createApp() {
  const app = express();

  app.disable('x-powered-by');
  app.use(express.json({ limit: '256kb' }));

  // The website is served from a different origin, so the browser needs these headers.
  // ALLOWED_ORIGIN can be a comma-separated list; it defaults to "*" for the demo.
  const allowed = (process.env.ALLOWED_ORIGIN || '*').split(',').map((s) => s.trim());
  app.use((req, res, next) => {
    const origin = req.get('origin');
    if (allowed.includes('*')) {
      res.set('Access-Control-Allow-Origin', '*');
    } else if (origin && allowed.includes(origin)) {
      res.set('Access-Control-Allow-Origin', origin);
      res.set('Vary', 'Origin');
    }
    res.set('Access-Control-Allow-Headers', 'Authorization, Content-Type');
    res.set('Access-Control-Allow-Methods', 'GET, POST, PATCH, OPTIONS');
    if (req.method === 'OPTIONS') return res.sendStatus(204);
    return next();
  });

  // One line per request in the server log. The demo video shows these appearing as the app
  // is used.
  app.use((req, res, next) => {
    const started = Date.now();
    res.on('finish', () => {
      console.log(`${req.method} ${req.originalUrl} -> ${res.statusCode} (${Date.now() - started}ms)`);
    });
    next();
  });

  /**
   * GET /health — open, no token needed. The app's Settings screen calls this to show
   * "Server status: connected".
   */
  app.get('/health', async (_req, res) => {
    try {
      const row = await db.one('SELECT now() AS now');
      res.json({
        status: 'ok',
        service: 'mahms-api',
        version: require('../package.json').version,
        database: 'connected',
        time: row.now,
      });
    } catch (err) {
      res.status(503).json({
        status: 'degraded',
        service: 'mahms-api',
        database: 'unreachable',
        error: err.message,
      });
    }
  });

  // Everything under /api needs a verified Firebase ID token.
  const api = express.Router();
  api.use(authenticate);
  api.use(meRoutes);
  api.use(doctorRoutes);
  api.use(paymentRoutes);
  api.use('/appointments', appointmentRoutes);
  api.use('/prescriptions', prescriptionRoutes);
  api.use('/emergencies', emergencyRoutes);
  app.use('/api', api);

  app.use((req, res) => {
    res.status(404).json({ error: `No route for ${req.method} ${req.originalUrl}` });
  });

  // Last-resort handler. A database constraint becomes a 400 so the client can show a useful
  // message; anything else is a 500 and the detail stays in the server log.
  // eslint-disable-next-line no-unused-vars
  app.use((err, req, res, _next) => {
    const pgCodes = {
      '23505': 'That record already exists',
      '23503': 'A referenced record does not exist',
      '23514': 'A value is outside the range the database allows',
      '22P02': 'A value is the wrong type',
    };
    if (err && pgCodes[err.code]) {
      console.warn(`[api] rejected ${req.method} ${req.originalUrl}: ${err.message}`);
      return res.status(400).json({ error: pgCodes[err.code], detail: err.detail || undefined });
    }
    console.error(`[api] failed ${req.method} ${req.originalUrl}:`, err);
    return res.status(500).json({ error: 'Something went wrong on the server' });
  });

  return app;
}

module.exports = { createApp };
