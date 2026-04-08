#!/usr/bin/env python3
"""
Course seeder module.

Generates academic courses/programs linked to departments.
"""

from typing import TYPE_CHECKING
from tqdm import tqdm

from seeder.services.base_seeder import BaseSeeder
from seeder.config.constants import COURSE_DATA, SEEDING_COUNTS
from seeder.models.data_models import Course

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class CourseSeeder(BaseSeeder):
    """Seeder for academic courses/programs."""

    CREATE_TABLE_SQL = """
        CREATE TABLE TABLE_NAME (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            course_name VARCHAR(48),
            description CLOB,
            department_id BIGINT,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (department_id) REFERENCES APP.departments(id)
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize course seeder.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        super().__init__(db_manager, state)

    def seed(self, count: int = None) -> None:
        """Seed courses table with academic programs.

        Args:
            count: Number of courses to create (default from SEEDING_COUNTS)
        """
        count = count or SEEDING_COUNTS["courses"]
        print(f"Seeding {count} courses...")

        self.create_table_if_not_exists("courses", self.CREATE_TABLE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for name, description, dept_idx in tqdm(
                COURSE_DATA[:count], desc="Creating courses", unit="course"
            ):
                if dept_idx < len(self.state.departments):
                    dept_id = self.state.departments[dept_idx].id
                    last_id = self.execute_insert(
                        "courses",
                        ["course_name", "description", "department_id"],
                        [name, description, dept_id],
                        cursor=cursor,
                    )

                    self.state.courses.append(
                        Course(
                            id=last_id,
                            course_name=name,
                            department_id=dept_id,
                            description=description,
                        )
                    )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.courses)} courses")
