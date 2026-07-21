"""M10 deterministic scope and inventory regression tests."""

import sys
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/validation"))
import m10_architecture
import m10_inventories

class M10GovernanceTest(unittest.TestCase):
    def test_architecture(self) -> None:
        self.assertEqual([], m10_architecture.validate())

    def test_inventory_totals(self) -> None:
        rows = m10_inventories.inventory()
        self.assertEqual(115, len(rows))
        self.assertEqual(28, sum(str(row["gui_page"]).startswith("zartra:m10/") for row in rows))

    def test_historical_state_requires_ordered_completion(self) -> None:
        completed = [f"M{value:02d}" for value in range(13)]
        self.assertTrue(m10_architecture.is_closed_m10_state({
            "active_milestone": "M13",
            "next_milestone": "M13",
            "completed_milestones": completed,
        }))
        self.assertFalse(m10_architecture.is_closed_m10_state({
            "active_milestone": "M13",
            "next_milestone": "M13",
            "completed_milestones": completed[:9] + completed[10:],
        }))
        self.assertFalse(m10_architecture.is_closed_m10_state({
            "active_milestone": "M12",
            "next_milestone": "M13",
            "completed_milestones": completed,
        }))

if __name__ == "__main__":
    unittest.main()
