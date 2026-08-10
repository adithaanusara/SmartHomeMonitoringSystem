/**
 * Firebase Web SDK configuration.
 *
 * Values below are copied from app/google-services.json, except the two marked TODO which only
 * exist once you register a Web app in the console (Project settings → Your apps → Add app → Web).
 */
export const firebaseConfig = {
  apiKey: 'AIzaSyAatEFc7HiknLyb4u8sDHTuLtU1tLenbms',
  authDomain: 'smarthomeapp-c60d9.firebaseapp.com',

  // TODO: paste the databaseURL shown in the console once the Realtime Database exists.
  // e.g. https://smarthomeapp-c60d9-default-rtdb.asia-southeast1.firebasedatabase.app
  databaseURL: '',

  projectId: 'smarthomeapp-c60d9',
  storageBucket: 'smarthomeapp-c60d9.firebasestorage.app',
  messagingSenderId: '797898156562',

  // TODO: paste the Web app's appId. Auth and Realtime Database work without it, so it is safe to
  // leave blank while developing.
  appId: '',
};

/** Must match HOUSE_ID in the worker and the house key used by the app. */
export const HOUSE_ID = 'demo-house';

/**
 * Heartbeat cadence. The worker marks a device DISCONNECTED after 30s of silence, so this leaves
 * room for two missed beats before it reacts. Keep in sync with the worker's config.js and the
 * app's Constants.kt.
 */
export const HEARTBEAT_INTERVAL_MS = 10_000;
