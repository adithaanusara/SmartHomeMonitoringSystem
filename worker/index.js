import { loadConfig } from './src/config.js';
import { initDatabase } from './src/db.js';
import { createLogger } from './src/logger.js';
import { startHeartbeatMonitor } from './src/heartbeatMonitor.js';
import { startSafetyWatcher } from './src/safetyWatcher.js';
import { startScheduleRunner } from './src/scheduleRunner.js';

const log = createLogger('worker');

function main() {
  const config = loadConfig();
  const db = initDatabase(config);

  log.info(`connected to ${config.databaseURL}`);
  log.info(`house: ${config.houseId}`);

  const stops = [
    startSafetyWatcher({ db, houseId: config.houseId }),
    startScheduleRunner({ db, houseId: config.houseId }),
    startHeartbeatMonitor({ db, houseId: config.houseId }),
  ];

  log.info('worker ready — press Ctrl+C to stop');

  const shutdown = (signal) => {
    log.info(`${signal} received, detaching listeners`);
    for (const stop of stops) stop();
    process.exit(0);
  };

  process.on('SIGINT', () => shutdown('SIGINT'));
  process.on('SIGTERM', () => shutdown('SIGTERM'));
}

try {
  main();
} catch (error) {
  log.error(error.message);
  process.exit(1);
}
