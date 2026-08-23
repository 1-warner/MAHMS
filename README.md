# MAHMS — Mobile Appointment & Healthcare Management System

A native Android app for **Mpilo Central Hospital**, Bulawayo, Zimbabwe. Built in Kotlin
with Jetpack Compose (Material 3), Room, and a local-only (mock) data layer.

> *"Mpilo" means life.*

## Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin · Jetpack Compose (Material 3) |
| Min / Target SDK | 26 / 35 |
| Database | Room (WAL + FK enforcement), seeded on first launch |
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
├── domain/model/Role                  the six MAHMS roles
├── notifications/                     NotificationHelper + Medication/Appointment workers
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

## Feature notes

- **Accounts are linked to clinical records.** Registering as a Patient or Doctor creates the
  matching `PatientEntity`/`DoctorEntity`, and the session carries those ids, so each account
  sees its own appointments, prescriptions and alerts.
- **Payments.** Patients settle the consultation fee (EcoCash / Cash / Medical Aid / Bank Card)
  from the appointment card; admin revenue and reports read the same records.
- **Password reset.** Self-service from the login screen (email + registered phone), plus an
  administrator reset in **Admin → Accounts**.
- **Reminders.** Booking an appointment schedules a reminder 24 hours ahead; medication
  reminders are daily, and the reminder sheet's take/missed actions drive the adherence ring.
- **SOS.** Requires a deliberate ~2s press-and-hold; releasing early cancels.

## Known simplifications (local MVP)

- **Passwords** use SHA-256 (no salt) — adequate for a local demo only.
- **Bottom-nav hosts** keep tab state in a `Scaffold` rather than nested navigation graphs.
- **Charts** are drawn with Canvas rather than Vico/MPAndroidChart, to keep the dependency
  graph (and the build's memory footprint) small.
- **GPS** is mocked; the SOS location is a placeholder string.
- **No automated tests** yet.
- All copy is English; isiNdebele + Shona are noted as pre-launch work.
