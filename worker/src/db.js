import { initializeApp, cert } from 'firebase-admin/app';
import { getDatabase, ServerValue } from 'firebase-admin/database';
import { SOURCE } from './config.js';

export { ServerValue };

/**
 * The Admin SDK authenticates with a service-account key and therefore bypasses the Realtime
 * Database security rules entirely. That is what lets the worker write `/alerts`, which the rules
 * make read-only to every signed-in client.
 */
export function initDatabase({ serviceAccount, databaseURL }) {
  initializeApp({ credential: cert(serviceAccount), databaseURL });
  return getDatabase();
}

/**
 * Builds the `/events` fragment of an atomic update.
 *
 * Returned as a path map rather than written separately so a state change and its audit entry
 * cannot come apart — the reporting screen folds this log to derive usage, and a missing
 * transition would silently skew the totals.
 */
export function eventUpdate(db, houseId, deviceId, { from, to, source }) {
  const key = db.ref(`events/${houseId}/${deviceId}`).push().key;
  return {
    [`/events/${houseId}/${deviceId}/${key}`]: {
      ts: ServerValue.TIMESTAMP,
      from,
      to,
      source: source ?? SOURCE.WORKER,
      actorUid: null,
    },
  };
}

/** Builds the `/alerts` fragment of an atomic update. */
export function alertUpdate(db, houseId, { deviceId, kind, message }) {
  const key = db.ref(`alerts/${houseId}`).push().key;
  return {
    [`/alerts/${houseId}/${key}`]: {
      ts: ServerValue.TIMESTAMP,
      deviceId,
      kind,
      message,
      acknowledged: false,
    },
  };
}
