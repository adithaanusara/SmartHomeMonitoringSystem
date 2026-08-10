import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { formatHhMm, isoDayOfWeek, minuteKey, parseHhMm, scheduleEdgeAt } from '../src/time.js';

/** Local-time Date, so these assertions do not depend on the machine's timezone. */
const at = (year, month, day, hour, minute) => new Date(year, month - 1, day, hour, minute, 0, 0);

describe('parseHhMm', () => {
  it('parses valid times to minutes since midnight', () => {
    assert.equal(parseHhMm('00:00'), 0);
    assert.equal(parseHhMm('18:30'), 1110);
    assert.equal(parseHhMm('23:59'), 1439);
    assert.equal(parseHhMm('9:05'), 545);
  });

  it('rejects malformed and out-of-range values', () => {
    for (const bad of ['', '24:00', '12:60', '1830', 'ab:cd', null, undefined, 18]) {
      assert.equal(parseHhMm(bad), null, `expected null for ${JSON.stringify(bad)}`);
    }
  });
});

describe('formatHhMm', () => {
  it('zero-pads both components', () => {
    assert.equal(formatHhMm(at(2026, 8, 10, 9, 5)), '09:05');
    assert.equal(formatHhMm(at(2026, 8, 10, 23, 0)), '23:00');
  });
});

describe('isoDayOfWeek', () => {
  it('maps Sunday to 7 and Monday to 1', () => {
    // 2026-08-10 is a Monday.
    assert.equal(isoDayOfWeek(at(2026, 8, 10, 12, 0)), 1);
    assert.equal(isoDayOfWeek(at(2026, 8, 16, 12, 0)), 7);
  });
});

describe('minuteKey', () => {
  it('changes between minutes and is stable within one', () => {
    const a = minuteKey(at(2026, 8, 10, 18, 30));
    const b = new Date(at(2026, 8, 10, 18, 30));
    b.setSeconds(59);
    assert.equal(minuteKey(b), a);
    assert.notEqual(minuteKey(at(2026, 8, 10, 18, 31)), a);
  });
});

describe('scheduleEdgeAt', () => {
  const everyDay = { enabled: true, onAt: '18:30', offAt: '23:00', days: [] };

  it('fires ON exactly at onAt', () => {
    assert.equal(scheduleEdgeAt(everyDay, at(2026, 8, 10, 18, 30)), 'ON');
  });

  it('fires OFF exactly at offAt', () => {
    assert.equal(scheduleEdgeAt(everyDay, at(2026, 8, 10, 23, 0)), 'OFF');
  });

  it('does nothing inside the window, so a manual override survives', () => {
    assert.equal(scheduleEdgeAt(everyDay, at(2026, 8, 10, 20, 0)), null);
  });

  it('does nothing outside the window', () => {
    assert.equal(scheduleEdgeAt(everyDay, at(2026, 8, 10, 8, 0)), null);
  });

  it('ignores a disabled schedule', () => {
    assert.equal(scheduleEdgeAt({ ...everyDay, enabled: false }, at(2026, 8, 10, 18, 30)), null);
  });

  it('ignores a missing schedule', () => {
    assert.equal(scheduleEdgeAt(undefined, at(2026, 8, 10, 18, 30)), null);
    assert.equal(scheduleEdgeAt(null, at(2026, 8, 10, 18, 30)), null);
  });

  describe('day filtering', () => {
    const weekdays = { enabled: true, onAt: '07:00', offAt: '09:00', days: [1, 2, 3, 4, 5] };

    it('fires ON on a selected day', () => {
      assert.equal(scheduleEdgeAt(weekdays, at(2026, 8, 10, 7, 0)), 'ON');
    });

    it('does not fire ON on an unselected day', () => {
      // 2026-08-15 is a Saturday.
      assert.equal(scheduleEdgeAt(weekdays, at(2026, 8, 15, 7, 0)), null);
    });

    it('still fires OFF on an unselected day, so an overnight window closes', () => {
      const overnight = { enabled: true, onAt: '23:00', offAt: '06:00', days: [5] };
      // Friday night switches on...
      assert.equal(scheduleEdgeAt(overnight, at(2026, 8, 14, 23, 0)), 'ON');
      // ...and Saturday morning still switches off.
      assert.equal(scheduleEdgeAt(overnight, at(2026, 8, 15, 6, 0)), 'OFF');
    });
  });

  it('prefers OFF when onAt and offAt collide, failing safe', () => {
    const degenerate = { enabled: true, onAt: '10:00', offAt: '10:00', days: [] };
    assert.equal(scheduleEdgeAt(degenerate, at(2026, 8, 10, 10, 0)), 'OFF');
  });

  it('tolerates a malformed time without throwing', () => {
    const broken = { enabled: true, onAt: 'not-a-time', offAt: '23:00', days: [] };
    assert.equal(scheduleEdgeAt(broken, at(2026, 8, 10, 18, 30)), null);
    assert.equal(scheduleEdgeAt(broken, at(2026, 8, 10, 23, 0)), 'OFF');
  });
});
