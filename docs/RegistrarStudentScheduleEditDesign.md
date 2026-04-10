# Registrar Student Schedule Edit Design

## Goal

Allow registrars to review and edit a student's schedule from the registrar side without breaking the canonical class schedule defined by offerings and schedules.

## Design Principle

A student's schedule should be treated as an enrollment outcome, not as the canonical class timetable.

- Canonical class timing lives in `schedules`.
- Student registration choices live in `enrollments` and `enrollment_details`.
- Registrar edits should update the student's enrollment selections, section assignments, or status, not the shared class timetable unless the user is explicitly editing class operations.

## What The Registrar Can Edit

The registrar should be able to:

- Add a subject to a student’s current enrollment.
- Remove a subject from a student’s current enrollment.
- Move a student to a different offering/section for the same subject.
- Change enrollment status for a selected subject, when allowed by policy.
- Recalculate units and detect conflicts after every edit.

The registrar should not directly edit the shared class schedule rows unless a separate schedule administration screen is introduced for faculty/registrar operations.

## Recommended UI

### Screen: Student Schedule Editor

Entry point from the registrar student record view.

Layout:

- Student header panel: student name, ID, course, year level, current enrollment period.
- Current schedule table: subject code, subject name, section, instructor, room, day, start time, end time, status, units.
- Action bar:
  - `Add Subject`
  - `Change Section`
  - `Drop Subject`
  - `Save Changes`
  - `Cancel`
- Conflict summary panel:
  - overlapping schedules
  - max units exceeded
  - missing prerequisite warning
  - section capacity warning

### Interaction Flow

1. Registrar opens a student profile.
2. Registrar selects `Edit Schedule`.
3. The editor loads the student's active enrollment and current enrollment details.
4. Registrar applies one or more edits in a draft state.
5. The system validates all changes before save.
6. On save, the enrollment detail rows are updated in a transaction.
7. The student dashboard reflects the updated schedule immediately after refresh.

## Data Model Impact

### Preferred Approach: No New Table

Use existing tables if schedule edits are just enrollment changes:

- `enrollments` for the overall student term record
- `enrollment_details` for individual subject selections
- `offerings` for the available section/subject/term combinations
- `schedules` for the canonical class meeting times

### Optional Extension: Audit Table

If you need traceability for registrar edits, add a lightweight audit table such as:

- `student_schedule_audit`
  - `id`
  - `enrollment_detail_id`
  - `action_type`
  - `before_offering_id`
  - `after_offering_id`
  - `performed_by_user_id`
  - `performed_at`
  - `reason`

This is optional, but recommended if schedule edits must be reviewable later.

## Service Layer Design

Create a registrar-focused service instead of putting logic in the form.

### Suggested Service

- `RegistrarStudentScheduleService`

Responsibilities:

- Load a student's active enrollment snapshot.
- Validate proposed edits.
- Apply edits in a transaction.
- Recompute unit totals.
- Return readable validation errors.

### Suggested Methods

- `getStudentScheduleSnapshot(studentId, enrollmentPeriodId)`
- `addSubject(studentId, offeringId)`
- `dropSubject(enrollmentDetailId)`
- `changeSection(enrollmentDetailId, newOfferingId)`
- `validateScheduleChanges(studentId, proposedChanges)`
- `saveScheduleChanges(studentId, changes)`

## Validation Rules

Validation should happen before any write:

- Student must have an active enrollment for the target period.
- Offering must belong to the same enrollment period.
- Subject must not duplicate an already enrolled subject.
- Total units after changes must not exceed the maximum allowed.
- The new offering must not create a day/time conflict with the student's other enrolled subjects.
- The offering must not exceed capacity.
- Prerequisites must be satisfied if the subject requires them.
- Changes to closed or finalized enrollments should be blocked unless registrar override is allowed.

## Transaction Rules

All schedule edits must be atomic.

- Start a database transaction.
- Validate all candidate changes.
- Update the affected `enrollment_details` rows.
- Update total units on the parent enrollment row if needed.
- Commit only if all writes succeed.
- Roll back on any validation or persistence failure.

## Suggested Screen Behavior

### Table Behavior

The current schedule table should be read-only by default.

- Double-click row or click `Edit` opens the section-change dialog.
- `Drop` is enabled only for editable rows.
- `Add Subject` opens an offering picker filtered by course, year level, and enrollment period.

### Edit Dialog Behavior

When changing a section:

- Show available offerings for the same subject.
- Disable offerings that are full or conflict with the student's current schedule.
- Display instructor, room, and time slots before confirming.

## Security and Permissions

Only registrar users should access the editor.

- Student users remain read-only.
- Faculty users remain read-only unless a separate instructor scheduling workflow exists.
- All registrar schedule edits should be logged if auditing is enabled.

## Implementation Order

1. Add a registrar student schedule editor screen.
2. Add a registrar schedule service layer.
3. Reuse existing enrollment and offering queries.
4. Add conflict and capacity validation.
5. Wire save/update actions with transactions.
6. Add optional audit logging.
7. Verify student dashboard refreshes from the updated enrollment data.

## Recommended Scope Decision

If the goal is to let registrars adjust what a student is enrolled in, implement this design.

If the goal is to let registrars edit the actual class meeting time, room, or faculty assignment, that should be a separate schedule administration feature because it affects every student enrolled in that offering.
