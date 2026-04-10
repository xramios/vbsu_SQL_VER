#!/usr/bin/env python3
"""One-command reset-and-seed workflow for BSIT + NITEN2023 + students."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Any

from seeder.core.database import DatabaseManager

from real_seeder.curriculum_seeder import CurriculumSeedSummary, RealCurriculumSeeder
from real_seeder.students_seeder import RealStudentsSeeder, StudentsSeedSummary


@dataclass(frozen=True, slots=True)
class BundleSeedSummary:
    """Summary for bundled reset-and-seed operations."""

    cleared_tables: tuple[str, ...]
    curriculum: CurriculumSeedSummary
    students: StudentsSeedSummary


class BsitNiten2023BundleSeeder:
    """Clears related tables and seeds only BSIT, NITEN2023, and students."""

    TABLES_TO_CLEAR: tuple[str, ...] = (
        "student_semester_progress",
        "student_enrolled_subjects",
        "enrollments_details",
        "schedules",
        "offerings",
        "enrollments",
        "semester_subjects",
        "prerequisites",
        "semester",
        "students",
        "faculty",
        "registrar",
        "users",
        "subjects",
        "curriculum",
        "courses",
    )

    def __init__(self, db_manager: DatabaseManager, bcrypt_rounds: int = 12) -> None:
        self.db_manager = db_manager
        self.bcrypt_rounds = bcrypt_rounds
        self._table_prefix = "APP." if db_manager.db_type == "derby" else ""

    def run(
        self,
        curriculum_csv_path: Path,
        students_csv_path: Path,
        course_name: str = "Bachelor of Science in Information Technology",
        curriculum_name: str = "NITEN2023",
        curriculum_year: int = 2023,
        students_course_hint: str = "BSIT",
    ) -> BundleSeedSummary:
        """Execute reset + curriculum seeding + students seeding."""
        cleared_tables = self._clear_target_tables()

        curriculum_summary = RealCurriculumSeeder(self.db_manager).seed_from_csv(
            csv_path=curriculum_csv_path,
            curriculum_name=curriculum_name,
            curriculum_year=curriculum_year,
            course_id=None,
            course_name=course_name,
            create_course_if_missing=True,
        )

        students_summary = RealStudentsSeeder(
            self.db_manager,
            bcrypt_rounds=self.bcrypt_rounds,
        ).seed_from_csv(
            csv_path=students_csv_path,
            course_hint=students_course_hint,
            curriculum_name=curriculum_name,
        )

        return BundleSeedSummary(
            cleared_tables=cleared_tables,
            curriculum=curriculum_summary,
            students=students_summary,
        )

    def _clear_target_tables(self) -> tuple[str, ...]:
        if not self.db_manager.connect():
            raise RuntimeError("Failed to connect to database")

        cursor = self.db_manager.connection.cursor()
        cleared: list[str] = []
        try:
            for table in self.TABLES_TO_CLEAR:
                try:
                    cursor.execute(f"DELETE FROM {self._table(table)}")
                    cleared.append(table)
                except Exception as error:
                    if not self._is_ignorable_clear_error(error):
                        raise

            self.db_manager.commit()
            return tuple(cleared)
        except Exception:
            rollback = getattr(self.db_manager.connection, "rollback", None)
            if callable(rollback):
                rollback()
            raise
        finally:
            cursor.close()
            self.db_manager.disconnect()

    def _is_ignorable_clear_error(self, error: Exception) -> bool:
        message = str(error).lower()
        return any(
            marker in message
            for marker in (
                "does not exist",
                "not found",
                "42x05",
                "42s02",
            )
        )

    def _table(self, table_name: str) -> str:
        return f"{self._table_prefix}{table_name}"
