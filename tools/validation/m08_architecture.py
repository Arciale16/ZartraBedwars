#!/usr/bin/env python3
"""Deterministic architecture, evidence and scope validation for M08."""

from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[2]
FORBIDDEN = (
    "org.bukkit", "io.papermc", "net.minecraft", "com.velocitypowered",
    "net.md_5.bungee", "redis.clients", "io.lettuce", "java.sql", "javax.sql",
    "io.zartra.bedwars.command", "io.zartra.bedwars.ui",
)
INCOMPLETE = re.compile(
    r"\b(?:TODO|FIXME)\b|UnsupportedOperationException|\bstub\b|fake implementation",
    re.IGNORECASE,
)


def validate() -> list[str]:
    """Return every M08 architecture, scope and evidence violation."""
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    completed = [f"M{value:02d}" for value in range(0, 8)]
    valid = (("M08", completed), (None, completed + ["M08"]))
    if (state["active_milestone"], state["completed_milestones"]) not in valid:
        errors.append("milestone state must represent active or completed M08")
    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    materialized = {row["id"] for row in graph["materialized_build_modules"]}
    if "zbw-game" not in materialized:
        errors.append("zbw-game must be materialized in M08")
    later = {"zbw-command-api", "zbw-command-paper", "zbw-ui-api", "zbw-ui-paper"}
    if materialized.intersection(later):
        errors.append("M09 presentation modules must remain unmaterialized")
    source_root = ROOT / "game/zbw-game/src/main/java"
    sources = sorted(source_root.rglob("*.java"))
    if not sources:
        errors.append("M08 production sources are missing")
    for package in {path.parent for path in sources if path.name != "package-info.java"}:
        if not (package / "package-info.java").is_file():
            errors.append(f"M08 package lacks package-info.java: {package.relative_to(ROOT)}")
    for path in sources:
        content = path.read_text(encoding="utf-8")
        if INCOMPLETE.search(content):
            errors.append(f"incomplete M08 production marker: {path.relative_to(ROOT)}")
        for forbidden in FORBIDDEN:
            if forbidden in content:
                errors.append(f"forbidden M08 dependency {forbidden}: {path.relative_to(ROOT)}")
    evidence_path = ROOT / "build/evidence/m08-paper-primary.json"
    expected = {
        "runtime": "Paper 1.21.1 build 133", "waiting_through_reset": True,
        "reconnect_recovery": True, "exactly_once_completion": True,
        "player_state_restoration": True, "bossbar_create_update_remove": True,
        "event_runtime_register_unregister": True,
        "off_owner_mutation_rejected": True, "success": True,
    }
    if not evidence_path.is_file():
        errors.append("exact Paper 1.21.1 M08 evidence is missing")
    else:
        evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
        for key, value in expected.items():
            if evidence.get(key) != value:
                errors.append(f"M08 Paper evidence mismatch: {key}")
    report = ROOT / "game/zbw-game/target/site/jacoco/jacoco.xml"
    if report.is_file():
        counters = {row.attrib["type"]: row.attrib for row in ET.parse(report).getroot().findall("counter")}
        for counter, minimum in (("LINE", 0.90), ("BRANCH", 0.85)):
            values = counters.get(counter)
            covered = int(values["covered"]) if values else 0
            total = covered + int(values["missed"]) if values else 0
            if total == 0 or covered / total < minimum:
                errors.append(f"M08 {counter.lower()} coverage is below {minimum:.0%}")
    artifact = ROOT / "platform/paper/zbw-paper-modern/target/zbw-paper-modern-0.1.0-SNAPSHOT.jar"
    if artifact.is_file():
        with zipfile.ZipFile(artifact) as bundle:
            if any("M08PaperCertificationPlugin" in entry for entry in bundle.namelist()):
                errors.append("test-only M08 harness leaked into the release artifact")
    required = (
        "build/api-signature-baseline-m08.txt", "build/api-signature-baseline-m08-modern.txt",
        "docs/IMPLEMENTATION_M08.md",
        "docs/API_M08.md", "docs/GAME_ENGINE_M08.md", "docs/SESSION_RECOVERY_M08.md",
        "docs/PLAYER_STATE_RESTORATION_M08.md", "docs/PAPER_PROJECTIONS_M08.md",
    )
    for relative in required:
        if not (ROOT / relative).is_file():
            errors.append(f"M08 evidence/document missing: {relative}")
    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("M08 architecture PASS: Java 8 game core; exact primary Paper projections; no M09/M10.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
