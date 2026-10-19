# Report: what was updated, and what is still open

The combined report was updated to match the code as it now stands. The edited copy is
saved alongside the original as a **new file**, so nothing was overwritten:

- Original: `MAHMS_Combined_Report_Task2_updated.docx` (untouched)
- Updated: `MAHMS_Combined_Report_Task2_updated_v2.docx`

Structure, styles and all 37 embedded images are unchanged. Only the wording listed below
was replaced.

## What was changed

| Where | What it said before | What it says now |
|---|---|---|
| Part B 8, technology stack, Web | "the framework choice is still to be confirmed" | Static HTML, CSS and JavaScript calling the team API, with a framework noted as a possible later change |
| Part B 8, Backend/API | "a team-built REST API is planned" | The REST API the team built: Node.js 20 and Express over PostgreSQL, verifying the Firebase ID token |
| Part B 8, Database | PostgreSQL named only as the XISD5319 design | PostgreSQL behind the API, twelve tables with UUID keys; Room is the phone's offline cache |
| Part F 1, requirements table, "Online service / API created and integrated" | "The team has not yet built a REST server of its own" | The API, its endpoints, its token and role checks, and the app calling it through MahmsApi |
| Part F 1, requirements table, "GitHub, README and automated testing" | 51 JUnit tests | 54 Android unit tests, 40 API tests, a workflow that runs both |
| Part F 3, online services | Firestore only | Adds the API calls for booking, SOS and payment, and the Settings server-status line |
| Part F 7, automated testing | "47 of the 51 tests ... compiled and executed outside Android" | 54 Android tests run with Gradle on a clean checkout, plus 40 API tests against a real PostgreSQL database. 94 in total |
| Part F 8, DevOps, GitHub Actions | One Android job | Two jobs: Android, and the API against a PostgreSQL service container |
| Part F 8, DevOps, Azure | Board only | Adds `azure-pipelines.yml`, the setup guide and the 62-item work-item import |
| Part F 9, limitations, back end | "The team has not written a separate REST API of its own" | The API exists; the remaining limitation is that it is not yet deployed to a public host |
| Part F 9, limitations, roles | A client can pick its own role | The API enforces Patient-only registration properly; the Firestore path still needs custom claims |
| Part F 9, limitations, scoping | Any signed-in user can read every collection | The API scopes a patient to their own records; the Firestore rules are not yet that strict |
| Part B 6.2, task table, testing row | "51 unit tests and GitHub Actions workflow added" | 54 Android unit tests, 40 API tests and the workflow |
| Part G intro | Listed the Task 2 additions up to the Firestore work | Adds the REST API, the Android client for it, and the website |

## Still open: things only the team can supply

These are the placeholders the report still carries. None of them can be filled in without
the team's own accounts, hardware or recordings.

- [ ] **GitHub repository link** on the cover page, replacing the GitFront mirror.
- [ ] **Device model and Android version** of the phone used, Part F 1, first table row.
- [ ] **Demo video link**, Part F 1, last table row. Script: `docs/demo-script.md`.
- [ ] **Azure DevOps link and screenshots**, Part F 8. Steps: `docs/azure-devops-setup.md`.
- [ ] **Deployed API URL**. Put it in Part F 9 (which currently says the API is not yet
      hosted), in `backend/README.md`, and in the app's `mahms.api.baseUrl`.
- [ ] **Screenshots** throughout: Register, Login with the Google button, an invalid-input
      error, the Firebase Authentication users page, Firestore data and rules, the Database
      Inspector, Settings, Help, the Payments tab and a receipt, SOS with an address, and a
      green GitHub Actions run.
- [ ] **Confirm the Gantt dates** with the team. The report assumes Week 1 starts Monday
      31 August 2026 and Week 11 ends Friday 13 November 2026, which is what
      `docs/azure-work-items.csv` uses.

## Still open: Part G source listings

Part G embeds the source of each file, and it was last regenerated before the API work. It
does **not** yet include:

- `app/src/main/java/com/doodoobops/mahms/data/api/MahmsApi.kt`
- the `backend/` folder (the API itself, the schema, the seed and the tests)
- the `web/` folder

It also still shows the older versions of the five files that changed for the API work:
`Entities.kt`, `MahmsDatabase.kt`, `Repositories.kt`, `AppContainer.kt`,
`PatientPaymentsScreen.kt`, `Billing.kt` and `SettingsScreen.kt`.

Regenerating Part G from the current repository is the last documentation job before
submission.

## A note on what the report claims

Two claims in the report are worth checking against reality before submission, because a
marker may test them:

1. **"The API is not yet deployed to a public host."** True as written. Once it is deployed,
   update Part F 9, or the limitation reads as out of date.
2. **The demo video.** The script assumes the API is reachable during recording. If it is
   still only on the local network at that point, say so in the voice-over rather than
   implying it is hosted.
