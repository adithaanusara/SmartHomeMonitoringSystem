/**
 * Deliberately loud, aligned, single-line output.
 *
 * The demo video has to show a safety cutoff happening server-side, and this console is the only
 * visible evidence that the worker — not the phone — made the decision.
 */

const LEVEL_STYLE = {
  INFO: '\x1b[36m',
  WARN: '\x1b[33m',
  ALERT: '\x1b[41m\x1b[97m',
  ERROR: '\x1b[31m',
};
const RESET = '\x1b[0m';
const DIM = '\x1b[2m';

function timestamp() {
  const now = new Date();
  const hh = String(now.getHours()).padStart(2, '0');
  const mm = String(now.getMinutes()).padStart(2, '0');
  const ss = String(now.getSeconds()).padStart(2, '0');
  const ms = String(now.getMilliseconds()).padStart(3, '0');
  return `${hh}:${mm}:${ss}.${ms}`;
}

function write(level, scope, message) {
  const colour = LEVEL_STYLE[level] ?? '';
  const line =
    `${DIM}[${timestamp()}]${RESET} ` +
    `${colour}${level.padEnd(5)}${RESET} ` +
    `${DIM}${scope.padEnd(10)}${RESET} ` +
    message;
  if (level === 'ERROR') console.error(line);
  else console.log(line);
}

export function createLogger(scope) {
  return {
    info: (message) => write('INFO', scope, message),
    warn: (message) => write('WARN', scope, message),
    /** Reserved for the safety cutoff itself, so it is impossible to miss on screen. */
    alert: (message) => write('ALERT', scope, message),
    error: (message, error) => {
      write('ERROR', scope, message);
      if (error) console.error(error);
    },
  };
}
