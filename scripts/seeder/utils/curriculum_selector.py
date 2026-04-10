#!/usr/bin/env python3
"""Shared helpers to select curricula for seeded student data."""

from typing import Any


def extract_admission_year(student_id: str) -> int | None:
    """Extract admission year from student IDs like YYYY-#####."""
    if not student_id or "-" not in student_id:
        return None

    year_part = student_id.split("-", maxsplit=1)[0]
    if not year_part.isdigit():
        return None

    return int(year_part)


def select_student_curriculum(
    student_id: str,
    course_id: int,
    course_curriculums: dict[int, list[Any]],
) -> Any:
    """Pick the best curriculum for a student by course and admission year."""
    matching_curriculums = course_curriculums.get(course_id, [])
    if not matching_curriculums:
        return None

    admission_year = extract_admission_year(student_id)
    if admission_year is None:
        return max(
            matching_curriculums,
            key=lambda curriculum: getattr(curriculum.cur_year, "year", 0),
        )

    return min(
        matching_curriculums,
        key=lambda curriculum: abs(
            getattr(curriculum.cur_year, "year", admission_year) - admission_year
        ),
    )
