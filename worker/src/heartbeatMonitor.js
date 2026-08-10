import {
  ALERT_KIND,
  HEARTBEAT_SCAN_INTERVAL_MS,
  HEARTBEAT_TIMEOUT_MS,
  SOURCE,
  STATUS,
} from './config.js';
import { alertUpdate, eventUpdate } from './db.js';
import { createLogger } from './logger.js';

const log = createLogger('heartbeat');

/**
 * Marks devices DISCONNECTED when the simulator's heartbeat goes stale.
 *
 * This is what makes DISCONNECTED a real state rather than a decorative enum value: the simulator
 * writes `lastSeen` every ~10s, and two missed beats mean the appliance is unreachable.
 *
 * The worker never clears DISCONNECTED. Recovery belongs to the device — the simulator republishes
 * its own status when it reconnects, because only it knows whether the appliance came back on or
 * off. Guessing here would let the dashboard show a lamp as OFF while it is physically lit.
 */
export function startHeartbeatMonitor({ db, houseId }) {
  const devicesRef = db.ref(`devices/${houseId}`);
  let devices = {};

  const onValue = (snapshot) => {
    devices = snapshot.val() ?? {};
  };
  devicesRef.on('value', onValue);

  async function markDisconnected(deviceId, device, staleMs) {
    const name = device.name ?? deviceId;
    const updates = {
      [`/devices/${houseId}/${deviceId}/status`]: STATUS.DISCONNECTED,
      ...eventUpdate(db, houseId, deviceId, {
        from: device.status ?? STATUS.OFF,
        to: STATUS.DISCONNECTED,
        source: SOURCE.WORKER,
      }),
      ...alertUpdate(db, houseId, {
        deviceId,
        kind: ALERT_KIND.DEVICE_DISCONNECTED,
        message: `${name} stopped responding.`,
      }),
    };

    try {
      await db.ref().update(updates);
      log.warn(`${name} unreachable for ${Math.round(staleMs / 1000)}s -> DISCONNECTED`);
    } catch (error) {
      log.error(`failed to mark ${deviceId} disconnected`, error);
    }
  }

  function scan() {
    const now = Date.now();

    for (const [deviceId, device] of Object.entries(devices)) {
      if (!device) continue;

      // lastSeen of 0 means this device has never been simulated, which is not a fault.
      const lastSeen = device.lastSeen ?? 0;
      if (lastSeen <= 0) continue;
      if (device.status === STATUS.DISCONNECTED) continue;

      const staleMs = now - lastSeen;
      if (staleMs > HEARTBEAT_TIMEOUT_MS) {
        void markDisconnected(deviceId, device, staleMs);
      }
    }
  }

  const interval = setInterval(scan, HEARTBEAT_SCAN_INTERVAL_MS);

  log.info(`monitoring heartbeats (stale after ${HEARTBEAT_TIMEOUT_MS / 1000}s)`);

  return function stop() {
    clearInterval(interval);
    devicesRef.off('value', onValue);
  };
}
