#!/usr/bin/env python3
"""
Room seeder module.

Generates classroom and facility records with various capacities.
"""

import random
from typing import TYPE_CHECKING
from tqdm import tqdm

from seeder.services.base_seeder import BaseSeeder
from seeder.config.constants import (
    SEEDING_COUNTS,
    ROOM_TYPES,
    ROOM_STATUSES,
    BUILDING_NAMES,
    BUILDING_FLOORS,
    MIN_ROOM_CAPACITY,
    CAPACITY_VARIATION,
)
from seeder.models.data_models import Room

if TYPE_CHECKING:
    from seeder.core.database import DatabaseManager
    from seeder.models.data_models import SeedingState


class RoomSeeder(BaseSeeder):
    """Seeder for rooms and facilities."""

    CREATE_TABLE_SQL = """
        CREATE TABLE TABLE_NAME (
            id BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
            building VARCHAR(64) NOT NULL,
            room_type VARCHAR(32) NOT NULL CHECK (room_type IN ('LECTURE', 'LAB', 'SEMINAR', 'AUDITORIUM', 'OTHER')),
            status VARCHAR(32) NOT NULL CHECK (status IN ('AVAILABLE', 'UNAVAILABLE', 'MAINTENANCE')) DEFAULT 'AVAILABLE',
            room VARCHAR(32) NOT NULL,
            capacity INT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """

    def __init__(self, db_manager: "DatabaseManager", state: "SeedingState") -> None:
        """Initialize room seeder.

        Args:
            db_manager: Database manager instance
            state: Shared seeding state
        """
        super().__init__(db_manager, state)

    def seed(self, count: int = None) -> None:
        """Seed rooms table with various room types and capacities.

        Args:
            count: Number of rooms to create (default from SEEDING_COUNTS)
        """
        count = count or SEEDING_COUNTS["rooms"]
        print(f"Seeding {count} rooms...")

        self.create_table_if_not_exists("rooms", self.CREATE_TABLE_SQL)

        cursor = self.db_manager.connection.cursor()
        try:
            for i in tqdm(range(count), desc="Creating rooms", unit="room"):
                room_type, base_capacity = random.choice(ROOM_TYPES)
                building = random.choice(BUILDING_NAMES)
                floor = random.randint(*BUILDING_FLOORS)
                room_number = f"{building[0]}{floor:02d}{i+1:03d}"

                capacity = max(
                    MIN_ROOM_CAPACITY, base_capacity + random.randint(*CAPACITY_VARIATION)
                )
                status = random.choices(ROOM_STATUSES, weights=[0.9, 0.07, 0.03])[0]

                last_id = self.execute_insert(
                    "rooms",
                    ["building", "room_type", "status", "room", "capacity"],
                    [building, room_type, status, room_number, capacity],
                    cursor=cursor
                )

                self.state.rooms.append(
                    Room(
                        id=last_id,
                        building=building,
                        room_type=room_type,
                        status=status,
                        room=room_number,
                        capacity=capacity,
                    )
                )

            self.db_manager.commit()
        finally:
            cursor.close()

        print(f"Created {len(self.state.rooms)} rooms")
