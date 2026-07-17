from __future__ import annotations

import json
from pathlib import Path
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "validation"))

import m04_architecture


class MilestoneFourValidationTests(unittest.TestCase):
    def test_architecture_boundaries_are_exact(self) -> None:
        self.assertEqual([], m04_architecture.validate())

    def test_storage_modules_remain_materialized_through_m05_closure(self) -> None:
        graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
        materialized = {row["id"] for row in graph["materialized_build_modules"]}
        self.assertIn("zbw-storage-api", materialized)
        self.assertIn("zbw-storage-sql", materialized)
        state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
        if state["active_milestone"] == "M04":
            self.assertNotIn("zbw-observability", materialized)
        else:
            self.assertIn(
                state["active_milestone"],
                {f"M{value:02d}" for value in range(5, 25)} | {None},
            )
            self.assertIn("zbw-observability", materialized)

    def test_m04_dependencies_are_locked_and_never_bundled(self) -> None:
        lock = json.loads((ROOT / "build/maven-dependency-lock.json").read_text(encoding="utf-8"))
        identifiers = {row["id"] for row in lock["components"]}
        self.assertIn("maven:com.zaxxer:HikariCP:4.0.3", identifiers)
        self.assertIn("maven:org.flywaydb:flyway-core:10.20.1", identifiers)
        self.assertFalse(any(row["product_bundled"] for row in lock["components"]))

    def test_external_database_images_are_digest_gated(self) -> None:
        source = (ROOT / "storage/zbw-storage-sql/src/test/java/io/zartra/bedwars/storage/sql/ExternalSqlStorageContractTest.java").read_text(encoding="utf-8")
        image_lock = json.loads(
            (ROOT / "build/m04-database-container-lock.json").read_text(encoding="utf-8"))
        references = {row["engine"]: row["reference"] for row in image_lock["images"]}
        self.assertEqual({"mysql", "mariadb"}, set(references))
        self.assertTrue(all("://" not in reference and "@sha256:" in reference
                            for reference in references.values()))
        self.assertIn("ZBW_TEST_DATABASE_IMAGE", source)
        self.assertIn("ZBW_REQUIRE_EXTERNAL_DATABASES", source)

    def test_m04_binary_api_baseline_is_cross_platform_canonical(self) -> None:
        baseline = (ROOT / "build/api-signature-baseline-m04.txt").read_text(
            encoding="utf-8").splitlines()
        self.assertEqual([
            "# ZartraBedWars M04 JVM binary API baseline", "# class-major=52"],
            baseline[:2])
        self.assertEqual(baseline[2:], sorted(baseline[2:]))


if __name__ == "__main__":
    unittest.main()
