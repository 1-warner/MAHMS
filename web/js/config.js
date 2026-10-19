// MAHMS website configuration.
//
// Fill both of these in before the site will do anything beyond the public pages.
// Everything here is public by design: a Firebase web config and an API URL are meant to be
// visible in the browser. The real protection is the Firebase security rules and the token
// checks in the API. Never put a service-account key or any private key in this file.

window.MAHMS_CONFIG = {
  // Base URL of the team's REST API (the backend/ folder in this repository).
  // Local:    http://localhost:3000
  // Deployed: https://mahms-api.onrender.com
  apiBaseUrl: 'http://localhost:3000',

  // Firebase web app config.
  // Firebase console > Project settings > General > Your apps > Web app > Config.
  // This is the same Firebase project the Android app uses, so an account works on both.
  firebase: {
    apiKey: 'REPLACE_WITH_FIREBASE_API_KEY',
    authDomain: 'REPLACE_WITH_PROJECT.firebaseapp.com',
    projectId: 'REPLACE_WITH_PROJECT_ID',
    appId: 'REPLACE_WITH_APP_ID',
  },
};

/** True once the Firebase placeholders above have been replaced with real values. */
window.MAHMS_CONFIG.isFirebaseConfigured = !Object
  .values(window.MAHMS_CONFIG.firebase)
  .some((value) => typeof value === 'string' && value.startsWith('REPLACE_WITH'));
