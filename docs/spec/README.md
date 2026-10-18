# Specification

Behaviour of the app, one page per area. Every requirement has an ID; code comments and unit
tests reference these IDs. Change the spec before the code.

| Page | IDs | Covers |
| --- | --- | --- |
| [product.md](product.md) | APP-001 … APP-006 | Name, identifiers, platform, toolchain, versioning |
| [goal.md](goal.md) | GOAL-001 … GOAL-012 | Profile (weight, activity), recommended goal, custom goal, units, first run |
| [logging.md](logging.md) | LOG-001 … LOG-015 | Logging drinks, quick-add sizes, today's total, delete and undo, day boundary, ideal amount |
| [reminders.md](reminders.md) | REM-001 … REM-016 | Reminder settings, alarm scheduling, notification, permissions |
| [history.md](history.md) | HIST-001 … HIST-006 | Week view of daily totals against the goal |
| [screens.md](screens.md) | UI-001 … UI-023 | Screens, navigation, which layout each uses, and the home screen widget |
| [design-system.md](design-system.md) | DS-001 … DS-099 | The in-app design system, ported from Interval Trainer |
| [brand.md](brand.md) | BRAND-001 … BRAND-060 | Launcher icon, monochrome layer, notification icon, widget cup |

A bold **Invariant — …** sentence marks a rule a technically-correct implementation could still
break; treat it as a hard constraint on *how* the behaviour is built, not only what it outputs.

Pure logic (goal maths, unit conversion, day windows, reminder slots, history summaries, the ideal amount, the rings) lives in
the `:core` module, which is plain Kotlin/JVM, so its tests need no Android runtime, emulator or
Robolectric.
