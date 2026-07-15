#!/usr/bin/env python3
"""Deterministic architecture and runtime-evidence checks for Milestone 6."""

from __future__ import annotations

import json
from pathlib import Path
import re
import zipfile
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
MAVEN = {"m": "http://maven.apache.org/POM/4.0.0"}
NEUTRAL = ("compatibility/zbw-compat-api", "world/zbw-world")
MODERN = (
    "compatibility/zbw-compat-v1_20-v1_21",
    "platform/paper/zbw-paper-modern",
)
EXPECTED_MODULES = {
    "zbw-compat-api": ("compatibility/zbw-compat-api/pom.xml", 8),
    "zbw-world": ("world/zbw-world/pom.xml", 8),
    "zbw-compat-v1_20-v1_21": ("compatibility/zbw-compat-v1_20-v1_21/pom.xml", 21),
    "zbw-paper-modern": ("platform/paper/zbw-paper-modern/pom.xml", 21),
}
FORBIDDEN_NEUTRAL = (
    "org.bukkit", "io.papermc", "net.minecraft", "com.velocitypowered",
    "net.md_5.bungee", "redis.clients", "io.lettuce", "java.sql", "javax.sql",
    "io.zartra.bedwars.storage", "io.zartra.bedwars.paper",
)
LEGACY_PATHS = (
    "compatibility/zbw-compat-v1_8", "compatibility/zbw-compat-v1_9",
    "compatibility/zbw-compat-v1_10", "compatibility/zbw-compat-v1_11",
    "compatibility/zbw-compat-v1_12-v1_16_4", "compatibility/zbw-compat-v1_16_5",
    "compatibility/zbw-compat-v1_17-v1_19", "platform/paper/zbw-paper-legacy",
    "platform/paper/zbw-paper-j11", "platform/paper/zbw-paper-j16",
    "platform/paper/zbw-paper-j17",
)
INCOMPLETE = re.compile(
    r"\b(?:TODO|FIXME)\b|UnsupportedOperationException|\bstub\b|fake implementation",
    re.IGNORECASE,
)


def sources(module: str) -> list[Path]:
    """Return production Java sources in stable order."""
    return sorted((ROOT / module / "src/main/java").rglob("*.java"))


def dependencies(module: str) -> dict[str, str]:
    """Return exact Maven scopes keyed by coordinate."""
    root = ET.parse(ROOT / module / "pom.xml").getroot()
    result: dict[str, str] = {}
    for dependency in root.findall("m:dependencies/m:dependency", MAVEN):
        group = dependency.findtext("m:groupId", default="", namespaces=MAVEN)
        artifact = dependency.findtext("m:artifactId", default="", namespaces=MAVEN)
        scope = dependency.findtext("m:scope", default="compile", namespaces=MAVEN)
        result[f"{group}:{artifact}"] = scope
    return result


def validate() -> list[str]:
    """Return all M06 architecture, boundary, and certification inconsistencies."""
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    valid_states = (
        ("M06", ["M00", "M01", "M02", "M03", "M04", "M05"]),
        (None, ["M00", "M01", "M02", "M03", "M04", "M05", "M06"]),
    )
    if (state["active_milestone"], state["completed_milestones"]) not in valid_states:
        errors.append("milestone state must represent active or completed M06")

    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    materialized = {row["id"]: row for row in graph["materialized_build_modules"]}
    for identifier, (path, bytecode) in EXPECTED_MODULES.items():
        row = materialized.get(identifier)
        if row is None or row["path"] != path:
            errors.append(f"M06 materialized module mismatch: {identifier}")
        if not (ROOT / path).is_file():
            errors.append(f"M06 module descriptor missing: {path}")
        planned = next(
            (item for item in graph["planned_production_modules"] if item["id"] == identifier),
            None,
        )
        if planned is None or planned["bytecode"] != bytecode or planned["first_milestone"] != "M06":
            errors.append(f"M06 planned boundary mismatch: {identifier}")

    for module in NEUTRAL + MODERN:
        production = sources(module)
        if not production:
            errors.append(f"M06 production sources missing: {module}")
            continue
        packages = {path.parent for path in production if path.name != "package-info.java"}
        for package in sorted(packages):
            if not (package / "package-info.java").is_file():
                errors.append(f"M06 package lacks package-info.java: {package.relative_to(ROOT)}")
        for path in production:
            content = path.read_text(encoding="utf-8")
            if INCOMPLETE.search(content):
                errors.append(f"incomplete M06 production marker: {path.relative_to(ROOT)}")

    for module in NEUTRAL:
        pom = (ROOT / module / "pom.xml").read_text(encoding="utf-8")
        if "<maven.compiler.target>21</maven.compiler.target>" in pom:
            errors.append(f"neutral module declares modern bytecode: {module}")
        for path in sources(module):
            content = path.read_text(encoding="utf-8")
            for forbidden in FORBIDDEN_NEUTRAL:
                if forbidden in content:
                    errors.append(
                        f"forbidden neutral dependency {forbidden}: {path.relative_to(ROOT)}")

    for module in MODERN:
        pom = (ROOT / module / "pom.xml").read_text(encoding="utf-8")
        if "<maven.compiler.target>21</maven.compiler.target>" not in pom:
            errors.append(f"modern module lacks Java 21 boundary: {module}")

    paper_dependencies = dependencies("platform/paper/zbw-paper-modern")
    if paper_dependencies.get("io.zartra.mirror.paper:paper-api") != "provided":
        errors.append("Paper API must remain exact, compile-only/provided, and unbundled")
    for path in LEGACY_PATHS:
        if (ROOT / path).exists():
            errors.append(f"M22-only legacy adapter was materialized in M06: {path}")

    required_sources = (
        "compatibility/zbw-compat-api/src/main/java/io/zartra/bedwars/compat/api/CompatibilityAdapter.java",
        "compatibility/zbw-compat-api/src/main/java/io/zartra/bedwars/compat/api/SemanticMappingRegistry.java",
        "world/zbw-world/src/main/java/io/zartra/bedwars/world/api/WorldProvider.java",
        "world/zbw-world/src/main/java/io/zartra/bedwars/world/orchestration/WorldOrchestrator.java",
        "compatibility/zbw-compat-v1_20-v1_21/src/main/java/io/zartra/bedwars/compat/modern/Paper121CompatibilityAdapter.java",
        "platform/paper/zbw-paper-modern/src/main/java/io/zartra/bedwars/paper/world/PaperNativeWorldProvider.java",
        "platform/paper/zbw-paper-modern/src/main/java/io/zartra/bedwars/paper/bootstrap/ZartraBedWarsPlugin.java",
    )
    for relative in required_sources:
        if not (ROOT / relative).is_file():
            errors.append(f"required M06 source missing: {relative}")

    tests = "\n".join(
        path.read_text(encoding="utf-8")
        for module in NEUTRAL + MODERN
        for path in sorted((ROOT / module / "src/test/java").rglob("*Test.java"))
    )
    for token in (
        "SUPPORTED", "UNSUPPORTED", "FALLBACK", "DEGRADED", "LastKnownGood",
        "CLONE", "RESET", "UNLOAD", "cancel", "TIMED_OUT", "rollbackComplete",
        "leakFreeAfterUnload", "Affinity.OWNER", "Affinity.WORKER",
    ):
        if token not in tests:
            errors.append(f"M06 tests lack required evidence token: {token}")

    descriptor = ROOT / "platform/paper/zbw-paper-modern/src/main/resources/plugin.yml"
    if not descriptor.is_file():
        errors.append("M06 primary Paper descriptor is missing")
    else:
        content = descriptor.read_text(encoding="utf-8")
        for token in ("ZartraBedWarsPlugin", "api-version: '1.21'", "load: STARTUP"):
            if token not in content:
                errors.append(f"Paper descriptor lacks exact token: {token}")
        if "commands:" in content:
            errors.append("M06 bootstrap must not expose later-milestone commands")

    evidence_path = ROOT / "build/evidence/m06-paper-primary.json"
    if not evidence_path.is_file():
        errors.append("exact Paper 1.21.1 primary certification evidence is missing")
    else:
        evidence = json.loads(evidence_path.read_text(encoding="utf-8"))
        expected = {
            "runtime": "Paper 1.21.1 build 133",
            "server_sha256": "39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9",
            "operations": 5,
            "filesystem_evidence_off_owner": True,
            "leak_free_after_unload": True,
            "worker_shutdown": True,
            "success": True,
        }
        for key, value in expected.items():
            if evidence.get(key) != value:
                errors.append(f"M06 primary evidence mismatch: {key}")

    artifact = ROOT / "platform/paper/zbw-paper-modern/target/zbw-paper-modern-0.1.0-SNAPSHOT.jar"
    if artifact.is_file():
        with zipfile.ZipFile(artifact) as bundle:
            bundled = set(bundle.namelist())
        prohibited = ("org/bukkit/", "io/papermc/", "net/minecraft/", "net/kyori/")
        for prefix in prohibited:
            if any(name.startswith(prefix) for name in bundled):
                errors.append(f"provided platform API leaked into plugin artifact: {prefix}")

    for relative in (
        "build/api-signature-baseline-m06.txt",
        "build/api-signature-baseline-m06-modern.txt",
        "docs/IMPLEMENTATION_M06.md", "docs/API_M06.md",
        "docs/WORLD_PROVIDER_M06.md", "docs/PAPER_BOOTSTRAP_M06.md",
    ):
        if not (ROOT / relative).is_file():
            errors.append(f"M06 evidence/document missing: {relative}")
    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(
        "M06 architecture PASS: Java 8 neutral contracts, Java 21 primary adapter/bootstrap, "
        "bounded world lifecycle, exact Paper evidence, and no M22 adapter.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
