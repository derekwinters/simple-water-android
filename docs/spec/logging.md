# Logging drinks

- **LOG-001** A drink entry is an amount in whole millilitres and the instant it was logged. An
  entry's amount is 1–5,000 ml inclusive.
- **LOG-002** **Day boundary — local midnight.** "Today" is the half-open interval from local
  midnight today to local midnight tomorrow in the device's current time zone. An entry belongs to
  the local date of its instant. On a daylight-saving change the day is 23 or 25 hours long.
  **Invariant — a day is computed from the calendar date in the zone, never as "now minus 24
  hours" or "start + 24 hours".**
- **LOG-003** Today's total is the sum of today's entries. The home screen shows it against the
  effective goal (GOAL-007) as a progress ring that fills clockwise from 12 o'clock and is full at
  or above the goal, with the total, "of <goal>", and either "<remaining> to go" or "Goal met".
- **LOG-004** **Quick-add sizes.** There are 1–4 drink sizes, each 10–2,000 ml. Defaults:
  **250 ml and 500 ml**. The first size is the **default size**, used by the home screen's primary
  button and the reminder notification's action (REM-012).
- **LOG-005** Tapping a quick-add button logs one entry of that size, timestamped now.
- **LOG-006** "Custom…" opens a dialog with a number field in the current volume unit
  (GOAL-008). "Add" is enabled only while the field holds an amount that converts to 1–5,000 ml.
- **LOG-007** The home screen lists today's entries, newest first, each with its time (device
  12/24-hour setting) and amount, and a delete button.
- **LOG-008** Deleting an entry removes it at once (no confirmation, DS-004 — a single entry is
  cheap to re-add) and shows an undo bar for 5 seconds. "Undo" restores the same entry (same
  amount and same instant). A second delete replaces the bar.
- **LOG-009** Drink sizes are edited in settings, in the current volume unit. Blank fields are
  dropped; at least one valid size must remain, otherwise "Save" is disabled.
- **LOG-010** Entries are kept indefinitely (history reads them, HIST-001).
- **LOG-011** When a log brings today's total to the goal or above, any reminder notification
  still showing is dismissed (REM-013).

## Build checklist

- [x] `DayWindow` (start/end millis of a local date, date of an instant) in `:core`, unit tested
      including a DST day (LOG-002).
- [x] `DrinkSizes.parse`/validation in `:core`, unit tested (LOG-004, LOG-009).
- [x] `drink_entries` table with an index on `loggedAt`; today's entries are a range query
      `[dayStart, dayEnd)` (LOG-002).
- [x] Undo re-inserts the deleted row with its original id and instant (LOG-008).
