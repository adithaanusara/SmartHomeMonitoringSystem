# SCS 3311 Mini-Project — Analysis & Completion Checklist

Smart Home Monitoring & Control System. Derived from `CS_Mini-Project-v1.pdf`.

---

## 1. Where the repo actually stands

| Area | Status |
|---|---|
| Gradle / AGP 9.2.1 / Compose BOM 2026.02.01 | Configured |
| Compose theme (`Color`, `Type`, `Theme`) | Done |
| `MainActivity` | Default template hello-world |
| Package skeleton (33 files) | **Empty — package declaration only, 2 lines each** |
| Firebase SDK dependencies | **Absent** — plugin applied, no libraries added |
| `google-services.json` | **Wrong path** (see blockers) |
| Realtime Database schema | Not designed |
| Web hardware simulator | Not started |
| Server-side safety worker | Not started |
| Report / documentation / video | Not started |

Net: roughly 5% complete. Treat this as a greenfield build with a folder layout pre-agreed.

### The three deliverables are three separate programs

The spec is not just an Android app. Do not let the simulator and worker slip to the last week:

1. **Android client** (Kotlin + Compose) — floors, grid, devices, control, schedules, reports.
2. **Web hardware simulator** (static HTML/JS + Firebase Web SDK) — subscribes to the DB and *renders* appliance state. Represents the "physical" house.
3. **Server-side safety worker** (Node.js + `firebase-admin`) — enforces `maxOnDuration`, runs schedules, marks stale devices `DISCONNECTED`, writes alerts.

The bidirectional-sync requirement is only demonstrable when all three run at once: toggle in the app → simulator lamp lights instantly; worker trips the iron → app switch flips itself off without a refresh. **That simultaneous three-way demo is the centrepiece of your video.** Design toward it.

---

## 2. Blockers

### Resolved

1. ~~`google-services.json` at the wrong path.~~ Moved to `app/google-services.json`.
2. ~~No Firebase dependencies.~~ Added Firebase BOM 34.17.0, Auth, Realtime Database, plus
   navigation-compose, lifecycle-viewmodel/runtime-compose, Coil 3, and coroutines-play-services.
3. ~~`INTERNET` permission not declared.~~ Added, with `ACCESS_NETWORK_STATE` and `POST_NOTIFICATIONS`.
4. ~~`SmartHomeApplication` not registered.~~ Registered in the manifest and implemented; it enables
   Realtime Database disk persistence so the dashboard renders offline and replays offline toggles.
5. ~~No `local.properties`.~~ Created (gitignored).
6. ~~Unverified Kotlin compilation.~~ Clean build verified, plus install and launch on a Pixel 6 AVD.

**Toolchain changes this required** — three version conflicts surfaced during the first real build:

| Change | From | To | Why |
|---|---|---|---|
| `compileSdk` | 36.1 | 37 | androidx.core 1.19.0 and lifecycle 2.11.0 refuse to compile against 36. `targetSdk` deliberately stays at 36 — bumping it opts into new runtime behaviour we don't need |
| AGP | 9.2.1 | 9.3.1 | Its built-in Kotlin compiler was 2.2.0, but dependency resolution pulled `kotlin-stdlib` 2.4.0, whose metadata 2.2.0 cannot read |
| Gradle | 9.4.1 | 9.5.0 | Required by AGP 9.3.1 |

Note: AGP 9 **forbids** applying `org.jetbrains.kotlin.android` — built-in Kotlin is mandatory and
its version is tied to the AGP version. Bumping AGP is the only way to move the Kotlin compiler.

### Outstanding — these need Firebase console access, not code

7. **The Realtime Database does not exist yet.** Verified: `smarthomeapp-c60d9-default-rtdb` returns
   404 on `firebaseio.com`, `asia-southeast1`, and `europe-west1`. `google-services.json` has no
   `firebase_url` key, which is the tell.

   The SDK does *not* crash on this — it derives a default URL from the project ID, so
   `FirebaseDatabase.getInstance()` succeeds and the app launches fine. **Every read and write will
   fail instead.** That is a much more confusing failure mode, so create the database before
   anyone starts on the data layer:

   Firebase console → Build → Realtime Database → Create Database → pick a region → then
   **re-download `google-services.json` into `app/`** so the URL is pinned explicitly rather than guessed.

8. **Security rules.** Ship auth-scoped rules, not test mode — test mode expires after 30 days and
   will silently break your demo, quite possibly the night before submission. The committed API key
   is fine, Android API keys are public by design; the rules are what actually protect the data.

**Exit criterion:** `./gradlew assembleDebug` succeeds and the app launches. ✅ Both verified.

---

## 3. Data model — agree on this before splitting work

Use **Realtime Database**, not Firestore. RTDB's listener model, low-latency fan-out, and simple `onValue` triggers map directly onto what this spec asks for, and the web simulator subscribes with three lines of JS.

Proposed schema (flat, denormalised by `houseId` so each screen needs one listener):

```
/users/{uid}
    displayName, email
    houses: { {houseId}: true }

/houses/{houseId}
    name, ownerUid
    members: { {uid}: "owner" | "member" }

/floors/{houseId}/{floorId}
    name, level, planImageAsset, gridCols, gridRows

/devices/{houseId}/{deviceId}
    floorId, name
    type:    "OUTLET" | "MULTI_SWITCH" | "LIGHT" | "HAZARD" | "CAMERA"
    gridX, gridY                  // placement on the floor grid
    status:  "ON" | "OFF" | "ERROR" | "DISCONNECTED"
    lastSeen: <epoch millis>      // simulator heartbeat -> drives DISCONNECTED
    channels: {                   // MULTI_SWITCH only, 2..5 entries
        {channelId}: { label, status }
    }
    safety: {                     // HAZARD only
        maxOnDurationSec, onSince
    }
    schedule: {                   // LIGHT / HAZARD
        enabled, onAt: "18:30", offAt: "23:00", days: [1,2,3,4,5]
    }
    camera: { snapshotUrl, streamUrl }   // CAMERA only

/events/{houseId}/{deviceId}/{pushId}
    ts, from, to
    source: "APP" | "SIMULATOR" | "WORKER" | "SCHEDULE"
    actorUid

/alerts/{houseId}/{pushId}
    ts, deviceId, kind, message, acknowledged
```

Design notes that will save you rework:

- **`status` is one enum for all device types.** Multi-switch units also carry per-channel status; roll the unit's own `status` up from its channels (any ON → ON) so one status badge component works everywhere.
- **`onSince` is the safety mechanism.** The app writes it when a hazard device goes ON; the worker reads it. Never compute elapsed time on the phone — the phone can be offline, and the spec explicitly wants the cutoff server-side.
- **`/events` is your reporting source.** Derive usage totals from state-transition events rather than maintaining a running counter; a counter drifts whenever a client dies mid-session.
- **Store floor plans as bundled drawables**, not Firebase Storage. Storage adds an upload flow, more rules, and more failure modes for zero marks. Free sample plans are explicitly permitted by the spec. Drop `FirebaseStorageService.kt`.
- **Camera "streams" are mock URIs.** Static image URLs cycled on a timer read as a live feed and cost nothing.

---

## 4. Checklist

### Phase 0 — Foundation (do together, day 1–2)

- [x] Move `google-services.json` to `app/`
- [x] Add deps: Firebase BOM, `firebase-auth`, `firebase-database`, `navigation-compose`, `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `coil-compose`, `kotlinx-coroutines-play-services`
- [x] Add manifest permissions; register `SmartHomeApplication`
- [x] Resolve the compileSdk / AGP / Gradle version conflicts
- [x] Clean build produces a debug APK; installs and launches on a Pixel 6 AVD
- [x] Commit the agreed schema as `docs/SCHEMA.md` — this is your team's contract
- [x] Write auth-scoped security rules → `firebase/database.rules.json`
- [x] Write seed data covering one house, two floors, and ten devices across all five profiles → `firebase/seed-data.json`
- [ ] **Create the Realtime Database in the console, then re-download `google-services.json`** ← still blocks all runtime testing
- [ ] Enable the Email/Password sign-in provider
- [ ] Publish the security rules and import the seed — see `firebase/README.md`
- [ ] Confirm the build on the other two members' machines
- [ ] Agree branch strategy (`feat/<area>`) and that `main` always builds

### Phase 1 — Android client

**Data layer** — done, compiles clean

- [x] `DeviceStatus`, `DeviceType`, `EventSource`, `AlertKind` enums, each with a lenient `from()`
- [x] `Device` (+ `DeviceChannel`, `DeviceSafety`, `DeviceSchedule`, `CameraConfig`), `Floor`, `House`, `User`, `DeviceEvent`, `Alert`
- [x] `FirebaseAuthService` — sign-in, sign-up, password reset, sign-out, `authState()` flow
- [x] `FirebaseDatabaseService` — path builders, `valueEvents()` listener-to-Flow bridge, `observeObject`/`observeChildren`, atomic multi-path update
- [x] `UserRepository`, `HouseRepository`, `DeviceRepository`
- [x] Deleted `FirebaseStorageService.kt` and `Room.kt`

Three decisions worth knowing before building on this:

- **Enums are stored as strings, not Kotlin enums.** The Firebase mapper throws on an unrecognised
  constant, and two of the three writers to `/devices` are untyped JavaScript. Read them through
  `device.deviceType` / `device.effectiveStatus`, which fall back rather than crash the screen.
- **`Room` was dropped.** The schema places devices on a floor grid directly, so a room entity added
  a nesting level for no marks. The plan listed it before the schema existed; the schema wins.
- **Every toggle is one atomic multi-path update** covering status, `safety/onSince`, channel
  roll-up and the `/events` entry. A partial write would leave a hazard device ON with no `onSince`
  — precisely the state the worker cannot protect against.

**Auth & navigation** — done, verified on the emulator

- [x] `AuthViewModel` — `AuthFormState` with deferred validation, `AuthStatus` (Loading/SignedOut/SignedIn), sign-in, sign-up, password reset, sign-out
- [x] `LoginScreen` — stateless `LoginContent` inside a thin stateful wrapper, so it previews and tests without Firebase
- [x] `Screen` sealed class with typed route builders for all six destinations
- [x] `AppNavigation` — auth-gated, with a loading state while Firebase resolves
- [x] `MainActivity` wired to `AppNavigation`; `HomeScreen` is a marked placeholder for the dashboard workstream

Verified on a Pixel 6 AVD: login renders, empty-submit shows field errors, `imePadding` lifts the
form above the keyboard, and a real sign-in attempt surfaces the mapped provider error.

Three decisions:

- **Login and the authenticated graph are separate trees, not two destinations in one `NavHost`.**
  Swapping the whole tree on sign-out discards the authenticated back stack outright, so Back can
  never re-enter a signed-out user's dashboard.
- **Validation errors stay hidden until the first submit** (`showValidation`), so the form is not
  red on arrival.
- **Firebase errors are mapped to user-facing text**, including `CONFIGURATION_NOT_FOUND` →
  "Email/password sign-in is not enabled for this Firebase project yet", which is the exact state
  the project is in right now.
- **A failed `/users/{uid}` profile write does not block sign-in** — it is logged instead. The user
  is already authenticated, and this is the path that fails while the database does not exist.

**Dashboard**
- [ ] `HomeScreen` — house summary, floor list, active-device count, unread alerts
- [ ] `FloorScreen` — floor plan image with a grid overlay; devices positioned at `gridX`/`gridY`; tap to toggle, long-press for detail
- [ ] Add / edit / delete floors
- [ ] `FloorCard`, `DeviceCard`, `StatusBadge` components
- [ ] `HomeViewModel`

**Device profiles** — each needs a visibly distinct UI, this is graded
- [ ] Outlet — single toggle
- [ ] Multi-switch — expandable card, N independently addressable channels, rolled-up unit status
- [ ] Light — toggle plus schedule entry point
- [ ] Hazard (iron) — toggle, `maxOnDuration` config, live countdown, prominent warning styling
- [ ] Camera — snapshot grid, full-screen viewer, mock refresh cycle
- [ ] `DeviceScreen` detail view dispatching on `type`
- [ ] `DeviceViewModel`

**Scheduling**
- [ ] `ScheduleScreen` — time-range picker, day-of-week selection, enable toggle
- [ ] Max-on-duration config for hazard devices
- [ ] Writes to `/devices/.../schedule`; **all enforcement is the worker's job**

**Reporting**
- [ ] `ReportViewModel` — fold `/events` into per-device on-duration and switch counts
- [ ] `ReportScreen` — per-device usage bars, daily totals, most-used device, alert history
- [ ] Date-range filter

**Alerts**
- [ ] Observe `/alerts`; in-app banner or notification for safety cutoffs
- [ ] Acknowledge action

**Polish**
- [ ] Loading, empty, and error states on every screen
- [ ] Offline behaviour — `setPersistenceEnabled(true)` and a connectivity indicator
- [ ] Dark theme check, edge-to-edge insets, rotation survival

### Phase 2 — Web hardware simulator

- [ ] `simulator/index.html` + Firebase Web SDK (compat CDN is fine)
- [ ] Anonymous auth or a dedicated simulator account
- [ ] Subscribes to `/devices/{houseId}` and renders the house
- [ ] Visual appliance states — lamp glows, outlet LED, iron heat indicator, camera tile
- [ ] Multi-switch gang box rendering with per-channel indicators
- [ ] Heartbeat: write `lastSeen` every ~10s per device
- [ ] Fault injection buttons — force `ERROR`, force `DISCONNECTED`, simulate an externally-driven ON. **This is how you prove externally-driven updates reach the phone without a refresh.**
- [ ] Deploy to Firebase Hosting (free tier) so it has a shareable URL for the video

### Phase 3 — Server-side safety worker

**Choose the runtime first.** Cloud Functions now requires the Blaze billing plan. The spec explicitly permits *"a backend cloud listener **or a worker process**"*, so a plain Node.js process avoids the billing problem entirely:

- **Recommended:** Node.js + `firebase-admin` with a service-account key, run locally during the demo or hosted free on Render/Railway. Zero billing risk, trivial to debug, fully satisfies the wording.
- Cloud Functions only if someone on the team already has Blaze enabled.

- [ ] Project setup, service-account key, **key gitignored**
- [ ] Listener on `/devices/{houseId}` (`child_changed`)
- [ ] **Max-duration cutoff** — when a HAZARD device turns ON, arm a timer for `maxOnDurationSec`; on expiry write `status: "OFF"` and push an alert
- [ ] Cancel the timer if the device is turned off manually
- [ ] Re-arm outstanding timers on worker restart by scanning `onSince` (otherwise a restart silently disables safety)
- [ ] **Schedule engine** — minute tick, apply `onAt`/`offAt` for enabled schedules
- [ ] **Staleness sweep** — `lastSeen` older than ~30s → `DISCONNECTED`
- [ ] Append every worker-driven change to `/events` with `source: "WORKER"`
- [ ] Structured console logging — makes the cutoff visible on camera during the demo

### Phase 4 — Deliverables

- [ ] **APK** — signed release build, uploaded, link in the README
- [ ] **Technical report** covering exactly the three topics the spec names: the synchronisation mechanism, floor representation, and simulator operations. Include the schema, a component diagram, and a sync sequence diagram
- [ ] **README** — setup steps, Firebase config, how to run simulator and worker, APK link, team contributions
- [ ] **Demo video, ≤25 min** — all three members on camera, each introducing themselves and their own contribution
- [ ] Rehearse the three-way live demo: phone, simulator, and worker logs on screen together
- [ ] Verify every member has meaningful commits under their own Git identity — individual defence is explicitly assessed

---

## 5. Suggested three-way split

Aligned so each member owns one deliverable end-to-end and can defend it individually.

| Member | Owns | Also |
|---|---|---|
| **A** | Data layer, auth, navigation, Firebase services, schema | Report: sync mechanism |
| **B** | Dashboard, floor plan grid, all five device-profile UIs, camera | Report: floor representation |
| **C** | Worker (safety + schedules), web simulator, reporting screen, alerts | Report: simulator operations |

Everyone: own screens' ViewModels, own commits, own segment of the video.

**Dependency warning:** B and C are both blocked on A's data layer. A should ship models + repositories as a thin vertical slice in the first few days, even if the internals are stubbed, so the other two aren't idle.

---

## 6. Risks

| Risk | Mitigation |
|---|---|
| Simulator and worker deferred to the final week | They are ~40% of the marks. Start Phase 2/3 in parallel with Phase 1, not after it |
| Cloud Functions blocked by Blaze billing | Use the Node.js worker instead — explicitly allowed by the spec |
| RTDB test-mode rules expire mid-project | Write proper auth-scoped rules in Phase 0 |
| Schema churn breaking teammates | Freeze `docs/SCHEMA.md` after Phase 0; changes need a group decision |
| Live demo fails on the night | Record a backup take. Seed script must reset to a known-good state in one command |
| Uneven Git history undermining individual defence | Every member commits under their own identity, weekly |
