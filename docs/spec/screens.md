# Screens

Every screen is built from the design system (`design-system.md`) and its root is one of the
three layouts (DS-070). Text is drawn with `AppText` roles (DS-034), never a raw font size.

- **UI-001** **Navigation.** The app starts on first-run (GOAL-010) until a profile has been
  saved, and on home afterwards. Home → settings, home → history, settings → reminders. Back
  returns to the previous screen. Tapping a reminder notification opens home (REM-012).
- **UI-002** **First run** uses the form layout (DS-075): header "Welcome", the explanation, the
  weight field with kg/lb chips, the activity level as a 2 × 2 chip grid, the live recommended
  goal (in `numeric.stat`), a secondary "Skip" button, and the single primary action "Start
  tracking" (GOAL-010, GOAL-011). The content scrolls on short screens.
- **UI-003** **Home** uses the list layout (DS-071): header "Today" with the settings icon as its
  one trailing action. Items, in order: the progress ring card (LOG-003); the quick-add row — the
  default size as the screen's single `PrimaryButton` ("+ 250 ml"), every other size and
  "Custom…" as `SecondaryButton`s (LOG-004–006); the "This week" row opening history, with
  "<n> of 7 days met"; then today's entries (LOG-007), or "Nothing logged yet today." when there
  are none. The undo bar (LOG-008) sits in the layout's bottom slot.
- **UI-004** The progress ring card shows today's total in `numeric.large` inside the ring, "of
  <goal>" below it, and "<remaining> to go" or "Goal met" under the ring. This is the whole card
  whenever there is no ideal amount (LOG-014).
- **UI-013** **The ideal ring.** While there is an ideal amount (LOG-013, LOG-014), the ring card
  draws `ProgressRing`'s outer ring (DS-026): the ideal amount as a fraction of the goal, with a
  tick at each of today's reminder times. The centre text is unchanged. Under the rings, in place
  of UI-004's line:
  - after the first reminder time, a main line in `AppText` `BodyStrong` — "220 ml behind",
    "370 ml ahead" or "On track" (LOG-015) — over a `Meta` line "Ideal by <tk>: <ideal> ·
    <remaining> to go", where `tk` is the last reminder time that has passed (UI-009 format);
  - before the first reminder time, "Ideal starts at <t0>" over "<remaining> to go";
  - once the goal is met, UI-004's "Goal met" alone.
  The lines use the theme's normal text colours: behind and ahead are not colour-coded. The card
  follows the clock, so the ideal amount steps on time while home stays open.
- **UI-005** **History** uses the list layout with a back action, header "History" (HIST-001–004).
- **UI-006** **Settings** uses the list layout with a back action, header "Settings". Sections,
  each introduced by an `AppText` section label: **Goal** (Weight, Activity level, Daily goal,
  and "Use recommended goal" only while a custom goal is set); **Units** (Volume: ml / fl oz
  chips); **Drinks** (Quick-add sizes); **Reminders** (a summary row with a `Toggle`, opening the
  reminders screen); **Permissions** (Notifications status, REM-014; Alarms & reminders on
  Android 12/12L while not allowed, REM-015).
- **UI-007** Settings edits that need input open the design system's `AlertDialog` with content
  (DS-012): Weight (number field + kg/lb chips), Activity level (chips), Daily goal (number field
  in the volume unit), Quick-add sizes (four number fields). Cancel left, Save right (DS-011);
  Save is disabled while the input is invalid.
- **UI-008** **Reminders** uses the list layout with a back action, header "Reminders": the on/off
  `Toggle` row, the days of the week as seven multi-select chips (M T W T F S S), start and end
  as `ValueStepper`s in 30-minute steps (REM-003), the interval as four chips (30 / 60 / 90 /
  120 min), and a line saying when the next reminder is ("Next reminder: today 10:00", or "No
  reminders scheduled"). Every change is saved and rescheduled at once (REM-008).
- **UI-009** Times are formatted with the device's 12/24-hour setting.
- **UI-010** Every icon-only control has a content description (DS-091).
- **UI-011** The window is edge-to-edge with light system-bar icons (DS-023); the layouts inset
  their content (DS-080). No screen applies its own system-bar padding.
- **UI-012** The app's theme declares no action bar in its own body (DS-022).

## Home screen widget

A home screen widget shows today's two rings without opening the app. It has no text: the app's
cup in the middle identifies it.

- **UI-014** **Size.** The widget is placed at 2 × 2 cells and can be resized larger
  horizontally and vertically, never below 2 × 2 (`minWidth`/`minHeight` 110dp,
  `targetCellWidth`/`targetCellHeight` 2, `resizeMode` horizontal and vertical). The rings fill
  the widget's smaller side, less `spacing.sm` on each side, and stay centred.
- **UI-015** **What it shows.** On the home ring card's background (`chip`, `spacing.md`
  corners, UI-004) the widget draws the same rings as home (DS-027): the inner ring is today's
  total against the effective goal (LOG-003), and the outer ring with its ticks is the ideal
  amount (UI-013, LOG-013). When there is no ideal amount — reminders off, today not an active
  reminder day, or fewer than two reminder times today (LOG-014) — the outer ring and its ticks
  are hidden, as on home. No text is drawn; the widget's content description is the app's name
  (UI-010).
- **UI-016** **The cup.** The launcher icon's foreground (BRAND-056) sits in the centre, its
  108-unit canvas spanning the inner ring's opening, so the cup (inside BRAND-020's safe zone)
  never touches the ring.
- **UI-017** **Tap.** Tapping anywhere on the widget opens the app on the home screen, as the
  reminder notification does (UI-001, REM-012).
- **UI-018** **Refresh.** The widget is redrawn when an entry is logged, undone or deleted (from
  the app or the reminder's action, REM-012); when a reminder alarm is handled (REM-006), so the
  outer ring steps at each reminder time; at local midnight (LOG-002), by an alarm of its own,
  exact when the app may schedule exact alarms and inexact otherwise (as REM-007); after a time or
  time-zone change, a reboot or an app update (REM-008's broadcasts); when any setting changes
  (the goal, the volume unit and the reminder settings among them); and whenever the launcher
  asks (placed, resized, restored). The midnight alarm is armed while at least one widget is
  placed and cancelled when the last one is removed.
- **UI-019** **Widget picker.** The picker's preview image is the launcher icon's foreground,
  the same cup the widget shows (BRAND-056).

**Invariant — the widget's rings always equal the home screen's rings at the same moment; both
come from the same `:core` functions (`Rings`), never a second calculation.**

**Invariant — the widget never shows a previous day's rings.** Every redraw computes the rings
from the entries of the local date of the moment it is drawn (LOG-002), and the midnight refresh
of UI-018 redraws it when the date changes.

**Invariant — the widget's cup is the launcher foreground drawable itself (BRAND-056), whose
path data is the master icon's (BRAND-017), never a redrawn copy.**

## Build checklist

- [x] `WaterNavHost` with routes first-run, home, history, settings, reminders (UI-001).
- [x] Each screen's root is `ListLayout` or `FormLayout` (UI-002–008).
- [x] `ManifestDeclarationTest` covers the theme declaration (UI-012).
- [x] Home's ring card shows the ideal ring and its text from `Pace`, re-read on a clock tick
      (UI-013).
- [x] Home's ring card and the widget both take their rings from `Rings` in `:core`, unit tested
      for equal results, the hidden outer ring and midnight (UI-015, the widget invariants).
- [x] `WaterWidget` (Jetpack Glance) draws the rings with `RingBitmap`, opens home on a tap, and
      is refreshed by `WaterWidgetUpdater` at every point UI-018 lists (UI-014–019).
- [x] `WidgetDeclarationTest` checks the provider metadata and receivers (UI-014, UI-018, UI-019)
      and that the cup is the launcher foreground (BRAND-056).
- [ ] Seen on a device: placed, resized, refreshed and tapped (UI-014–018).
