'use strict';

// Authentication and role checks.
//
// Firebase Authentication stays the identity provider: the Android app and the website sign
// the user in, then send the Firebase ID token on every API call as
//   Authorization: Bearer <idToken>
// This file verifies that token with firebase-admin and maps the verified e-mail onto a row
// in the users table, which is where the hospital role lives. The API never sees a password.

const admin = require('firebase-admin');
const db = require('./db');

const ROLES = ['Patient', 'Doctor', 'Nurse', 'Pharmacist', 'Admin', 'Responder'];

// Role names the Android app uses, mapped onto the role names in the database.
const ROLE_ALIASES = {
  'hospital administrator': 'Admin',
  administrator: 'Admin',
  'emergency responder': 'Responder',
};

function normaliseRole(label) {
  if (!label) return null;
  const clean = String(label).trim();
  const alias = ROLE_ALIASES[clean.toLowerCase()];
  if (alias) return alias;
  return ROLES.find((r) => r.toLowerCase() === clean.toLowerCase()) || null;
}

let firebaseReady = false;

/**
 * Starts firebase-admin once. Credentials come from the environment:
 *  - GOOGLE_APPLICATION_CREDENTIALS, the path to a service-account JSON file, or
 *  - FIREBASE_SERVICE_ACCOUNT, the same JSON inlined (handy on hosts that only offer env vars).
 * With neither, token verification cannot run and every protected route answers 503.
 */
function initFirebase() {
  if (firebaseReady || admin.apps.length > 0) {
    firebaseReady = admin.apps.length > 0;
    return firebaseReady;
  }
  try {
    if (process.env.FIREBASE_SERVICE_ACCOUNT) {
      const serviceAccount = JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT);
      admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
    } else if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      admin.initializeApp({ credential: admin.credential.applicationDefault() });
    } else {
      return false;
    }
    firebaseReady = true;
  } catch (err) {
    console.error('[auth] firebase-admin failed to start:', err.message);
    firebaseReady = false;
  }
  return firebaseReady;
}

/**
 * True when the automated tests are allowed to send a stub token instead of a real one.
 *
 * This exists so `npm test` and the CI workflow can exercise the routes without a Firebase
 * service account. It is deliberately hard to switch on by accident: MAHMS_AUTH_MODE must be
 * exactly "test" AND NODE_ENV must not be "production". Never set MAHMS_AUTH_MODE on a
 * deployed server.
 */
function testAuthAllowed() {
  return process.env.MAHMS_AUTH_MODE === 'test' && process.env.NODE_ENV !== 'production';
}

/** Reads the bearer token out of the Authorization header. */
function bearerToken(req) {
  const header = req.get('authorization') || '';
  const match = /^Bearer\s+(.+)$/i.exec(header.trim());
  return match ? match[1].trim() : null;
}

/**
 * Finds the users row for a verified identity, creating it when the e-mail is new.
 *
 * A brand new account becomes a Patient, which matches the app: registration only ever makes
 * patient accounts and staff accounts are issued by an administrator. A caller therefore
 * cannot pick its own role by signing up, which is the hole the Firestore-only build had.
 */
async function findOrCreateUser({ uid, email, name, phone }) {
  const cleanEmail = String(email).trim().toLowerCase();

  let user = await db.one('SELECT * FROM users WHERE email = $1', [cleanEmail]);

  if (!user) {
    user = await db.one(
      `INSERT INTO users (firebase_uid, email, full_name, phone, role)
       VALUES ($1, $2, $3, $4, 'Patient')
       RETURNING *`,
      [uid || null, cleanEmail, name || '', phone || ''],
    );
    console.log(`[auth] new Patient account recorded for ${cleanEmail}`);
    return user;
  }

  // Remember the Firebase uid the first time this account calls the API.
  if (uid && user.firebase_uid !== uid) {
    user = await db.one(
      'UPDATE users SET firebase_uid = $1 WHERE user_id = $2 RETURNING *',
      [uid, user.user_id],
    );
  }
  return user;
}

/**
 * Express middleware. Verifies the bearer token, loads the matching users row and puts it on
 * req.user. Answers 401 when the token is missing or bad.
 */
async function authenticate(req, res, next) {
  const token = bearerToken(req);
  if (!token) {
    return res.status(401).json({ error: 'Missing Authorization: Bearer <idToken> header' });
  }

  try {
    let identity;

    if (testAuthAllowed() && token.startsWith('test:')) {
      // Stub identity for the automated tests: "test:<email>".
      const email = token.slice('test:'.length).trim().toLowerCase();
      if (!email) return res.status(401).json({ error: 'Invalid test token' });
      identity = { uid: `test-${email}`, email, name: 'Test User', phone: '' };
    } else {
      if (!initFirebase()) {
        return res.status(503).json({
          error: 'Token verification is not configured on this server',
          hint: 'Set GOOGLE_APPLICATION_CREDENTIALS or FIREBASE_SERVICE_ACCOUNT',
        });
      }
      const decoded = await admin.auth().verifyIdToken(token);
      if (!decoded.email) {
        return res.status(401).json({ error: 'The token has no e-mail address' });
      }
      identity = {
        uid: decoded.uid,
        email: decoded.email,
        name: decoded.name || '',
        phone: decoded.phone_number || '',
      };
    }

    req.user = await findOrCreateUser(identity);
    req.firebaseUid = identity.uid;
    return next();
  } catch (err) {
    if (err && (err.code === 'auth/id-token-expired' || err.code === 'auth/argument-error')) {
      return res.status(401).json({ error: 'The sign-in token is not valid or has expired' });
    }
    return next(err);
  }
}

/** Express middleware factory. Lets the request through only for the roles listed. */
function requireRole(...allowed) {
  const wanted = allowed.map((r) => normaliseRole(r) || r);
  return (req, res, next) => {
    if (!req.user) return res.status(401).json({ error: 'Not signed in' });
    if (!wanted.includes(req.user.role)) {
      return res.status(403).json({
        error: `This action needs one of these roles: ${wanted.join(', ')}`,
        yourRole: req.user.role,
      });
    }
    return next();
  };
}

/** True for the staff roles that may see any patient's records. */
function isStaff(user) {
  return !!user && user.role !== 'Patient';
}

/**
 * The patients row that belongs to the signed-in account, or null for staff.
 * Used to keep a patient inside their own data.
 */
function ownPatientRow(user) {
  return db.one('SELECT * FROM patients WHERE user_id = $1', [user.user_id]);
}

module.exports = {
  ROLES,
  normaliseRole,
  authenticate,
  requireRole,
  isStaff,
  ownPatientRow,
  initFirebase,
  testAuthAllowed,
};
