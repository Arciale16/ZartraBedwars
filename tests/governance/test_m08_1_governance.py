"""Governance regression for the corrective M08.1 scope."""

from __future__ import annotations

from pathlib import Path
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools/validation"))

from m08_1_architecture import validate  # noqa: E402


class M081GovernanceTest(unittest.TestCase):
    """Keep hardening evidence and later-milestone exclusions deterministic."""

    def test_architecture_and_scope(self) -> None:
        """Every M08.1 architecture assertion must remain satisfied."""
        self.assertEqual([], validate())


if __name__ == "__main__":
    unittest.main()
