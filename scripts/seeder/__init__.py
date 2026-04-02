#!/usr/bin/env python3
"""
University Database Seeder package.

A comprehensive database seeding system for university enrollment systems.
Supports both MySQL and Derby databases with realistic fake data generation.
"""

from seeder.core.database import DatabaseManager, DatabaseAdapter
from seeder.core.orchestrator import SeedingOrchestrator
from seeder.cli import main

__version__ = "2.0.0"
__all__ = [
    "DatabaseManager",
    "DatabaseAdapter",
    "SeedingOrchestrator",
    "main",
]
