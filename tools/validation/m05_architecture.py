#!/usr/bin/env python3
"""Deterministic architecture and source-governance checks for Milestone 5."""

from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
MODULES = (
    "api/zbw-api",
    "application/zbw-application",
    "observability/zbw-observability",
)
MAVEN = {"m": "http://maven.apache.org/POM/4.0.0"}
FORBIDDEN = (
    "org.bukkit", "io.papermc", "net.minecraft", "com.velocitypowered",
    "net.md_5.bungee", "redis.clients", "io.lettuce", "java.sql", "javax.sql",
    "io.zartra.bedwars.storage", "java.io", "java.nio.file",
)
INCOMPLETE = re.compile(
    r"\b(?:TODO|FIXME)\b|UnsupportedOperationException|\bstub\b|fake implementation",
    re.IGNORECASE,
)
UNBOUNDED = (
    "Executors.newCachedThreadPool", "Executors.newFixedThreadPool",
    "Executors.newSingleThreadExecutor", "LinkedBlockingQueue",
    "LinkedTransferQueue", "ConcurrentLinkedQueue",
)


def sources(module: str) -> list[Path]:
    """Return production Java sources in stable order."""
    return sorted((ROOT / module / "src/main/java").rglob("*.java"))


def dependencies(module: str) -> dict[str, str]:
    """Return Maven dependency scopes keyed by coordinate."""
    pom = ET.parse(ROOT / module / "pom.xml").getroot()
    result: dict[str, str] = {}
    for dependency in pom.findall("m:dependencies/m:dependency", MAVEN):
        group = dependency.findtext("m:groupId", default="", namespaces=MAVEN)
        artifact = dependency.findtext("m:artifactId", default="", namespaces=MAVEN)
        scope = dependency.findtext("m:scope", default="compile", namespaces=MAVEN)
        result[f"{group}:{artifact}"] = scope
    return result


def validate() -> list[str]:
    """Return all M05 architecture violations."""
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    if state["active_milestone"] != "M05":
        errors.append("active milestone must be M05")
    if state["completed_milestones"] != ["M00", "M01", "M02", "M03", "M04"]:
        errors.append("M05 must start from exactly the completed M00-M04 baseline")

    for module in MODULES:
        root = ROOT / module / "src/main/java"
        if not root.is_dir():
            errors.append(f"missing M05 source root: {module}")
            continue
        packages = {path.parent for path in sources(module) if path.name != "package-info.java"}
        for package in sorted(packages):
            if not (package / "package-info.java").is_file():
                errors.append(f"M05 package lacks package-info.java: {package.relative_to(ROOT)}")
        for path in sources(module):
            content = path.read_text(encoding="utf-8")
            for forbidden in FORBIDDEN:
                if forbidden in content:
                    errors.append(f"forbidden M05 dependency {forbidden}: {path.relative_to(ROOT)}")
            if INCOMPLETE.search(content):
                errors.append(f"incomplete production marker: {path.relative_to(ROOT)}")
            for unbounded in UNBOUNDED:
                if unbounded in content:
                    errors.append(f"unbounded concurrency primitive {unbounded}: {path.relative_to(ROOT)}")
            if re.search(r"\bstatic\s+(?!final\b)[^;=()]+(?:=|;)", content):
                errors.append(f"global mutable state in M05: {path.relative_to(ROOT)}")

    observation_dependencies = dependencies("observability/zbw-observability")
    expected_compile = {
        "io.zartra.bedwars:zbw-api",
        "io.zartra.bedwars:zbw-application",
    }
    actual_compile = {
        coordinate for coordinate, scope in observation_dependencies.items()
        if scope == "compile"
    }
    if actual_compile != expected_compile:
        errors.append(
            f"zbw-observability compile dependencies differ: {sorted(actual_compile)}")
    if observation_dependencies.get("org.junit.jupiter:junit-jupiter") != "test":
        errors.append("zbw-observability JUnit dependency must remain test scoped")

    root_pom = (ROOT / "pom.xml").read_text(encoding="utf-8")
    if "<module>observability/zbw-observability</module>" not in root_pom:
        errors.append("observability module is absent from the reactor")
    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    materialized = {row["id"] for row in graph["materialized_build_modules"]}
    if "zbw-observability" not in materialized:
        errors.append("zbw-observability is absent from the materialized module graph")

    required_sources = (
        "api/zbw-api/src/main/java/io/zartra/bedwars/api/scheduler/SchedulerPort.java",
        "api/zbw-api/src/main/java/io/zartra/bedwars/api/lifecycle/Lifecycle.java",
        "api/zbw-api/src/main/java/io/zartra/bedwars/api/failure/FailureReport.java",
        "api/zbw-api/src/main/java/io/zartra/bedwars/api/health/Health.java",
        "api/zbw-api/src/main/java/io/zartra/bedwars/api/diagnostic/Diagnostics.java",
        "api/zbw-api/src/main/java/io/zartra/bedwars/api/doctor/PluginDoctor.java",
        "api/zbw-api/src/main/java/io/zartra/bedwars/api/recovery/Recovery.java",
        "application/zbw-application/src/main/java/io/zartra/bedwars/application/scheduler/BoundedTaskScheduler.java",
        "application/zbw-application/src/main/java/io/zartra/bedwars/application/lifecycle/LifecycleCoordinator.java",
        "application/zbw-application/src/main/java/io/zartra/bedwars/application/recovery/RecoveryCoordinator.java",
    )
    for relative in required_sources:
        if not (ROOT / relative).is_file():
            errors.append(f"missing M05 source: {relative}")

    scheduler = (ROOT / required_sources[7]).read_text(encoding="utf-8")
    for token in ("ArrayBlockingQueue", "AbortPolicy", "stopAdmission",
                  "shutdownNow", "awaitTermination", "isCancellationRequested"):
        if token not in scheduler:
            errors.append(f"bounded scheduler lacks required behavior token: {token}")

    configuration = (
        ROOT / "configuration/zbw-config/src/main/java/io/zartra/bedwars/config/schema/"
        "ConfigurationModel.java"
    ).read_text(encoding="utf-8")
    for key in (
        "scheduler.worker-count", "scheduler.queue-capacity",
        "scheduler.default-task-timeout", "lifecycle.graceful-drain-budget",
        "lifecycle.force-stop-budget", "observability.maximum-metric-series",
        "observability.maximum-health-sources", "diagnostics.maximum-contributors",
        "diagnostics.maximum-export-fields", "doctor.maximum-checks",
        "doctor.per-check-timeout",
    ):
        if key not in configuration:
            errors.append(f"missing M05 configuration key: {key}")

    test_content = "\n".join(
        path.read_text(encoding="utf-8")
        for module in MODULES
        for path in sorted((ROOT / module / "src/test/java").rglob("*Test.java"))
    )
    for token in (
        "ThreadAccessException", "REJECTED", "TIMEOUT", "cancel",
        "shutdown", "CircuitBreaker", "MANUAL_REQUIRED", "seed-secret",
        "maximum", "UNAVAILABLE",
    ):
        if token not in test_content:
            errors.append(f"M05 tests lack required evidence token: {token}")

    baseline = ROOT / "build/api-signature-baseline-m05.txt"
    if not baseline.is_file() or not baseline.read_text(
            encoding="utf-8").startswith("# ZartraBedWars M05 JVM binary API baseline"):
        errors.append("M05 binary API baseline is missing or malformed")

    required_docs = (
        "docs/IMPLEMENTATION_M05.md", "docs/API_M05.md", "docs/SCHEDULER_M05.md",
        "docs/LIFECYCLE_M05.md", "docs/OBSERVABILITY_M05.md",
        "docs/PLUGIN_DOCTOR_M05.md",
    )
    for relative in required_docs:
        if not (ROOT / relative).is_file():
            errors.append(f"missing M05 documentation: {relative}")

    for later in ("platform", "proxy", "gameplay", "replay", "atlas"):
        if (ROOT / later).exists():
            errors.append(f"post-M05 production path materialized: {later}")
    if list(ROOT.rglob("plugin.yml")) or list(ROOT.rglob("paper-plugin.yml")):
        errors.append("Minecraft runtime descriptor is forbidden in M05")
    return errors


def main() -> int:
    """Run validation and print a stable result."""
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(
        "M05 architecture PASS: bounded scheduler/lifecycle/recovery, "
        "health/metrics/diagnostics/Doctor and platform isolation.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
