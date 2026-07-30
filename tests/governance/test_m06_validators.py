"""Regression tests for Milestone 6 deterministic governance."""

from __future__ import annotations

import importlib.util
import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]


def load_validator():
    """Load the validator without relying on repository PYTHONPATH state."""
    path = ROOT / "tools/validation/m06_architecture.py"
    specification = importlib.util.spec_from_file_location("m06_architecture", path)
    if specification is None or specification.loader is None:
        raise RuntimeError("M06 validator cannot be loaded")
    module = importlib.util.module_from_spec(specification)
    specification.loader.exec_module(module)
    return module


class M06GovernanceTest(unittest.TestCase):
    """Keep M06 runtime boundaries and certification evidence deterministic."""

    def test_architecture_validator_passes(self) -> None:
        self.assertEqual([], load_validator().validate())

    def test_m22_legacy_boundaries_are_isolated(self) -> None:
        state = json.loads(
            (ROOT / "build/milestone-state.json").read_text(encoding="utf-8")
        )
        for relative in load_validator().LEGACY_PATHS:
            module = ROOT / relative
            self.assertTrue((module / "pom.xml").is_file())
            sources = list(module.rglob("*.java"))
            if state["active_milestone"] < "M22":
                self.assertEqual([], sources)
            else:
                self.assertTrue(sources)
                for source in sources:
                    content = source.read_text(encoding="utf-8")
                    self.assertNotIn("org.bukkit", content)
                    self.assertNotIn("io.papermc", content)
                    self.assertNotIn("net.minecraft", content)

    def test_primary_runtime_lock_is_exact(self) -> None:
        content = (ROOT / "build/m06-paper-runtime-lock.json").read_text(encoding="utf-8")
        self.assertIn('"minecraft_version": "1.21.1"', content)
        self.assertIn('"build": 133', content)
        self.assertIn("39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9", content)


if __name__ == "__main__":
    unittest.main()
