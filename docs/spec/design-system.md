# Design system (`DS`)

Simple Water is styled like Interval Trainer. Its design system — colour tokens, spacing, the
monospaced numeric typography, buttons, icon buttons, the screen header, the three screen
layouts, the toggle, the stepper and the dialog — is **ported from Interval-trainer-android's
`:designsystem` module** (its `docs/spec/design-system.md`), keeping that page's requirement IDs
where the requirement carries over, so the two apps can be compared rule by rule. The port is a
package rename (`com.derekwinters.intervaltrainer.designsystem` →
`com.derekwinters.water.designsystem`) plus the deviations listed in §11; values are unchanged except the accent colour, which is the
brand blue (DS-050).

Components this app needs and Interval Trainer does not have are added here first, per the third
invariant (DS-017–019, DS-024–027, DS-034, DS-053, DS-054).

---

## Invariants

> **Invariant — a screen does not arrange itself; it fills a layout's named slots.** A screen's
> root is one of the three layouts in §8; it does not lay out its own header, spacing and action
> row.

> **Invariant — a raw Material component is reachable only from inside `:designsystem`.**
> `material3` is an `implementation` dependency of `:designsystem`, so an
> `androidx.compose.material3` import in `:app` is a compile error. `:app` draws text with
> `AppText` and builds from this module's components and Compose foundation.

> **Invariant — adding a component or a layout is a specification change, not a call-site
> decision.** A new shape is added to this page first, with its reason.

> **Invariant — the system bars are inset by the layout, never by a screen.** Only `ListLayout`,
> `FullBleedLayout` and `FormLayout` apply system-bar padding (DS-080).

> **Invariant — the system bars' icons are light whatever the system's light/dark setting,
> because the palette has no light variant.** `MainActivity` uses `SystemBarStyle.dark` with a
> transparent scrim for both bars (DS-023).

> **Invariant — the accent is the brand blue, and the primary button's text is chosen for
> contrast against it.** `accent` has the value of BRAND-002's `blue`, not a near match, and the
> primary button's ink is whichever token reaches 4.5:1 against the `accent` fill, checked by a test
> that computes the WCAG contrast ratio from the token values (DS-054). Changing either colour
> without that check passing is ruled out.

> **Invariant — a value control reports what the user did, never what it found.** A stepper,
> chip or field calls its change callback only in response to the user's input, never on first
> composition.

---

## 1. Buttons and actions (ported)

- **DS-001** `PrimaryButton`: filled, `accent` fill, light ink (`fg`) text, weight 700, one fixed
  padding. The single primary action per screen (home: log the default size; first run: Start
  tracking).
- **DS-002** `SecondaryButton`: dashed `line` border, `chip` fill, `dim` text. Optional or
  additive actions (other quick-add sizes, Custom…, Skip, week navigation).
- **DS-003** `DestructiveButton`: red fill, reserved for confirmed or saved-state deletion. (Not
  used by v1 screens; kept with the port.)
- **DS-004** Deleting a single drink entry is plain — no confirmation, no red — because undo
  restores it (LOG-008).
- **DS-005** `StatusIconButton`: drawn 38dp, 48dp minimum touch target.
- **DS-006** `TransportIconButton`: drawn 50dp, 48dp minimum touch target. (Kept with the port.)
- **DS-007** `FabIconButton`: 56dp. (Kept with the port.)
- **DS-008** `CountStepper`: a `−`/`+` mini-stepper with 26dp drawn buttons and 48dp touch
  targets. (Kept with the port; `ValueStepper`, DS-019, is its general form.)
- **DS-014** `Toggle`: bespoke track-and-knob on/off switch; `accent` track when on, `chip` when
  off, `fg` knob. Never a Material `Switch`.
- **DS-015** `StockListItem`: wraps Material 3 `ListItem` (headline, optional supporting text).
- **DS-016** `StockListItem` has an optional `trailingContent` slot (used for `Toggle` and chips).
- **DS-017** `ProgressRing` *(added)*: a circular track in `chip` with a clockwise arc from 12
  o'clock in `accent`, round caps, stroke `spacing.md`, filled to `fraction` clamped to 0–1, with a
  centred content slot. Used for today's progress (LOG-003). An empty fraction draws the track
  only.
- **DS-018** `ChoiceChips` *(added)*: a row (or a grid of `columns` per row) of equal-width chips
  for choosing one or several of a few short options. Selected: `accent` fill, `bg` ink, bold;
  unselected: `chip` fill, `fg` ink. 48dp minimum touch target; each chip reports its selected
  state to accessibility. Used for units, activity level, days of the week and the interval.
- **DS-019** `ValueStepper` *(added)*: `CountStepper`'s `−`/`+` buttons around a formatted label
  in `numeric.stat`, for stepping a value that is not a plain count (a time of day). The caller
  owns the value and the step; the stepper reports only taps, and each button can be disabled.

## 2. Dialogs (ported)

- **DS-010** `AlertDialog` is the only overlay component. No bottom sheet.
- **DS-011** Cancel left, affirmative right; a destructive affirmative is red text.
- **DS-012** The body is either a sentence or composable content (settings' editors, UI-007);
  the affirmative action can be disabled.

## 3. Screen scaffolding (ported)

- **DS-013** `ScreenHeader` has an optional leading back action.
- **DS-020** `ScreenHeader`: optional back, title top-left, exactly one optional trailing action
  (an icon or a text button).
- **DS-021** Every screen uses `ScreenHeader` (through its layout), never a `TopAppBar`.
- **DS-022** `<application>` declares a theme whose own body sets `android:windowActionBar` false
  and `android:windowNoTitle` true. *(auto: `ManifestDeclarationTest.kt`.)*
- **DS-023** `MainActivity` calls `enableEdgeToEdge` with `SystemBarStyle.dark(TRANSPARENT)` for
  both bars.
- **DS-024** `NumberField` *(added)*: a single-line whole-number field on `chip` with rounded
  corners, the value in `numeric.stat`, an optional unit suffix in `dim`, an `accent` cursor, the
  number keyboard, and a content description. It accepts digits only (at most 5); validation is
  the caller's.
- **DS-026** `ProgressRing` outer ring *(added)*: an optional second, thinner ring drawn just
  outside DS-017's ring, sharing its centre — a `line` track, stroke `spacing.xs`, a gap of
  `spacing.xs` from the inner ring, and a clockwise `ideal` arc from 12 o'clock with round caps,
  filled to its own fraction clamped to 0–1 (an empty fraction draws the track only). Optional
  ticks, each given as a fraction of the circle, are short radial `dim` marks, 2dp wide and
  `spacing.xs` long, just outside the outer ring. The inner ring keeps DS-017's size; the outer
  ring and ticks make the component larger rather than the inner ring smaller. Without an outer
  fraction the component is exactly DS-017. Used for the ideal amount (UI-013).
- **DS-027** *Retired: `RingBitmap` and its `RingGeometry`, which drew the rings for the home
  screen widget. The widget now draws the cup (docs/spec/screens.md UI-015, docs/spec/brand.md
  BRAND-056), and home draws its rings with `ProgressRing`.*
- **DS-025** `UndoBar` *(added)*: a full-width `chip` bar with a message and one `accent` text
  action, shown in a list layout's bottom slot (LOG-008).

## 4. Numeric typography (ported, renamed)

- **DS-030** `numeric.large` (Interval Trainer's `timer.large`): JetBrains Mono, tabular figures,
  64sp — today's total inside the ring. The font is bundled (`res/font/jetbrains_mono_regular.ttf`,
  SIL OFL, licence in `designsystem/OFL-jetbrains-mono.txt`).
- **DS-031** `numeric.stat` (`timer.stat`): 20sp — goals, stepper values, field values.
- **DS-032** `numeric.inline` (`timer.inline`): 16sp — amounts in list rows.
- **DS-033** These three are the only roles a volume, weight or time value renders in.
- **DS-034** `AppText` *(added)*: every other piece of text uses one of five roles — `Title`
  (20sp bold `fg`), `Body` (16sp `fg`), `BodyStrong` (16sp bold `fg`), `Meta` (13sp `dim`) and
  `Section` (13sp bold `dim`, letter-spaced) — the sizes Interval Trainer's screens use inline,
  named once here so `:app` never writes a font size. `NumericText` draws a value in one of the
  three numeric roles (DS-030–032).

## 5. Spacing (ported)

- **DS-040** 4dp base: `xs` 4, `sm` 8, `md` 12, `lg` 16, `xl` 20, `xxl` 24. No screen introduces
  a padding value outside this scale.

## 6. Colour tokens (ported)

- **DS-050** Tokens: `accent` #1361A7, the brand blue (BRAND-002 `blue`) — primary actions,
  progress, selected chips, toggles on, "Goal met". The other values are unchanged from Interval
  Trainer: `recovery` #F5C518, `neutral` #8A93A3, `bg` #121212, `fg` #F5F5F5, `dim` #9AA0A6,
  `line` #3C3C3C, `chip` #262626, destructive #E53935. `accent` is Interval Trainer's `work`
  (#2ECC71) renamed and recoloured (§11); the other names are kept from the port so the two apps'
  tokens stay comparable. *(auto: `DesignSystemColorsTest.kt`.)*
- **DS-053** `ideal` *(added)* #818589, the brand grey (BRAND `grey`): the outer ring's arc
  (DS-026). It is not mapped onto a Material 3 role. *(auto: `DesignSystemColorsTest.kt`.)*
- **DS-054** *(added)* The primary button's text (DS-001) has a WCAG 2 contrast ratio of at least
  4.5:1 against its `accent` fill, the threshold for normal-size text. With the DS-050 values,
  `fg` on `accent` is about 5.8:1. *(auto: `DesignSystemColorsTest.kt`.)*
- **DS-051** Mapped onto Material 3 `ColorScheme`: `surface`/`background`←`bg`,
  `onSurface`←`fg`, `primary`←`accent`, `outline`←`line`, `surfaceVariant`←`chip`,
  `error`←destructive. *(auto: `ColorSchemeMappingTest.kt`.)*
- **DS-052** No raw colour literal outside the token layer (review-time rule).

## 8. Screen layouts (ported)

- **DS-070** Exactly three layouts: **list**, **full-bleed**, **form**. Every screen's root is
  one of them.
- **DS-071** List: `ScreenHeader`, scrollable `LazyColumn` content, optional bottom slot.
- **DS-073** Full-bleed: free composition inside the insets. (Not used by v1 screens.)
- **DS-075** Form: optional `ScreenHeader`, centred content, a single `PrimaryButton`.
- **DS-080** Each layout insets its content by the system bars; the background still fills the
  window.

## 9. Enforcement

- **DS-090** The module boundary (material3 `implementation`-only in `:designsystem`) is the
  structural check.
- **DS-091** Every icon-only control has a content description; touch targets are at least 48dp.
  *(manual in this app: Interval Trainer's Robolectric gallery test is not ported, to keep
  `./gradlew test` free of Robolectric; `ComponentGallery` is kept as a `@Preview` for review.)*

## 11. Deviations from the Interval Trainer original

- Package renamed; `TimerTypography`/`timerTypography`/`timer.*` renamed to
  `NumericTypography`/`numericTypography`/`numeric.*`, since a water app shows volumes, not
  timers. Values unchanged.
- `DurationScrollPicker` (DS-009) is not ported: nothing here enters a duration.
- `CountStepper`'s private button is `internal` so `ValueStepper` (DS-019) reuses it.
- The Robolectric tests (DS-091 gallery, DS-080 insets, DS-009 picker) are not ported (DS-091).
- `work` is renamed `accent` and takes the brand blue (#1361A7) instead of Interval Trainer's
  green, so the app's accent matches its launcher icon; `PrimaryButton`'s text moves from dark
  `bg` ink to light `fg` to keep 4.5:1 contrast on the blue (DS-001, DS-050, DS-054).
- Added: `ProgressRing` (with its outer ring, DS-026), the `ideal` colour token (DS-053), `ChoiceChips`, `ValueStepper`, `NumberField`, `UndoBar`, `AppText`/`NumericText`.
- The ported files' KDoc is kept as written; where it cites Interval Trainer's own screens
  (`SCREEN-…`, `TIMER-…`, `CUE-…`), those IDs refer to that repository's specification.

## Traceability

| IDs | Tests |
|---|---|
| DS-022 | `app/src/test/.../ManifestDeclarationTest.kt` |
| DS-051 | `designsystem/src/test/.../ColorSchemeMappingTest.kt` |
| DS-050, DS-053, DS-054 | `designsystem/src/test/.../DesignSystemColorsTest.kt` |
| everything else | *(manual: design facts, checked in review and on a device)* |
