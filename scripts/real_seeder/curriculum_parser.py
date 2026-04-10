#!/usr/bin/env python3
"""Parses curriculum CSV files into normalized curriculum records."""

from __future__ import annotations

import csv
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True, slots=True)
class CurriculumCsvRow:
    """Normalized curriculum row extracted from the source CSV."""

    subject_code: str
    subject_name: str
    units: float
    year_level: int
    semester_label: str
    prerequisite_codes: tuple[str, ...]


def parse_curriculum_csv(csv_path: Path) -> list[CurriculumCsvRow]:
    """Read and normalize curriculum rows from a CSV file."""
    if not csv_path.exists():
        raise FileNotFoundError(f"Curriculum CSV not found: {csv_path}")

    rows: list[CurriculumCsvRow] = []
    with csv_path.open("r", encoding="utf-8-sig", newline="") as handle:
        reader = csv.reader(handle)
        for raw_row in reader:
            parsed = _parse_row(raw_row)
            if parsed is not None:
                rows.append(parsed)

    if not rows:
        raise ValueError(f"No curriculum records found in: {csv_path}")

    return rows


def _parse_row(raw_row: list[str]) -> CurriculumCsvRow | None:
    padded = list(raw_row)
    if len(padded) < 12:
        padded.extend([""] * (12 - len(padded)))

    cells = [cell.strip() for cell in padded]
    subject_code = cells[1]
    subject_name = cells[2]
    units_text = cells[6]
    prerequisites_text = cells[7]
    year_text = cells[10]
    semester_text = cells[11]

    if not subject_code or subject_code.lower() == "subject_code":
        return None

    if not subject_name or not year_text or not semester_text:
        return None

    year_level = _parse_year_level(year_text)
    if year_level is None:
        return None

    semester_label = _normalize_semester_label(semester_text)
    if semester_label is None:
        return None

    return CurriculumCsvRow(
        subject_code=subject_code,
        subject_name=subject_name,
        units=_parse_units(units_text),
        year_level=year_level,
        semester_label=semester_label,
        prerequisite_codes=_parse_prerequisite_codes(prerequisites_text),
    )


def _parse_units(units_text: str) -> float:
    cleaned = units_text.strip()
    if not cleaned:
        return 0.0

    return float(cleaned)


def _parse_year_level(year_text: str) -> int | None:
    cleaned = year_text.strip()
    if not cleaned:
        return None

    return int(cleaned)


def _normalize_semester_label(semester_text: str) -> str | None:
    cleaned = semester_text.strip()
    if not cleaned:
        return None

    lowered = cleaned.lower()
    if lowered in {"1", "1st", "first", "semester 1", "sem 1"}:
        return "Semester 1"
    if lowered in {"2", "2nd", "second", "semester 2", "sem 2"}:
        return "Semester 2"
    if lowered in {"3", "summer", "sum", "midyear"}:
        return "Summer"

    return cleaned


def _parse_prerequisite_codes(prerequisites_text: str) -> tuple[str, ...]:
    if not prerequisites_text.strip():
        return tuple()

    parsed_codes: list[str] = []
    seen: set[str] = set()

    for token in prerequisites_text.split(","):
        cleaned = token.strip()
        if not cleaned.startswith("P-"):
            continue

        subject_code = cleaned[2:].strip()
        if not subject_code:
            continue

        if subject_code not in seen:
            parsed_codes.append(subject_code)
            seen.add(subject_code)

    return tuple(parsed_codes)
