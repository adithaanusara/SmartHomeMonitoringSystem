# Firebase setup

One-time steps for project `smarthomeapp-c60d9-77331`. Do these before anyone runs the app against real
data — the client currently builds and launches fine but every read and write fails until step 1
is done.

## 1. Create the Realtime Database

Console → **Build → Realtime Database → Create Database** → pick a region → start in **locked
mode** (the rules in step 3 replace whatever you pick here).

Then **re-download `google-services.json`** into `app/`. Until you do, the SDK guesses the database
URL from the project id instead of reading it from config, which works only if you happened to pick
the default region — and fails confusingly if you did not.

## 2. Enable email/password auth

Console → **Authentication → Sign-in method → Email/Password → Enable**.

Without this, every call in `FirebaseAuthService` fails with `CONFIGURATION_NOT_FOUND`.

## 3. Publish the security rules

Copy `database.rules.json` into Console → **Realtime Database → Rules → Publish**.

Do not leave the database in test mode. Test-mode rules expire after 30 days and will silently
break the demo, quite possibly the night before submission.

What the rules enforce:

- Nothing is readable or writable while signed out.
- A user can only touch `/users/{uid}` for their own uid.
- `/floors`, `/devices`, `/events` and `/alerts` under a house are restricted to that house's members.
- `status` and `type` are validated against the enums in `docs/SCHEMA.md`, so a typo from the
  JavaScript simulator is rejected at the database rather than rendering as a broken device.
- `/events` is append-only: existing entries cannot be edited or deleted, which is what makes the
  usage report trustworthy.
- `/alerts` is read-only to the app except for the `acknowledged` flag. The worker writes alerts
  through the Admin SDK, which bypasses rules by design.

## 4. Seed the demo data

Console → **Realtime Database → ⋮ → Import JSON** → upload `seed-data.json`.

**Importing at the root replaces the entire database**, so only do this on the demo project.

Before importing, replace all three occurrences of `REPLACE_WITH_YOUR_UID` with the uid of an
account you have already signed up (Console → Authentication → Users → copy the User UID). The
seed cannot know your uid in advance, and without this the house has no members, so the rules
will correctly deny you access to it.

The seed gives you one house, two floors, and ten devices covering all five profiles:

| Floor | Devices |
|---|---|
| Ground | 2 outlets, 1 light (scheduled 18:30–23:00), 1 three-gang box, 1 camera, 1 iron (15 min cutoff) |
| First | 1 outlet, 1 light (schedule disabled), 1 two-gang box, 1 camera |

The two floors reference drawables `plan_ground_floor` and `plan_first_floor`. Add those to
`app/src/main/res/drawable/` when building the floor screen — any free sample floor plan will do.
