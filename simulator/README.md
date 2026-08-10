# Hardware Simulator

The spec's *Companion Hardware Simulator*: a web dashboard standing in for the physical appliances.
It subscribes to the same Realtime Database nodes as the Android client, so state moves both ways
between them with no direct connection.

Plain HTML, CSS and ES modules with the Firebase Web SDK from a CDN. No build step, no bundler.

## Setup

1. Register a Web app: **Firebase console → Project settings → Your apps → Add app → Web**.
2. Paste `databaseURL` and `appId` into `config.js`. The other fields are already filled in from
   `app/google-services.json`.
3. Make sure `HOUSE_ID` matches the app and the worker (`demo-house` by default).

## Run

```bash
npx serve simulator
```

Then open the printed `localhost` URL.

Serve it over `localhost` rather than opening `index.html` directly: Firebase Auth only accepts
requests from authorised domains, and `localhost` is authorised by default while `file://` is not.

To deploy for the demo:

```bash
firebase deploy --only hosting
```

## Sign-in

The database rules restrict every node to members of the house, so the simulator authenticates like
any other client rather than running anonymously.

Create a dedicated account (e.g. `simulator@yourteam.dev`) under **Authentication → Users**, then
add its uid to the house:

```
houses/demo-house/members/<simulator-uid> = "member"
```

Anonymous auth would need a special case in the rules for no benefit; a real account keeps one
membership model across all three programs.

## What it does

- Renders every device grouped by floor, with the appliance lit or dark according to `status`
- Shows gang boxes as individually addressable channels with per-channel LEDs
- Counts down to the worker's safety cutoff on hazard devices
- Writes `lastSeen` for every device every 10s — the heartbeat that makes `DISCONNECTED` real

### Fault injection

Three buttons per device. None of them originate in the app, which is exactly the point: each one
must appear on the phone within about a second and with no manual refresh.

| Button | Effect |
|---|---|
| **Switch on / off** | An externally-driven state change, as if someone used the physical switch |
| **Fault** | Forces `ERROR` |
| **Cut beat** | Stops that device's heartbeat. The worker marks it `DISCONNECTED` after 30s |

**Resume beat** restarts the heartbeat, and the device reports `OFF` — a power-on reset. The worker
deliberately never clears `DISCONNECTED` itself, because only the device knows what state it came
back in.

## The three-way demo

This is the centrepiece of the video, and it needs the phone, this page and the worker console all
visible at once:

1. Toggle a light on the phone → the lamp lights here.
2. **Switch on** a device here → the phone's switch flips, no refresh.
3. **Cut beat** on any device → 30s later the worker logs it and both clients show `DISCONNECTED`.
4. Turn the iron on, with `maxOnDurationSec` lowered to ~20s for the recording → the countdown runs
   here, the worker logs `CUTOFF`, and the phone's switch flips itself off with an alert.

Step 4 is the one that demonstrates the safety requirement end-to-end. Rehearse it.
