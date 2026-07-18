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


if __name__ == "__main__":
    unittest.main()
