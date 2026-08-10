import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

/**
 * Values mirrored from the Android app's `utils/Constants.kt`.
 * Changing one without the other desynchronises the three programs — see docs/SCHEMA.md.
 */
export const HEARTBEAT_TIMEOUT_MS = 30_000;
export const HEARTBEAT_SCAN_INTERVAL_MS = 10_000;
export const SCHEDULE_TICK_INTERVAL_MS = 15_000;

export const DEVICE_TYPE = {
  OUTLET: 'OUTLET',
  MULTI_SWITCH: 'MULTI_SWITCH',
  LIGHT: 'LIGHT',
  HAZARD: 'HAZARD',
  CAMERA: 'CAMERA',
};

export const STATUS = {
  ON: 'ON',
  OFF: 'OFF',
  ERROR: 'ERROR',
  DISCONNECTED: 'DISCONNECTED',
};

export const SOURCE = {
  APP: 'APP',
  SIMULATOR: 'SIMULATOR',
  WORKER: 'WORKER',
  SCHEDULE: 'SCHEDULE',
};

export const ALERT_KIND = {
  MAX_DURATION_EXCEEDED: 'MAX_DURATION_EXCEEDED',
  DEVICE_ERROR: 'DEVICE_ERROR',
  DEVICE_DISCONNECTED: 'DEVICE_DISCONNECTED',
};

function required(name) {
  const value = process.env[name];
  if (!value) {
    throw new Error(
      `Missing required environment variable ${name}. ` +
        'Copy .env.example to .env and fill it in — see worker/README.md.',
    );
  }
  return value;
}

export function loadConfig() {
  const serviceAccountPath = resolve(
    process.env.SERVICE_ACCOUNT_PATH ?? './service-account.json',
  );

  let serviceAccount;
  try {
    serviceAccount = JSON.parse(readFileSync(serviceAccountPath, 'utf8'));
  } catch (cause) {
    throw new Error(
      `Could not read the service-account key at ${serviceAccountPath}. ` +
        'Download one from Firebase console → Project settings → Service accounts → ' +
        'Generate new private key.',
      { cause },
    );
  }

  return {
    serviceAccount,
    databaseURL: required('FIREBASE_DATABASE_URL'),
    houseId: process.env.HOUSE_ID ?? 'demo-house',
  };
}
