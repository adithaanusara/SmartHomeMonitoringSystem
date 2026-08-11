# Smart Home Monitoring & Control System

SCS 3311 — Mobile Application Design & Development Mini-Project.

A Smart Home monitoring and control system in three parts:

| Part | Stack | What it is |
|---|---|---|
| **`app/`** | Kotlin, Jetpack Compose, Firebase | The Android client — multi-floor dashboard, device control, scheduling, usage reporting |
| **`simulator/`** | HTML, CSS, Firebase Web SDK | The companion hardware simulator standing in for the physical appliances |
| **`worker/`** | Node.js, firebase-admin | The server-side safety worker enforcing cut-offs, schedules and heartbeats |

All three share one Firebase Realtime Database and never talk to each other directly. State moves
between them purely through database listeners, which is what makes the sync bidirectional.

📄 **[Download the APK](RELEASE_URL_HERE)** ← replace with the GitHub Release link

---

## Download / install

The release APK is signed with a self-signed certificate, so Android will warn about installing
from an unknown source. Allow it for your browser or file manager and the install proceeds normally.

- **minSdk 26** (Android 8.0), **targetSdk 36**
- Needs a network connection; there is no offline-only mode, though the dashboard renders
  last-known state while disconnected.

---

## Documentation

| Document | Contents |
|---|---|
| [`docs/SCHEMA.md`](docs/SCHEMA.md) | The Realtime Database schema and its invariants — the contract all three programs share |
| [`PROJECT_PLAN.md`](PROJECT_PLAN.md) | Build plan, design decisions and verification record |
| [`firebase/README.md`](firebase/README.md) | Firebase console setup: database, auth, rules, seed data |
| [`worker/README.md`](worker/README.md) | Running the safety worker, and why it is a worker rather than a Cloud Function |
| [`simulator/README.md`](simulator/README.md) | Running the simulator, fault injection, and the three-way demo script |

---

## Running it

### 1. Firebase

Follow [`firebase/README.md`](firebase/README.md) — create the Realtime Database, enable
Email/Password sign-in, publish `firebase/database.rules.json`, and import `firebase/seed-data.json`.

### 2. Android app

Open the project in Android Studio and run, or:

```bash
./gradlew assembleDebug
```

`app/google-services.json` must match your Firebase project. If you create your own project,
re-download it — the database URL inside it is not guessable.

### 3. Simulator

```bash
npx serve simulator
```

Serve over `localhost`, not `file://` — Firebase Auth only accepts authorised domains.

### 4. Worker

```bash
cd worker
npm install
cp .env.example .env          # set FIREBASE_DATABASE_URL
# add service-account.json from the Firebase console
npm start
```

---

## Building a release APK

Signing credentials are deliberately not in the repository. To build a signed release you need
`keystore.properties` and the `.jks` it points at, both in the project root:

```properties
storeFile=release-keystore.jks
storePassword=…
keyAlias=smarthome
keyPassword=…
```

Then:

```bash
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Without `keystore.properties` the build still succeeds but produces an unsigned APK, so a fresh
clone is never blocked from building.

Release builds run R8. The Firebase model classes are held by `app/src/main/keepRules/rules.keep`
— the Realtime Database maps snapshots onto them reflectively, so without those rules every device
would deserialise empty **in release only**, showing a blank dashboard with no error.

---

## Tests

```bash
./gradlew testDebugUnitTest     # usage-report maths, 17 tests
cd worker && npm test           # schedule window logic, 16 tests
```

Both cover the pure logic most likely to be wrong and least likely to be noticed: deriving
on-durations from a transition log, and deciding when a schedule boundary fires.

---

## Team

| Member | Contribution |
|---|---|
| _name_ | _area_ |
| _name_ | _area_ |
| _name_ | _area_ |
