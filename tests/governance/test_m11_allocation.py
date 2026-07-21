"""Regression tests for reconciled M11 ownership and Phase 1 materialization."""

from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]


def load_validator():
    """Load the allocation validator without repository PYTHONPATH assumptions."""
    path = ROOT / "tools/validation/m11_allocation.py"
    specification = importlib.util.spec_from_file_location("m11_allocation", path)
    if specification is None or specification.loader is None:
        raise RuntimeError("M11 allocation validator cannot be loaded")
    module = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(module)
    return module


class M11AllocationTest(unittest.TestCase):
    """Prevent M11 Phase 1 from absorbing later providers or execution scope."""

    def test_reconciled_allocation_is_consistent(self) -> None:
        self.assertEqual([], load_validator().validate())

    def test_historical_state_accepts_only_ordered_m11_or_later(self) -> None:
        validator = load_validator()
        active_m11 = [f"M{value:02d}" for value in range(11)]
        completed_m12 = [f"M{value:02d}" for value in range(13)]
        self.assertTrue(validator.is_active_or_closed_m11_state({
            "active_milestone": "M11",
            "next_milestone": "M11",
            "completed_milestones": active_m11,
        }))
        self.assertTrue(validator.is_active_or_closed_m11_state({
            "active_milestone": "M13",
            "next_milestone": "M13",
            "completed_milestones": completed_m12,
        }))
        self.assertFalse(validator.is_active_or_closed_m11_state({
            "active_milestone": "M13",
            "next_milestone": "M13",
            "completed_milestones": completed_m12[:11] + completed_m12[12:],
        }))


if __name__ == "__main__":
    unittest.main()
