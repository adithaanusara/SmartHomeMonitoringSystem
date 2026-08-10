/**
 * Pure time helpers for the schedule engine.
 *
 * Kept free of Firebase and of `Date.now()` so the window logic — the most bug-prone part of the
 * worker — can be unit tested against fixed clocks. See test/time.test.js.
 */

/** Parses "HH:mm" into minutes since midnight, or null when malformed. */
export function parseHhMm(value) {
  if (typeof value !== 'string') return null;
  const match = /^(\d{1,2}):(\d{2})$/.exec(value.trim());
  if (!match) return null;
  const hours = Number(match[1]);
  const minutes = Number(match[2]);
  if (hours > 23 || minutes > 59) return null;
  return hours * 60 + minutes;
}

/** Formats a Date as "HH:mm" in local time. */
export function formatHhMm(date) {
  const hh = String(date.getHours()).padStart(2, '0');
  const mm = String(date.getMinutes()).padStart(2, '0');
  return `${hh}:${mm}`;
}

/** ISO day of week: 1 = Monday .. 7 = Sunday, matching java.time.DayOfWeek. */
export function isoDayOfWeek(date) {
  const day = date.getDay();
  return day === 0 ? 7 : day;
}

/** A minute-resolution key, used to fire each schedule edge at most once per minute. */
export function minuteKey(date) {
  return `${date.getFullYear()}-${date.getMonth() + 1}-${date.getDate()}T${formatHhMm(date)}`;
}

function dayMatches(schedule, date) {
  const days = schedule?.days;
  // An absent or empty day list means every day.
  if (!Array.isArray(days) || days.length === 0) return true;
  return days.includes(isoDayOfWeek(date));
}

/**
 * Returns the status a schedule wants applied at exactly this minute, or null for "do nothing".
 *
 * Edge-triggered rather than level-triggered on purpose. A level-triggered engine would re-assert
 * ON every tick for the whole window, so a user who switched a scheduled light off manually would
 * see it snap back on within a minute. Firing only on the boundaries lets a manual override stand
 * until the next boundary, which is how physical timers behave.
 *
 * The day filter is applied to the ON edge only; an overnight window that starts on a selected day
 * still switches off the following morning.
 */
export function scheduleEdgeAt(schedule, date) {
  if (!schedule?.enabled) return null;

  const now = formatHhMm(date);
  const onAt = parseHhMm(schedule.onAt);
  const offAt = parseHhMm(schedule.offAt);
  const nowMinutes = parseHhMm(now);

  if (offAt !== null && nowMinutes === offAt) return 'OFF';
  if (onAt !== null && nowMinutes === onAt && dayMatches(schedule, date)) return 'ON';
  return null;
}
