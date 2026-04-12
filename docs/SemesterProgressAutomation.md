# Semester Progress Automation

This document defines how student semester progress should be tracked and updated automatically.

## Goal

Avoid manual updates of semester status by deriving progress from enrollment and completion records.

## Status Rules

### NOT_STARTED

Use this when:

- The student has no enrollment activity for the semester.
- The student has not selected or enrolled in any subject that belongs to the semester.

### IN_PROGRESS

Use this when:

- The student has at least one enrolled subject in the semester.
- The student has started an enrollment record for that semester, even if no grades are posted yet.

### COMPLETED

Use this when:

- All required subjects for the semester are marked completed for the student.
- There are no remaining required subjects in enrolled or dropped state that still block completion.

## Enrollment Eligibility Gate (Catalog and Save)

The semester progress status remains strict (`COMPLETED` still means the full curriculum semester is done), but subject eligibility is more flexible:

- A student may open the immediate next semester in the same year level once the subjects they actually carried in the current semester are all completed.
- Unfinished subjects from prior semesters remain eligible as backtrack load.
- Subject-level prerequisites are still enforced. A next-semester subject remains blocked until all of its prerequisite subjects are completed.

## Recommended Automation Points

### On Enrollment Save

When a student saves an enrollment for any subject in a semester, ensure the semester progress record is created or updated to `IN_PROGRESS`.

### On Grade or Completion Update

When a subject completion is finalized, recompute whether all required semester subjects are complete. If they are, update the semester progress record to `COMPLETED`.
Also refresh eligibility so the next semester can open when the student's active current-semester load is fully completed.

### On Faculty Completion Action

When faculty marks a student as `COMPLETED` for the subject they handle, persist the status in `student_enrolled_subjects`, then run semester progress sync for that student.

### On Academic Year Completion

After semester sync, evaluate all semesters for the student's current year level.

- If every semester in the current year level is `COMPLETED`, promote the student to the next year level.
- Promotion must not happen when only one semester is completed.
- Promotion must stop at the final curriculum year (no semester exists for the next year level).

### On Subject Drop or Failure

If a required subject is dropped or not passed, keep the semester at `IN_PROGRESS` until the subject is retaken and completed.

### On Reconciliation Job

Run a periodic reconciliation job to recalculate all semester progress records from the source tables. This keeps progress consistent if one update event was missed.

## Source of Truth

Treat subject-level enrollment and completion data as the source of truth. The semester progress table should be a derived summary that can be rebuilt at any time.

## Suggested Implementation Order

1. Update semester progress when enrollment records are saved.
2. Recalculate progress after grades or completion status changes.
3. Add a nightly or on-demand reconciliation pass.
4. Display the computed status in the registrar dashboard.
