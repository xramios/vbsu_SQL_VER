#!/usr/bin/env python3
"""
User seeder module.

Generates user accounts, student records, and faculty records.
"""

import random
from datetime import datetime, timedelta
from typing import TYPE_CHECKING

import bcrypt
from tqdm import tqdm

from seeder.config.constants import (
    BACHELOR_MAX_YEAR,
    BCRYPT_ROUNDS,
    DEFAULT_PASSWORD,
    SEEDING_COUNTS,
    STUDENT_AGE_RANGE,
    STUDENT_DEMOGRAPHICS,
    STUDENT_STATUS_CONFIG,
    STUDENT_YEAR_LEVEL_RANGE,
    USER_ROLES,
)
from seeder.models.data_models import Faculty, Registrar, Student, User
from seeder.services.base_seeder import BaseSeeder
from seeder.utils.faker_instance import fake

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class UserSeeder(BaseSeeder):
    """Seeder for users, students, and faculty."""

    USERS_CREATE_SQL = """
        CREATE TABLE TABLE_NAME (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            email VARCHAR(255),
            password CHAR(60),
            role VARCHAR(20) CHECK (role IN ('STUDENT', 'REGISTRAR', 'FACULTY')),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    STUDENTS_CREATE_SQL = """
        CREATE TABLE TABLE_NAME (
            student_id VARCHAR(32) PRIMARY KEY,
            user_id BIGINT,
            first_name VARCHAR(128) NOT NULL,
            last_name VARCHAR(128) NOT NULL,
            middle_name VARCHAR(48),
            birthdate DATE NOT NULL,
            student_status VARCHAR(20) DEFAULT 'REGULAR' CHECK (student_status IN ('REGULAR', 'IRREGULAR')),
            course_id BIGINT,
            year_level INT DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES APP.users(id),
            FOREIGN KEY (course_id) REFERENCES APP.courses(id)
        )
    """

    FACULTY_CREATE_SQL = """
        CREATE TABLE TABLE_NAME (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            user_id BIGINT,
            first_name VARCHAR(128),
            last_name VARCHAR(128),
            department_id BIGINT,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES APP.users(id),
            FOREIGN KEY (department_id) REFERENCES APP.departments(id)
        )
    """

    REGISTRAR_CREATE_SQL = """
        CREATE TABLE TABLE_NAME (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            user_id BIGINT,
            employee_id VARCHAR(20),
            first_name VARCHAR(128),
            last_name VARCHAR(128),
            contact_number VARCHAR(20),
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES APP.users(id)
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize user seeder.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        super().__init__(db_manager, state)

    def seed_users(
        self,
        student_count: int = None,
        faculty_count: int = None,
        registrar_count: int = None,
    ) -> None:
        """Seed users table with students, faculty, and registrar accounts.

        Args:
            student_count: Number of student users to create
            faculty_count: Number of faculty users to create
            registrar_count: Number of registrar users to create
        """
        student_count = student_count or SEEDING_COUNTS["students"]
        faculty_count = faculty_count or SEEDING_COUNTS["faculty"]
        registrar_count = registrar_count or SEEDING_COUNTS["registrars"]

        print(
            f"Seeding {student_count} students, {faculty_count} faculty, "
            f"and {registrar_count} registrars..."
        )

        self.create_table_if_not_exists("users", self.USERS_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            self._create_users(cursor, student_count, USER_ROLES["STUDENT"], "student")
            self._create_users(cursor, faculty_count, USER_ROLES["FACULTY"], "faculty")
            self._create_users(
                cursor, registrar_count, USER_ROLES["REGISTRAR"], "registrar"
            )
            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.users)} users")

    def _create_users(self, cursor: any, count: int, role: str, user_type: str) -> None:
        """Create users of a specific type.

        Args:
            cursor: Database cursor
            count: Number of users to create
            role: User role (STUDENT, FACULTY, REGISTRAR)
            user_type: Type identifier for internal tracking
        """
        for _ in tqdm(range(count), desc=f"Creating {user_type} users", unit="user"):
            email = fake.unique.email()
            password = bcrypt.hashpw(
                DEFAULT_PASSWORD.encode("utf-8"), bcrypt.gensalt(rounds=BCRYPT_ROUNDS)
            ).decode("utf-8")

            last_id = self.execute_insert(
                "users",
                ["email", "password", "role"],
                [email, password, role],
                cursor=cursor,
            )

            self.state.users.append(
                User(
                    id=last_id,
                    email=email,
                    role=role,
                    user_type=user_type,
                    password=password,
                )
            )

    def seed_students(self) -> None:
        """Seed students table with realistic student data."""
        print("Seeding students...")

        self.create_table_if_not_exists("students", self.STUDENTS_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            student_users = [u for u in self.state.users if u.user_type == "student"]

            for user in tqdm(
                student_users, desc="Creating student records", unit="student"
            ):
                self._create_student_record(cursor, user)

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.students)} students")

    def _create_student_record(self, cursor: any, user: User) -> None:
        """Create a single student record.

        Args:
            cursor: Database cursor
            user: User object for the student
        """
        year = random.randint(*STUDENT_DEMOGRAPHICS["year_range"])
        student_number = (
            f"{year}-{random.randint(*STUDENT_DEMOGRAPHICS['student_number_range'])}"
        )

        # Ensure uniqueness
        existing_ids = [s.student_id for s in self.state.students]
        while student_number in existing_ids:
            student_number = f"{year}-{random.randint(10000, 99999)}"

        first_name = fake.first_name()
        last_name = fake.last_name()
        middle_name = (
            fake.first_name()
            if random.random() > STUDENT_DEMOGRAPHICS["middle_name_probability"]
            else None
        )

        age = random.randint(*STUDENT_AGE_RANGE)
        birthdate = datetime.now() - timedelta(days=age * 365)
        birthdate_str = self.format_datetime(birthdate)

        student_status = (
            STUDENT_STATUS_CONFIG["irregular_status"]
            if random.random() < STUDENT_STATUS_CONFIG["irregular_probability"]
            else STUDENT_STATUS_CONFIG["regular_status"]
        )

        course = random.choice(self.state.courses)
        year_level = min(
            random.randint(*STUDENT_YEAR_LEVEL_RANGE),
            BACHELOR_MAX_YEAR if course.name.startswith("Bachelor") else 5,
        )

        columns = [
            "student_id",
            "user_id",
            "first_name",
            "last_name",
            "middle_name",
            "birthdate",
            "student_status",
            "course_id",
            "year_level",
        ]
        values = [
            student_number,
            user.id,
            first_name,
            last_name,
            middle_name,
            birthdate_str,
            student_status,
            course.id,
            year_level,
        ]

        self.execute_insert("students", columns, values, cursor=cursor)

        self.state.students.append(
            Student(
                student_id=student_number,
                user_id=user.id,
                first_name=first_name,
                last_name=last_name,
                course_id=course.id,
                year_level=year_level,
                middle_name=middle_name,
                birthdate=birthdate,
                student_status=student_status,
            )
        )

    def seed(self) -> None:
        """Execute all user-related seeding operations.

        Orchestrates the four-phase seeding process:
        1. User accounts (students, faculty, registrars)
        2. Student records
        3. Faculty records
        4. Registrar records
        """
        self.seed_users()
        self.seed_students()
        self.seed_faculty()
        self.seed_registrars()

    def seed_faculty(self) -> None:
        """Seed faculty table with instructor information."""
        print("Seeding faculty...")

        self.create_table_if_not_exists("faculty", self.FACULTY_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            faculty_users = [u for u in self.state.users if u.user_type == "faculty"]

            for user in tqdm(
                faculty_users, desc="Creating faculty records", unit="faculty"
            ):
                first_name = fake.first_name()
                last_name = fake.last_name()
                department = random.choice(self.state.departments)

                last_id = self.execute_insert(
                    "faculty",
                    ["user_id", "first_name", "last_name", "department_id"],
                    [user.id, first_name, last_name, department.id],
                    cursor=cursor,
                )

                self.state.faculty.append(
                    Faculty(
                        id=last_id,
                        user_id=user.id,
                        first_name=first_name,
                        last_name=last_name,
                        department_id=department.id,
                    )
                )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.faculty)} faculty members")

    def seed_registrars(self) -> None:
        """Seed registrar table with registrar staff information."""
        print("Seeding registrars...")

        self.create_table_if_not_exists("registrar", self.REGISTRAR_CREATE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            registrar_users = [
                u for u in self.state.users if u.user_type == "registrar"
            ]

            for user in tqdm(
                registrar_users, desc="Creating registrar records", unit="registrar"
            ):
                first_name = fake.first_name()
                last_name = fake.last_name()
                # Generate employee ID like EMP-XXXX
                employee_id = f"EMP-{random.randint(1000, 9999)}"
                # Generate Philippines mobile number format: 09XX-XXX-XXXX
                contact_number = f"09{random.randint(10, 99)}-{random.randint(100, 999)}-{random.randint(1000, 9999)}"

                last_id = self.execute_insert(
                    "registrar",
                    [
                        "user_id",
                        "employee_id",
                        "first_name",
                        "last_name",
                        "contact_number",
                    ],
                    [user.id, employee_id, first_name, last_name, contact_number],
                    cursor=cursor,
                )

                self.state.registrars.append(
                    Registrar(
                        id=last_id,
                        user_id=user.id,
                        employee_id=employee_id,
                        first_name=first_name,
                        last_name=last_name,
                        contact_number=contact_number,
                    )
                )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.registrars)} registrars")
