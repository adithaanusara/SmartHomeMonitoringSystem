/**
 * Firebase Web SDK configuration.
 *
 * `apiKey` and `appId` come from the **Web** app registration, not from the Android app. Firebase
 * issues a separate key per platform — this one differs from the key in app/google-services.json —
 * and the Android key can carry package-name/SHA-1 restrictions that a browser request cannot
 * satisfy, failing with an API-key error that looks nothing like the real cause.
 *
 * The key is safe to commit: Firebase web API keys are public by design and ship in the source of
 * every web app. The database rules are what protect the data.
 */
export const firebaseConfig = {
  apiKey: 'AIzaSyBdSSiBc-pfmnOeI7No7uC7zM6ZlFTnB1g',

  authDomain: 'smarthomeapp-c60d9-77331.firebaseapp.com',
  databaseURL:
    'https://smarthomeapp-c60d9-77331-default-rtdb.asia-southeast1.firebasedatabase.app',
  projectId: 'smarthomeapp-c60d9-77331',
  storageBucket: 'smarthomeapp-c60d9-77331.firebasestorage.app',
  messagingSenderId: '402335890171',

  appId: '1:402335890171:web:736dbca650bd9d67700e5e',
};

/** Must match HOUSE_ID in the worker and the house key used by the app. */
export const HOUSE_ID = 'demo-house';

/**
 * Heartbeat cadence. The worker marks a device DISCONNECTED after 30s of silence, so this leaves
 * room for two missed beats before it reacts. Keep in sync with the worker's config.js and the
 * app's Constants.kt.
 */
export const HEARTBEAT_INTERVAL_MS = 10_000;
