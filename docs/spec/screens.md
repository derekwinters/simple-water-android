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
  <goal>" below it, and "<remaining> to go" or "Goal met" under the ring.
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

## Build checklist

- [x] `WaterNavHost` with routes first-run, home, history, settings, reminders (UI-001).
- [x] Each screen's root is `ListLayout` or `FormLayout` (UI-002–008).
- [x] `ManifestDeclarationTest` covers the theme declaration (UI-012).
