# Brand (`BRAND`)

The launcher icon and the notification icon: what they draw, in which colours, how the icon
survives a launcher's mask and Android's themed-icon mode, and which files carry them. Modelled on
Interval-trainer-android's `docs/spec/brand.md`.

The icon is **a water drop with a water-level wave inside it**, drawn in the same family style
as the owner's other apps (chores-web-android, Interval Trainer, Simple Stretch, Roadtrip): a
white background, a single bold glyph in brand blue with one grey accent, round line caps, inside
the adaptive-icon safe zone.

---

## Invariants

> **Invariant — the launcher icon's colours are the fixed brand palette in `BRAND-002`, not the
> design-system tokens.** The palette is shared with the sibling apps so the icons read as a
> family. The in-app theme (`design-system.md` DS-050) and the icon are independent. Every colour
> in the icon resources equals its `BRAND-002` value; drift fails `./gradlew test` (`BRAND-003`).

> **Invariant — the master SVG is the source of truth.** `docs/brand/icon.svg` (`BRAND-017`) is
> the drawing; the foreground, the monochrome layer and both VectorDrawables are derived from it.

---

## 1. Canvas and colours

- **BRAND-001** Coordinates are in the adaptive-icon canvas: **108 × 108 units, 1 unit = 1dp**.
- **BRAND-002** The icon uses exactly these colours:

  | Role | Hex |
  |---|---|
  | `background` — the background | `#FFFFFF` |
  | `blue` — the drop outline | `#1361A7` |
  | `grey` — the water-level wave | `#818589` |

  *(auto: `LauncherIconColourTest.kt`, for the Android resources.)*
- **BRAND-003** Every colour in `ic_launcher_background.xml` and `ic_launcher_foreground.xml`
  equals its `BRAND-002` value. The expected values are constants in the test, never read from
  `DesignSystemColors`. *(auto: `LauncherIconColourTest.kt`.)*

## 2. The drawing

The foreground is exactly these two elements, in this order:

- **BRAND-010** **Drop:** a closed outline — from the tip (54, 27) straight to the tangent point
  (68.86, 53.74), then the large clockwise arc of the circle centred (54, 62), radius 17, round to
  the tangent point (39.14, 53.74), then straight back to the tip. Stroke `blue`, width 6, round
  joins, no fill.
- **BRAND-011** **Wave:** a line of four half-waves from (43, 63) to (65, 63), each 5.5 units
  wide with quadratic control points 4 units above/below the line. Stroke `grey`, width 4, round
  caps, no fill.
- **BRAND-017** `docs/brand/icon.svg` is the master:

  ```svg
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
    <rect width="108" height="108" fill="#FFFFFF"/>
    <path d="M54 27 L68.86 53.74 A17 17 0 1 1 39.14 53.74 Z" fill="none" stroke="#1361A7" stroke-width="6" stroke-linejoin="round"/>
    <path d="M43 63 q2.75 -4 5.5 0 t5.5 0 t5.5 0 t5.5 0" fill="none" stroke="#818589" stroke-width="4" stroke-linecap="round"/>
  </svg>
  ```

## 3. Safe zone

- **BRAND-020** Nothing in the foreground extends more than 33 units from the canvas centre
  (54, 54). As drawn, the tip with its round join reaches 30 units above the centre, the bottom
  of the drop (outer edge) 28 units below it, and the sides about 21.5 units from it.

## 4. Monochrome (themed icon)

- **BRAND-030** The monochrome layer is the same two elements in the same geometry, painted
  `#FFFFFF` on transparent. Android 13+ reads only its alpha.

## 5. Notification icon

- **BRAND-055** The notification small icon (`ic_notification.xml`, 24 × 24) is a filled white
  water drop — tip (12, 2.5), circle centred (12, 15) radius 6.5 — a flat single-alpha silhouette,
  since the system colours it.

## 6. Files

- **BRAND-050** SVG sources in `docs/brand/`: `icon.svg` (master), `icon-foreground.svg`
  (elements only, transparent), `icon-monochrome.svg` (elements in white, transparent).
- **BRAND-051** Android resources — an adaptive icon alone (`minSdk` 26, so no PNG mipmaps):
  `drawable/ic_launcher_foreground.xml`, `drawable/ic_launcher_monochrome.xml`,
  `values/ic_launcher_background.xml` (`#FFFFFF`), and `mipmap-anydpi-v26/ic_launcher.xml` and
  `ic_launcher_round.xml` (identical: background, foreground and monochrome layers).
- **BRAND-052** `<application>` declares `android:icon="@mipmap/ic_launcher"` and
  `android:roundIcon="@mipmap/ic_launcher_round"`. *(auto: `ManifestDeclarationTest.kt`.)*

## 7. On a device

- **BRAND-060** On a real launcher the drop shows whole inside a circular mask, and with themed
  icons on it shows the monochrome form. *(manual: on a device, by the repository owner.)*

## Out of scope

A logo lockup, Play Store graphics, and a splash screen.
