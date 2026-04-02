#!/usr/bin/env python3
"""
Enrollment seeder module.

Generates enrollment periods, schedules, and enrollment records.
"""

import random
from datetime import datetime, timedelta
from typing import TYPE_CHECKING
from tqdm import tqdm

from seeder.services.base_seeder import BaseSeeder
from seeder.config.constants import (
    SEEDING_COUNTS,
    DAYS_OF_WEEK,
    START_HOURS,
    START_MINUTES,
    DURATION_HOURS,
    ENROLLMENT_STATUSES,
    ENROLLMENT_DETAIL_STATUSES,
    ENROLLMENTS_PER_STUDENT,
)
from seeder.utils.faker_instance import fake
from seeder.models.data_models import EnrollmentPeriod, Schedule

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class EnrollmentSeeder(BaseSeeder):
    """Seeder for enrollment periods, schedules, and enrollments."""

    ENROLLMENT_PERIOD_CREATE_SQL = """
        CREATE TABLE APP.enrollment_period (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            school_year VARCHAR(50),
            semester INTEGER,
            start_date DATE,
            end_date DATE
        )
    """

    SCHEDULES_CREATE_SQL = """
        CREATE TABLE APP.schedules (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            section_id INTEGER,
            room_id INTEGER,
            faculty_id INTEGER,
            day VARCHAR(10),
            start_time VARCHAR(10),
            end_time VARCHAR(10),
            FOREIGN KEY (section_id) REFERENCES APP.sections(id),
            FOREIGN KEY (room_id) REFERENCES APP.rooms(id),
            FOREIGN KEY (faculty_id) REFERENCES APP.faculty(id)
        )
    """

    ENROLLMENTS_CREATE_SQL = """
        CREATE TABLE APP.enrollments (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            student_id VARCHAR(50),
            school_year VARCHAR(50),
            semester INTEGER,
            status VARCHAR(50),
            max_units DECIMAL(5,2),
            total_units DECIMAL(5,2),
            submitted_at TIMESTAMP
        )
    """

    ENROLLMENT_DETAILS_CREATE_SQL = """
        CREATE TABLE APP.enrollments_details (
            id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            enrollment_id INTEGER,
            section_id INTEGER,
            subject_id INTEGER,
            units DECIMAL(3,1),
            status VARCHAR(50),
            FOREIGN KEY (enrollment_id) REFERENCES APP.enrollments(id),
            FOREIGN KEY (section_id) REFERENCES APP.sections(id),
            FOREIGN KEY (subject_id) REFERENCES APP.subjects(id)
        )
    """

    STUDENT_ENROLLED_SUBJECTS_CREATE_SQL = """
        CREATE TABLE APP.student_enrolled_subjects (
            student_id VARCHAR(50),
            semester_subject_id INTEGER,
            status VARCHAR(20) DEFAULT 'ENROLLED',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (student_id, semester_subject_id),
            FOREIGN KEY (student_id) REFERENCES APP.students(student_id)
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize enrollment seeder.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        super().__init__(db_manager, state)

    def seed_enrollment_periods(self, count: int = None) -> None:
        """Seed enrollment_period table with academic periods.

        Args:
            count: Number of enrollment periods to create
        """
        count = count or SEEDING_COUNTS["enrollment_periods"]
        print(f"Seeding {count} enrollment periods...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("enrollment_period", self.ENROLLMENT_PERIOD_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for i in range(count):
                year = 2021 + i
                for semester in [1, 2]:
                    if semester == 1:
                        start_date = datetime(year - 1, 10, 1)
                        end_date = datetime(year - 1, 11, 30)
                    else:
                        start_date = datetime(year, 3, 1)
                        end_date = datetime(year, 4, 30)

                    start_date_str = self.format_datetime(start_date)
                    end_date_str = self.format_datetime(end_date)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.enrollment_period
                            (school_year, semester, start_date, end_date)
                            VALUES (?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (f"{year-1}-{year}", semester, start_date_str, end_date_str),
                        )
                    else:
                        query = """
                            INSERT INTO enrollment_period
                            (school_year, semester, start_date, end_date)
                            VALUES (%s, %s, %s, %s)
                        """
                        cursor.execute(
                            query, (f"{year-1}-{year}", semester, start_date, end_date)
                        )

                    last_id = self.adapter.get_last_insert_id(cursor, "enrollment_period")

                    self.state.enrollment_periods.append(
                        EnrollmentPeriod(
                            id=last_id,
                            school_year=f"{year-1}-{year}",
                            semester=semester,
                            start_date=start_date,
                            end_date=end_date,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.enrollment_periods)} enrollment periods")

    def seed_schedules(self) -> None:
        """Seed schedules table with class schedules."""
        print("Seeding schedules...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("schedules", self.SCHEDULES_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for section in self.state.sections:
                num_schedules = random.randint(1, 2)

                for _ in range(num_schedules):
                    day = random.choice(DAYS_OF_WEEK)

                    start_hour = random.choice(START_HOURS)
                    start_minute = random.choice(START_MINUTES)
                    end_hour = start_hour + random.choice(DURATION_HOURS)
                    end_minute = start_minute

                    start_time = f"{start_hour:02d}:{start_minute:02d}"
                    end_time = f"{end_hour:02d}:{end_minute:02d}"

                    room = random.choice(self.state.rooms)
                    faculty = random.choice(self.state.faculty)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.schedules
                            (section_id, room_id, faculty_id, day, start_time, end_time)
                            VALUES (?, ?, ?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (section.id, room.id, faculty.id, day, start_time, end_time),
                        )
                    else:
                        query = """
                            INSERT INTO schedules
                            (section_id, room_id, faculty_id, day, start_time, end_time)
                            VALUES (%s, %s, %s, %s, %s, %s)
                        """
                        cursor.execute(
                            query,
                            (section.id, room.id, faculty.id, day, start_time, end_time),
                        )

            self.db_manager.commit()
        finally:
            cursor.close()

        print("Created schedules")

    def seed_enrollments(self) -> None:
        """Seed enrollments and enrollment details tables."""
        print("Seeding enrollments...")

        if self.db_manager.db_type == "derby":
            self.create_table_if_not_exists("enrollments", self.ENROLLMENTS_CREATE_SQL)
            self.create_table_if_not_exists(
                "enrollments_details", self.ENROLLMENT_DETAILS_CREATE_SQL
            )
            self.create_table_if_not_exists(
                "student_enrolled_subjects", self.STUDENT_ENROLLED_SUBJECTS_CREATE_SQL
            )

        cursor = self.db_manager.connection.cursor()
        try:
            statuses = ENROLLMENT_STATUSES

            for student in self.state.students:
                num_enrollments = random.randint(*ENROLLMENTS_PER_STUDENT)

                for _ in range(num_enrollments):
                    school_year = random.choice([f"{y}-{y+1}" for y in range(2021, 2025)])
                    semester = random.randint(1, 2)
                    status = random.choice(statuses)

                    max_units = random.uniform(15, 24)
                    total_units = random.uniform(12, max_units)

                    submitted_at = fake.date_time_between(start_date="-2y", end_date="now")
                    submitted_at_str = self.format_timestamp(submitted_at)

                    if self.db_manager.db_type == "derby":
                        query = """
                            INSERT INTO APP.enrollments
                            (student_id, school_year, semester, status, max_units, total_units, submitted_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                        """
                        cursor.execute(
                            query,
                            (
                                student.student_id,
                                school_year,
                                semester,
                                status,
                                max_units,
                                total_units,
                                submitted_at_str,
                            ),
                        )
                    else:
                        query = """
                            INSERT INTO enrollments
                            (student_id, school_year, semester, status, max_units, total_units, submitted_at)
                            VALUES (%s, %s, %s, %s, %s, %s, %s)
                        """
                        cursor.execute(
                            query,
                            (
                                student.student_id,
                                school_year,
                                semester,
                                status,
                                max_units,
                                total_units,
                                submitted_at,
                            ),
                        )

                    if self.db_manager.db_type == "mysql":
                        enrollment_id = cursor.lastrowid
                    else:
                        enrollment_id = self.adapter.get_last_insert_id(cursor, "enrollments")

                    if status in ["APPROVED", "ENROLLED"]:
                        self._create_enrollment_details(cursor, enrollment_id, student.student_id, semester)

            self.db_manager.commit()
        finally:
            cursor.close()

        print("Created enrollments and enrollment details")

    def seed(self) -> None:
        """Execute all enrollment-related seeding operations.

        Orchestrates enrollment periods, schedules, and enrollments seeding.
        """
        self.seed_enrollment_periods()
        self.seed_schedules()
        self.seed_enrollments()

    def _create_enrollment_details(
        self, cursor: any, enrollment_id: int, student_id: str, semester: int = 1
    ) -> None:
        """Create enrollment details for an enrollment.

        Args:
            cursor: Database cursor
            enrollment_id: Enrollment ID
            student_id: Student ID
            semester: Semester number (1 or 2) for finding correct semester_subject
        """
        num_subjects = random.randint(3, 7)
        available_sections = random.sample(
            self.state.sections, min(num_subjects, len(self.state.sections))
        )

        # Get student's course_id for finding correct semester_subjects
        student = next((s for s in self.state.students if s.student_id == student_id), None)
        student_course_id = student.course_id if student else None
        student_year_level = student.year_level if student else 1

        for section in available_sections:
            detail_status = random.choice(ENROLLMENT_DETAIL_STATUSES)
            units = next(
                (s.units for s in self.state.subjects if s.id == section.subject_id), 3
            )

            if self.db_manager.db_type == "derby":
                query = """
                    INSERT INTO APP.enrollments_details
                    (enrollment_id, section_id, subject_id, units, status)
                    VALUES (?, ?, ?, ?, ?)
                """
                cursor.execute(
                    query, (enrollment_id, section.id, section.subject_id, units, detail_status)
                )
            else:
                query = """
                    INSERT INTO enrollments_details
                    (enrollment_id, section_id, subject_id, units, status)
                    VALUES (%s, %s, %s, %s, %s)
                """
                cursor.execute(
                    query, (enrollment_id, section.id, section.subject_id, units, detail_status)
                )

            # Find the correct semester_subject_id for this subject and semester
            semester_subject_id = self._find_semester_subject_id(
                section.subject_id, semester, student_course_id, student_year_level
            )
            if semester_subject_id:
                self._insert_student_enrolled_subject(cursor, student_id, semester_subject_id)

    def _find_semester_subject_id(
        self, subject_id: int, semester_num: int, course_id: int = None, year_level: int = None
    ) -> int | None:
        """Find the semester_subject_id for a given subject and semester context.

        Args:
            subject_id: The subject ID
            semester_num: Semester number (1 or 2)
            course_id: Optional course ID to narrow down by curriculum
            year_level: Optional student year level for filtering

        Returns:
            The semester_subject ID if found, None otherwise
        """
        if not self.state.semester_subjects:
            return None

        # Build a list of semester IDs matching the semester number
        semester_name = f"Semester {semester_num}"
        matching_semester_ids = {
            s.id for s in self.state.semesters if s.semester == semester_name
        }

        # If course_id provided, narrow down to semesters from that course's curriculums
        if course_id:
            course_curriculum_ids = {c.id for c in self.state.curriculums if c.course_id == course_id}
            matching_semester_ids = {
                s.id for s in self.state.semesters
                if s.semester == semester_name and s.curriculum_id in course_curriculum_ids
            }

        # Find semester_subjects matching the criteria
        candidates = [
            ss for ss in self.state.semester_subjects
            if ss.subject_id == subject_id and ss.semester_id in matching_semester_ids
        ]

        # Filter by year level if provided (allow subjects at or below student's year)
        if year_level:
            candidates = [ss for ss in candidates if ss.year_level <= year_level]

        if candidates:
            return random.choice(candidates).id
        return None

    def _insert_student_enrolled_subject(
        self, cursor: any, student_id: str, semester_subject_id: int
    ) -> None:
        """Insert student enrolled subject with duplicate handling.

        Args:
            cursor: Database cursor
            student_id: Student ID
            semester_subject_id: Semester Subject ID (from semester_subjects table)
        """
        if self.db_manager.db_type == "derby":
            # Derby doesn't support INSERT IGNORE, check existence first
            cursor.execute(
                """
                    SELECT COUNT(*) FROM APP.student_enrolled_subjects
                    WHERE student_id = ? AND semester_subject_id = ?
                """,
                (student_id, semester_subject_id),
            )
            if cursor.fetchone()[0] == 0:
                query = """
                    INSERT INTO APP.student_enrolled_subjects (student_id, semester_subject_id, status)
                    VALUES (?, ?, 'ENROLLED')
                """
                cursor.execute(query, (student_id, semester_subject_id))
        else:
            query = """
                INSERT IGNORE INTO student_enrolled_subjects (student_id, semester_subject_id, status)
                VALUES (%s, %s, 'ENROLLED')
            """
            cursor.execute(query, (student_id, semester_subject_id))
