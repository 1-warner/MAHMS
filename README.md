# MAHMS — Mobile Appointment & Healthcare Management System

MAHMS for **Mpilo Central Hospital**, Bulawayo, Zimbabwe. Three parts in one repository:

| Folder | What it is |
|---|---|
| `app/` | Native Android app. Kotlin, Jetpack Compose (Material 3), Room, Firebase. |
| `backend/` | The team's REST API. Node.js and Express over PostgreSQL. [Read more](backend/README.md) |
| `web/` | The website. HTML, CSS and JavaScript, no build step. [Read more](web/README.md) |

> *"Mpilo" means life.*

## Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin · Jetpack Compose (Material 3) |
| Min / Target SDK | 26 / 35 |
| Local database | Room (WAL + FK enforcement), seeded on first launch |
| Authentication | Firebase Authentication: email/password + Google single sign-on |
| Online database | Cloud Firestore (profiles/roles, patients, appointments, prescriptions, emergencies, staff) |
| Team REST API | Node.js 20 + Express over PostgreSQL, in `backend/`. Verifies the Firebase ID token on every request. |
| External API | OpenStreetMap Nominatim, for the SOS street address |
| Navigation | Jetpack Navigation Compose |
| Background work | WorkManager (medication reminders) |
| DI | Manual container (`AppContainer`) provided via a `CompositionLocal` |
| Charts | Lightweight Canvas-drawn bar/line charts (no external charting dependency) |
| Fonts | Public Sans (UI) + IBM Plex Mono (clinical data), bundled in `res/font/` |

## Building

Open the project in **Android Studio** (Giraffe+/Hedgehog) and let it sync, or from the CLI:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug
```

On Linux or macOS:

```bash
export JAVA_HOME=/path/to/android-studio/jbr
./gradlew testDebugUnitTest assembleDebug
```

`local.properties` points `sdk.dir` at the local Android SDK. The first build downloads
Gradle 8.13 and the dependencies, and needs **SDK platform 35** and **build-tools 35**
installed (Android Studio > SDK Manager, or `sdkmanager "platforms;android-35" "build-tools;35.0.0"`).

### Pointing the app at the REST API

The app works without the API, from Room and Firebase alone. To switch the API on, give it a
base URL. Either add a line to `local.properties` (which is git-ignored):

```properties
mahms.api.baseUrl=https://mahms-api.onrender.com
```

or pass it on the command line, or set the `MAHMS_API_BASE_URL` environment variable:

```bash
./gradlew assembleDebug -Pmahms.api.baseUrl=http://10.0.2.2:3000
```

`10.0.2.2` is how the Android emulator reaches `localhost` on the host machine. A physical
phone needs the machine's LAN address, or the deployed URL.

Left unset, `BuildConfig.MAHMS_API_BASE_URL` is empty, `MahmsApi.isConfigured` is false and
every API call is skipped. Settings then shows "Server status: not configured for this build".

## Demo accounts

The database seeds six accounts (one per role). Password for all: **`Password1`**

| Role | Email |
|---|---|
| Patient | `patient@mpilo.co.zw` |
| Doctor | `doctor@mpilo.co.zw` |
| Nurse | `nurse@mpilo.co.zw` |
| Hospital Administrator | `admin@mpilo.co.zw` |
| Pharmacist | `pharmacist@mpilo.co.zw` |
| Emergency Responder | `responder@mpilo.co.zw` |

On a fresh install the app shows **Register** first (because `is_registered` is false).
Register any account to flip the flag, then sign in with a demo account above — or sign in
directly with a demo account (registration only gates the first-run routing, not the seeded logins).

## Project structure

```
com.doodoobops.mahms/
├── data/api/                         NominatimApi (external) + MahmsApi (the team's API)
├── data/cloud/                       Firebase auth repository + two-way CloudSync
├── data/location/LocationProvider     device GPS for the SOS button
├── data/{entity,db,repository,seed}   Room layer + DatabaseSeeder
├── di/AppContainer                    manual DI container
├── domain/model/Role                  roles + demo record bindings
├── notifications/                     NotificationHelper + MedicationReminderWorker
├── navigation/{Screen,NavGraph}       routes + NavHost
├── ui/theme/                          Color · Type · Shape · Theme · StatusColors
├── ui/components/Components           shared composables (cards, buttons, chips, fields…)
├── ui/{auth,patient,doctor,nurse,admin,pharmacist,responder}
└── utils/{AppLog,Billing,DateUtils,GeoUtils,ValidationUtils,SessionManager,PdfExporter}
```

```
backend/   src/{app,server,db,auth,refs}.js · src/routes/ · db/*.sql · test/ · openapi.yaml
web/       index · services · contact · login · appointments · css/ · js/
docs/      azure-devops-setup.md · azure-work-items.csv · demo-script.md
```

## Design tokens

Ported verbatim from `design-system/colors_and_type.css`:
Mpilo Blue `#1A66BE`, Care Teal `#199F8E`, Emergency Red `#C1272D`, blue-grey neutrals,
12dp default radius. The website in `web/` uses the same tokens, so the two look like one
system. Status chips are colour-coded
(Pending=Warning, Confirmed=Primary, Completed/Resolved=Success, Cancelled=Neutral, Active=Danger).

## Known simplifications (local MVP)

- **Auth ↔ clinical records:** each login is linked to its own `PatientEntity` / `DoctorEntity`
  by e-mail (`AppContainer.startLinkedSession`); a record is created on first login. The demo
  accounts map to seeded records through `Demo` in `domain/model/Role.kt`.
- **Registration** creates Patient accounts only. Staff accounts are issued by the administrator.
  Firestore rules make the role immutable, but creating a profile with a chosen role through the REST API is
  still possible; closing that needs custom claims or a Cloud Function.
- **Ids:** patients, doctors and payments use time-based ids; appointments, prescriptions and
  emergencies still use device-local auto-increment ids, so two phones could collide on those.
  The API avoids this: it generates UUIDs and stores the app's local id as `client_ref`.
- **Passwords** are held by Firebase Authentication (hashed server-side). The phone keeps an unsalted SHA-256 hash only for the offline-login fallback.
- **Sync** is two-way: Sync now (and each login) pulls Firestore documents into Room with non-destructive upserts, then pushes local changes. Last write wins; there is no conflict resolution.
- **Bottom-nav hosts** keep tab state in a `Scaffold` rather than nested navigation graphs.
- **Orientation** is portrait-locked app-wide (the Admin-dashboard landscape exception is not wired up).
- **Payments** use a simulated gateway (no money moves). The record, the receipt and the
  revenue figures are real; when the API is reachable the receipt number is issued by a
  database sequence on the server, otherwise it is derived from the local payment id.
  **SOS** uses real device GPS plus OpenStreetMap Nominatim reverse geocoding.
- All copy is English; isiNdebele + Shona are noted as pre-launch work.

## Features added for Task 2

- **Settings** (Profile tab, every role): account details, medication-reminder notification switch, Sync now, change password, About, log out.
- **Help system**: in-app *Help and user guide* plus [USER_GUIDE.md](USER_GUIDE.md).
- **Online services**: Firebase Authentication (email/password and Google sign-in) and Cloud Firestore; see [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
- **Payments and receipts**: Appointments > Payments tab, pay with EcoCash/Card/Cash (simulated), receipt with Save as PDF.
- **SOS with GPS**: device location plus a street address from the free OpenStreetMap Nominatim API; responders open it in a maps app.
- **Logging**: `AppLog` writes to Logcat (tag `MAHMS`) at sign-in, registration, sync, booking, prescriptions, SOS, payments and reminders.
- **Database**: nine Room tables, each seeded with at least ten records on first launch.
- **A REST API the team wrote**: `backend/`, Node.js and Express over PostgreSQL, twelve
  tables, Firebase ID token verification, role checks and per-patient scoping. 40 route tests.
- **The app talking to that API**: `data/api/MahmsApi.kt`. Booking an appointment, raising an
  SOS and recording a payment are sent to the server as well as saved locally, and Settings
  shows a live "Server status" line driven by `GET /health`.
- **Website**: `web/`, public pages plus patient sign-in and an appointment list read from
  the API.

## Testing and CI

**Android: 54 unit tests** (JUnit 4) in `app/src/test`, covering validation, dates, hashing,
billing and receipt numbering, GPS helpers and the Firestore mappers:

```bash
./gradlew testDebugUnitTest
```

**API: 40 route tests** (node:test and supertest) in `backend/test`, covering health,
authentication, role checks, per-patient scoping, booking, retry behaviour, payments, receipt
numbering and the SOS flow. They run against a real PostgreSQL database, so point
`DATABASE_URL` at a throw-away one: the suite reloads the schema and drops every table first.

```bash
cd backend
DATABASE_URL=postgres://mahms:mahms@localhost:5432/mahms npm test
```

94 automated tests in total.

The GitHub Actions workflow `.github/workflows/android.yml` runs both jobs on every push and
pull request: the Android tests and debug APK, and the API tests against a PostgreSQL service
container. If no `app/google-services.json` is committed the workflow uses a placeholder so
the build still compiles. `azure-pipelines.yml` mirrors the same two jobs for Azure DevOps;
see [docs/azure-devops-setup.md](docs/azure-devops-setup.md).

## Commit convention

Small commits, present tense, describing the change. When the work matches an Azure DevOps
work item, reference it at the end so the board links the commit:

```
Add payments tab and numbered receipt #42
```

For the GitHub repository with an Azure Boards connection, write `AB#42` instead of `#42`,
because a bare `#42` means a GitHub issue. Branches: `main` is demo-ready, `dev` integrates,
feature work goes on `feature/<short-name>`.

## Documentation

| File | What it covers |
|---|---|
| [backend/README.md](backend/README.md) | API endpoints, running it, deploying it, the tests |
| [backend/openapi.yaml](backend/openapi.yaml) | Full API description, 13 paths |
| [web/README.md](web/README.md) | The website, its config and how to serve it |
| [FIREBASE_SETUP.md](FIREBASE_SETUP.md) | Firebase project, SHA-1, Firestore rules |
| [USER_GUIDE.md](USER_GUIDE.md) | How to use the app, per role |
| [docs/azure-devops-setup.md](docs/azure-devops-setup.md) | Board, work items, pipeline |
| [docs/demo-script.md](docs/demo-script.md) | Timed script for the demo video |

## Team

doodoobops: Lunga (ST10445079, Android), Lunathi Danster (ST10448224, web front end), Bohlokwa Letsoso (ST10096006, backend / DevOps). Module XISD6329, Rosebank International.

## Firebase setup

The app needs a Firebase project before it will build. Follow [FIREBASE_SETUP.md](FIREBASE_SETUP.md)
(create project, add the Android app and SHA-1, enable Email/Password + Google, download
`google-services.json` into `app/`, create Firestore and publish `firestore.rules`).
