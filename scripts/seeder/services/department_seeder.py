#!/usr/bin/env python3
"""
Department seeder module.

Generates realistic academic departments for the university system.
"""

from typing import TYPE_CHECKING
from tqdm import tqdm

from seeder.services.base_seeder import BaseSeeder
from seeder.config.constants import DEPARTMENT_DATA, SEEDING_COUNTS
from seeder.models.data_models import Department

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class DepartmentSeeder(BaseSeeder):
    """Seeder for academic departments."""

    # SQL for creating departments table in Derby
    CREATE_TABLE_SQL = """
        CREATE TABLE TABLE_NAME (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            department_name VARCHAR(48),
            description CLOB,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize department seeder.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        super().__init__(db_manager, state)

    def seed(self, count: int = None) -> None:
        """Seed departments table with realistic academic departments.

        Args:
            count: Number of departments to create (default from SEEDING_COUNTS)
        """
        count = count or SEEDING_COUNTS["departments"]
        print(f"Seeding {count} departments...")

        self.create_table_if_not_exists("departments", self.CREATE_TABLE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for name, description in tqdm(
                DEPARTMENT_DATA[:count], desc="Creating departments", unit="dept"
            ):
                last_id = self.execute_insert(
                    "departments", ["department_name", "description"], [name, description],
                    cursor=cursor
                )

                self.state.departments.append(
                    Department(
                        id=last_id,
                        department_name=name,
                        description=description,
                    )
                )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.departments)} departments")
