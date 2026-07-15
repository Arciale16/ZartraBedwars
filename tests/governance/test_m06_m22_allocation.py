"""Regression tests for the approved M06/M22 planning allocation."""

from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]


def load_validator():
    """Load the allocation validator without repository PYTHONPATH assumptions."""
    path = ROOT / "tools/validation/m06_m22_allocation.py"
    specification = importlib.util.spec_from_file_location("m06_m22_allocation", path)
    if specification is None or specification.loader is None:
        raise RuntimeError("M06/M22 allocation validator cannot be loaded")
    module = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(module)
    return module


class M06M22AllocationTest(unittest.TestCase):
    """Prevent earlier milestones from depending on deferred compatibility work."""

    def test_reconciled_allocation_is_consistent(self) -> None:
        self.assertEqual([], load_validator().validate())


if __name__ == "__main__":
    unittest.main()
