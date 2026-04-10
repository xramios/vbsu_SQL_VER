# Real Data Seeder

Seeds real data from CSV into selected tables.

## Scope

Supported targets:

- `bundle` (clean reset + BSIT + NITEN2023 + students)
- `curriculum`
- `subjects`
- `semester`
- `semester_subjects`
- `prerequisites`
- `users` (student accounts)
- `students`

It does not seed faculty, schedules, enrollments, or offerings.

## Input Format

The importer is designed for the provided curriculum CSV format with these relevant columns:

- `subject_code`
- `subject_name`
- `units`
- `prerequisite` (uses only `P-` prerequisite codes)
- `year`
- `semester`

## Usage

### One-Command Bundle Target (Recommended)

This is the simple reset workflow. It clears related tables first, then seeds:

- Course: BSIT (`Bachelor of Science in Information Technology`)
- Curriculum: `NITEN2023`
- Students from `docs/students.csv`

Run from the `scripts` directory:

```bash
python -m real_seeder.cli \
  --db-type derby \
  --database university_db \
  --user app \
  --password derby
```

`bundle` is the default target, so `--target bundle` is optional.

### Curriculum Target

Run from the `scripts` directory:

```bash
python -m real_seeder.cli
```

With explicit Derby connection:

```bash
python -m real_seeder.cli \
  --db-type derby \
  --host localhost \
  --database sample \
  --user APP \
  --password derby
```

If the target course is missing and you want the script to create it:

```bash
python -m real_seeder.cli \
  --target curriculum \
  --course-name "Bachelor of Science in Information Technology" \
  --create-course-if-missing
```

### Students Target

Seed users + students from `docs/students.csv` with fixed course/curriculum mapping:

```bash
python -m real_seeder.cli \
  --target students \
  --db-type derby \
  --database university_db \
  --user app \
  --password derby \
  --students-csv ../docs/students.csv \
  --students-course BSIT \
  --students-curriculum NITEN2023
```

Credential rules for student import:

- Email: `lastname.firstname@vbsu.edu.ph`
- Plain password basis: `lastname_YYYY` (YYYY = birth year)
- Stored password: bcrypt hash of the plain password basis

## Notes

- Default CSV: `docs/ENROLLMENT SYSTEM CCP FINALS - Curriculum NITEN2023.csv`
- Default curriculum name/year: `NITEN2023` / `2023`
- Subject names are capped to 32 chars by default to align with current schema constraints (`--subject-name-max-length`).
- Student target defaults: `--students-course BSIT`, `--students-curriculum NITEN2023`.
