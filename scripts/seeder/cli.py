#!/usr/bin/env python3
"""
Command-line interface for the University Database Seeder.

Provides argument parsing and main entry point for the seeding system.
"""

import argparse
import sys
from typing import Optional

from seeder.core.database import DatabaseManager
from seeder.core.orchestrator import SeedingOrchestrator
from seeder.config.settings import DATABASE_CONFIG


def create_parser() -> argparse.ArgumentParser:
    """Create and configure argument parser.

    Returns:
        Configured ArgumentParser instance
    """
    parser = argparse.ArgumentParser(description="University Database Seeder")
    parser.add_argument(
        "--db-type",
        default="mysql",
        choices=["mysql", "derby"],
        help="Database type (mysql or derby)",
    )
    parser.add_argument(
        "--host", default=DATABASE_CONFIG["host"], help="Database host"
    )
    parser.add_argument(
        "--database", default=DATABASE_CONFIG["database"], help="Database name"
    )
    parser.add_argument(
        "--user", default=DATABASE_CONFIG["user"], help="Database user"
    )
    parser.add_argument(
        "--password", default=DATABASE_CONFIG["password"], help="Database password"
    )
    parser.add_argument(
        "--no-clear", action="store_true", help="Do not clear existing data"
    )
    return parser


def main(argv: Optional[list[str]] = None) -> int:
    """Main function to run the database seeder.

    Args:
        argv: Command-line arguments (defaults to sys.argv)

    Returns:
        Exit code (0 for success, 1 for failure)
    """
    parser = create_parser()
    args = parser.parse_args(argv)

    db_manager = DatabaseManager(
        db_type=args.db_type,
        host=args.host,
        database=args.database,
        user=args.user,
        password=args.password,
    )

    orchestrator = SeedingOrchestrator(db_manager)
    success = orchestrator.seed_all(clear_existing=not args.no_clear)

    if success:
        print("Database seeding completed successfully!")
        return 0
    else:
        print("Database seeding failed!")
        return 1


if __name__ == "__main__":
    sys.exit(main())
