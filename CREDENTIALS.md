# MAHMS — Demo Login Credentials

These six accounts are seeded into the local database on first launch
(`data/seed/DatabaseSeeder.kt`). **Password for every account: `Password1`**

| Role | Email | Password |
|---|---|---|
| Patient | `patient@mpilo.co.zw` | `Password1` |
| Doctor | `doctor@mpilo.co.zw` | `Password1` |
| Nurse | `nurse@mpilo.co.zw` | `Password1` |
| Hospital Administrator | `admin@mpilo.co.zw` | `Password1` |
| Pharmacist | `pharmacist@mpilo.co.zw` | `Password1` |
| Emergency Responder | `responder@mpilo.co.zw` | `Password1` |

## Forgot your password?

Two routes, both in-app (there is no backend, so nothing is emailed or texted):

1. **Self-service** — on the login screen tap **Forgot password?**, confirm the account's
   email *and* its registered phone number, then set a new password.
2. **Administrator** — sign in as `admin@mpilo.co.zw` and open **Accounts**, then
   **Reset password** on the relevant user to set a temporary password for them.

## Notes
- On a **fresh install** the app opens on the **Register** screen (the `is_registered`
  flag is false). Register any account once to flip the flag — or just sign in directly
  with a demo account above; registration only gates first-run routing, not the seeded logins.
- After signing in you land on the home screen for that account's role.
- Passwords are stored as SHA-256 hashes (local demo only — not production-grade auth).
- To reset the seed data, uninstall the app (or clear its storage) and relaunch.
