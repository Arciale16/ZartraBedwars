"""Regression tests for Milestone 5 deterministic governance."""

from __future__ import annotations

import importlib.util
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]


def load_validator():
    """Load the validator without relying on repository PYTHONPATH state."""
    path = ROOT / "tools/validation/m05_architecture.py"
    specification = importlib.util.spec_from_file_location("m05_architecture", path)
    if specification is None or specification.loader is None:
        raise RuntimeError("M05 validator cannot be loaded")
    module = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(module)
    return module


class M05GovernanceTest(unittest.TestCase):
    """Keep M05 architecture and compatibility evidence deterministic."""

    def test_architecture_validator_passes(self) -> None:
        self.assertEqual([], load_validator().validate())

    def test_binary_baseline_is_additive(self) -> None:
        current = (ROOT / "build/api-signature-baseline-m05.txt").read_text(
            encoding="utf-8").splitlines()
        self.assertEqual(
            ["# ZartraBedWars M05 JVM binary API baseline", "# class-major=52"],
            current[:2],
        )
        current_rows = {line for line in current if line and not line.startswith("#")}
        for previous_name in (
                "api-signature-baseline.txt",
                "api-signature-baseline-m03.txt",
                "api-signature-baseline-m04.txt"):
            previous = (ROOT / "build" / previous_name).read_text(
                encoding="utf-8").splitlines()
            previous_rows = {
                line for line in previous if line and not line.startswith("#")
            }
            self.assertTrue(previous_rows.issubset(current_rows), previous_name)

    def test_observability_module_has_no_platform_or_storage_dependencies(self) -> None:
        content = "\n".join(
            path.read_text(encoding="utf-8")
            for path in sorted((ROOT / "observability/zbw-observability").rglob("*.java"))
        )
        for forbidden in (
                "org.bukkit", "io.papermc", "net.minecraft", "java.sql",
                "io.zartra.bedwars.storage", "redis.clients", "io.lettuce"):
            self.assertNotIn(forbidden, content)


if __name__ == "__main__":
    unittest.main()
