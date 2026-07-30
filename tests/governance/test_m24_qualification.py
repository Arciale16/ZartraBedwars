from __future__ import annotations

import json
from pathlib import Path
import sys
import unittest


ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / "tools" / "validation"))

import m24_qualification


class M24QualificationTests(unittest.TestCase):
    """Regression coverage for ZBW-QA-001..007 and release no-claim rules."""

    def test_repository_qualification_is_consistent(self) -> None:
        self.assertEqual([], m24_qualification.validate_all())

    def test_permission_policy_is_namespace_scoped(self) -> None:
        self.assertTrue(m24_qualification.canonical_permission(
            "zartrabedwars.replay.staff"))
        self.assertFalse(m24_qualification.canonical_permission("replay.staff"))
        self.assertFalse(m24_qualification.canonical_permission(
            "zartrabedwars.replay.*"))

    def test_manifest_never_claims_external_results(self) -> None:
        manifest = json.loads(
            (ROOT / "build/m24-qualification.json").read_text(encoding="utf-8"))
        self.assertFalse(manifest["release_ready"])
        self.assertFalse(manifest["support_claim_allowed"])
        self.assertTrue(all(
            profile["status"] == "PENDING_NORMATIVE_ENVIRONMENT"
            and "observed" not in profile
            for profile in manifest["performance"]["profiles"]))

    def test_artifact_report_comparison_is_exact(self) -> None:
        report = {"schema_version": 1, "artifacts": [
            {"module": "a", "path": "a.jar", "sha256": "0" * 64, "size": 1}]}
        self.assertEqual(
            [], m24_qualification.compare_artifact_reports(report, dict(report)))
        changed = json.loads(json.dumps(report))
        changed["artifacts"][0]["sha256"] = "1" * 64
        self.assertEqual(
            ["artifact checksum reports differ"],
            m24_qualification.compare_artifact_reports(report, changed))

    def test_artifact_inventory_excludes_shade_intermediates(self) -> None:
        paths = {
            artifact["path"]
            for artifact in m24_qualification.artifact_inventory()["artifacts"]
        }
        self.assertFalse(any(path.endswith("-shaded.jar") for path in paths))
        self.assertFalse(any("/original-" in path for path in paths))
    def test_external_gate_order_is_stable(self) -> None:
        manifest = json.loads(
            (ROOT / "build/m24-qualification.json").read_text(encoding="utf-8"))
        self.assertEqual(
            m24_qualification.EXTERNAL_GATES,
            tuple(row["id"] for row in manifest["external_gates"]))


if __name__ == "__main__":
    unittest.main()
