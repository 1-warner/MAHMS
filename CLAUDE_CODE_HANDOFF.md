# MAHMS Task 2: handoff for Claude Code

Project: MAHMS (Mobile Appointment and Healthcare Management System), Mpilo Central Hospital.
Module XISD6329 WIL, Task 2 (50% of the module). Team doodoobops: Lunga (Android), Lunathi Danster (web), Bohlokwa Letsoso (backend, database, DevOps).

Read this whole file first. It says what is already done, what is not, and exactly what to build next.

---

## 0. Ground rules

- Style: no em dashes in any text you write. Short, direct wording. Student-level English, not marketing copy.
- Do not invent facts for the report (device model, links, screenshots). Leave clearly marked placeholders.
- The rubric forbids zip files on GitHub. Never commit a zip.
- Do not commit a real `google-services.json` secret. It is not a secret key (see FIREBASE_SETUP.md), but the CI workflow already falls back to `.github/ci/google-services.placeholder.json`.
- **Nothing in this repo has been compiled since the last round of edits.** The previous session had no Android SDK. Step 1 below is to build it and fix any compile error before doing anything else.

## 1. First job: build and test (do this before everything else)

```bash
./gradlew testDebugUnitTest --stacktrace
./gradlew assembleDebug --stacktrace
```

Expected: 51 unit tests pass (27 old plus 24 new: `BillingTest` 6, `GeoUtilsTest` 9, `CloudPullMappersTest` 9). The 24 new tests and the pure helpers (`Billing`, `GeoUtils`, `CloudMappers`) were compiled and run on a plain JVM with the Room annotations stubbed, so those are low risk. The Compose screens and Android classes were only reviewed by reading, so compile errors there are possible. Likely spots:

- `ui/patient/PatientPaymentsScreen.kt` (new)
- `ui/patient/PatientSosScreen.kt` (permission launcher, location, Nominatim)
- `data/location/LocationProvider.kt` (`LocationManagerCompat.getCurrentLocation`)
- `data/cloud/CloudSync.kt` (`pullAll` generic local function)
- `di/AppContainer.kt` (`startLinkedSession`)
- `ui/responder/ResponderScreens.kt` (maps intent)
- `ui/auth/RegisterScreen.kt` (role dropdown removed, some imports now unused)

Fix errors minimally. Do not redesign.

## 2. What was changed in the previous session (already in this repo)

| Area | Change |
|---|---|
| Account to record link | No more `Demo.PATIENT_ID` / `Demo.DOCTOR_ID`. `AppContainer.startLinkedSession` finds or creates the signed-in user's own patient or doctor row (by e-mail, then the demo map in `Role.kt`, else new). Ids stored in `SessionManager.currentPatientId` / `currentDoctorId`. |
| Registration | Patient only. Role dropdown removed. Staff accounts are meant to be issued by an administrator. |
| Firestore rules | `firestore.rules` rewritten: role is immutable, collections are allow-listed, staff-only writes for `doctors` and `staff`. |
| Sync | Two-way. `CloudSync.pullAll()` downloads all collections into Room with `@Upsert` (not REPLACE, because REPLACE would cascade-delete children), then `pushAll()`. Patients do not push `doctors` / `staff`. |
| Payments | `PatientPaymentsScreen.kt`, third tab "Payments" on patient Appointments. Pay (simulated gateway), numbered receipt `RCP-yyyy-NNNNN`, Save as PDF. `Billing.kt`. |
| SOS | Real GPS (`LocationProvider`) plus OpenStreetMap Nominatim reverse geocoding (`NominatimApi`), saved in `EmergencyEntity.location` as `Address (lat, lon)`. Responder has an "Open location in maps" button. Alert is sent even without permission. |
| Logging | `utils/AppLog.kt` (Logcat tag `MAHMS`), about 47 call sites. |
| Docs | README, USER_GUIDE.md, FIREBASE_SETUP.md, in-app Help updated. |

Known weaknesses that were documented, not fixed (candidates for later, see section 10):
appointments, prescriptions and emergencies still use device-local autoincrement ids (cross-device collisions possible); Firestore rules do not scope a patient to their own data; a client can still create a `users` doc with any valid role through the REST API.

## 3. The big missing piece: a team-built API (rubric: 10 marks created, 10 marks integrated)

Right now the app only uses the Firebase SDK plus one public API (Nominatim). The rubric asks for an API the team created and integrated. Build one.

### Recommended design (keep it small, it must be demonstrable in a video)

- **Stack:** Node.js 20 and Express, PostgreSQL. Reuse Bohlokwa's files `mahms_schema.sql` and `mahms_seed.sql` (tables: departments, users, patients, staff, patient_vitals, appointments, prescriptions, payments, receipts, medication_reminders, emergencies). Put them in `backend/db/`.
- **Auth:** keep Firebase Authentication as the identity provider. The API verifies the Firebase ID token on every request with `firebase-admin` (`Authorization: Bearer <idToken>`), then maps the Firebase e-mail to a row in `users`. This keeps SSO working and avoids a second password system.
- **Hosting:** Render, Railway or Fly.io free tier with a managed Postgres. (A Supabase Postgres database also works as the host for the same schema.) Note the URL in the README.
- **Endpoints (minimum viable, JSON):**
  - `GET /health`
  - `GET /api/me`
  - `GET /api/appointments?patientId=` and `POST /api/appointments`, `PATCH /api/appointments/:id`
  - `GET /api/doctors?department=`
  - `GET /api/prescriptions?patientId=`, `POST /api/prescriptions` (doctor role only)
  - `POST /api/payments`, `GET /api/payments?patientId=`, `GET /api/receipts/:paymentId`
  - `POST /api/emergencies`, `GET /api/emergencies?status=active`, `PATCH /api/emergencies/:id`
- **Role checks** in middleware (patient, doctor, nurse, pharmacist, admin, responder).
- **Docs:** `backend/README.md` with run steps and an OpenAPI file (`backend/openapi.yaml`) or a Postman collection. Add a few API tests (supertest) and run them in CI.
- Use UUIDs for ids on the server. This also removes the id-collision weakness.

### Integration into the Android app (this is the 10 marks for "integrated")

Keep Room as the offline cache. Add a thin client next to `data/api/NominatimApi.kt`:

- `data/api/MahmsApi.kt` using `HttpURLConnection` (same pattern as `NominatimApi`) or Retrofit.
- Add the base URL in `BuildConfig` (`buildConfigField`), and `INTERNET` is already declared.
- Get the Firebase ID token with `FirebaseAuth.getInstance().currentUser?.getIdToken(false)` and send it as the Bearer header.
- Wire at least these flows through the API, with Room fallback when offline: book appointment, create emergency (SOS), record payment and fetch receipt. Do it inside the existing repositories (`AppointmentRepository`, `EmergencyRepository`, `PaymentRepository`) so the UI code does not change.
- Log each call with `AppLog`.
- Add a visible line in Settings, "Server status: connected / offline", driven by `GET /health`. It shows well in the demo video.

Acceptance: the demo video can show a request in the API logs (or Postman) appearing when the app books an appointment, and the row appearing in Postgres.

## 4. Website (the report still says Web: TBD, now filled with a placeholder choice)

The brief and Task 1 include a website. The repo is Android only. Ask Lunathi which framework they chose, then:

- Create `web/` in the repo, built from the Part E wireframes in the report.
- Minimum: public pages (home, services, contact) plus patient login and appointment list using the new API.
- Add its build to the CI workflow if it has one.
- Update the report line "Web: HTML5, CSS3 and JavaScript front end ..." to the real framework.

## 5. Azure DevOps (the user will do the "screen takeover" part himself)

The user will drive the Azure DevOps web UI. Claude Code can prepare everything around it:

- Write `azure-pipelines.yml` that mirrors `.github/workflows/android.yml` (JDK 17, `./gradlew testDebugUnitTest assembleDebug`, publish test results and the APK artifact).
- Write `docs/azure-devops-setup.md` with exact steps: create project, import or mirror the GitHub repo, create the Kanban board, the work item list (generate it from the Gantt chart and the task table in Part B 6.2, with weeks 1 to 11 starting Mon 31 Aug 2026), link commits to work items with `#ID` in commit messages, and create the pipeline from `azure-pipelines.yml`.
- Generate a CSV of work items that can be imported into the board (Title, Work Item Type, State, Iteration, Tags).
- Write the commit-message convention into the README.

What stays manual: logging in, creating the organisation or project, running the import, and taking the screenshots for the report.

## 6. GitHub hygiene (rubric: GitHub, README, automated testing)

- Real GitHub repo link must replace the GitFront link in the report cover page (placeholder is there now).
- Make sure `.gitignore` excludes `build/`, `.gradle/`, `local.properties`, `*.zip`.
- Check the README has: what it is, features, how to run, test, demo accounts, API link, screenshots folder, team, licence.
- Confirm the Actions run is green and keep a screenshot of it for the report.
- Use feature branches and small commits with clear messages. Tag `v1.0-task2`.

## 7. Demo video (rubric: demo with voice-over)

Claude Code cannot record it. Write `docs/demo-script.md` with a timed script (about 6 to 8 minutes) the team can read while recording on a **physical phone**:

1. Install and open the app on the phone, show Settings and About.
2. Register a patient, show the validation errors, then sign in. Show Google sign-in.
3. Show Firebase console Authentication > Users while signing in.
4. Book an appointment, show the row in the API database and in Firestore.
5. Pay for a visit, open the receipt, save it as PDF.
6. Press SOS with location on, then show the responder side with the address and the maps button.
7. Change a record in the Firebase console, press Sync now, show it arrive.
8. Show the API (Postman or logs) and the Postgres table.
9. Show the GitHub Actions green run and the Azure DevOps board.

Add a pre-recording checklist: phone charged, screen recorder on, microphone test, demo accounts reset (see section 9), airplane-mode test for the offline sign-in case.

## 8. Report items that need real data (do not invent)

File: `MAHMS_Combined_Report_Task2_updated.docx`. Placeholders still open:

- Device model and Android version (Part F, section 1, table row 1).
- Demo video link (same table, last row).
- GitHub repository link (cover page).
- Azure DevOps link and screenshots (Part F, section 8).
- Screenshots: Register, Login with Google button, invalid input error, Firebase Authentication users page, Firestore data and rules, Database Inspector table, Settings, Help, Payments tab and receipt, SOS with address, GitHub Actions green run.

Once the API and website exist, update: Part B tech stack, Part F sections 1, 3 and 9 (known limitations say "no REST API of its own"; change that), and the source-code Part G (it was regenerated from the repo files, so regenerate again after code changes).

The Gantt dates in the report assume Week 1 starts Mon 31 Aug 2026 and Week 11 ends Fri 13 Nov 2026 (anchored on Task 1 submission, 04 Sep 2026). Confirm with the team.

## 9. Firebase and sign-in setup (needs the team's own accounts)

- A real `app/google-services.json` for the team's Firebase project, with the debug and release **SHA-1** added, or Google sign-in will fail with error 10 or 12500. Steps are in FIREBASE_SETUP.md.
- Publish `firestore.rules` in the Firebase console.
- The seeded demo accounts (`patient@mpilo.co.zw`, `doctor@mpilo.co.zw`, `nurse@...`, `admin@...`, `pharmacist@...`, `responder@...`, password `Password1`) are created in Firebase on first sign-in. Staff demo profiles are written by the client, which the current rules allow for any valid role.
- To reset a demo: clear app data, delete test users in the Firebase console.

## 10. Security and quality improvements (do after sections 1 to 3)

In priority order:

1. **Staff provisioning with custom claims.** Add a Cloud Function or an API admin endpoint that sets a `role` custom claim. Change the rules to check `request.auth.token.role` instead of the `users` document. This closes the hole where a client can create its own profile with any role.
2. **Per-patient scoping in the rules and the API.** A patient may read and write only documents with their own `patientId`.
3. **UUID ids** for appointments, prescriptions and emergencies (Room entities, DAOs, mappers, tests, seed data). Needs a Room migration (the database is version 1 with `fallbackToDestructiveMigration`).
4. **Real payment gateway.** Paynow (Zimbabwe) for EcoCash. Replace the `delay(900)` in `PayDialog`. Needs a merchant account; the user must supply keys. Keep a simulated mode behind a build flag.
5. **Offline login hash.** The local SHA-256 hash is unsalted. Use a salted slow hash or remove the offline password fallback.
6. **Release build.** Signing config, R8 minify check, versionCode bump, app icon check, Play Store listing text.
7. **Instrumented or Compose UI tests** for login and booking, if time allows.

## 11. Suggested order of work

1. Build and fix compile errors (section 1). Push, confirm the Actions run is green.
2. API: schema, endpoints, token verification, deploy (section 3).
3. Android integration of the API, with Room fallback (section 3).
4. Azure pipeline file, work-item CSV, setup doc (section 5), then the user does the web UI part.
5. Website skeleton (section 4).
6. Demo script (section 7), README polish (section 6).
7. Report updates (section 8) and security items (section 10) if time remains.

## 12. Repo map

```
app/src/main/java/com/doodoobops/mahms/
  data/api/        NominatimApi.kt (external API client)
  data/cloud/      FirebaseAuthRepository, CloudSync (two-way), CloudMappers
  data/db/         Room DAOs and database
  data/location/   LocationProvider.kt (GPS)
  data/repository/ repositories (push to cloud after each write)
  data/seed/       DatabaseSeeder (10+ rows per table)
  di/              AppContainer (manual DI, startLinkedSession)
  ui/<role>/       Compose screens per role
  utils/           AppLog, Billing, GeoUtils, SessionManager, PdfExporter ...
app/src/test/      51 JUnit tests
firestore.rules    security rules
.github/workflows/android.yml   CI
FIREBASE_SETUP.md, USER_GUIDE.md, README.md
```
