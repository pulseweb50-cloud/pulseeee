<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/f585af9d-0d65-4963-bcbd-8b03fd4f6297

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Set up Firebase (see below)
6. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
7. Run the app on an emulator or physical device

## Firebase setup

`app/google-services.json` contains your Firebase project's real keys, so it is
**gitignored** and must never be committed. `app/google-services.json.example`
shows the expected shape of the file.

**Local development:**
1. Create a Firebase project at https://console.firebase.google.com, register an
   Android app with package name `com.aistudio.pulse.qwrztb`, and download its
   `google-services.json`.
2. Place it at `app/google-services.json` (this path is already in `.gitignore`,
   so it will stay local to your machine).
3. Enable **Authentication** (Email/Password) and **Realtime Database** for the
   project in the Firebase console.

If `app/google-services.json` is missing, the app still builds and runs -
Firebase-backed features (auth, sync) will simply be unavailable until you add
a real config, or set one up from the in-app Settings -> "Firebase Account
Session" screen.

**GitHub Actions (CI/CD):**
The workflow in `.github/workflows/android.yml` needs the following repo
secrets (**Settings -> Secrets and variables -> Actions**):

| Secret | Purpose |
|---|---|
| `GEMINI_API_KEY` | Your Gemini API key |
| `GOOGLE_SERVICES_JSON` | The full contents of your `google-services.json`, pasted as-is |

Without `GOOGLE_SERVICES_JSON` set, CI builds fall back to a safe placeholder
config (from `app/google-services.json.example`) so forks and pull requests
still build successfully - they just won't have working Firebase features.

For release builds, also add `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD`
secrets to match the `release` signing config in `app/build.gradle.kts`.
