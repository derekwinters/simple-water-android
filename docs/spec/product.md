# Product

- **APP-001** The app is called **Simple Water**. Its application ID and Kotlin package are
  `com.derekwinters.water`.
- **APP-002** It runs on Android 8.0 (API 26, `minSdk 26`) and later; `compileSdk`/`targetSdk`
  are 35. It is sideloaded (no Play Store listing).
- **APP-003** The toolchain matches Interval-trainer-android so that the ported design system
  (`design-system.md`) compiles unchanged: Gradle wrapper 8.11.1, AGP 8.7.3, Kotlin 2.0.21
  (Compose compiler plugin of the same version), Compose BOM 2024.12.01, Java 17. Persistence and
  alarms follow simple-stretch-android: Room (KSP, schema exported to `app/schemas/`) and
  `AlarmManager`. Settings are stored with Jetpack DataStore (Preferences).
- **APP-004** The build has three modules: `:core` (pure Kotlin/JVM: all maths and rules that
  need no Android type), `:designsystem` (Android library: tokens and components, the only module
  that depends on Material 3), and `:app`.
- **APP-005** The version lives in `gradle.properties` (`VERSION_NAME`, owned by release-please
  via its `x-release-please-version` marker, and `VERSION_CODE`). Every APK is named
  `simple-water-<versionName>-<buildType>.apk`.
- **APP-006** Release builds are signed only with the stable release keystore from repository
  secrets (`docs/adr/0001-release-signing-with-a-stable-keystore.md`).
  **Invariant — without all four signing inputs the release build is left unsigned, never
  debug-signed.**

## Build checklist

- [x] `settings.gradle.kts` includes `:app`, `:core`, `:designsystem`.
- [x] `app/build.gradle.kts` reads `VERSION_NAME`/`VERSION_CODE` and names APKs per APP-005.
- [x] Release signing from env vars only (APP-006).
