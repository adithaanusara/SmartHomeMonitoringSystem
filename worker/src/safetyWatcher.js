import { ALERT_KIND, DEVICE_TYPE, SOURCE, STATUS } from './config.js';
import { alertUpdate, eventUpdate } from './db.js';
import { createLogger } from './logger.js';

const log = createLogger('safety');

/**
 * Enforces `safety.maxOnDurationSec` on hazard devices.
 *
 * This is the spec's server-side cutoff. It lives here rather than on the phone because the phone
 * may be offline, backgrounded or killed at the moment the limit is reached — none of which should
 * leave an iron powered on.
 *
 * Restart safety comes for free: Firebase replays `child_added` for every existing device when the
 * listener attaches, so a worker that restarts mid-countdown re-arms from the persisted `onSince`
 * rather than losing the timer. Without that, a crash would silently disable the protection.
 */
export function startSafetyWatcher({ db, houseId }) {
  /** deviceId -> { deadline, timeout } */
  const armed = new Map();
  const devicesRef = db.ref(`devices/${houseId}`);

  function disarm(deviceId, reason) {
    const entry = armed.get(deviceId);
    if (!entry) return;
    clearTimeout(entry.timeout);
    armed.delete(deviceId);
    log.info(`disarmed ${deviceId} (${reason})`);
  }

  async function trip(deviceId, device, limitSec) {
    armed.delete(deviceId);
    const name = device.name ?? deviceId;

    const updates = {
      [`/devices/${houseId}/${deviceId}/status`]: STATUS.OFF,
      [`/devices/${houseId}/${deviceId}/safety/onSince`]: null,
      ...eventUpdate(db, houseId, deviceId, {
        from: STATUS.ON,
        to: STATUS.OFF,
        source: SOURCE.WORKER,
      }),
      ...alertUpdate(db, houseId, {
        deviceId,
        kind: ALERT_KIND.MAX_DURATION_EXCEEDED,
        message: `${name} was switched off automatically after ${limitSec}s.`,
      }),
    };

    try {
      await db.ref().update(updates);
      log.alert(` CUTOFF  ${name} exceeded ${limitSec}s — forced OFF and alert raised `);
    } catch (error) {
      log.error(`failed to apply cutoff for ${deviceId}`, error);
    }
  }

  function evaluate(deviceId, device) {
    if (!device || device.type !== DEVICE_TYPE.HAZARD) {
      disarm(deviceId, 'not a hazard device');
      return;
    }

    const onSince = device.safety?.onSince;
    const limitSec = device.safety?.maxOnDurationSec;

    if (device.status !== STATUS.ON || !onSince || !limitSec) {
      disarm(deviceId, 'not on, or no cutoff configured');
      return;
    }

    const deadline = onSince + limitSec * 1000;

    // Re-arming on every change would restart the countdown whenever an unrelated field was
    // written, letting a device stay on indefinitely. Only react when the deadline itself moves.
    const existing = armed.get(deviceId);
    if (existing?.deadline === deadline) return;
    if (existing) clearTimeout(existing.timeout);

    const remainingMs = deadline - Date.now();

    if (remainingMs <= 0) {
      log.warn(
        `${device.name ?? deviceId} was already past its ${limitSec}s limit on startup — ` +
          'tripping now',
      );
      void trip(deviceId, device, limitSec);
      return;
    }

    const timeout = setTimeout(() => void trip(deviceId, device, limitSec), remainingMs);
    armed.set(deviceId, { deadline, timeout });
    log.info(
      `armed ${device.name ?? deviceId} — cutoff in ${Math.round(remainingMs / 1000)}s ` +
        `(limit ${limitSec}s)`,
    );
  }

  const onUpsert = (snapshot) => evaluate(snapshot.key, snapshot.val());
  const onRemove = (snapshot) => disarm(snapshot.key, 'device removed');

  devicesRef.on('child_added', onUpsert);
  devicesRef.on('child_changed', onUpsert);
  devicesRef.on('child_removed', onRemove);

  log.info(`watching /devices/${houseId} for max-on-duration breaches`);

  return function stop() {
    devicesRef.off('child_added', onUpsert);
    devicesRef.off('child_changed', onUpsert);
    devicesRef.off('child_removed', onRemove);
    for (const { timeout } of armed.values()) clearTimeout(timeout);
    armed.clear();
  };
}
