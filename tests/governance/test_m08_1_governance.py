"""Governance regression for the corrective M08.1 scope."""

from __future__ import annotations

from pathlib import Path
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/validation"))

import api_compatibility  # noqa: E402
from m08_1_architecture import validate  # noqa: E402


class M081GovernanceTest(unittest.TestCase):
    """Keep hardening evidence and later-milestone exclusions deterministic."""

    def test_architecture_and_scope(self) -> None:
        """Every M08.1 architecture assertion must remain satisfied."""
        self.assertEqual([], validate())

    def test_historical_api_baseline_allows_only_additive_evolution(self) -> None:
        """An immutable old baseline permits additions but detects removals."""
        baseline = "# old\nCLASS example.Contract\nMETHOD example.Contract value ()I\n"
        additive = baseline + "CLASS example.Addition\n"
        removal = "# current\nCLASS example.Contract\n"
        self.assertEqual([], api_compatibility.missing_signatures(baseline, additive))
        self.assertEqual(
            ["METHOD example.Contract value ()I"],
            api_compatibility.missing_signatures(baseline, removal))


if __name__ == "__main__":
    unittest.main()
