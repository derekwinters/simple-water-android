# Reminders

## Settings

- **REM-001** Reminder settings are: **on/off**, the **active days of the week**, a **start time**
  and an **end time** (each a time of day in 30-minute steps), and an **interval** of 30, 60, 90
  or 120 minutes.
- **REM-002** Defaults: **on**, every day, **08:00 to 20:00, every 60 minutes**.
- **REM-003** The end time is never before the start time: moving the start past the end moves
  the end with it, and moving the end before the start moves the start with it. Start and end may
  be equal (one reminder a day).
- **REM-004** A day's reminder times are the start time, then every interval after it, up to and
  **including** the end time when it falls on the grid. Examples: 08:00–20:00 every 90 min →
  08:00, 09:30, …, 18:30, 20:00 (eight intervals land exactly on the end); 08:00–19:00 every
  90 min → the last is 18:30.
- **REM-005** Reminders fire only on active days. With no active day selected, with reminders off,
  or before first run has been completed or skipped (GOAL-010), no alarm is set.

## Scheduling

- **REM-006** Exactly one alarm is armed at a time: the first reminder time strictly after now, on
  an active day, at most 8 days ahead. After an alarm is handled the next one is armed.
- **REM-007** Alarms are exact (`setExactAndAllowWhileIdle`) when the app may schedule exact
  alarms, and inexact (`setAndAllowWhileIdle`) otherwise — including when an exact request is
  refused at run time (same approach as simple-stretch-android).
- **REM-008** Alarms are re-armed when the process starts, when the app is resumed, after boot,
  after a time or time-zone change, after the app is updated, after the exact-alarm permission
  changes, and whenever reminder settings change.
- **REM-009** **Invariant — a just-due alarm is not skipped by a reschedule.** If a reschedule
  happens after an alarm's time but before it was handled (within 30 minutes), that same
  occurrence is re-armed (it fires at once), rather than jumping to the next slot.

## Delivery

- **REM-010** When an alarm is delivered, a notification is shown unless any of these holds:
  this occurrence was already handled (**Invariant — one occurrence produces at most one
  notification**); reminders are now off or the day is no longer active; it was delivered more
  than 30 minutes late; or **today's total is at or above the goal**.
- **REM-011** **Invariant — a met goal suppresses the notification, never the schedule.** The next
  alarm is armed exactly as if the notification had been shown, so deleting an entry (dropping
  below the goal again) or a new day starting needs no reschedule for reminders to resume.
- **REM-012** The notification is titled "Time for some water", reads e.g. "1,250 of 2,450 ml
  today · 1,200 ml to go", and has one action, **"Log <default size>"** (e.g. "Log 250 ml"), which
  logs the default size (LOG-004) without opening the app and dismisses the notification. Tapping
  the notification opens the app on the home screen and dismisses it. Each reminder replaces the
  previous one (one notification id), so they never pile up. Reminders after the start-time slot
  lead with a pace sentence instead (REM-019).
- **REM-013** The notification is dismissed when a log (from the app or the action) brings
  today's total to the goal or above (LOG-011).

## Pace

- **REM-017** The **expected amount by now** ramps linearly across the reminder window (REM-001):
  `goal × (now − start) / (end − start)`, clamped to 0…goal, where now, start and end are times of
  day. Before the start it is 0, after the end it is the goal. With start equal to end there is no
  ramp and no pace at all (REM-019, UI-013 show nothing). **Invariant — the expected amount never
  exceeds the goal and never goes negative**, whatever the time of day.
- **REM-018** The **difference** is today's total minus the (unrounded) expected amount. The
  expected amount and the difference are each rounded in the volume unit (GOAL-008): to the
  nearest 10 ml, or to the nearest whole fl oz, with an exact half rounding away from zero. A
  negative difference reads "<amount> behind", a positive one "<amount> ahead", and one that rounds
  to 0 reads "right on track".
- **REM-019** A reminder's pace is taken at the occurrence's scheduled time (not its delivery
  time, so an inexact alarm delivered late still treats the start slot as the start). When the
  expected amount rounds to 0 (the start-time slot) or start equals end, the notification keeps
  the plain text of REM-012. Every later reminder that is shown (REM-010 still applies) reads, e.g.,
  "Ideally you'd have had 1,000 ml by now to stay on track. You're 250 ml behind." (or "You're
  200 ml ahead.", or "You're right on track."), and its expanded text adds the REM-012 totals line
  ("1,250 of 2,450 ml today · 1,200 ml to go") on the next line. Title and the Log action are
  unchanged.

## Permissions

- **REM-014** On Android 13+ the app asks for `POST_NOTIFICATIONS` when the home screen first
  shows after first run. Settings shows whether notifications are allowed; tapping the row opens
  the app's notification settings.
- **REM-015** On Android 12/12L (where exact alarms need the user's grant) settings shows an
  "Alarms & reminders" row while exact alarms are not allowed, opening the system screen to allow
  them. On Android 13+ `USE_EXACT_ALARM` is declared and granted at install.
- **REM-016** The manifest declares `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`,
  `SCHEDULE_EXACT_ALARM` (max SDK 32) and `USE_EXACT_ALARM`; the alarm and notification-action
  receivers are not exported; the reschedule receiver is exported for the system broadcasts of
  REM-008.

## Build checklist

- [x] `ReminderSchedule.slotsFor`, `next`, `normalized` in `:core`, unit tested (REM-003–006).
- [x] `ReminderPolicy.shouldNotify` in `:core`, unit tested (REM-010, REM-011).
- [x] One `PendingIntent` request code for the reminder alarm; the trigger time is remembered so
      a reschedule can keep a just-due occurrence (REM-009) and delivery can de-duplicate (REM-010).
- [x] All `PendingIntent`s are `FLAG_IMMUTABLE`.
- [x] `ManifestDeclarationTest` (JVM, reads the manifest) covers REM-016.
- [x] `Pace.at` and `PaceText` in `:core`, unit tested: clamping, start = end, rounding in both
      units, the "on track" case (REM-017–019, UI-013).
- [x] The alarm receiver passes the occurrence's scheduled time to the notification (REM-019).
- [ ] Robolectric test for notification content (not yet).
