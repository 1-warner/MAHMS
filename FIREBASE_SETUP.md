# Firebase setup (one-time)

MAHMS uses Firebase for its **online-hosted authentication service** (email/password and
Google single sign-on) and **Cloud Firestore** as its **online database**.
The app will not build until `app/google-services.json` exists (step 4).

## 1. Create the Firebase project
1. Go to <https://console.firebase.google.com> and sign in with a Google account.
2. **Add project** → name it `mahms` → (Google Analytics can be turned off) → **Create project**.

## 2. Register the Android app
1. Project overview → **Add app** → Android icon.
2. **Android package name:** `com.doodoobops.mahms` (must match exactly).
3. **Debug signing certificate SHA-1** (required for Google sign-in): in Android Studio open
   the **Terminal** tab and run:
   ```
   ./gradlew signingReport
   ```
   (Windows: `gradlew signingReport`). Copy the **SHA1** line under `Variant: debug`.
   Paste it into the SHA-1 box. Every teammate who runs the app from their own
   computer must add *their* SHA-1 too (Project settings → Your apps → Add fingerprint).
4. Click **Register app**.

## 3. Turn on sign-in methods
Build → **Authentication** → **Get started** → **Sign-in method**:
- Enable **Email/Password**.
- Enable **Google** (choose a support email) → Save.

## 4. Download `google-services.json`
Do this **after** step 3 and after adding the SHA-1, so the file contains the web client id
that Google sign-in needs.
Project settings (gear icon) → Your apps → **Download google-services.json** →
place it in the `app/` folder (next to `app/build.gradle.kts`).

## 5. Create the database
Build → **Firestore Database** → **Create database** → choose a location near you
(e.g. `eur3` or `africa-south1` if offered) → **Start in production mode**.
Then open the **Rules** tab, replace everything with the contents of `firestore.rules`
in this repo, and **Publish**.

## 6. Run
Sync Gradle, then run on a **physical Android phone** (USB debugging on) with internet.
- Sign in with any demo account (e.g. `doctor@mpilo.co.zw` / `Password1`). The first
  sign-in creates the account in Firebase automatically.
- Check the console: **Authentication → Users** lists the accounts;
  **Firestore Database → Data** shows `users`, `patients`, `appointments`, etc.

## Security rules
`firestore.rules` makes the role in `users/{uid}` immutable, limits each collection to the signed-in
users of the app, and lets only staff roles write `doctors` and `staff`. Remaining gap: a client can
still create its own `users` profile with any valid role. Closing it needs custom claims or a Cloud
Function; staff accounts should be provisioned by the administrator that way.

## What is stored where
| Data | Location |
|---|---|
| Passwords | Firebase Authentication only (hashed server-side). |
| Profile and role | Firestore `users/{uid}` |
| Patients, appointments, prescriptions, emergencies, staff, ... | Firestore collections of the same name |
| Local copy (offline use) | Room database on the phone |

## Notes
- `google-services.json` identifies the project but does not contain secrets, so it is
  committed to the repo so GitHub Actions can build the app.
- If Google sign-in shows "no credential" or error 10/12500, the SHA-1 in step 2 was not
  added or was added after downloading the JSON: add it, re-download the file, rebuild.
