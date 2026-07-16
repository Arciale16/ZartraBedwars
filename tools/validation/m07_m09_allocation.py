#!/usr/bin/env python3
"""Validate the approved M07/M09 application/presentation allocation."""

from __future__ import annotations

import json
from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]
PRESENTATION_MODULES = {
    "zbw-command-api",
    "zbw-command-paper",
    "zbw-ui-api",
    "zbw-ui-paper",
}


def read(relative: str) -> str:
    """Read one normative UTF-8 governance file."""
    return (ROOT / relative).read_text(encoding="utf-8")


def require(text: str, token: str, location: str, errors: list[str]) -> None:
    """Record one missing normative allocation assertion."""
    if token not in text:
        errors.append(f"{location}: missing allocation assertion: {token}")


def milestone_number(value: str) -> int:
    """Convert an exact milestone identifier to its order value."""
    match = re.fullmatch(r"M([0-9]{2})", value)
    if match is None:
        raise ValueError(f"invalid milestone identifier: {value}")
    return int(match.group(1))


def validate() -> list[str]:
    """Return every M07/M09 allocation inconsistency."""
    errors: list[str] = []
    graph = json.loads(read("build/module-graph.json"))
    modules = {row["id"]: row for row in graph["planned_production_modules"]}
    state = json.loads(read("build/milestone-state.json"))
    m08_started = state["active_milestone"] == "M08" or "M08" in state["completed_milestones"]

    if graph["policy"].get("later_milestone_dependencies_allowed") is not False:
        errors.append("module graph must explicitly prohibit later-milestone dependencies")

    expected = {
        "zbw-arena": (
            8,
            "M07",
            ["zbw-api", "zbw-domain", "zbw-application", "zbw-world"],
        ),
        "zbw-game": (
            8,
            "M08",
            ["zbw-api", "zbw-domain", "zbw-application", "zbw-arena"],
        ),
        "zbw-command-api": (8, "M09", ["zbw-api", "zbw-application"]),
        "zbw-command-paper": (
            21,
            "M09",
            ["zbw-command-api", "zbw-arena", "zbw-game"],
        ),
        "zbw-ui-api": (8, "M09", ["zbw-api", "zbw-application"]),
        "zbw-ui-paper": (
            21,
            "M09",
            ["zbw-ui-api", "zbw-arena", "zbw-game", "zbw-compat-api"],
        ),
    }
    for identifier, expected_values in expected.items():
        row = modules.get(identifier)
        if row is None:
            errors.append(f"missing planned module: {identifier}")
            continue
        actual = (row["bytecode"], row["first_milestone"], row["depends_on"])
        if actual != expected_values:
            errors.append(f"{identifier}: incorrect allocation {actual}")

    m07_modules = {
        identifier
        for identifier, row in modules.items()
        if row["first_milestone"] == "M07"
    }
    if m07_modules != {"zbw-arena"}:
        errors.append(f"M07 planned modules must be only zbw-arena, found {sorted(m07_modules)}")
    m08_modules = {
        identifier
        for identifier, row in modules.items()
        if row["first_milestone"] == "M08"
    }
    if m08_modules != {"zbw-game"}:
        errors.append(f"M08 planned modules must retain zbw-game, found {sorted(m08_modules)}")
    m09_modules = {
        identifier
        for identifier, row in modules.items()
        if row["first_milestone"] == "M09"
    }
    if m09_modules != PRESENTATION_MODULES:
        errors.append(f"M09 presentation module allocation drift: {sorted(m09_modules)}")

    for identifier, row in modules.items():
        for dependency in row["depends_on"]:
            dependency_row = modules.get(dependency)
            if dependency_row is None:
                errors.append(f"{identifier}: unknown dependency {dependency}")
                continue
            activation_milestone = milestone_number(
                row.get("dependency_since", {}).get(
                    dependency, row["first_milestone"]
                )
            )
            dependency_milestone = milestone_number(dependency_row["first_milestone"])
            if dependency_milestone > activation_milestone:
                errors.append(
                    f"{identifier}: dependency on {dependency} activates in "
                    f"M{activation_milestone:02d} before "
                    f"{dependency_row['first_milestone']}"
                )
        if row["first_milestone"] == "M07" and PRESENTATION_MODULES.intersection(
            row["depends_on"]
        ):
            errors.append(f"{identifier}: M07 must not depend on M09 presentation modules")

    materialized = {row["id"] for row in graph["materialized_build_modules"]}
    if "zbw-arena" not in materialized:
        errors.append("M07 implementation must materialize zbw-arena")
    premature = materialized.intersection(PRESENTATION_MODULES)
    if premature:
        errors.append(f"M07/M08 must not materialize M09 modules: {sorted(premature)}")
    if not m08_started and "zbw-game" in materialized:
        errors.append("pre-M08 state must not materialize zbw-game")
    if m08_started and "zbw-game" not in materialized:
        errors.append("active/completed M08 must materialize zbw-game")
    for relative in (
        "command/zbw-command-api",
        "command/zbw-command-paper",
        "ui/zbw-ui-api",
        "ui/zbw-ui-paper",
    ):
        if (ROOT / relative).exists():
            errors.append(f"M07/M08 created M09 implementation path: {relative}")
    if not (ROOT / "arena/zbw-arena/pom.xml").is_file():
        errors.append("M07 arena module descriptor is missing")

    milestones = read("docs/MILESTONES.md")
    require(milestones, "### M07 — Arena, map and setup application lifecycle", "milestones", errors)
    require(milestones, "core/application portions of ARENA-001..009 and ZBW-ADDON-408..423", "milestones", errors)
    require(milestones, "No production command or GUI is delivered", "milestones", errors)
    require(milestones, "M07 supplies real arena/map/setup use cases", "milestones", errors)

    architecture = read("docs/ARCHITECTURE.md")
    require(architecture, "### M07/M09 application and presentation boundary", "architecture", errors)
    require(architecture, "M07 creates no temporary production command", "architecture", errors)
    require(architecture, "no M07 module may depend on a module first allocated to M09", "architecture", errors)
    require(architecture, "presentation adapter → application use case → domain/ports", "architecture", errors)

    traceability = read("docs/REQUIREMENTS_TRACEABILITY.md")
    require(traceability, "## M07/M09 ArenaSetup allocation for continuing requirements", "traceability", errors)
    for number in range(408, 424):
        require(
            traceability,
            f"| M07/M09 / ZBW-ADDON-{number:03d} |",
            "traceability",
            errors,
        )
    for requirement in range(1, 10):
        line = next(
            (
                value
                for value in traceability.splitlines()
                if value.startswith(f"| ZBW-ARENA-{requirement:03d} |")
            ),
            "",
        )
        if "M07" not in line or "M09" not in line:
            errors.append(f"traceability: ZBW-ARENA-{requirement:03d} lacks split ownership")

    catalogue = read("docs/ADDON_FEATURE_CATALOG.md")
    catalogue_rows = {
        int(match.group(1)): match.group(0)
        for match in re.finditer(
            r"^\| ZBW-ADDON-(40[89]|41[0-9]|42[0-3]) \|.*$",
            catalogue,
            re.MULTILINE,
        )
    }
    if set(catalogue_rows) != set(range(408, 424)):
        errors.append("catalogue must retain exactly ZBW-ADDON-408..423 ArenaSetup rows")
    for number, row in catalogue_rows.items():
        for token in (
            "| M07 core; M09 command/GUI presentation |",
            "| M09 presentation:",
            "| M07 core:",
            "| COVERED |",
        ):
            if token not in row:
                errors.append(f"catalogue: ZBW-ADDON-{number:03d} missing {token}")

    prd = read("docs/PRD/PRD.md")
    require(prd, "UX-001..006 govern final M09 presentation acceptance, not M07 application-layer construction", "PRD", errors)
    require(prd, "UX-001/002 govern final M09 presentation acceptance, not M07 application-layer construction", "PRD", errors)

    risks = read("docs/RISKS_AND_CONFLICTS.md")
    require(risks, "| RC-081 | Fact — **", "risks", errors)
    require(risks, "M07 owns presentation-neutral arena/map/setup policies", "risks", errors)
    return errors


def main() -> int:
    """Run the deterministic allocation validation."""
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(
        "M07/M09 allocation PASS: arena application module isolated; "
        "command/UI presentation remains M09."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
