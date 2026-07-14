from __future__ import annotations

import json
from pathlib import Path
import tempfile
import unittest

import sys


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "validation"))
sys.path.insert(0, str(ROOT / "tools" / "dependencies"))
sys.path.insert(0, str(ROOT / "tools" / "build"))

import foundation
import lock_dependencies
import maven_wrapper


class FoundationValidationTests(unittest.TestCase):
    def test_planned_module_graph_is_acyclic(self) -> None:
        self.assertEqual([], foundation.validate_module_graph())

    def test_cycle_detector_reports_a_closed_path(self) -> None:
        cycles = foundation.graph_cycles({"a": ["b"], "b": ["c"], "c": ["a"]})
        self.assertEqual([["a", "b", "c", "a"]], cycles)

    def test_class_major_reads_big_endian_header(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "Example.class"
            path.write_bytes(b"\xca\xfe\xba\xbe\x00\x00\x00\x41")
            self.assertEqual(65, foundation.class_major(path))

    def test_fixture_inventory_is_exact_and_uncertified(self) -> None:
        self.assertEqual([], foundation.validate_fixtures())

    def test_empty_asset_inventory_matches_repository(self) -> None:
        self.assertEqual([], foundation.validate_assets())

    def test_quality_and_bytecode_policies_are_valid(self) -> None:
        self.assertEqual([], foundation.validate_quality())
        self.assertEqual([], foundation.validate_bytecode())

    def test_sbom_generation_is_deterministic(self) -> None:
        lock = json.loads((ROOT / "build" / "dependency-lock.json").read_text(encoding="utf-8"))
        first = lock_dependencies.canonical_json(lock_dependencies.make_sbom(lock))
        second = lock_dependencies.canonical_json(lock_dependencies.make_sbom(lock))
        self.assertEqual(first, second)

    def test_jdk_assembly_exception_evidence_is_required(self) -> None:
        acquisition = json.loads((ROOT / "build" / "dependency-acquisition.json").read_text(encoding="utf-8"))
        lock = json.loads((ROOT / "build" / "dependency-lock.json").read_text(encoding="utf-8"))
        jdk = next(artifact for artifact in lock["artifacts"] if "assembly_exception_sha256" in artifact["license"])
        del jdk["license"]["observed_assembly_exception_sha256"]
        errors = lock_dependencies.validate_schema(acquisition, lock)
        self.assertIn(f"{jdk['id']}: observed assembly-exception hash missing or mismatched", errors)

    def test_direct_plugin_goal_is_default_denied(self) -> None:
        with self.assertRaises(SystemExit):
            maven_wrapper.validate_arguments(["compiler:compile"])


if __name__ == "__main__":
    unittest.main()
