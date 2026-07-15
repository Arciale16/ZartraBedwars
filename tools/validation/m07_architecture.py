#!/usr/bin/env python3
"""Deterministic architecture, evidence, and scope validation for Milestone 7."""

from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile


ROOT = Path(__file__).resolve().parents[2]
MODULE = "arena/zbw-arena"
MAVEN = {"m": "http://maven.apache.org/POM/4.0.0"}
FORBIDDEN = (
    "org.bukkit", "io.papermc", "net.minecraft", "com.velocitypowered",
    "net.md_5.bungee", "redis.clients", "io.lettuce", "java.sql", "javax.sql",
    "io.zartra.bedwars.storage", "io.zartra.bedwars.command", "io.zartra.bedwars.ui",
)
INCOMPLETE = re.compile(
    r"\b(?:TODO|FIXME)\b|UnsupportedOperationException|\bstub\b|fake implementation",
    re.IGNORECASE,
)


def dependencies(path: str) -> dict[str, str]:
    """Return exact dependency scopes keyed by artifact ID."""
    root = ET.parse(ROOT / path).getroot()
    result: dict[str, str] = {}
    for dependency in root.findall("m:dependencies/m:dependency", MAVEN):
        artifact = dependency.findtext("m:artifactId", default="", namespaces=MAVEN)
        result[artifact] = dependency.findtext(
            "m:scope", default="compile", namespaces=MAVEN
        )
    return result


def validate() -> list[str]:
    """Return every M07 architecture, scope, and evidence violation."""
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    valid_states = (
        ("M07", ["M00", "M01", "M02", "M03", "M04", "M05", "M06"]),
        (None, ["M00", "M01", "M02", "M03", "M04", "M05", "M06", "M07"]),
    )
    if (state["active_milestone"], state["completed_milestones"]) not in valid_states:
        errors.append("milestone state must represent active or completed M07")

    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    materialized = {row["id"]: row for row in graph["materialized_build_modules"]}
    arena = materialized.get("zbw-arena")
    if arena is None or arena["path"] != "arena/zbw-arena/pom.xml":
        errors.append("zbw-arena must be the sole materialized M07 module")
    for identifier in ("zbw-game", "zbw-command-api", "zbw-command-paper",
                       "zbw-ui-api", "zbw-ui-paper"):
        if identifier in materialized:
            errors.append(f"later milestone module was materialized: {identifier}")
    planned = next(
        (row for row in graph["planned_production_modules"] if row["id"] == "zbw-arena"),
        None,
    )
    expected = ["zbw-api", "zbw-domain", "zbw-application", "zbw-world"]
    if planned is None or planned["bytecode"] != 8 or planned["first_milestone"] != "M07" \
            or planned["depends_on"] != expected:
        errors.append("zbw-arena planned Java 8 dependency boundary drifted")

    scopes = dependencies("arena/zbw-arena/pom.xml")
    production = {artifact for artifact, scope in scopes.items() if scope != "test"}
    if production != set(expected):
        errors.append(f"zbw-arena production dependencies are not exact: {sorted(production)}")
    paper_scopes = dependencies("platform/paper/zbw-paper-modern/pom.xml")
    if paper_scopes.get("zbw-arena") != "test":
        errors.append("Paper M07 certification dependency must remain test-only")

    source_root = ROOT / MODULE / "src/main/java"
    sources = sorted(source_root.rglob("*.java"))
    if not sources:
        errors.append("M07 production sources are missing")
    packages = {path.parent for path in sources if path.name != "package-info.java"}
    for package in sorted(packages):
        if not (package / "package-info.java").is_file():
            errors.append(f"M07 package lacks package-info.java: {package.relative_to(ROOT)}")
    for path in sources:
        content = path.read_text(encoding="utf-8")
        if INCOMPLETE.search(content):
            errors.append(f"incomplete M07 production marker: {path.relative_to(ROOT)}")
        for forbidden in FORBIDDEN:
            if forbidden in content:
                errors.append(f"forbidden M07 dependency {forbidden}: {path.relative_to(ROOT)}")

    required = (
        "application/ArenaApplicationService.java",
        "application/SetupApplicationService.java",
        "application/ArenaArchiveService.java",
        "application/ArenaWorldLifecycleService.java",
        "application/ArenaOperationalView.java",
        "model/ArenaDefinition.java", "model/MapDefinition.java",
        "setup/SetupSession.java", "setup/SetupMutation.java", "setup/SetupPreview.java",
        "validation/ArenaValidation.java", "archive/CanonicalArenaArchiveCodec.java",
        "spi/ArenaRepository.java", "spi/SetupCommitPort.java",
    )
    for relative in required:
        if not (source_root / "io/zartra/bedwars/arena" / relative).is_file():
            errors.append(f"required M07 source missing: {relative}")

    test_text = "\n".join(
        path.read_text(encoding="utf-8")
        for path in sorted((ROOT / MODULE / "src/test/java").rglob("*.java"))
    )
    for token in (
        "M04SqliteArenaContractTest", "concurrent", "cancel", "rollback", "undo", "redo",
        "marker", "duplicate", "restore", "FORBIDDEN", "assertFalse(preview.matches",
    ):
        if token.lower() not in test_text.lower():
            errors.append(f"M07 tests lack required evidence token: {token}")

    evidence_path = ROOT / "build/evidence/m07-paper-primary.json"
    if not evidence_path.is_file():
        errors.append("exact Paper 1.21.1 M07 evidence is missing")
    else:
        evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
        expected_evidence = {
            "runtime": "Paper 1.21.1 build 133",
            "operations": 5,
            "arena_validation": True,
            "setup_undo_redo": True,
            "archive_round_trip": True,
            "filesystem_evidence_off_owner": True,
            "leak_free_after_unload": True,
            "success": True,
        }
        for key, value in expected_evidence.items():
            if evidence.get(key) != value:
                errors.append(f"M07 Paper evidence mismatch: {key}")

    report = ROOT / MODULE / "target/site/jacoco/jacoco.xml"
    if report.is_file():
        counters = {row.attrib["type"]: row.attrib for row in ET.parse(report).getroot()
                    .findall("counter")}
        for counter, minimum in (("LINE", 0.90), ("BRANCH", 0.85)):
            values = counters.get(counter)
            if values is None:
                errors.append(f"M07 JaCoCo report lacks {counter}")
                continue
            covered = int(values["covered"])
            total = covered + int(values["missed"])
            if total == 0 or covered / total < minimum:
                errors.append(f"M07 {counter.lower()} coverage is below {minimum:.0%}")

    paper_artifact = ROOT / "platform/paper/zbw-paper-modern/target/zbw-paper-modern-0.1.0-SNAPSHOT.jar"
    if paper_artifact.is_file():
        with zipfile.ZipFile(paper_artifact) as bundle:
            entries = set(bundle.namelist())
        if any("M07PaperCertificationPlugin" in entry for entry in entries):
            errors.append("test-only M07 Paper harness leaked into the release artifact")
        if any(entry.startswith("io/zartra/bedwars/arena/") for entry in entries):
            errors.append("test-only arena dependency leaked into the Paper release artifact")

    for relative in (
        "build/api-signature-baseline-m07.txt", "docs/IMPLEMENTATION_M07.md",
        "docs/API_M07.md", "docs/ARENA_LIFECYCLE_M07.md",
        "docs/SETUP_LIFECYCLE_M07.md", "docs/ARENA_ARCHIVES_M07.md",
        "docs/ARENA_VALIDATION_M07.md",
    ):
        if not (ROOT / relative).is_file():
            errors.append(f"M07 evidence/document missing: {relative}")
    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(
        "M07 architecture PASS: Java 8 presentation-neutral arena lifecycle; test-only exact "
        "Paper integration; no M08 gameplay or M09 presentation modules."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
