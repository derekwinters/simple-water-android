# Brand (`BRAND`)

The launcher icon, the notification icon and the home screen widget's cup: what they draw, in
which colours, how the icon survives a launcher's mask and Android's themed-icon mode, and which
files carry them. Modelled on Interval-trainer-android's `docs/spec/brand.md`.

The icon is **an open cup, three-quarters full of water**, drawn in the same family style as the
owner's other apps (chores-web-android, Interval Trainer, Simple Stretch, Roadtrip): a white
background, a single bold glyph in brand blue and grey, round line caps, inside the adaptive-icon
safe zone.

---

## Invariants

> **Invariant — the launcher icon's colours are the fixed brand palette in `BRAND-002`, not the
> design-system tokens.** The palette is shared with the sibling apps so the icons read as a
> family. The in-app theme (`design-system.md` DS-050, DS-053) borrows the blue and the grey as
> its `accent` and `ideal` tokens, but neither side reads the other's values. Every colour
> in the icon resources equals its `BRAND-002` value; drift fails `./gradlew test` (`BRAND-003`).

> **Invariant — the master SVG is the source of truth.** `docs/brand/icon.svg` (`BRAND-017`) is
> the drawing; the foreground, the monochrome layer and both VectorDrawables are derived from it.

> **Invariant — coordinates are absolute and baked.** Every path in the SVG sources and the
> VectorDrawables uses absolute commands (`M`, `L`, `Q`, `Z`) in final canvas coordinates: no
> relative commands, no smooth-curve shorthand (`t`) and no transform groups, so each file reads
> the same numbers as this page and no renderer's handling of shorthand or transforms can move a
> point.

> **Invariant — the water never touches the cup in the monochrome layer.** With one colour for
> everything, the gap between the water and the cup's walls and bottom is the only thing that
> separates them, so it stays visible (`BRAND-031`).

---

## 1. Canvas and colours

- **BRAND-001** Coordinates are in the adaptive-icon canvas: **108 × 108 units, 1 unit = 1dp**.
- **BRAND-002** The icon uses exactly these colours:

  | Role | Hex |
  |---|---|
  | `background` — the background | `#FFFFFF` |
  | `blue` — the water | `#1361A7` |
  | `grey` — the cup | `#818589` |

  *(auto: `LauncherIconColourTest.kt`, for the Android resources.)*
- **BRAND-003** Every colour in `ic_launcher_background.xml` and `ic_launcher_foreground.xml`
  equals its `BRAND-002` value. The expected values are constants in the test, never read from
  `DesignSystemColors`. *(auto: `LauncherIconColourTest.kt`.)*

## 2. The drawing

The design was drawn at a larger size and scaled by 0.84 about the canvas centre (54, 54); the
coordinates below are the scaled ones. The foreground is exactly these two elements, in this
order (the water first, so the cup draws over its edges):

- **BRAND-010** *Retired: the drop outline of the original icon, replaced by `BRAND-013`.*
- **BRAND-011** *Retired: the water-level wave of the original icon, replaced by `BRAND-012`.*
- **BRAND-012** **Water:** a closed shape — a surface line of four quadratic half-waves from
  (41.736, 45.6) to (66.264, 45.6), each 6.132 units wide with control points 2.52 units
  above/below the line (above first), then straight down to (63.576, 72.48), across to
  (44.424, 72.48) and back up to the start. Fill and stroke `blue`, stroke width 1.26, round joins.
- **BRAND-013** **Cup:** an open outline — from the left rim (37.2, 33.84) down to (41.064, 73.32),
  a quadratic corner (control (41.4, 76.68)) into the bottom at (44.76, 76.68), across to
  (63.24, 76.68), a quadratic corner (control (66.6, 76.68)) up to (66.936, 73.32), then up to
  the right rim (70.8, 33.84). Stroke `grey`, width 4.2, round caps and joins, no fill.
- **BRAND-017** `docs/brand/icon.svg` is the master:

  ```svg
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108">
    <rect width="108" height="108" fill="#FFFFFF"/>
    <path d="M41.736 45.6 Q44.802 43.08 47.868 45.6 Q50.934 48.12 54 45.6 Q57.066 43.08 60.132 45.6 Q63.198 48.12 66.264 45.6 L63.576 72.48 L44.424 72.48 Z" fill="#1361A7" stroke="#1361A7" stroke-width="1.26" stroke-linejoin="round"/>
    <path d="M37.2 33.84 L41.064 73.32 Q41.4 76.68 44.76 76.68 L63.24 76.68 Q66.6 76.68 66.936 73.32 L70.8 33.84" fill="none" stroke="#818589" stroke-width="4.2" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>
  ```

## 3. Safe zone

- **BRAND-020** Nothing in the foreground extends more than 33 units from the canvas centre
  (54, 54). As drawn, the rims with their round caps are the farthest points, about 28.3 units
  from the centre (22.3 above it, 18.9 to each side); the cup's bottom (outer edge) is 24.8 units
  below it.

## 4. Monochrome (themed icon)

- **BRAND-030** The monochrome layer is the same two elements with the same path data, both
  painted solid `#FFFFFF` on transparent: the cup as its `BRAND-013` stroke, the water as a fill.
  Android 13+ reads only its alpha.
- **BRAND-031** In the monochrome layer the water is fill only, with no stroke, so the clear gap
  between it and the inner edge of the cup is at least 1.2 units everywhere (about 1.27 at the
  walls, 2.1 at the bottom). With the foreground's 1.26 stroke on the water the gap would halve,
  to about 0.64.

## 5. Notification icon

- **BRAND-055** The notification small icon (`ic_notification.xml`, 24 × 24) is the same cup and
  water as a flat single-alpha silhouette in white, since the system colours it. It is the
  unscaled design (before `BRAND-012`/`BRAND-013`'s 0.84) scaled by 0.4 about (54, 55.5), which
  lands on (12, 12):
  - water, fill only: `M6.16 7.4 Q7.62 6.2 9.08 7.4 Q10.54 8.6 12 7.4 Q13.46 6.2 14.92 7.4
    Q16.38 8.6 17.84 7.4 L16.56 20.2 L7.44 20.2 Z`;
  - cup, stroke width 2, round caps and joins: `M4 1.8 L5.84 20.6 Q6 22.2 7.6 22.2 L16.4 22.2
    Q18 22.2 18.16 20.6 L20 1.8`.

  As in `BRAND-031`, the water has no stroke, leaving a gap of about 0.6 units to the cup.

## 5a. Home screen widget

- **BRAND-056** The home screen widget (UI-016) draws the cup itself, at a variable water level,
  from the master icon's path data and BRAND-002 colours, in icon units (BRAND-001) scaled to the
  widget:
  - the cup is BRAND-013's outline exactly, in `grey`, drawn over the water;
  - the water at a fraction `f` (0–1) of full is BRAND-012's shape with its top line moved to
    `y = 72.48 − 35.84 f`, so that `f` = 0.75 is BRAND-012 exactly and `f` = 1 puts the top at
    36.64. Its sides continue BRAND-012's sides (out 0.1 unit per unit up, from (44.424, 72.48)
    and (63.576, 72.48)), which keeps about BRAND-012's gap to the cup at every level. The top
    is four quadratic half-waves, each a quarter of the water's width at that height, with
    control points 2.52 units above and below the line (above first). Fill and stroke `blue`,
    stroke 1.26, round joins, clipped at y = 73.11 (72.48 plus half the stroke) so a low wave never dips below the
    bottom. At `f` = 0 no water is drawn;
  - the outline's bounds, including its stroke, are x 35.1–72.9 and y 31.74–78.78; this is the box
    UI-015 scales to the card.

  The geometry is `CupGeometry` in `:core`; `CupBitmap` draws it. The widget picker's preview
  (UI-019) is `drawable/ic_launcher_foreground.xml`, the cup at three-quarters. No drawable apart
  from the foreground and the monochrome layer (BRAND-030) carries the cup's path data.
  *(auto: `CupGeometryTest.kt` against `docs/brand/icon.svg`; `WidgetDeclarationTest.kt`.)*

## 6. Files

- **BRAND-050** SVG sources in `docs/brand/`: `icon.svg` (master), `icon-foreground.svg`
  (elements only, transparent), `icon-monochrome.svg` (elements in white, transparent, as
  `BRAND-030`/`BRAND-031`).
- **BRAND-051** Android resources — an adaptive icon alone (`minSdk` 26, so no PNG mipmaps):
  `drawable/ic_launcher_foreground.xml`, `drawable/ic_launcher_monochrome.xml`,
  `values/ic_launcher_background.xml` (`#FFFFFF`), and `mipmap-anydpi-v26/ic_launcher.xml` and
  `ic_launcher_round.xml` (identical: background, foreground and monochrome layers).
- **BRAND-052** `<application>` declares `android:icon="@mipmap/ic_launcher"` and
  `android:roundIcon="@mipmap/ic_launcher_round"`. *(auto: `ManifestDeclarationTest.kt`.)*

## 7. On a device

- **BRAND-060** On a real launcher the cup shows whole inside a circular mask, and with themed
  icons on it shows the monochrome form. *(manual: on a device, by the repository owner.)*

## Build checklist

- [x] Master and derived SVGs in `docs/brand/` with absolute, baked coordinates (BRAND-017,
      BRAND-050).
- [x] Foreground and monochrome VectorDrawables; colours, element order and the monochrome
      water's missing stroke checked by `LauncherIconColourTest` (BRAND-003, BRAND-030, BRAND-031).
- [x] Notification icon follows the cup (BRAND-055).
- [x] The widget draws the cup from `CupGeometry` at any water level, checked against the master
      SVG; the picker preview is the launcher foreground (BRAND-056).
- [ ] Seen on a device, whole in a circular mask and as a themed icon (BRAND-060).

## Out of scope

A logo lockup, Play Store graphics, and a splash screen.
