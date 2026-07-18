#!/usr/bin/env python3
"""Validate reconciled M11 ownership and the Phase 1 materialization boundary."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
M11_MODULES = {
    "zbw-scripting-api": (8, "M11", ["zbw-api"]),
    "zbw-scripting-engine": (
        8, "M11", ["zbw-api", "zbw-application", "zbw-scripting-api"]),
    "zbw-shop": (
        8, "M11", ["zbw-api", "zbw-domain", "zbw-application", "zbw-storage-api",
                    "zbw-compat-api", "zbw-arena", "zbw-game", "zbw-scripting-api"]),
    "zbw-content": (
        8, "M11", ["zbw-api", "zbw-domain", "zbw-application", "zbw-shop"]),
}
PHASE_1_MODULES = {"zbw-scripting-api", "zbw-shop", "zbw-content"}
STATISTICS_IDS = {
    "ZBW-ADDON-010", "ZBW-ADDON-061", "ZBW-ADDON-300",
    "ZBW-ADDON-315", "ZBW-ADDON-341", "ZBW-ADDON-438",
}
SWAPPAGE_IDS = {f"ZBW-ADDON-{value:03d}" for value in range(236, 245)}
M11_ONLY_RANGES = (
    (10, 25), (61, 70), (141, 147), (184, 201), (300, 322),
    (341, 349), (363, 368), (379, 397), (438, 452),
)


def read(relative: str) -> str:
    """Read a normative UTF-8 repository file."""
    return (ROOT / relative).read_text(encoding="utf-8")


def require(text: str, token: str, location: str, errors: list[str]) -> None:
    """Record a missing normative assertion."""
    if token not in text:
        errors.append(f"{location}: missing M11 allocation assertion: {token}")


def expected_addon_ids() -> set[str]:
    """Return every atomic addon requirement with an M11-owned portion."""
    identifiers = set(SWAPPAGE_IDS)
    for start, end in M11_ONLY_RANGES:
        identifiers.update(f"ZBW-ADDON-{value:03d}" for value in range(start, end + 1))
    return identifiers


def catalogue_allocations() -> dict[str, str]:
    """Read atomic requirement milestone cells from the generated catalogue."""
    allocations: dict[str, str] = {}
    for line in read("docs/ADDON_FEATURE_CATALOG.md").splitlines():
        if not line.startswith("| ZBW-ADDON-"):
            continue
        fields = line.split(" | ")
        if len(fields) < 7:
            raise ValueError(f"malformed addon row: {line[:80]}")
        allocations[fields[0].removeprefix("| ")] = fields[5]
    return allocations


def validate() -> list[str]:
    """Return every M11 reconciliation inconsistency."""
    errors: list[str] = []
    graph = json.loads(read("build/module-graph.json"))
    planned = {row["id"]: row for row in graph["planned_production_modules"]}
    materialized = {row["id"] for row in graph["materialized_build_modules"]}

    for identifier, expected in M11_MODULES.items():
        row = planned.get(identifier)
        if row is None:
            errors.append(f"missing planned M11 module: {identifier}")
            continue
        actual = (row["bytecode"], row["first_milestone"], row["depends_on"])
        if actual != expected:
            errors.append(f"{identifier}: incorrect M11 allocation {actual}")
        if identifier in PHASE_1_MODULES and identifier not in materialized:
            errors.append(f"{identifier}: M11 Phase 1 module is not materialized")
        if identifier == "zbw-scripting-engine" and identifier in materialized:
            errors.append("zbw-scripting-engine must remain deferred until an M11 execution phase")

    for identifier in sorted(PHASE_1_MODULES):
        row = next((entry for entry in graph["materialized_build_modules"]
                    if entry["id"] == identifier), None)
        if row is None or not (ROOT / row["path"]).is_file():
            errors.append(f"{identifier}: missing materialized Phase 1 Maven module")
    if (ROOT / "scripting/zbw-scripting-engine/pom.xml").exists():
        errors.append("M11 Phase 1 must not create zbw-scripting-engine")

    forbidden = ("org.bukkit", "io.papermc", "net.minecraft", "java.sql", "java.nio.file")
    for relative in ("scripting/zbw-scripting-api/src/main/java",
                     "shop/zbw-shop/src/main/java", "content/zbw-content/src/main/java"):
        source = ROOT / relative
        for path in source.rglob("*.java"):
            text = path.read_text(encoding="utf-8")
            if any(token in text for token in forbidden):
                errors.append(f"M11 Phase 1 platform/storage leak: {path.relative_to(ROOT)}")

    milestone_number = lambda value: int(value[1:])
    for identifier, row in planned.items():
        if row["first_milestone"] != "M11":
            continue
        for dependency in row["depends_on"]:
            dependency_row = planned.get(dependency)
            if dependency_row is None:
                errors.append(f"{identifier}: unknown dependency {dependency}")
            elif milestone_number(dependency_row["first_milestone"]) > 11:
                errors.append(
                    f"{identifier}: depends on later module {dependency} "
                    f"({dependency_row['first_milestone']})")

    state = json.loads(read("build/milestone-state.json"))
    if state.get("active_milestone") != "M11":
        errors.append("milestone state must identify M11 as active during implementation")
    if state.get("next_milestone") != "M11":
        errors.append("milestone state must identify M11 as the next milestone")
    if state.get("completed_milestones") != [f"M{value:02d}" for value in range(11)]:
        errors.append("milestone state must retain exactly M00 through M10 as completed")
    if "M11_SCOPE_RECONCILIATION" not in state.get("completed_governance_checkpoints", []):
        errors.append("milestone state must record the M11 governance checkpoint")

    allocations = catalogue_allocations()
    expected_ids = expected_addon_ids()
    actual_ids = {identifier for identifier, value in allocations.items() if "M11" in value}
    if actual_ids != expected_ids:
        errors.append(
            f"addon M11 ownership mismatch: expected {len(expected_ids)}, found {len(actual_ids)}")
    for identifier in sorted(expected_ids):
        milestone = allocations.get(identifier, "")
        for owner in ("M11", "M16", "M22"):
            if owner not in milestone:
                errors.append(f"{identifier}: missing retained {owner} ownership")
    for identifier in sorted(STATISTICS_IDS | SWAPPAGE_IDS):
        if "M15" not in allocations.get(identifier, ""):
            errors.append(f"{identifier}: missing retained M15 statistics ownership")
    for identifier in sorted(SWAPPAGE_IDS):
        if "M10" not in allocations.get(identifier, ""):
            errors.append(f"{identifier}: missing M10 registration/selection ownership")
    rotation = allocations.get("ZBW-ADDON-387", "")
    for owner in ("M19", "M20"):
        if owner not in rotation:
            errors.append(f"ZBW-ADDON-387: missing retained {owner} ownership")

    milestones = read("docs/MILESTONES.md")
    require(milestones, "### M11 — Shop, item, generator and upgrade platform", "milestones", errors)
    require(milestones, "M12 owns persistent progression/virtual-currency ledgers", "milestones", errors)
    require(milestones, "M15 owns all mode/addon statistics", "milestones", errors)
    require(milestones, "M16 owns every PlaceholderAPI cell", "milestones", errors)
    require(milestones, "M19 owns Redis coordination and M20 proxy/server distribution", "milestones", errors)
    require(milestones, "M21 owns Vault plus concrete NPC/hologram/shopkeeper providers", "milestones", errors)
    require(milestones, "M22 owns legacy adapters, fallbacks", "milestones", errors)

    traceability = read("docs/REQUIREMENTS_TRACEABILITY.md")
    require(traceability, "## M11 reconciled allocation for continuing requirements", "traceability", errors)
    require(traceability, "M11 / ZBW-SHOP-003/004", "traceability", errors)
    require(traceability, "M19 cross-node coordination; M20 proxy/server distribution", "traceability", errors)

    architecture = read("docs/ARCHITECTURE.md")
    require(architecture, "## M11 planned shop, content and action-platform boundary", "architecture", errors)
    require(architecture, "allocates four Java-8-neutral production modules", "architecture", errors)
    require(architecture, "full 1.8–1.21.x support remains the M22 release gate", "architecture", errors)

    prd = read("docs/PRD/PRD.md")
    require(prd, "M11 owns the platform-neutral shop, match-resource tender", "PRD", errors)
    require(prd, "M12 supplies persistent/virtual currency ledger providers", "PRD", errors)

    risks = read("docs/RISKS_AND_CONFLICTS.md")
    require(risks, "| RC-086 | Fact — **RESOLVED 2026-07-17**", "risks", errors)
    return errors


def main() -> int:
    """Run the validator from CI or a local governance checkpoint."""
    errors = validate()
    for error in errors:
        print(f"ERROR: {error}")
    if errors:
        return 1
    print(
        "M11 allocation PASS: 4 planned Java-8 modules, 3 Phase 1 modules materialized, "
        "scripting engine deferred, 132 addon rows and later ownership retained")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
