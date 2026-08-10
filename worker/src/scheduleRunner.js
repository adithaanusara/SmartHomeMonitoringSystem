import { SCHEDULE_TICK_INTERVAL_MS, SOURCE, STATUS } from './config.js';
import { eventUpdate } from './db.js';
import { createLogger } from './logger.js';
import { minuteKey, scheduleEdgeAt } from './time.js';

const log = createLogger('schedule');

/**
 * Applies `schedule.onAt` / `schedule.offAt` for lights and hazard devices.
 *
 * Ticks faster than once a minute and de-duplicates by minute key, because a strict 60s interval
 * drifts and can skip a minute entirely — which for a schedule engine means silently missing a
 * switch-on for a whole day.
 */
export function startScheduleRunner({ db, houseId }) {
  const devicesRef = db.ref(`devices/${houseId}`);
  let devices = {};
  let lastMinute = null;

  const onValue = (snapshot) => {
    devices = snapshot.val() ?? {};
  };
  devicesRef.on('value', onValue);

  async function apply(deviceId, device, desired) {
    if (device.status === desired) return;

    const updates = {
      [`/devices/${houseId}/${deviceId}/status`]: desired,
      ...eventUpdate(db, houseId, deviceId, {
        from: device.status ?? STATUS.OFF,
        to: desired,
        source: SOURCE.SCHEDULE,
      }),
    };

    // A scheduled hazard device still needs its cutoff armed, so maintain onSince here too.
    if (device.safety) {
      updates[`/devices/${houseId}/${deviceId}/safety/onSince`] =
        desired === STATUS.ON ? Date.now() : null;
    }

    // Channels follow the unit, mirroring DeviceRepository on the client.
    if (device.channels && desired === STATUS.OFF) {
      for (const channelId of Object.keys(device.channels)) {
        updates[`/devices/${houseId}/${deviceId}/channels/${channelId}/status`] = STATUS.OFF;
      }
    }

    try {
      await db.ref().update(updates);
      log.info(`${device.name ?? deviceId} -> ${desired} (scheduled)`);
    } catch (error) {
      log.error(`failed to apply schedule for ${deviceId}`, error);
    }
  }

  function tick() {
    const now = new Date();
    const key = minuteKey(now);
    if (key === lastMinute) return;
    lastMinute = key;

    for (const [deviceId, device] of Object.entries(devices)) {
      const edge = scheduleEdgeAt(device?.schedule, now);
      if (edge) void apply(deviceId, device, edge);
    }
  }

  const interval = setInterval(tick, SCHEDULE_TICK_INTERVAL_MS);
  tick();

  log.info(`schedule engine running (checking every ${SCHEDULE_TICK_INTERVAL_MS / 1000}s)`);

  return function stop() {
    clearInterval(interval);
    devicesRef.off('value', onValue);
  };
}
