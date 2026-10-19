// Shared helpers for the MAHMS website: Firebase sign-in and calls to the MAHMS API.
//
// Loaded as a module by login.html and appointments.html. The public pages do not need it.
//
// The flow matches the Android app: Firebase Authentication signs the user in, the site asks
// Firebase for an ID token, and the API verifies that token on every request. The site never
// handles a password beyond passing it straight to Firebase.

import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js';
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut,
  onAuthStateChanged,
} from 'https://www.gstatic.com/firebasejs/10.14.1/firebase-auth.js';

const config = window.MAHMS_CONFIG;

let auth = null;

/** Starts Firebase, or returns null when config.js still has its placeholders. */
export function getFirebaseAuth() {
  if (!config.isFirebaseConfigured) return null;
  if (!auth) auth = getAuth(initializeApp(config.firebase));
  return auth;
}

export async function signIn(email, password) {
  const a = getFirebaseAuth();
  if (!a) throw new Error('Firebase is not configured yet. See web/js/config.js.');
  const credential = await signInWithEmailAndPassword(a, email, password);
  return credential.user;
}

export async function signOutUser() {
  const a = getFirebaseAuth();
  if (a) await signOut(a);
}

/**
 * Runs [callback] with the signed-in user, or with null. Firebase restores the session
 * asynchronously after a page load, so a page must wait for this rather than checking once.
 */
export function onUser(callback) {
  const a = getFirebaseAuth();
  if (!a) {
    callback(null);
    return () => {};
  }
  return onAuthStateChanged(a, callback);
}

/** Sends the user to the login page unless they are signed in. */
export function requireUser(onReady) {
  return onUser((user) => {
    if (!user) {
      window.location.replace('login.html');
      return;
    }
    onReady(user);
  });
}

/** Turns a Firebase error code into something worth showing a patient. */
export function friendlyAuthError(error) {
  switch (error && error.code) {
    case 'auth/invalid-email':
      return 'That email address does not look right.';
    case 'auth/missing-password':
      return 'Enter your password.';
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
    case 'auth/user-not-found':
      return 'Incorrect email or password.';
    case 'auth/too-many-requests':
      return 'Too many attempts. Wait a few minutes and try again.';
    case 'auth/network-request-failed':
      return 'Could not reach the sign-in service. Check your connection.';
    default:
      return (error && error.message) || 'Could not sign you in. Try again.';
  }
}

/**
 * Calls the MAHMS API with the signed-in user's Firebase ID token.
 * Throws an Error carrying the server's message, so callers can show it directly.
 */
export async function api(path, options = {}) {
  const a = getFirebaseAuth();
  const user = a && a.currentUser;
  if (!user) throw new Error('You are not signed in.');

  const token = await user.getIdToken(false);

  let response;
  try {
    response = await fetch(config.apiBaseUrl.replace(/\/$/, '') + path, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(options.headers || {}),
        Authorization: `Bearer ${token}`,
      },
    });
  } catch (err) {
    throw new Error('Could not reach the hospital server. It may be offline.');
  }

  const text = await response.text();
  let body = null;
  if (text) {
    try {
      body = JSON.parse(text);
    } catch (err) {
      body = null;
    }
  }

  if (!response.ok) {
    throw new Error((body && body.error) || `The server answered ${response.status}.`);
  }
  return body;
}

/** Health check used by the footer status line. No token needed. */
export async function serverIsUp() {
  try {
    const response = await fetch(config.apiBaseUrl.replace(/\/$/, '') + '/health');
    if (!response.ok) return false;
    const body = await response.json();
    return body.status === 'ok';
  } catch (err) {
    return false;
  }
}

// ------------------------------------------------------------------ small helpers

/** Escapes text before it goes into innerHTML. */
export function escapeHtml(value) {
  return String(value == null ? '' : value).replace(/[&<>"']/g, (c) => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;',
  }[c]));
}

/** CSS class for the status chip. */
export function statusClass(status) {
  return `chip chip-${String(status || '').toLowerCase().replace(/\s+/g, '-')}`;
}

/** "2026-12-01" to "01 Dec 2026". Leaves anything unexpected alone. */
export function prettyDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value || ''));
  if (!match) return value || '';
  const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
  return `${match[3]} ${months[Number(match[2]) - 1]} ${match[1]}`;
}

/** Shows a message in an element, or hides it when the message is empty. */
export function setNotice(element, message, kind = 'error') {
  if (!element) return;
  if (!message) {
    element.classList.add('hidden');
    element.textContent = '';
    return;
  }
  element.className = `notice notice-${kind}`;
  element.textContent = message;
}
