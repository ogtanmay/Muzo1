# Firebase Setup Guide for Muzo (Google OAuth)

This guide walks you through setting up **Firebase Authentication with Google Sign-In** for Muzo on Android.

---

## 1. Create a Firebase Project

1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Click **Add project** (or select an existing project).
3. Enter a project name (e.g., `Muzo-Music`) and click **Continue**.
4. Enable or disable Google Analytics (optional), then click **Create project**.

---

## 2. Register Your Android App

1. In the Project Overview page, click the **Android** icon (`</>`) to add an app.
2. Enter your **Android package name**:
   - By default in this project, it is:
     ```
     com.aistudio.muzo.kpxvmw
     ```
     *(If you changed `applicationId` in `app/build.gradle.kts`, enter that exact value).*
3. Enter an App nickname (e.g., `Muzo Android`).
4. **Debug signing certificate SHA-1** (Required for Google Sign-In):
   - In your project terminal or command line, run:
     ```bash
     ./gradlew signingReport
     ```
   - Look for the `SHA1` fingerprint under the `debug` variant:
     ```
     Variant: debug
     Config: debug
     Store: .../debug.keystore
     Alias: androiddebugkey
     SHA1: XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX:XX
     ```
   - Copy and paste that SHA1 string into the Firebase Console.
5. Click **Register app**.

---

## 3. Download and Place `google-services.json`

1. Download the `google-services.json` file provided by Firebase.
2. Place it directly inside the `app/` directory of this project:
   ```
   muzo/
   ├── app/
   │   ├── google-services.json   <-- Place file here
   │   ├── build.gradle.kts
   │   └── src/
   ├── build.gradle.kts
   └── settings.gradle.kts
   ```
3. Muzo's build script automatically detects `google-services.json` and applies the Google Services plugin:
   ```kotlin
   if (file("google-services.json").exists()) {
       apply(plugin = "com.google.gms.google-services")
   }
   ```

---

## 4. Enable Google Sign-In in Firebase Console

1. In the Firebase Console left sidebar, go to **Build** → **Authentication**.
2. Click **Get Started** (if you haven't enabled Authentication yet).
3. Under the **Sign-in method** tab, click **Google**.
4. Switch the toggle to **Enable**.
5. Select a project support email from the dropdown.
6. Click **Save**.

---

## 5. Web Client ID (Optional / Auto-configured)

- Once you download the updated `google-services.json` after enabling Google Sign-In, Firebase includes your `client_id` (type 3 - Web client) automatically.
- The app reads the default Web Client ID directly from resources provided by `google-services.json`.

---

## 6. Build and Test

1. Run the app:
   ```bash
   ./gradlew assembleDebug
   ```
   or open in Android Studio and hit **Run**.
2. Go to **Settings** → **Account / Sign In** (or tap the user profile button in the top corner).
3. Tap **Sign in with Google**.
4. Select your Google account. Your profile name, email, and avatar will sync seamlessly!

> **Note:** If `google-services.json` is not yet added, Muzo operates in **Guest Mode** with all local playlist creation, history, favorites, and music streaming fully functioning offline and locally without errors!
