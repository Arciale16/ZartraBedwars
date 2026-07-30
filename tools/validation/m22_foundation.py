#!/usr/bin/env python3
"""Validate the M22 governance, module-boundary and matrix foundation."""

from __future__ import annotations

import json
from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
MAVEN = {"m": "http://maven.apache.org/POM/4.0.0"}
MODULES = {
    "zbw-compat-client": (
        8, ["zbw-compat-api"], "compatibility/zbw-compat-client/pom.xml"),
    "zbw-compat-v1_8": (8, ["zbw-compat-api"], "compatibility/zbw-compat-v1_8/pom.xml"),
    "zbw-compat-v1_9": (8, ["zbw-compat-api"], "compatibility/zbw-compat-v1_9/pom.xml"),
    "zbw-compat-v1_10": (8, ["zbw-compat-api"], "compatibility/zbw-compat-v1_10/pom.xml"),
    "zbw-compat-v1_11": (8, ["zbw-compat-api"], "compatibility/zbw-compat-v1_11/pom.xml"),
    "zbw-compat-v1_12-v1_16_4": (
        11, ["zbw-compat-api"], "compatibility/zbw-compat-v1_12-v1_16_4/pom.xml"),
    "zbw-compat-v1_16_5": (
        16, ["zbw-compat-api"], "compatibility/zbw-compat-v1_16_5/pom.xml"),
    "zbw-compat-v1_17-v1_19": (
        17, ["zbw-compat-api"], "compatibility/zbw-compat-v1_17-v1_19/pom.xml"),
    "zbw-paper-legacy": (
        8, ["zbw-application", "zbw-compat-v1_8", "zbw-compat-v1_9",
            "zbw-compat-v1_10", "zbw-compat-v1_11", "zbw-command-api", "zbw-ui-api"],
        "platform/paper/zbw-paper-legacy/pom.xml"),
    "zbw-paper-j11": (
        11, ["zbw-application", "zbw-compat-v1_12-v1_16_4",
             "zbw-command-api", "zbw-ui-api"],
        "platform/paper/zbw-paper-j11/pom.xml"),
    "zbw-paper-j16": (
        16, ["zbw-application", "zbw-compat-v1_16_5",
             "zbw-command-api", "zbw-ui-api"],
        "platform/paper/zbw-paper-j16/pom.xml"),
    "zbw-paper-j17": (
        17, ["zbw-application", "zbw-compat-v1_17-v1_19",
             "zbw-command-api", "zbw-ui-api"],
        "platform/paper/zbw-paper-j17/pom.xml"),
}
CLIENTS = {
    "NATIVE": (None, None),
    "VIAVERSION": ("ViaVersion", "5.4.2"),
    "VIABACKWARDS": ("ViaBackwards", "5.4.2"),
    "VIAREWIND": ("ViaRewind", "4.0.6"),
    "GEYSER_FLOODGATE": ("Geyser/Floodgate", "2.7.0/2.2.4"),
}
PROVIDERS = {
    "protocollib": ("5.4.0", "GPL-2.0-only"),
    "viaversion": ("5.4.2", "GPL-3.0-only"),
    "viabackwards": ("5.4.2", "GPL-3.0-only"),
    "viarewind": ("4.0.6", "GPL-3.0-only"),
    "geyser": ("2.7.0", "MIT"),
    "floodgate": ("2.2.4", "MIT"),
}


def read_json(relative: str) -> dict[str, object]:
    """Read one UTF-8 JSON governance artifact."""
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def validate() -> list[str]:
    """Return every M22 Phase 1 inconsistency."""
    errors: list[str] = []
    state = read_json("build/milestone-state.json")
    if state["active_milestone"] != "M22" or state["next_milestone"] != "M23":
        errors.append("milestone state must activate M22 and retain M23 as next")

    graph = read_json("build/module-graph.json")
    planned = {row["id"]: row for row in graph["planned_production_modules"]}
    materialized = {row["id"]: row for row in graph["materialized_build_modules"]}
    for identifier, (bytecode, dependencies, path) in MODULES.items():
        row = planned.get(identifier)
        if row is None or (
                row["bytecode"], row["first_milestone"], row["depends_on"]) != (
                bytecode, "M22", dependencies):
            errors.append(f"{identifier}: invalid planned M22 boundary")
        built = materialized.get(identifier)
        if built is None or built["path"] != path or built["packaging"] != "jar":
            errors.append(f"{identifier}: missing POM-only materialization")
            continue
        module_root = ROOT / Path(path).parent
        production_sources = list((module_root / "src/main/java").rglob("*.java"))
        if not production_sources:
            errors.append(f"{identifier}: Phase 2 production sources are missing")
        for source in production_sources:
            text = source.read_text(encoding="utf-8")
            if any(name in text for name in ("org.bukkit", "io.papermc", "net.minecraft")):
                errors.append(f"{identifier}: platform implementation type leaked into {source.name}")
        pom = ET.parse(ROOT / path).getroot()
        artifact = pom.findtext("m:artifactId", namespaces=MAVEN)
        pom_dependencies = [
            node.findtext("m:artifactId", namespaces=MAVEN)
            for node in pom.findall("m:dependencies/m:dependency", MAVEN)
            if node.findtext("m:groupId", namespaces=MAVEN) == "io.zartra.bedwars"
        ]
        if artifact != identifier or pom_dependencies != dependencies:
            errors.append(f"{identifier}: POM dependency boundary differs from graph")

    m22_ids = set(MODULES)
    for row in graph["planned_production_modules"]:
        if row["layer"] in {"api", "domain", "application"}:
            leaked = m22_ids.intersection(row["depends_on"])
            if leaked:
                errors.append(f"{row['id']}: core depends on M22 adapters {sorted(leaked)}")

    matrix = read_json("build/m22-compatibility-matrix.json")
    families = matrix["server_families"]
    clients = matrix["client_paths"]
    if matrix["phase"] != "M22_PHASE_3_CLIENT_TRANSLATION":
        errors.append("M22 matrix must record the Phase 3 client-translation checkpoint")
    if any(row["status"] != "ADAPTER_IMPLEMENTED_CERTIFICATION_PENDING"
           for row in families):
        errors.append("every server family must retain certification-pending status")
    fixtures = read_json("build/private-runtime-fixtures.json")["fixtures"]
    matrix_versions = [
        version for family in families for version in family["fixtures"]
    ]
    fixture_versions = [row["minecraft"] for row in fixtures]
    if matrix_versions != fixture_versions:
        errors.append("M22 matrix fixture order differs from the normative runtime lock")
    if len(families) != 9 or len(clients) != 5 or len(families) * len(clients) != 45:
        errors.append("M22 deterministic matrix must contain 9 x 5 = 45 cells")
    actual_clients = {
        row["id"]: (row["provider"], row["version"]) for row in clients
    }
    if actual_clients != CLIENTS:
        errors.append("M22 client-path inventory/version drift")
    expected_client_status = {
        "NATIVE": "PARITY_IMPLEMENTED_CERTIFICATION_PENDING",
        "VIAVERSION": "ADAPTER_IMPLEMENTED_LOCK_PENDING",
        "VIABACKWARDS": "ADAPTER_IMPLEMENTED_LOCK_PENDING",
        "VIAREWIND": "ADAPTER_IMPLEMENTED_LOCK_PENDING",
        "GEYSER_FLOODGATE": "ADAPTER_IMPLEMENTED_LOCK_PENDING",
    }
    if {row["id"]: row["status"] for row in clients} != expected_client_status:
        errors.append("M22 Phase 3 client adapter status drift")
    if matrix["policy"] != {
            "adapter_implementation_started": True,
            "client_translation_implementation_started": True,
            "provider_resolution_blocked": True,
            "expected_client_feature_count": 10,
            "support_claim_allowed": False,
            "server_runtime_and_client_translation_are_independent": True,
            "unknown_runtime_fails_closed": True,
            "expected_server_family_count": 9,
            "expected_client_path_count": 5,
            "expected_matrix_cell_count": 45,
    }:
        errors.append("M22 Phase 3 no-claim policy drift")

    lock = read_json("build/m22-provider-lock-requirements.json")
    actual_providers = {
        row["id"]: (row["version"], row["spdx_license"])
        for row in lock["providers"]
    }
    if actual_providers != PROVIDERS:
        errors.append("M22 provider selection/version/licence drift")
    if lock["policy"]["resolution_allowed"] or lock["policy"]["maven_dependency_declaration_allowed"]:
        errors.append("M22 provider artifacts must remain blocked before exact locking")
    for row in lock["providers"]:
        if (row["scope"] != "PROVIDED"
                or row["lock_state"] != "REQUIRED_BEFORE_RESOLUTION"
                or row["artifact_sha256"] is not None
                or row["license_text_sha256"] is not None
                or not row["provenance"]):
            errors.append(f"{row['id']}: invalid pre-resolution lock state")

    return errors


def main() -> int:
    """Run the deterministic M22 foundation validator."""
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(
        "M22 Phase 3 PASS: 12 source modules, 22 fixtures, exact fail-closed "
        "server/client adapter boundaries; provider binary resolution remains blocked.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
