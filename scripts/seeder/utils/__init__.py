#!/usr/bin/env python3
"""Utils package exports."""

from seeder.utils.curriculum_selector import (
    extract_admission_year,
    select_student_curriculum,
)
from seeder.utils.faker_instance import fake

__all__ = ["extract_admission_year", "select_student_curriculum", "fake"]
