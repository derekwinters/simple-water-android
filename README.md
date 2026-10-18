# Simple Water

A small Android app for tracking how much water you drink against a daily goal, with reminders
until you get there. Styled like Interval Trainer, with a launcher icon in the same family as the
owner's other apps.

## Features

- **A goal that fits you.** On first launch, enter your weight (kg or lb) and how active you are
  (Sedentary, Lightly active, Active, Very active). The recommended goal is your weight in kg ×
  35 ml, plus 350 / 700 / 1,050 ml for the more active levels, rounded to the nearest 50 ml —
  70 kg and sedentary is 2,450 ml. Skip it and you start from 70 kg, sedentary. You can set your
  own goal in settings and go back to the recommendation at any time.
- **One-tap logging.** The home screen shows today's total against the goal as a ring, with
  quick-add buttons for your drink sizes (250 ml and 500 ml to start, or 8 and 16 fl oz; up to four, set in settings)
  and a custom amount. Today's drinks are listed underneath; delete one by mistake and "Undo"
  brings it back. The day starts at local midnight.
- **Pace.** On a reminder day, a thin grey ring outside the progress ring shows how much you
  would ideally have drunk by now, stepping up at each reminder time, with a line saying how far
  behind or ahead you are.
- **Home screen widget.** The app's cup fills the widget, sitting straight on the wallpaper, and
  the water rises toward today's goal, with a dashed line where the ideal amount says you should
  be. Settings can add a percentage or the amount (fl oz, or litres) inside the cup, which turns
  into a check once the goal is met, hide the line, put a card behind the cup, or turn off the
  translucent fill inside it. Place it at 2 × 2 and resize it as you like. It updates when you log or
  delete a drink, at each reminder time, at midnight and when settings change; tap it to open
  the app.
- **Reminders you control.** On or off, which days, from and until when, and how often (every
  30, 60, 90 or 120 minutes). Reminders stop for the day once the goal is met. Each reminder shows
  your progress and has a "Log 250 ml" button (your first drink size) so you can log without
  opening the app. Reminders survive reboots and clock or time-zone changes, and use exact alarms
  when Android allows them.
- **ml or fl oz.** Everything is shown and entered in the unit you choose.
- **History.** A week at a time: each day's total against the goal that applied that day, how
  many days met it, and the average.

Behaviour is specified in [docs/spec/](docs/spec/README.md).

## Requirements

- Android 8.0 (API 26) or later.
- To build: JDK 17+, Android SDK with platform 35 and build-tools 35.0.0 (set `ANDROID_HOME` or
  `sdk.dir` in `local.properties`).

## Build and install

```sh
./gradlew test             # unit tests (:core logic, design-system mapping, resource checks)
./gradlew assembleDebug    # app/build/outputs/apk/debug/simple-water-<version>-debug.apk
adb install -r app/build/outputs/apk/debug/simple-water-*-debug.apk
```

Or copy the APK to the phone and open it (allow installs from that source when asked).

Every pull request's CI run uploads its debug APK as the `simple-water-debug-apk` workflow
artifact (kept for 14 days). Pull requests from branches of this repository also upload
`simple-water-release-apk`, signed with the release key, which installs over the released app in
place. A debug APK and a release-signed APK cannot be installed over each other.

On first launch, allow notifications so reminders can appear. On Android 12/12L, also allow
"Alarms & reminders" from settings if the app shows that row, so reminders arrive on time.

## Releases

Merging to `main` runs release-please, which keeps a release PR up to date from Conventional
Commit titles. Merging that PR tags a release and attaches `simple-water-<version>-release.apk`.
Release APKs are signed with a stable release key
([docs/adr/0001](docs/adr/0001-release-signing-with-a-stable-keystore.md)), so each release
upgrades the installed app in place and keeps its data.

## Project layout

```
core/            pure Kotlin/JVM: goal maths, units, day windows, reminder slots, pace, history
designsystem/    Compose tokens and components ported from Interval Trainer
app/src/main/java/com/derekwinters/water/
  data/          Room entities and DAOs, DataStore settings, repository
  reminders/     alarm scheduling, receivers, notifications
  ui/            first run, home, history, settings, reminders screens
docs/spec/       behaviour, with requirement IDs
docs/brand/      launcher icon SVG sources
```

Work in this repository follows [ai-sdlc](https://github.com/derekwinters/ai-sdlc); see
`CLAUDE.md` and `.ai-sdlc/`.
