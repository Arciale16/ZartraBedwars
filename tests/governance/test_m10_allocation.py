from __future__ import annotations

from pathlib import Path
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "validation"))

import m10_allocation


class MilestoneTenAllocationTests(unittest.TestCase):
    def test_framework_and_later_mechanics_are_reconciled(self) -> None:
        self.assertEqual([], m10_allocation.validate())


if __name__ == "__main__":
    unittest.main()
