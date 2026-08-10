# Realtime Database Schema — Team Contract

**Status: frozen after Phase 0.** Changes require a group decision, because the Android client,
the web simulator, and the safety worker all read and write these exact paths.

Firebase project: `smarthomeapp-c60d9-77331`

---

## Why Realtime Database rather than Firestore

- Listener-based fan-out is the primitive this project needs; `onValue` *is* the bidirectional sync requirement.
- The web simulator subscribes in three lines of JS with no query planning.
- `firebase-admin` gives the worker the same listener model server-side.
- Free tier is generous enough for a demo and needs no billing account.

---

## Tree

```
/users/{uid}
    displayName : String
    email       : String
    houses      : { {houseId}: true }

/houses/{houseId}
    name     : String
    ownerUid : String
    members  : { {uid}: "owner" | "member" }

/floors/{houseId}/{floorId}
    name           : String        // "Ground Floor"
    level          : Int           // 0, 1, 2 — sort order
    planImageAsset : String        // drawable name, e.g. "plan_ground_floor"
    gridCols       : Int           // e.g. 8
    gridRows       : Int           // e.g. 6

/devices/{houseId}/{deviceId}
    floorId  : String
    name     : String              // "Living Room Iron"
    type     : "OUTLET" | "MULTI_SWITCH" | "LIGHT" | "HAZARD" | "CAMERA"
    gridX    : Int                 // 0..gridCols-1
    gridY    : Int                 // 0..gridRows-1
    status   : "ON" | "OFF" | "ERROR" | "DISCONNECTED"
    lastSeen : Long                // epoch millis, simulator heartbeat

    channels : {                   // MULTI_SWITCH only, 2..5 entries
        {channelId}: { label: String, status: String }
    }

    safety : {                     // HAZARD only
        maxOnDurationSec : Int     // e.g. 1800
        onSince          : Long?   // epoch millis, null when OFF
    }

    schedule : {                   // LIGHT and HAZARD
        enabled : Boolean
        onAt    : String           // "18:30", 24h local
        offAt   : String           // "23:00"
        days    : [Int]            // 1=Mon .. 7=Sun
    }

    camera : {                     // CAMERA only
        snapshotUrl : String
        streamUrl   : String
    }

/events/{houseId}/{deviceId}/{pushId}
    ts       : Long
    from     : String              // previous status
    to       : String              // new status
    source   : "APP" | "SIMULATOR" | "WORKER" | "SCHEDULE"
    actorUid : String?             // present when source == "APP"

/alerts/{houseId}/{pushId}
    ts           : Long
    deviceId     : String
    kind         : "MAX_DURATION_EXCEEDED" | "DEVICE_ERROR" | "DEVICE_DISCONNECTED"
    message      : String
    acknowledged : Boolean
```

---

## Invariants

These are the rules every one of the three programs must respect.

1. **`status` is a single enum across all device types.** For `MULTI_SWITCH`, the unit's own
   `status` is rolled up from its channels: any channel `ON` → unit `ON`. This lets one
   `StatusBadge` composable serve every device.

2. **`safety.onSince` is written by whoever turns a `HAZARD` device ON**, and cleared to `null`
   on OFF. The worker reads it to arm the cutoff timer. **Never compute elapsed time on the
   phone** — the phone may be offline or killed, and the spec requires the cutoff to be
   server-side.

3. **`/events` is append-only** and is the sole source for the reporting screen. Derive usage
   totals by folding state transitions; do not maintain a running counter, which drifts whenever
   a client dies mid-session.

4. **Every writer appends to `/events`** with its own `source` value. This is what makes the
   report able to distinguish a user toggle from a safety cutoff.

5. **`lastSeen` is written only by the simulator**, roughly every 10s per device. The worker
   flips a device to `DISCONNECTED` when `lastSeen` is older than 30s. This is how the
   `DISCONNECTED` state in the spec becomes real rather than decorative.

6. **Floor plans are bundled drawables, not Firebase Storage.** `planImageAsset` is a drawable
   resource name resolved on-device. The spec permits free sample plans; Storage would add an
   upload flow and more security rules for no marks.

7. **Camera URLs are mock static images.** Cycling a snapshot URL on a timer reads as a live
   feed and costs nothing.

---

## Ownership

| Path | Written by |
|---|---|
| `/users`, `/houses`, `/floors` | Android app |
| `/devices/*/status`, `/channels` | Android app, simulator, worker |
| `/devices/*/lastSeen` | Simulator only |
| `/devices/*/safety.onSince` | Whoever toggles the device |
| `/devices/*/schedule` | Android app only |
| `/events` | All three |
| `/alerts` | Worker only |
