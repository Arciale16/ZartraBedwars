from __future__ import annotations

import json
from pathlib import Path
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "validation"))
sys.path.insert(0, str(ROOT / "tools" / "dependencies"))

import m02_architecture
import maven_lock


class MilestoneTwoValidationTests(unittest.TestCase):
    def test_architecture_boundaries_are_exact(self) -> None:
        self.assertEqual([], m02_architecture.validate())

    def test_maven_lock_is_complete_and_non_bundled(self) -> None:
        self.assertEqual([], maven_lock.validate_lock())
        lock = json.loads((ROOT / "build" / "maven-dependency-lock.json").read_text(encoding="utf-8"))
        self.assertGreater(len(lock["components"]), 0)
        self.assertTrue(all(not row["product_bundled"] for row in lock["components"]))

    def test_maven_outputs_are_deterministic(self) -> None:
        lock = json.loads((ROOT / "build" / "maven-dependency-lock.json").read_text(encoding="utf-8"))
        self.assertEqual(
            maven_lock.canonical_json(maven_lock.make_sbom(lock)),
            maven_lock.canonical_json(maven_lock.make_sbom(lock)),
        )
        self.assertEqual(maven_lock.make_notices(lock), maven_lock.make_notices(lock))


if __name__ == "__main__":
    unittest.main()
