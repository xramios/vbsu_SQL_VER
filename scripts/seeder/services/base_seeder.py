#!/usr/bin/env python3
"""
Base seeder class providing common functionality for all seeders.
"""

from abc import ABC, abstractmethod
from typing import Optional, List, Any, TYPE_CHECKING

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class BaseSeeder(ABC):
    """Abstract base class for all entity seeders."""

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize seeder with database manager and state.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        self.db_manager = db_manager
        self.state = state
        self.adapter = db_manager.adapter

    @abstractmethod
    def seed(self) -> None:
        """Execute seeding operation. Must be implemented by subclasses."""
        pass

    def execute_insert(
        self, table_name: str, columns: List[str], values: List[Any], return_id: bool = True,
        cursor: Any = None
    ) -> Optional[int]:
        """Execute insert and return ID.

        Args:
            table_name: Target table name
            columns: List of column names
            values: List of values to insert
            return_id: Whether to return the last inserted ID
            cursor: Optional cursor for transaction context

        Returns:
            Last inserted ID if return_id is True, None otherwise
        """
        return self.db_manager.execute_insert(table_name, columns, values, return_id, cursor)

    def create_table_if_not_exists(self, table_name: str, create_sql: str) -> None:
        """Create table if it doesn't exist.

        Args:
            table_name: Name of the table to create
            create_sql: CREATE TABLE SQL statement
        """
        self.db_manager.create_table_if_not_exists(table_name, create_sql)

    def format_datetime(self, dt: Any) -> Any:
        """Format datetime for database compatibility.

        Args:
            dt: Datetime object to format

        Returns:
            Formatted datetime
        """
        return self.adapter.format_datetime(dt)

    def format_timestamp(self, ts: Any) -> Any:
        """Format timestamp for database compatibility.

        Args:
            ts: Datetime object to format

        Returns:
            Formatted timestamp
        """
        return self.adapter.format_timestamp(ts)
