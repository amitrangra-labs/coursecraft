# CourseCraft Android app

Kotlin + Jetpack Compose. Phase 0 scope: app skeleton + a screen that proves it can reach the
backend (`GET /api/health`). Supabase sign-in and the feature screens come next.

The MVP is distributed as a **sideloadable signed APK** — see
[../docs/DISTRIBUTION.md](../docs/DISTRIBUTION.md). CI builds it; there is no Play Store for the MVP.

## Project layout

```
android/
├── settings.gradle.kts / build.gradle.kts / gradle.properties
└── app/
    ├── build.gradle.kts        # AGP 8.7.3, Kotlin 2.0.21, Compose, minSdk 24 / target 35
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/org/coursecraft/app/
        │   ├── MainActivity.kt          # Compose entry + theme
        │   ├── data/BackendClient.kt     # tiny HTTP client (health check)
        │   └── ui/HomeScreen.kt          # skeleton screen
        └── res/                          # strings, theme, launcher icon
```

## Build

> Requires the Android SDK + JDK 17. If you don't have Gradle installed, generate the wrapper
> once with `gradle wrapper` (or just let CI build — see below).

```bash
cd android
gradle :app:assembleDebug                       # debug APK
gradle :app:assembleRelease                      # release APK (unsigned unless keystore env set)
# point the app at a real backend instead of the emulator default:
gradle :app:assembleDebug -PBACKEND_BASE_URL=https://your-backend.example.com
```

Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`. Install with `adb install <path>`.

`BACKEND_BASE_URL` defaults to `http://10.0.2.2:8080` (the host loopback from an emulator).
Cleartext HTTP is enabled for development; production must use HTTPS.

## Release signing (portable APK)

Release builds are signed from **environment variables**, so no keystore lives in the repo.

1. Create a keystore once (keep it safe — the same key must sign every release):
   ```bash
   keytool -genkeypair -v -keystore coursecraft-release.keystore \
     -alias coursecraft -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Add these **GitHub Actions secrets** to the repo (Settings → Secrets → Actions):
   | Secret | Value |
   | --- | --- |
   | `ANDROID_KEYSTORE_BASE64` | `base64 -i coursecraft-release.keystore` output |
   | `ANDROID_KEYSTORE_PASSWORD` | keystore password |
   | `ANDROID_KEY_ALIAS` | `coursecraft` |
   | `ANDROID_KEY_PASSWORD` | key password |
3. Cut a release by pushing a tag:
   ```bash
   git tag v0.1.0-mvp && git push origin v0.1.0-mvp
   ```
   The `android-release` workflow builds the signed universal APK and publishes a **GitHub
   Release** with the APK, its `.sha256`, and the signing-certificate fingerprints.

To sign locally instead, export `ANDROID_KEYSTORE_FILE`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` before `gradle :app:assembleRelease`.

## Install & verify on a phone

1. Download the APK from the GitHub Release.
2. Verify integrity: `sha256sum coursecraft-*.apk` matches the `.sha256`.
3. Enable "Install unknown apps" for your browser/file manager, then tap the APK — or
   `adb install coursecraft-*.apk` over USB.

## CI

- `android` workflow — builds a debug APK on every push/PR touching `android/**` (compile check).
- `android-release` workflow — builds the signed APK + GitHub Release on a `v*` tag.
