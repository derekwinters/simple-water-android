# Logging drinks

- **LOG-001** A drink entry is an amount in decimal millilitres (GOAL-008) and the instant it was
  logged. An entry's amount is 1–5,000 ml inclusive.
- **LOG-002** **Day boundary — local midnight.** "Today" is the half-open interval from local
  midnight today to local midnight tomorrow in the device's current time zone. An entry belongs to
  the local date of its instant. On a daylight-saving change the day is 23 or 25 hours long.
  **Invariant — a day is computed from the calendar date in the zone, never as "now minus 24
  hours" or "start + 24 hours".**
- **LOG-003** Today's total is the sum of today's entries, rounded only for display (GOAL-008
  invariant); so is "<remaining> to go". The home screen shows it against the
  effective goal (GOAL-007) as a progress ring that fills clockwise from 12 o'clock and is full at
  or above the goal, with the total, "of <goal>", and either "<remaining> to go" or "Goal met".
- **LOG-004** **Quick-add sizes.** There are 1–4 drink sizes, each 10–2,000 ml, stored as decimal
  ml. Defaults follow the volume unit while the sizes have not been customised (the stored sizes
  are still 250 and 500 ml): **250 ml and 500 ml** in ml, **8 fl oz and 16 fl oz** in fl oz. The
  first size is the **default size**, used by the home screen's primary button and the reminder
  notification's action (REM-012).
- **LOG-005** Tapping a quick-add button logs one entry of that size, timestamped now.
  **Invariant — a quick-add logs exactly the amount its label shows.** A size shown rounded logs
  the shown whole amount: a size saved as 250 ml and shown as "8 fl oz" logs 8 fl oz
  (236.588 ml); a size saved as 236.588 ml and shown as "237 ml" logs 237 ml. The same holds for
  the notification's "Log <default size>" action.
- **LOG-006** "Custom…" opens a dialog with a number field in the current volume unit
  (GOAL-008). "Add" is enabled only while the field holds an amount that converts to 1–5,000 ml.
- **LOG-007** The home screen lists today's entries, newest first, each with its time (device
  12/24-hour setting) and amount, and a delete button.
- **LOG-008** Deleting an entry removes it at once (no confirmation, DS-004 — a single entry is
  cheap to re-add) and shows an undo bar for 5 seconds. "Undo" restores the same entry (same
  amount and same instant). A second delete replaces the bar.
- **LOG-009** Drink sizes are edited in settings, in the current volume unit, starting from the
  sizes as LOG-004 shows them. Blank fields are dropped; at least one valid size must remain,
  otherwise "Save" is disabled. Saved sizes are converted exactly to decimal ml (GOAL-008).
- **LOG-010** Entries are kept indefinitely (history reads them, HIST-001).
- **LOG-011** When a log brings today's total to the goal or above, any reminder notification
  still showing is dismissed (REM-013).
- **LOG-012** Upgrading from a version that stored whole-ml entries (database version 1) to
  decimal-ml storage (version 2) keeps every entry, with the same id, amount and instant.

## Ideal amount (pace)

The home screen shows how much the user would ideally have drunk by now next to what they have
drunk (UI-013). The ideal amount follows today's reminder times; reminder notifications are not
affected by it (REM-012 is unchanged).

- **LOG-013** **The ideal amount steps at reminder times.** For today's reminder times
  `t0 … tn` (REM-004), the ideal amount at a time of day `now` is `goal × k / n`, where `tk` is
  the last reminder time at or before `now`. It is 0 before `t0`, holds steady between reminder
  times, and equals the goal from `tn` on. Example: 08:00–20:00 every 60 minutes (n = 12) and a
  2,450 ml goal → 0 ml at 07:59, 204.17 ml from 09:00, 816.67 ml from 12:00, 2,450 ml from 20:00.
  **Invariant — the ideal amount changes only at a reminder time, and is never below 0 or above
  the goal.** It is computed from today's reminder settings, the effective goal and the clock,
  never stored and never interpolated between reminder times.
- **LOG-014** **When there is no ideal amount.** There is none — the ideal ring and its
  behind/ahead text are hidden and the ring card is as UI-004 describes — when reminders are
  off, when today is not an active reminder day, or when today has fewer than two reminder times
  (start equal to end, REM-003, or an interval longer than the span from start to end), since
  `goal × k / n` needs at least one interval.
- **LOG-015** **Behind, ahead, on track.** The difference is today's total minus the ideal
  amount, both unrounded (GOAL-008 invariant). Its size is shown rounded to the nearest **10 ml**
  (an exact half rounds up), or to the nearest whole **fl oz**: "220 ml behind" when the total
  is below the ideal amount, "370 ml ahead" when above, and **"On track"** when the rounded size
  is 0. The ideal amount is shown with the same rounding ("Ideal by 12:00: 820 ml"). "<remaining>
  to go" keeps LOG-003's rounding, so it agrees with the total and goal in the ring.

## Build checklist

- [x] `DayWindow` (start/end millis of a local date, date of an instant) in `:core`, unit tested
      including a DST day (LOG-002).
- [x] `DrinkSizes.parse`/validation in `:core`, unit tested (LOG-004, LOG-009).
- [x] `drink_entries` table with an index on `loggedAt`; today's entries are a range query
      `[dayStart, dayEnd)` (LOG-002).
- [x] Undo re-inserts the deleted row with its original id and instant (LOG-008).
- [x] `DrinkSizes.quickAdd` gives the unit's defaults and snaps each size to its shown amount, unit
      tested (LOG-004, LOG-005).
- [x] `Pace` in `:core` computes the ideal amount, its tick positions and the behind/ahead text,
      unit tested for the steps at reminder times, before the first and from the last reminder
      time, hidden days, both units, rounding and "On track" (LOG-013–015).
- [x] `drink_entries.amountMl` is `REAL`; the 1 → 2 migration is tested on a v1 database with
      entries, and schemas v1 and v2 are committed under `app/schemas/` (LOG-012).
