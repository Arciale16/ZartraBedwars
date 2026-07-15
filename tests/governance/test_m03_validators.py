from __future__ import annotations

import json
from pathlib import Path
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "validation"))

import m03_architecture


class MilestoneThreeValidationTests(unittest.TestCase):
    def test_architecture_boundaries_are_exact(self) -> None:
        self.assertEqual([], m03_architecture.validate())

    def test_config_module_is_materialized_without_m04_modules(self) -> None:
        graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
        materialized = {row["id"] for row in graph["materialized_build_modules"]}
        self.assertIn("zbw-config", materialized)
        self.assertNotIn("zbw-storage-api", materialized)
        self.assertNotIn("zbw-storage-sql", materialized)

    def test_m03_has_no_runtime_dependency_additions(self) -> None:
        pom = (ROOT / "configuration/zbw-config/pom.xml").read_text(encoding="utf-8")
        self.assertNotIn("snakeyaml", pom)
        self.assertNotIn("jackson", pom)
        self.assertNotIn("bukkit", pom.lower())


if __name__ == "__main__":
    unittest.main()
