# MAHMS demo video script

A timed script for the Task 2 demo video. Target length is **6 to 8 minutes** with a
voice-over, recorded on a **physical phone**, not an emulator, because the brief asks for the
app running on a real device.

Read the lines as a guide, not word for word. It is better to sound like you know the app
than to read a script aloud.

---

## Before you record

Tick all of these off first. Re-recording because of a missed step is the main time waster.

- [ ] Phone charged above 50 percent, and plugged in if possible.
- [ ] Do Not Disturb **on**, so no notification banners appear over the app.
- [ ] Screen timeout set to 5 minutes or more.
- [ ] Screen recorder set to capture **microphone** audio, and test it with a 10-second clip.
      Check the clip back before recording the whole thing.
- [ ] Brightness up, so the recording is readable.
- [ ] Demo accounts reset: clear the app data (Settings > Apps > MAHMS > Storage > Clear data)
      and delete the test users in the Firebase console, so registration works from clean.
- [ ] Internet on and working. Check `https://<your-api>/health` answers in the phone browser.
- [ ] The API is deployed and awake. Free hosting tiers sleep when idle: open `/health` a
      minute before you start so the first call is not slow.
- [ ] Laptop ready on a second screen with these tabs open, each already signed in:
      Firebase console (Authentication and Firestore), the API logs on the hosting dashboard,
      a database viewer or `psql`, GitHub Actions, and the Azure DevOps board.
- [ ] Know what you will say about the one thing that is simulated: the payment gateway.
      Say it plainly rather than hoping nobody notices.

Record the laptop screen separately, or point the phone camera at it, or screen-share the
laptop and the phone into one recording. Any of these is fine. The simplest reliable approach
is to record the phone screen for the app sections and the laptop screen for the console
sections, then cut them together.

---

## The script

### 0:00 to 0:30 — Introduction

> This is MAHMS, the Mobile Appointment and Healthcare Management System for Mpilo Central
> Hospital. I am [name] from team doodoobops, with [names]. This is our Task 2 prototype for
> XISD6329. It is an Android app with a REST API we built, backed by Firebase Authentication
> and a PostgreSQL database.

Show the home screen of the phone, then open the app.

### 0:30 to 1:10 — The app, Settings and About

- Let the splash screen play, land on Login.
- Open **Settings**.

> Every role has a settings menu: the account, a notifications switch, sync, change password,
> help and log out.

- Point at the **Server status** line.

> This line is live. It calls the health endpoint on our API, so it says connected when the
> server is reachable, and offline when it is not.

- Open **Help**, scroll one or two topics, come back.

### 1:10 to 2:10 — Registration and validation

- Log out and go to **Register**.
- Type an invalid email on purpose, move to the next field, show the error.
- Type a weak password, no capital and no number, show the error.
- Make the confirmation not match, show the error.

> Each field is checked before anything is sent, and the error appears under the field.

- Point out that there is no role picker.

> Registration only ever creates a patient account. Staff accounts are issued by the hospital
> administrator, so nobody can sign themselves up as a doctor.

- Fix the fields and register properly.

### 2:10 to 2:50 — Sign-in and the authentication service

- Sign out, sign back in with email and password.
- Sign out and use **Continue with Google**.

> Sign-in goes through Firebase Authentication, which is an online service. The app never
> stores the password. Firebase keeps a salted hash of it.

- **Switch to the laptop.** Firebase console > Authentication > Users.

> Here is the account that was just created, with its sign-in method and the time.

### 2:50 to 3:40 — Book an appointment, and the API behind it

- On the phone, as the patient, **Book an appointment**: choose a department, a doctor, a date
  and a time, give a reason, confirm.
- Show it appear in the appointments list.

> The appointment is saved on the phone first, so the app works offline, and it is also sent
> to our own REST API.

- **Switch to the laptop.** Show the API log line appearing, something like
  `POST /api/appointments -> 201`.
- Then show the row in the database:

```sql
SELECT appointment_date, appointment_time, status, reason
FROM appointments ORDER BY created_at DESC LIMIT 3;
```

> That is the row in PostgreSQL, written by the API we wrote, not by Firebase.

- Optionally show the same record in Firestore, to make the point that both back ends are live.

### 3:40 to 4:30 — Pay for a visit and open the receipt

- Appointments > **Payments** tab.
- Pay an outstanding visit with EcoCash.
- Show the receipt number, open the receipt, **Save as PDF**, open the saved file.

> The payment, the receipt and the revenue figures are real records. The gateway itself is
> simulated for this prototype, so no money actually moves. A live gateway such as Paynow would
> replace that one step.

- **Switch to the laptop** and show the payment and its receipt in the database:

```sql
SELECT p.amount, p.payment_method, r.receipt_no
FROM payments p JOIN receipts r ON r.payment_id = p.payment_id
ORDER BY p.created_at DESC LIMIT 3;
```

> The receipt number comes from a database sequence on the server, so two phones paying at the
> same moment can never be given the same number.

### 4:30 to 5:20 — Emergency SOS with a real location

- Make sure location is on.
- Press **SOS** and confirm.

> The alert carries the GPS position, and the app turns that into a street address using the
> OpenStreetMap Nominatim API.

- Sign out, sign in as the responder (`responder@mpilo.co.zw`).
- Show the alert with the patient name, the phone number and the street address.
- Tap **Open location in maps** and let the maps app open.

> The responder sees the address and can open the exact point in maps.

- If you have time, show the refused-permission case:

> If the patient refuses the location permission, the alert is still sent, with a note that
> the location is unavailable. Losing the alert would be worse than losing the position.

### 5:20 to 6:00 — Two-way sync

- **On the laptop**, in the Firebase console, edit a patient record, for example change the
  phone number.
- **On the phone**, Settings > **Sync now**.
- Open the patient profile and show the changed value.

> Sync runs both ways. It downloads every collection first and merges it into the local
> database, then uploads. That is how a change made in the console, or on another phone,
> reaches this device.

### 6:00 to 6:50 — The API and the tests

- **On the laptop**, show Postman or `curl` calling the API:

```bash
curl https://<your-api>/health
curl -H "Authorization: Bearer <idToken>" https://<your-api>/api/me
```

> Every endpoint except health needs the Firebase ID token. The API verifies it, then looks up
> the role. A patient can only ever see their own records, whatever ids they put in the
> request.

- Show one refusal on purpose, which is the most convincing part:

> Here is the same request as a patient, asking for another patient's data. It comes back with
> only their own records.

- Show GitHub Actions with a green run, and the test report artifact.

> Every push runs the Android unit tests, builds the APK, and runs the API tests against a
> PostgreSQL container.

### 6:50 to 7:30 — Azure DevOps and closing

- Show the Azure DevOps **board** with work items across the columns.
- Show a work item with its linked commits.
- Show the pipeline run.

> Work is tracked on the Kanban board, and commits reference the work item they belong to.

> That is MAHMS: an Android app on a real phone, signing in through an online authentication
> service, reading and writing both Cloud Firestore and our own REST API over PostgreSQL, with
> automated tests and a build pipeline. Thank you.

---

## Manual test cases to show or mention

Part F section 7 of the report lists test cases to run on the phone. The video does not have
room for all of them. These are the ones worth showing on camera, because they are the ones a
marker is most likely to doubt:

| Test | Where in the script |
|---|---|
| Register with an invalid email, weak password, mismatched confirmation | 1:10 |
| Sign in with Google | 2:10 |
| New account appears in the Firebase console | 2:50 |
| Appointment reaches the API database | 2:50 |
| Payment produces a numbered receipt and a PDF | 3:40 |
| SOS carries a street address, responder opens it in maps | 4:30 |
| SOS still sent when the location permission is refused | 4:30 |
| A console edit reaches the phone after Sync now | 5:20 |
| A patient cannot read another patient's records | 6:00 |

Run the rest off camera and record the result in the report table.

Two worth testing before you record, even though they are hard to film well:

- **Airplane mode sign-in.** Turn airplane mode on and sign in with an account that has signed
  in before. It should work from the local copy. Mention it rather than filming it, because
  toggling airplane mode mid-recording tends to look messy.
- **Cancelling the Google account picker.** It should show a message and not crash.

---

## If something goes wrong on the day

- **Google sign-in fails with error 10 or 12500.** The SHA-1 of the signing key is not in the
  Firebase project. See FIREBASE_SETUP.md. This cannot be fixed in the moment, so test it the
  day before.
- **Server status says offline.** The free hosting tier has gone to sleep. Open `/health` in a
  browser, wait for it to wake, then pull down to refresh in the app.
- **The SOS has no address.** Nominatim is rate limited and occasionally slow. The alert still
  goes out with the raw coordinates, so carry on and say so.
- **A screen is slow to load.** Say what it is doing rather than waiting in silence. Dead air
  is more noticeable than a slow screen.
