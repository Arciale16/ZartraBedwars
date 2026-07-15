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

    def test_storage_modules_are_materialized_without_m05(self) -> None:
        graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
        materialized = {row["id"] for row in graph["materialized_build_modules"]}
        self.assertIn("zbw-storage-api", materialized)
        self.assertIn("zbw-storage-sql", materialized)
        self.assertNotIn("zbw-observability", materialized)

    def test_m04_dependencies_are_locked_and_never_bundled(self) -> None:
        lock = json.loads((ROOT / "build/maven-dependency-lock.json").read_text(encoding="utf-8"))
        identifiers = {row["id"] for row in lock["components"]}
        self.assertIn("maven:com.zaxxer:HikariCP:4.0.3", identifiers)
        self.assertIn("maven:org.flywaydb:flyway-core:10.20.1", identifiers)
        self.assertFalse(any(row["product_bundled"] for row in lock["components"]))

    def test_external_database_images_are_digest_gated(self) -> None:
        source = (ROOT / "storage/zbw-storage-sql/src/test/java/io/zartra/bedwars/storage/sql/ExternalSqlStorageContractTest.java").read_text(encoding="utf-8")
        self.assertIn("@sha256:", source)
        self.assertIn("ZBW_TEST_MYSQL_IMAGE", source)
        self.assertIn("ZBW_TEST_MARIADB_IMAGE", source)


if __name__ == "__main__":
    unittest.main()
