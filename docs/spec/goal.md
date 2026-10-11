# Goal, profile and units

The daily goal is a volume of water. By default it is recommended from the user's weight and
activity level; the user may override it with a custom goal and go back to the recommendation at
any time.

## Profile

- **GOAL-001** The profile is a **weight** and an **activity level**. Weight is entered as a whole
  number in kilograms or pounds (the weight unit, `kg` or `lb`, is chosen next to the field and
  remembered). It is stored as kilograms (1 lb = 0.45359237 kg), so switching the weight unit never
  changes the recommendation. A valid weight is 20–300 kg (44–661 lb, inclusive); anything else
  is rejected and the previous value kept.
- **GOAL-002** The activity levels are, in order: **Sedentary**, **Lightly active**, **Active**,
  **Very active**.
- **GOAL-003** The default profile (used when first run is skipped, GOAL-010) is **70 kg,
  Sedentary**, weight unit `kg`.

## Recommended goal

- **GOAL-004** The recommended goal is `weight_kg × 35 ml` plus an activity allowance of
  **0 / 350 / 700 / 1050 ml** for Sedentary / Lightly active / Active / Very active, **rounded to
  the nearest 50 ml** (an exact half rounds up). Examples: 70 kg Sedentary → 2,450 ml;
  70 kg Very active → 3,500 ml; 65 kg Sedentary (2,275) → 2,300 ml; 154 lb Sedentary → 2,450 ml.
- **GOAL-005** **Invariant — the recommendation is computed, never stored.** It is always derived
  from the current profile, so editing the weight or activity level updates it immediately, and
  there is no second copy that can disagree with the profile.

## Custom goal

- **GOAL-006** The user may set a **custom goal**, a whole number of ml (or fl oz, GOAL-008)
  between 500 and 10,000 ml inclusive. While a custom goal is set, it is the effective goal and
  profile changes do not alter it.
- **GOAL-007** "Use recommended goal" clears the custom goal; the effective goal is then the
  recommendation again. The effective goal is `custom ?: recommended`.
  **Invariant — a custom goal is cleared only by the user.** Editing the profile never clears or
  rewrites it.

## Units

- **GOAL-008** The **volume unit** is `ml` (default) or `fl oz` (US fluid ounce, 29.5735 ml). It
  affects display and entry only: every volume is stored in whole millilitres. A volume shown in
  fl oz is rounded to the nearest whole fl oz; a value entered in fl oz is converted to the
  nearest whole ml.
- **GOAL-009** Volumes in ml are shown with a thousands separator and the unit, e.g. `2,450 ml`;
  in fl oz as e.g. `83 fl oz`.

## First run

- **GOAL-010** On first launch (no profile saved yet) the app shows the first-run screen: a short
  explanation, a weight field with the kg/lb choice, the activity level, and the recommended goal
  they produce, updated live. "Start tracking" saves the profile; "Skip" saves the default profile
  (GOAL-003). Either way the first-run screen is not shown again and the home screen opens.
- **GOAL-011** "Start tracking" is disabled while the weight field does not hold a valid weight
  (GOAL-001).

## Goal history

- **GOAL-012** Each time the effective goal changes (first run, profile edit without a custom
  goal, custom goal set or cleared), the new value is recorded against **today's date**,
  replacing any earlier record for today. History (HIST-002) reads the goal in force on a past
  day from these records. **Invariant — changing the goal never rewrites a past day's goal.**

## Build checklist

- [x] `GoalMath.recommendedGoalMl`, `effectiveGoalMl`, weight validation in `:core`, unit tested
      (GOAL-001, GOAL-004, GOAL-006, GOAL-007).
- [x] `Units` conversion and formatting in `:core`, unit tested (GOAL-008, GOAL-009).
- [x] `goal_changes` table keyed by epoch day, upserted on every effective-goal change (GOAL-012).
- [x] First-run flag stored in settings; the nav host starts at first-run until it is set (GOAL-010).
