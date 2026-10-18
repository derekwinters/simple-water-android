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
  reminders screen); **Widget** ("Text in cup" with None / Percent / Amount chips, then "Ideal
  line", "Background" and "Cup fill", each with a `Toggle`, UI-023; each change is saved at
  once); **Permissions** (Notifications status, REM-014; Alarms & reminders on
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

A home screen widget shows today's progress without opening the app: the app's cup fills the
widget, and the water in it rises toward today's goal. There is one widget. By default it has no
text; a setting adds a number inside the cup (UI-021).

- **UI-014** **Size.** The widget is placed at 2 × 2 cells and can be resized larger
  horizontally and vertically, never below 2 × 2 (`minWidth`/`minHeight` 110dp,
  `targetCellWidth`/`targetCellHeight` 2, `resizeMode` horizontal and vertical).
- **UI-015** **What it shows.** The widget draws the cup (UI-016), scaled to fill the widget's
  smaller side: the cup's outline, including its stroke, is made as large as fits inside the
  widget less `spacing.xs` on each side, and is centred.
  - **Background.** While the "Background" setting is on (UI-023), the cup sits on the home ring
    card's background (`chip`, `spacing.md` corners, UI-004) filling the widget. While it is off
    (the default), nothing is drawn behind the cup and the wallpaper shows through.
  - **Cup fill.** While the "Cup fill" setting is on (the default, UI-023), the inside of the cup
    — BRAND-013's outline closed straight across its top — is filled with `bg` at 55% opacity, so
    the water, the ideal line and the text read on any wallpaper. While it is off, the inside of
    the cup is left clear.
  - **Order.** From the back: the background, the cup fill, the water, the ideal line (UI-020),
    the cup's outline, and the text or check (UI-021, UI-022).

  The water's height is today's total against the effective goal (LOG-003), the same fraction as
  home's inner ring, clamped between empty (no water drawn) and full. The widget's content
  description is the app's name (UI-010).
- **UI-016** **The cup.** The cup and its water are drawn from the master icon's path data and
  colours at a variable water level (BRAND-056): the cup is BRAND-013's outline, and the water is
  BRAND-012's shape with its wavy top and its gap to the cup, its top at the water's height.
- **UI-017** **Tap.** Tapping anywhere in the widget's area, with or without the background
  (UI-015), opens the app on the home screen, as the reminder notification does (UI-001,
  REM-012).
- **UI-018** **Refresh.** The widget is redrawn when an entry is logged, undone or deleted (from
  the app or the reminder's action, REM-012); when a reminder alarm is handled (REM-006), so the
  ideal line steps at each reminder time; at local midnight (LOG-002), by an alarm of its own,
  exact when the app may schedule exact alarms and inexact otherwise (as REM-007); after a time or
  time-zone change, a reboot or an app update (REM-008's broadcasts); when any setting changes
  (the goal, the volume unit, the reminder settings and every widget setting of UI-023 among
  them); and whenever the launcher asks (placed, resized, restored). The midnight alarm is armed
  while at least one widget is placed and cancelled when the last one is removed.
- **UI-019** **Widget picker.** The picker's preview image is the launcher icon's foreground
  (BRAND-056), the cup at three-quarters full.
- **UI-020** **The ideal line.** A dashed line across the inside of the cup at the height of the
  ideal fraction (the fraction home's outer ring shows, UI-013, LOG-013), drawn over the water. It
  is drawn only while the "Ideal line" setting is on (UI-023) and there is an ideal amount; when
  there is none — reminders off, today not an active reminder day, or fewer than two reminder
  times today (LOG-014) — it is hidden, as home's outer ring is. In icon units (BRAND-001) it runs
  from 0.6 outside the water's left edge to 0.6 outside its right edge at that height, stroke 0.9
  with round caps, dashes 2.2 on and 1.6 off, in `fg` at 75% opacity.
- **UI-021** **Text in the cup.** The "Text in cup" setting (UI-023) chooses what is written in
  the cup while the goal is not yet met:
  - **None** (the default): nothing.
  - **Percent**: today's total as a percentage of the effective goal, rounded **down** to a whole
    number, with a `%` sign: "45%", and "99%" at 99.6%.
  - **Amount**: today's total rounded **down**, in fl oz to a whole number ("84" at 84.6 fl oz)
    for fl oz users, and in litres to one decimal ("0.8" at 0.85 L, "1.1" at 1.1 L) for ml users
    (GOAL-008), with the unit — "fl oz" or "L" — under the number in smaller text. Only the
    widget shows litres; the volume unit setting stays ml / fl oz.

  Rounding down means a number never reaches the goal early. Before the goal is met the number is
  also never the goal itself: it is at most one step (1%, 1 fl oz or 0.1 L) below the goal as
  shown in that unit — 100%, the goal's whole fl oz as home shows it (GOAL-009), or the goal to the
  nearest 0.1 L. A total that is a whole number of steps but stored a hair under it (fl oz
  quick-adds are decimal ml, LOG-012) still shows that whole number.

  The text is drawn in `fg` in the numeric font (DS-030's JetBrains Mono), over a thin `bg`
  outline at 35% opacity (stroke 12 where the number's size is 100, half of it hidden under the
  fill) so it reads over the water's edge. It is centred in the cup at half the full water height
  (icon y 54.56) and scaled, number and unit together, to fit a box inside the water's edges with
  2.6 units of padding on each side, measured where the water is narrower 6 units below the
  centre (16.336 units wide), and 17.5 units tall. Each line is measured by its advance across
  and its font's ascent to descent down. Longer text is drawn smaller, so "9" and "999" fit the
  same cup. The unit is 0.42 of the number's size, its baseline 0.46 of the number's size below
  the number's.
- **UI-022** **Goal met.** While today's total is at or above the effective goal (home's "Goal
  met", UI-004), the Percent or Amount text is replaced by a check in the same place: a
  three-point line (−30, 2), (−10, 22), (30, −22) with stroke 13 and round caps and joins, in a
  frame where the number's size is 100, fitted to the text box of UI-021 at 75% of its height,
  in `fg` over the same `bg` outline at stroke 20. It stays a check for as long as the total
  stays at or above the goal, however far over. With the text set to None nothing is written;
  the full cup alone shows the goal is met.
- **UI-023** **Widget settings.** Four settings, stored with the other settings (APP-003) and kept
  across restarts: "Text in cup" — None, Percent or Amount, None by default (UI-021); "Ideal
  line" — on or off, on by default (UI-020); "Background" — on or off, **off** by default
  (UI-015); and "Cup fill" — on or off, **on** by default (UI-015). They are edited in Settings'
  Widget section (UI-006).

**Invariant — the widget's water height, ideal line and check always come from the same `:core`
values as home's rings at the same moment, never a second calculation.** The water height is
`Rings`' inner fraction, the ideal line its outer fraction, and goal met is
`GoalMath.remainingMl(…) == 0`, home's own test for "Goal met".

**Invariant — the check appears exactly when today's total reaches the effective goal; the
number never shows the goal or 100% before then.**

**Invariant — the widget never shows a previous day's total.** Every redraw computes the cup from
the entries of the local date of the moment it is drawn (LOG-002), and the midnight refresh of
UI-018 redraws it when the date changes.

**Invariant — the widget's cup and water use the master icon's path data and BRAND-002 colours
(BRAND-017), never a hand-drawn approximation.**

**Invariant — with Background off, the widget draws nothing outside the cup's outline.** No card,
no corner radius and no other backdrop; the cup fill is BRAND-013's own path closed across its
top, never a box around it.

## Build checklist

- [x] `WaterNavHost` with routes first-run, home, history, settings, reminders (UI-001).
- [x] Each screen's root is `ListLayout` or `FormLayout` (UI-002–008).
- [x] `ManifestDeclarationTest` covers the theme declaration (UI-012).
- [x] Home's ring card shows the ideal ring and its text from `Pace`, re-read on a clock tick
      (UI-013).
- [x] Home's ring card takes its rings from `Rings` in `:core` (UI-013).
- [x] `WidgetCup` in `:core` gives the widget's water height, ideal line, goal met and text from
      `Rings` and `GoalMath`, unit tested for every text mode, the rounding, the hidden ideal line
      and midnight (UI-015, UI-020–022, the widget invariants).
- [x] `CupGeometry` in `:core` holds the cup, the water at any level, the ideal line, the text box
      and the fitting, unit tested against the master icon (UI-015, UI-016, UI-020–022,
      BRAND-056).
- [x] `WaterWidget` (Jetpack Glance) draws the cup with `CupBitmap`, opens home on a tap, and is
      refreshed by `WaterWidgetUpdater` at every point UI-018 lists (UI-014–022).
- [x] `WidgetDeclarationTest` checks the provider metadata and receivers (UI-014, UI-018, UI-019)
      and that the widget draws from `CupGeometry` and `WidgetCup` (BRAND-056, the invariants).
- [x] Settings' Widget section, stored by `SettingsStore` (UI-006, UI-023; `SettingsStoreTest`).
- [x] The cup at a `spacing.xs` margin; the background and cup fill as settings, off and on by
      default, with the cup fill from `CupGeometry.fillPath` (UI-015, UI-023, the background
      invariant).
- [ ] Seen on a device: placed, resized, refreshed and tapped, with each text setting, the ideal
      line, the background and the cup fill (UI-014–023).
