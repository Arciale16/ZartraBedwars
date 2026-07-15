#!/usr/bin/env python3
"""Validate the approved M08/M09 gameplay/presentation allocation."""

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
ADDON_NUMBERS = {
    number
    for start, end in (
        (1, 9),
        (108, 114),
        (124, 130),
        (148, 154),
        (334, 340),
        (398, 407),
        (424, 437),
    )
    for number in range(start, end + 1)
}


def read(relative: str) -> str:
    """Read one normative UTF-8 governance file."""
    return (ROOT / relative).read_text(encoding="utf-8")


def require(text: str, token: str, location: str, errors: list[str]) -> None:
    """Record one missing normative allocation assertion."""
    if token not in text:
        errors.append(f"{location}: missing allocation assertion: {token}")


def milestone_number(value: str) -> int:
    """Convert one exact milestone identifier to its order value."""
    match = re.fullmatch(r"M([0-9]{2})", value)
    if match is None:
        raise ValueError(f"invalid milestone identifier: {value}")
    return int(match.group(1))


def validate() -> list[str]:
    """Return every M08/M09 allocation inconsistency."""
    errors: list[str] = []
    graph = json.loads(read("build/module-graph.json"))
    modules = {row["id"]: row for row in graph["planned_production_modules"]}

    game = modules.get("zbw-game")
    expected_game = {
        "layer": "application",
        "bytecode": 8,
        "first_milestone": "M08",
        "depends_on": ["zbw-api", "zbw-domain", "zbw-application", "zbw-arena"],
    }
    if game is None:
        errors.append("missing planned module: zbw-game")
    else:
        for field, expected in expected_game.items():
            if game.get(field) != expected:
                errors.append(f"zbw-game: incorrect {field}: {game.get(field)!r}")

    expected_presentation = {
        "zbw-command-api": (8, ["zbw-api", "zbw-application"]),
        "zbw-command-paper": (21, ["zbw-command-api", "zbw-arena", "zbw-game"]),
        "zbw-ui-api": (8, ["zbw-api", "zbw-application"]),
        "zbw-ui-paper": (21, ["zbw-ui-api", "zbw-arena", "zbw-game", "zbw-compat-api"]),
    }
    for identifier, (bytecode, dependencies) in expected_presentation.items():
        row = modules.get(identifier)
        if row is None:
            errors.append(f"missing planned presentation module: {identifier}")
        elif (
            row.get("bytecode"),
            row.get("first_milestone"),
            row.get("depends_on"),
        ) != (bytecode, "M09", dependencies):
            errors.append(f"{identifier}: M09 allocation drift")

    paper = modules.get("zbw-paper-modern")
    if paper is None:
        errors.append("missing planned module: zbw-paper-modern")
    else:
        if "zbw-game" not in paper.get("depends_on", []):
            errors.append("zbw-paper-modern must declare its planned zbw-game dependency")
        if paper.get("dependency_since", {}).get("zbw-game") != "M08":
            errors.append("zbw-paper-modern -> zbw-game must activate only in M08")

    m08_modules = {
        identifier
        for identifier, row in modules.items()
        if row["first_milestone"] == "M08"
    }
    if m08_modules != {"zbw-game"}:
        errors.append(f"M08 planned modules must be only zbw-game, found {sorted(m08_modules)}")
    m09_modules = {
        identifier
        for identifier, row in modules.items()
        if row["first_milestone"] == "M09"
    }
    if m09_modules != PRESENTATION_MODULES:
        errors.append(f"M09 presentation module allocation drift: {sorted(m09_modules)}")

    for identifier, row in modules.items():
        if row["first_milestone"] != "M08":
            continue
        for dependency in row["depends_on"]:
            dependency_row = modules.get(dependency)
            if dependency_row is None:
                errors.append(f"{identifier}: unknown dependency {dependency}")
                continue
            activation = row.get("dependency_since", {}).get(
                dependency, row["first_milestone"]
            )
            if milestone_number(dependency_row["first_milestone"]) > milestone_number(activation):
                errors.append(
                    f"{identifier}: M08 dependency on later {dependency} "
                    f"({dependency_row['first_milestone']})"
                )
        if PRESENTATION_MODULES.intersection(row["depends_on"]):
            errors.append(f"{identifier}: M08 must not depend on M09 presentation modules")

    materialized = {row["id"] for row in graph["materialized_build_modules"]}
    premature = materialized.intersection({"zbw-game"} | PRESENTATION_MODULES)
    if premature:
        errors.append(f"reconciliation must not materialize M08/M09 modules: {sorted(premature)}")
    for relative in (
        "game/zbw-game",
        "command/zbw-command-api",
        "command/zbw-command-paper",
        "ui/zbw-ui-api",
        "ui/zbw-ui-paper",
    ):
        if (ROOT / relative).exists():
            errors.append(f"reconciliation created implementation path: {relative}")

    milestones = read("docs/MILESTONES.md")
    require(
        milestones,
        "### M08 — Game engine, sessions, teams, lobby and primary Paper projections",
        "milestones",
        errors,
    )
    require(milestones, "closed primary Paper 1.21.1 portions", "milestones", errors)
    require(milestones, "Final commands, GUIs, editors and confirmation flows remain M09", "milestones", errors)
    require(milestones, "No production command, inventory GUI, editor, confirmation framework", "milestones", errors)

    architecture = read("docs/ARCHITECTURE.md")
    require(architecture, "### M08/M09 gameplay and presentation boundary", "architecture", errors)
    require(architecture, "M08 creates no temporary production command", "architecture", errors)
    for projection in (
        "Paper event translation",
        "player-state effects",
        "hotbar application/restoration",
        "direct localized chat/title/action-bar/sound feedback",
        "scoreboard projection",
        "tab-list projection",
        "native boss-bar projection",
        "stale-view cleanup",
    ):
        require(architecture, projection, "architecture", errors)

    traceability = read("docs/REQUIREMENTS_TRACEABILITY.md")
    require(
        traceability,
        "## M08/M09 game and addon allocation for continuing requirements",
        "traceability",
        errors,
    )
    for number in sorted(ADDON_NUMBERS):
        require(
            traceability,
            f"| M08/M09 / ZBW-ADDON-{number:03d} |",
            "traceability",
            errors,
        )
    for requirement in (1, 2, 3, 6, 8, 10):
        line = next(
            (
                value
                for value in traceability.splitlines()
                if value.startswith(f"| ZBW-GAME-{requirement:03d} |")
            ),
            "",
        )
        if "M08" not in line or "M09" not in line:
            errors.append(f"traceability: ZBW-GAME-{requirement:03d} lacks split ownership")

    catalogue = read("docs/ADDON_FEATURE_CATALOG.md")
    catalogue_rows = {
        int(match.group(1)): match.group(0)
        for match in re.finditer(
            r"^\| ZBW-ADDON-([0-9]{3}) \|.*$", catalogue, re.MULTILINE
        )
        if int(match.group(1)) in ADDON_NUMBERS
    }
    if set(catalogue_rows) != ADDON_NUMBERS:
        errors.append("catalogue must retain exactly all 61 M08 addon rows")
    for number, row in catalogue_rows.items():
        for token in (
            "M08 core/primary Paper; M09 final command/GUI/editor presentation",
            "zbw-game",
            "zbw-paper-modern",
            "M09 final presentation:",
            "M16:",
            "M22 full cross-version tests",
            "| COVERED |",
        ):
            if token not in row:
                errors.append(f"catalogue: ZBW-ADDON-{number:03d} missing {token}")
        modules_cell = row.split(" | ")[7]
        if "zbw-paper," in modules_cell or modules_cell.endswith("zbw-paper"):
            errors.append(f"catalogue: ZBW-ADDON-{number:03d} references nonexistent zbw-paper")
    for number in range(124, 131):
        if "M20 proxy delivery" not in catalogue_rows.get(number, ""):
            errors.append(f"catalogue: ZBW-ADDON-{number:03d} must retain M20 proxy delivery")

    prd = read("docs/PRD/PRD.md")
    require(
        prd,
        "UX-001 governs final M09 presentation acceptance, not M08 engine/application or primary-Paper construction",
        "PRD",
        errors,
    )

    risks = read("docs/RISKS_AND_CONFLICTS.md")
    require(risks, "| RC-082 | Verified integrity defect", "risks", errors)
    require(risks, "| RC-083 | Fact — **", "risks", errors)
    require(risks, "M08 owns Java-8 game/session/team/lobby policies", "risks", errors)
    return errors


def main() -> int:
    """Run the deterministic allocation validation."""
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(
        "M08/M09 allocation PASS: game/application and primary Paper projections "
        "are isolated from final command/UI presentation."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
