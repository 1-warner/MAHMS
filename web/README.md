# MAHMS website

The public website and patient pages for MAHMS, built from the Part E wireframes in the
combined report.

Plain HTML5, CSS3 and JavaScript, with no build step and no framework. That matches what the
report's technology stack says and keeps the site deployable as static files anywhere.

> **Note for the team.** The report says the framework choice is still to be confirmed by the
> web developer. This is a working skeleton using the stack already described in the report,
> so nothing is blocked while that is decided. If Lunathi picks React, Vue or similar, the
> pages here are the content and layout reference to port, and the API calls in `js/app.js`
> carry over almost unchanged.

## Pages

| File | What it is |
|---|---|
| `index.html` | Home. What MAHMS does, and a pointer to the mobile app. |
| `services.html` | Departments, consultation fees, how booking works. |
| `contact.html` | Phone, email, address. The message form is not wired to a mailbox yet. |
| `login.html` | Patient sign-in through Firebase Authentication. |
| `appointments.html` | The signed-in patient's appointments and payments, read from the API. |
| `css/app.css` | Styles. Colours and spacing match the Android app's theme. |
| `js/config.js` | **The file to edit.** API URL and Firebase web config. |
| `js/app.js` | Firebase sign-in and the API client used by the two signed-in pages. |

## How it fits together

The website uses the same two online services as the Android app:

1. **Firebase Authentication** signs the patient in. The same account works on the phone and
   on the website, because it is the same Firebase project.
2. **The MAHMS API** (the `backend/` folder) serves the data. The site asks Firebase for an ID
   token and sends it as `Authorization: Bearer <token>` on every call. The API verifies the
   token and only ever returns that patient's own records.

The site holds no database of its own and no password. If the API is down, the public pages
still work and the signed-in pages say so.

## Run it locally

The pages use ES modules, which browsers refuse to load from a `file://` URL, so serve the
folder over HTTP rather than double-clicking the HTML:

```bash
cd web
python3 -m http.server 8080
```

Then open <http://localhost:8080>.

Before sign-in or the appointments page will work, edit `web/js/config.js`:

1. `apiBaseUrl`: where the API is running, for example `http://localhost:3000`.
2. `firebase`: the web app config from
   Firebase console > Project settings > General > Your apps > Web app > Config.

Both of these are public values by design. The protection is the Firebase security rules and
the token check in the API, not secrecy. Never put a service-account key in this folder.

You also need the API running and reachable, and `ALLOWED_ORIGIN` on the API set to the site's
origin (or left at `*` for the demo), or the browser will block the calls.

## Deploying

Any static host works: GitHub Pages, Netlify, Vercel, Cloudflare Pages, or the same host as
the API behind a path.

For GitHub Pages, the simplest route is Settings > Pages > Deploy from a branch, folder
`/web`. Then set `ALLOWED_ORIGIN` on the API to the Pages URL.

## Still to do

- Book an appointment from the website. The API endpoint (`POST /api/appointments`) already
  exists and is tested, so this is front-end work only.
- Google sign-in on the website, to match the app.
- Wire the contact form to a mailbox or a form service.
- Accessibility pass: the pages use semantic elements and labelled inputs, but they have not
  been tested with a screen reader.
