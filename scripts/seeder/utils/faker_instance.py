#!/usr/bin/env python3
"""Shared Faker instance for generating realistic fake data."""

from faker import Faker

# Shared Faker instance configured for Philippines locale
fake: Faker = Faker("en_PH")
