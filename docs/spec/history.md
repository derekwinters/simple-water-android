# History

Deliberately modest: one week at a time.

- **HIST-001** The history screen shows seven consecutive days ending on a chosen day — by default
  the last seven days ending today. "Previous week" and "Next week" move the window by seven
  days; "Next week" is disabled when the window already ends today.
- **HIST-002** Each day shows its total (LOG-003, using that day's local-midnight window, LOG-002)
  against **the goal in force on that day**: the most recent goal record (GOAL-012) dated on or
  before it; for a day before any record, the earliest record; with no records at all, the
  current effective goal.
- **HIST-003** Each day is drawn as a horizontal bar filled in proportion to total ÷ goal (capped
  at full), with the weekday and date, the total, and a "met" mark when total ≥ goal.
- **HIST-004** Above the days: how many of the seven days met their goal ("4 of 7 days met") and
  the average daily total over the seven days.
- **HIST-005** Days after today are never shown (the window never extends past today).
- **HIST-006** **Invariant — a day's goal comes from the goal history, never from the current
  goal**, so changing the goal today does not turn last week's met days into misses.

## Build checklist

- [x] `History.goalOn`, `History.week`, `History.totalsByDay` in `:core`, unit tested
      (HIST-001, HIST-002, HIST-004, HIST-006).
- [x] One range query for the seven days' entries, grouped by local date in `:core` (LOG-002).
