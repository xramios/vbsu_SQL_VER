#!/usr/bin/env python3
"""Parses student CSV files into normalized student records."""

from __future__ import annotations

import csv
from dataclasses import dataclass
from datetime import date, datetime
from pathlib import Path


@dataclass(frozen=True, slots=True)
class StudentCsvRow:
    """Normalized student row extracted from CSV."""

    student_id: str
    first_name: str
    last_name: str
    middle_name: str | None
    birthdate: date
    status: str
    year_level: int
    course_label: str
    curriculum_label: str


def parse_students_csv(csv_path: Path) -> list[StudentCsvRow]:
    """Read and normalize student rows from a CSV file."""
    if not csv_path.exists():
        raise FileNotFoundError(f"Students CSV not found: {csv_path}")

    rows: list[StudentCsvRow] = []
    with csv_path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.DictReader(handle)
        for row_index, raw_row in enumerate(reader, start=2):
            if _is_blank_row(raw_row):
                continue
            rows.append(_parse_row(raw_row, row_index))

    if not rows:
        raise ValueError(f"No student rows found in: {csv_path}")

    return rows


def _is_blank_row(raw_row: dict[str, str | None]) -> bool:
    for value in raw_row.values():
        if value is not None and value.strip():
            return False
    return True


def _parse_row(raw_row: dict[str, str | None], row_index: int) -> StudentCsvRow:
    student_id = _required(raw_row, "student_id", row_index)
    first_name = _required(raw_row, "first_name", row_index)
    last_name = _required(raw_row, "last_name", row_index)
    middle_name = _optional(raw_row, "middle_name")

    birthdate_text = _required(raw_row, "birthdate (YYYY-MM-DD)", row_index)
    try:
        birthdate = datetime.strptime(birthdate_text, "%Y-%m-%d").date()
    except ValueError as error:
        raise ValueError(
            f"Invalid birthdate at row {row_index}: {birthdate_text}. Expected YYYY-MM-DD."
        ) from error

    status = _optional(raw_row, "status")
    normalized_status = (status or "REGULAR").strip().upper()
    if normalized_status not in {"REGULAR", "IRREGULAR"}:
        raise ValueError(
            f"Invalid status at row {row_index}: {normalized_status}. "
            "Expected REGULAR or IRREGULAR."
        )

    year_level_text = _required(raw_row, "year_level", row_index)
    try:
        year_level = int(year_level_text)
    except ValueError as error:
        raise ValueError(
            f"Invalid year_level at row {row_index}: {year_level_text}."
        ) from error
    if year_level < 1:
        raise ValueError(f"Invalid year_level at row {row_index}: {year_level}. Must be >= 1.")

    course_label = (_optional(raw_row, "course") or "").strip()
    curriculum_label = (_optional(raw_row, "curriculum") or "").strip()

    return StudentCsvRow(
        student_id=student_id,
        first_name=first_name,
        last_name=last_name,
        middle_name=middle_name,
        birthdate=birthdate,
        status=normalized_status,
        year_level=year_level,
        course_label=course_label,
        curriculum_label=curriculum_label,
    )


def _required(raw_row: dict[str, str | None], column: str, row_index: int) -> str:
    value = _optional(raw_row, column)
    if value is None:
        raise ValueError(f"Missing required column '{column}' at row {row_index}")
    return value


def _optional(raw_row: dict[str, str | None], column: str) -> str | None:
    value = raw_row.get(column)
    if value is None:
        return None
    cleaned = value.strip()
    return cleaned if cleaned else None
