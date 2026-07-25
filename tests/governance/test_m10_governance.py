"""M10 deterministic scope and inventory regression tests."""

import sys
import json
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

    @staticmethod
    def _load_state() -> dict[str, object]:
        return json.loads((ROOT / "build" / "milestone-state.json").read_text(encoding="utf-8"))

    def _ordered_completed(self) -> list[str]:
        completed = [str(item) for item in self._load_state().get("completed_milestones", [])]
        return [f"M{value:02d}" for value in range(len(completed))]

    def test_historical_state_requires_ordered_completion(self) -> None:
        completed = self._ordered_completed()
        next_milestone = f"M{len(completed):02d}"
        expected = {
            "active_milestone": next_milestone,
            "next_milestone": next_milestone,
            "completed_milestones": completed,
        }
        self.assertTrue(m10_architecture.is_closed_m10_state(expected))
        self.assertFalse(m10_architecture.is_closed_m10_state({
            **expected,
            "completed_milestones": completed[:9] + completed[10:],
        }))
        self.assertFalse(m10_architecture.is_closed_m10_state({
            **expected,
            "active_milestone": "M13",
        }))


if __name__ == "__main__":
    unittest.main()
