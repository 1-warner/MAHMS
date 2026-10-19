# MAHMS — Mobile Appointment & Healthcare Management System

A native Android app for **Mpilo Central Hospital**, Bulawayo, Zimbabwe. Built in Kotlin
with Jetpack Compose (Material 3), Room (local cache), and Firebase (online authentication + Cloud Firestore).

> *"Mpilo" means life.*

## Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin · Jetpack Compose (Material 3) |
| Min / Target SDK | 26 / 35 |
| Local database | Room (WAL + FK enforcement), seeded on first launch |
| Authentication | Firebase Authentication: email/password + Google single sign-on |
| Online database | Cloud Firestore (profiles/roles, patients, appointments, prescriptions, emergencies, staff) |
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

`local.properties` points `sdk.dir` at the local Android SDK. The first build downloads
Gradle 8.13 and dependencies (and SDK platform 35 if missing).

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
├── data/{entity,db,repository,seed}   Room layer + DatabaseSeeder
├── di/AppContainer                    manual DI container
├── domain/model/Role                  roles + demo record bindings
├── notifications/                     NotificationHelper + MedicationReminderWorker
├── navigation/{Screen,NavGraph}       routes + NavHost
├── ui/theme/                          Color · Type · Shape · Theme · StatusColors
├── ui/components/Components           shared composables (cards, buttons, chips, fields…)
├── ui/{auth,patient,doctor,nurse,admin,pharmacist,responder}
└── utils/{DateUtils,ValidationUtils,SessionManager,PdfExporter}
```

## Design tokens

Ported verbatim from `design-system/colors_and_type.css`:
Mpilo Blue `#1A66BE`, Care Teal `#199F8E`, Emergency Red `#C1272D`, warm-grey neutrals,
12dp default radius, blue-grey shadows. Status chips are colour-coded
(Pending=Warning, Confirmed=Primary, Completed/Resolved=Success, Cancelled=Neutral, Active=Danger).

## Known simplifications (local MVP)

- **Auth ↔ clinical records:** each login is linked to its own `PatientEntity` / `DoctorEntity`
  by e-mail (`AppContainer.startLinkedSession`); a record is created on first login. The demo
  accounts map to seeded records through `Demo` in `domain/model/Role.kt`.
- **Registration** creates Patient accounts only. Staff accounts are issued by the administrator.
  Firestore rules make the role immutable, but creating a profile with a chosen role through the REST API is
  still possible; closing that needs custom claims or a Cloud Function (see CLAUDE_CODE_HANDOFF.md).
- **Ids:** patients, doctors and payments use time-based ids; appointments, prescriptions and
  emergencies still use device-local auto-increment ids, so two phones could collide on those.
- **Passwords** are held by Firebase Authentication (hashed server-side). The phone keeps an unsalted SHA-256 hash only for the offline-login fallback.
- **Sync** is two-way: Sync now (and each login) pulls Firestore documents into Room with non-destructive upserts, then pushes local changes. Last write wins; there is no conflict resolution.
- **Bottom-nav hosts** keep tab state in a `Scaffold` rather than nested navigation graphs.
- **Orientation** is portrait-locked app-wide (the Admin-dashboard landscape exception is not wired up).
- **Payments** use a simulated gateway (no money moves). **SOS** uses real device GPS plus OpenStreetMap Nominatim reverse geocoding.
- All copy is English; isiNdebele + Shona are noted as pre-launch work.

## Features added for Task 2

- **Settings** (Profile tab, every role): account details, medication-reminder notification switch, Sync now, change password, About, log out.
- **Help system**: in-app *Help and user guide* plus [USER_GUIDE.md](USER_GUIDE.md).
- **Online services**: Firebase Authentication (email/password and Google sign-in) and Cloud Firestore; see [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
- **Payments and receipts**: Appointments > Payments tab, pay with EcoCash/Card/Cash (simulated), receipt with Save as PDF.
- **SOS with GPS**: device location plus a street address from the free OpenStreetMap Nominatim API; responders open it in a maps app.
- **Logging**: `AppLog` writes to Logcat (tag `MAHMS`) at sign-in, registration, sync, booking, prescriptions, SOS, payments and reminders.
- **Database**: nine Room tables, each seeded with at least ten records on first launch.

## Testing and CI

Unit tests (JUnit 4, 51 tests) live in `app/src/test`; they cover validation, dates, hashing, billing, GPS helpers and the Firestore mappers:

```
./gradlew testDebugUnitTest
```

The GitHub Actions workflow `.github/workflows/android.yml` runs the tests and builds the debug APK on every push and pull request. If no `app/google-services.json` is committed the workflow uses a placeholder so the build still compiles.

## Team

doodoobops: Lunga (ST10445079, Android), Lunathi Danster (ST10448224, web front end), Bohlokwa Letsoso (ST10096006, backend / DevOps). Module XISD6329, Rosebank International.

## Firebase setup

The app needs a Firebase project before it will build. Follow [FIREBASE_SETUP.md](FIREBASE_SETUP.md)
(create project, add the Android app and SHA-1, enable Email/Password + Google, download
`google-services.json` into `app/`, create Firestore and publish `firestore.rules`).
