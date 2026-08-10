# Safety Worker

The backend half of the spec's *Server-Side Safety Cutoffs*. A long-running Node process that
listens to the Realtime Database and enforces rules the mobile client cannot be trusted to enforce.

## Why a worker process rather than Cloud Functions

The spec allows *"a backend cloud listener **or a worker process**"*. Cloud Functions now requires
the Blaze billing plan, which a student project should not need. A plain Node process using
`firebase-admin` satisfies the wording, needs no billing account, is far easier to debug, and — for
the demo — prints every decision to a console you can put on screen.

The Admin SDK authenticates with a service-account key, so it **bypasses the database security
rules**. That is deliberate: it is what allows the worker to write `/alerts`, which the rules make
read-only to every signed-in client.

## What it does

| Module | Responsibility |
|---|---|
| `src/safetyWatcher.js` | Forces a hazard device OFF when `safety.maxOnDurationSec` is exceeded, and raises an alert |
| `src/scheduleRunner.js` | Applies `schedule.onAt` / `schedule.offAt` for lights and hazard devices |
| `src/heartbeatMonitor.js` | Marks a device DISCONNECTED after 30s without a simulator heartbeat |

Every state change the worker makes is appended to `/events` with `source: "WORKER"` or
`"SCHEDULE"`, so the reporting screen can tell an automatic cutoff apart from a user toggle.

## Three design decisions worth defending in the viva

**The cutoff survives a restart.** Firebase replays `child_added` for every existing device when a
listener attaches, so a worker that restarts mid-countdown re-arms from the persisted
`safety.onSince` rather than losing the timer. Without that, a crash would silently disable the
protection while everything still looked healthy.

**The schedule is edge-triggered, not level-triggered.** It acts only at the exact `onAt` and
`offAt` minutes. A level-triggered engine would re-assert ON every tick for the whole window, so a
user who switched a scheduled light off manually would watch it snap back on within a minute.
Firing only on boundaries lets a manual override stand until the next boundary, which is how
physical timers behave. The trade-off: if the worker is down at `onAt`, that switch-on is missed
rather than caught up.

**The worker never clears DISCONNECTED.** It sets that state on staleness, but recovery belongs to
the device — the simulator republishes its own status on reconnect, because only it knows whether
the appliance came back on or off. Guessing here would let the dashboard show a lamp as OFF while
it was physically lit.

## Setup

```bash
cd worker
npm install
cp .env.example .env       # then fill in FIREBASE_DATABASE_URL
```

Download a service-account key from **Firebase console → Project settings → Service accounts →
Generate new private key** and save it as `worker/service-account.json`.

Both `.env` and `service-account.json` are gitignored. **The key grants full admin access to the
project — never commit it.**

## Run

```bash
npm start
```

Expected output:

```
[14:32:01.004] INFO  worker     connected to https://…firebasedatabase.app
[14:32:01.006] INFO  worker     house: demo-house
[14:32:01.180] INFO  safety     watching /devices/demo-house for max-on-duration breaches
[14:32:01.181] INFO  schedule   schedule engine running (checking every 15s)
[14:32:01.182] INFO  heartbeat  monitoring heartbeats (stale after 30s)
[14:32:01.183] INFO  worker     worker ready — press Ctrl+C to stop
```

Turning the iron on from the phone or the simulator then logs:

```
[14:32:14.902] INFO  safety     armed Laundry Iron — cutoff in 900s (limit 900s)
```

…and when the limit is reached:

```
[14:47:14.910] ALERT safety      CUTOFF  Laundry Iron exceeded 900s — forced OFF and alert raised
```

## Tests

```bash
npm test
```

Covers the schedule window logic — `parseHhMm`, day filtering, overnight windows, and the
edge-triggered behaviour — against fixed clocks, so it runs without Firebase. That is the part most
likely to be wrong and least likely to be noticed.

## Demo tip

For the recording, set a hazard device's `maxOnDurationSec` to something like 20 so the cutoff
fires on camera instead of fifteen minutes later.
