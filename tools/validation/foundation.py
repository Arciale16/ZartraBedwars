#!/usr/bin/env python3
"""Deterministic structural checks for the accepted M1 foundation."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
import re
import struct
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
EXCLUDED_PARTS = {".git", ".tools", ".m2", "target"}
MAVEN_NAMESPACE = {"m": "http://maven.apache.org/POM/4.0.0"}
MASTER_PROMPT_SHA256 = "afce1250079945a7543f027bb23df14cedee7913ac52f8cb0775da784b280afa"


def read_json(relative: str) -> dict[str, object]:
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def graph_cycles(nodes: dict[str, list[str]]) -> list[list[str]]:
    cycles: list[list[str]] = []
    active: list[str] = []
    visited: set[str] = set()

    def visit(node: str) -> None:
        if node in active:
            start = active.index(node)
            cycles.append(active[start:] + [node])
            return
        if node in visited:
            return
        active.append(node)
        for dependency in nodes.get(node, []):
            visit(dependency)
        active.pop()
        visited.add(node)

    for node in sorted(nodes):
        visit(node)
    return cycles


def milestone_number(value: object) -> int:
    """Return the numeric milestone while rejecting malformed graph metadata."""
    match = re.fullmatch(r"M([0-9]{2})", str(value))
    if match is None:
        raise ValueError(f"invalid milestone identifier: {value}")
    return int(match.group(1))


def class_major(path: Path) -> int:
    with path.open("rb") as stream:
        header = stream.read(8)
    if len(header) != 8 or header[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError(f"invalid class file: {path}")
    return struct.unpack(">H", header[6:8])[0]


def validate_module_graph() -> list[str]:
    errors: list[str] = []
    graph = read_json("build/module-graph.json")
    materialized = graph["materialized_build_modules"]
    planned = graph["planned_production_modules"]
    materialized_ids = [str(row["id"]) for row in materialized]
    planned_ids = [str(row["id"]) for row in planned]
    if len(set(materialized_ids)) != len(materialized_ids):
        errors.append("duplicate materialized module ID")
    if len(set(planned_ids)) != len(planned_ids):
        errors.append("duplicate planned production module ID")
    for row in materialized:
        path = ROOT / str(row["path"])
        if not path.is_file():
            errors.append(f"missing materialized build module descriptor: {row['path']}")
            continue
        if path.name == "pom.xml":
            packaging = ET.parse(path).getroot().findtext("m:packaging", default="jar", namespaces=MAVEN_NAMESPACE)
            if packaging != row["packaging"]:
                errors.append(f"{row['id']}: packaging differs from module graph")
    root_pom = ET.parse(ROOT / "pom.xml").getroot()
    reactor_modules = [value.text for value in root_pom.findall("m:modules/m:module", MAVEN_NAMESPACE)]
    expected_reactor = [
        str(row["path"]).removesuffix("/pom.xml")
        for row in materialized
        if row["id"] != "zartrabedwars-parent"
    ]
    if reactor_modules != expected_reactor:
        errors.append("reactor module order differs from the materialized module graph")
    planned_lookup = {str(row["id"]): row for row in planned}
    dependencies: dict[str, list[str]] = {}
    for identifier, row in planned_lookup.items():
        dependencies[identifier] = [str(value) for value in row["depends_on"]]
        for dependency in dependencies[identifier]:
            if dependency not in planned_lookup:
                errors.append(f"{identifier}: unknown planned dependency {dependency}")
                continue
            try:
                module_milestone = milestone_number(row["first_milestone"])
                dependency_milestone = milestone_number(
                    planned_lookup[dependency]["first_milestone"])
            except ValueError as error:
                errors.append(str(error))
                continue
            if dependency_milestone > module_milestone:
                errors.append(
                    f"{identifier}: {row['first_milestone']} depends on later "
                    f"{dependency} ({planned_lookup[dependency]['first_milestone']})")
        if row["layer"] in {"api", "domain", "application"} and "platform" in identifier:
            errors.append(f"{identifier}: platform naming leaked into a core layer")
    for cycle in graph_cycles(dependencies):
        errors.append("planned module cycle: " + " -> ".join(cycle))
    state = read_json("build/milestone-state.json")
    java_files = [path for path in ROOT.rglob("*.java") if not any(part in EXCLUDED_PARTS for part in path.parts)]
    if state["active_milestone"] == "M01" and java_files:
        errors.append("M1 may not contain Java implementation: " + ", ".join(str(path.relative_to(ROOT)) for path in java_files))
    if int(graph["policy"]["functional_modules_materialized_in_m01"]) != 0:
        errors.append("M1 functional module count must remain zero")
    return errors


def validate_bytecode() -> list[str]:
    errors: list[str] = []
    allowed = {str(row["id"]): int(row["class_major"]) for row in read_json("build/toolchains.json")["jdks"]}
    if allowed != {"8": 52, "11": 55, "16": 60, "17": 61, "21": 65}:
        errors.append("class-major toolchain mapping drift")
    for path in ROOT.rglob("*.class"):
        if any(part in {".git", ".tools", ".m2"} for part in path.parts):
            continue
        try:
            major = class_major(path)
        except ValueError as error:
            errors.append(str(error))
            continue
        if major not in allowed.values():
            errors.append(f"unsupported class major {major}: {path.relative_to(ROOT)}")
    return errors


def validate_fixtures() -> list[str]:
    errors: list[str] = []
    manifest = read_json("build/private-runtime-fixtures.json")
    fixtures = manifest["fixtures"]
    expected_versions = {
        "1.8.8", "1.9.4", "1.10.2", "1.11.2", "1.12.2", "1.13.2", "1.14.4", "1.15.2",
        "1.16.5", "1.17.1", "1.18.2", "1.19.4", "1.20.1", "1.20.2", "1.20.4", "1.20.6",
        "1.21.1", "1.21.3", "1.21.4", "1.21.8", "1.21.10", "1.21.11",
    }
    versions = {str(row["minecraft"]) for row in fixtures}
    if versions != expected_versions:
        errors.append("private runtime fixture version inventory drift")
    for row in fixtures:
        if row["certification"] != "NOT_STARTED":
            errors.append(f"{row['minecraft']}: M1 cannot claim runtime certification")
        if row["distribution"] == "PAPER" and not re.fullmatch(r"[0-9a-f]{64}", str(row.get("sha256", ""))):
            errors.append(f"{row['minecraft']}: Paper fixture lacks exact SHA-256")
        if row["distribution"] == "PRIVATE_BUILDTOOLS" and row.get("sha256_state") != "PRIVATE_LOCK_REQUIRED":
            errors.append(f"{row['minecraft']}: legacy private fixture lock state missing")
    prohibited_names = re.compile(r"(?i)(paper|spigot|bukkit|server).+\.jar$")
    for path in ROOT.rglob("*.jar"):
        if any(part in EXCLUDED_PARTS for part in path.parts):
            continue
        if prohibited_names.search(path.name):
            errors.append(f"server binary prohibited in repository: {path.relative_to(ROOT)}")
    return errors


def validate_assets() -> list[str]:
    errors: list[str] = []
    manifest = read_json("build/asset-provenance.json")
    approved = {str(row["path"]): row for row in manifest["approved_assets"]}
    discovered: dict[str, str] = {}
    asset_suffixes = {".png", ".jpg", ".jpeg", ".gif", ".webp", ".ogg", ".wav", ".mp3", ".ttf", ".otf", ".bbmodel", ".schem", ".schematic"}
    for path in ROOT.rglob("*"):
        if not path.is_file() or any(part in EXCLUDED_PARTS for part in path.parts):
            continue
        if "src" in path.parts and "main" in path.parts and "resources" in path.parts and path.suffix.lower() in asset_suffixes:
            relative = path.relative_to(ROOT).as_posix()
            discovered[relative] = hashlib.sha256(path.read_bytes()).hexdigest()
    if set(discovered) != set(approved):
        errors.append("production asset inventory does not match approved provenance rows")
    for path, digest in discovered.items():
        if approved[path].get("sha256") != digest or approved[path].get("status") != "APPROVED":
            errors.append(f"asset lacks matching approved hash/status: {path}")
    return errors


def validate_quality() -> list[str]:
    errors: list[str] = []
    policy = read_json("build/quality-policy.json")
    if policy["coverage"]["domain_application"] != {"line_percent": 90, "branch_percent": 85}:
        errors.append("domain/application coverage thresholds drifted")
    if policy["coverage"]["adapter_ui_integration"] != {"line_percent": 80, "branch_percent": 70}:
        errors.append("adapter coverage thresholds drifted")
    for required in ("config/checkstyle/checkstyle.xml", "config/spotbugs/exclude.xml"):
        if not (ROOT / required).is_file():
            errors.append(f"missing static-analysis configuration: {required}")
    marker_pattern = re.compile(r"\b(?:TODO|FIXME)\b|\bstub\b|fake provider|mock-only", re.IGNORECASE)
    for path in ROOT.rglob("*.java"):
        if any(part in EXCLUDED_PARTS for part in path.parts):
            continue
        for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
            if marker_pattern.search(line):
                errors.append(f"forbidden production marker at {path.relative_to(ROOT)}:{line_number}")
    return errors


def validate_ci() -> list[str]:
    errors: list[str] = []
    workflows = sorted((ROOT / ".github" / "workflows").glob("*.yml"))
    if len(workflows) < 2:
        errors.append("M1 requires separate governance and toolchain-matrix workflows")
    allowed_actions = {
        "actions/checkout@34e114876b0b11c390a56381ad16ebd13914f8d5",
        "actions/setup-python@a26af69be951a213d495a4c3e4e4022e16d87065",
        "actions/upload-artifact@ea165f8d65b6e75b540449e92b4886f43607fa02",
    }
    found_actions: set[str] = set()
    combined = ""
    for workflow in workflows:
        text = workflow.read_text(encoding="utf-8")
        combined += text
        for action in re.findall(r"\buses:\s*([^\s#]+)", text):
            found_actions.add(action)
            if action not in allowed_actions:
                errors.append(f"unapproved or unpinned action in {workflow.name}: {action}")
        if re.search(r"\brun:\s*mvn\b", text):
            errors.append(f"direct Maven invocation bypasses wrapper in {workflow.name}")
    if found_actions != allowed_actions:
        errors.append("CI action inventory differs from locked action set")
    for token in ("3.12.13", "ubuntu-22.04", "'8'", "'11'", "'16'", "'17'", "'21'"):
        if token not in combined:
            errors.append(f"CI matrix is missing pinned value {token}")
    return errors


def validate_authoritative_source() -> list[str]:
    errors: list[str] = []
    source_hash = hashlib.sha256((ROOT / "MASTER_PROMPT.md").read_bytes()).hexdigest()
    if source_hash != MASTER_PROMPT_SHA256:
        errors.append(f"MASTER_PROMPT.md byte hash drift: {source_hash}")
    attributes = (ROOT / ".gitattributes").read_text(encoding="utf-8").splitlines()
    if "/MASTER_PROMPT.md -text !eol" not in attributes:
        errors.append("MASTER_PROMPT.md must bypass cross-platform line-ending conversion")
    return errors


def validate_all() -> list[str]:
    return (
        validate_module_graph()
        + validate_bytecode()
        + validate_fixtures()
        + validate_assets()
        + validate_quality()
        + validate_ci()
        + validate_authoritative_source()
    )
