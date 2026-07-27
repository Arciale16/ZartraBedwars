#!/usr/bin/env python3
"""Deterministic architecture, scope and evidence validation for M08.1."""

from __future__ import annotations

import json
from pathlib import Path
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]


def _normalize(text: str) -> str:
    return (
        text
        .replace("â€“", "–")
        .replace("â€”", "—")
        .replace("â†’", "→")
    )


def require_text(relative: str, values: tuple[str, ...], errors: list[str]) -> None:
    """Require every value in one UTF-8 repository document."""
    path = ROOT / relative
    if not path.is_file():
        errors.append(f"M08.1 required file is missing: {relative}")
        return
    content = _normalize(path.read_text(encoding="utf-8"))
    for value in values:
        if _normalize(value) not in content:
            errors.append(f"{relative} lacks M08.1 assertion: {value}")


def coverage(module: str, errors: list[str]) -> None:
    """Enforce neutral domain/application JaCoCo thresholds when evidence exists."""
    report = ROOT / module / "target/site/jacoco/jacoco.xml"
    if not report.is_file():
        return
    counters = {
        row.attrib["type"]: row.attrib
        for row in ET.parse(report).getroot().findall("counter")
    }
    for counter, minimum in (("LINE", 0.90), ("BRANCH", 0.85)):
        values = counters.get(counter)
        covered = int(values["covered"]) if values else 0
        total = covered + int(values["missed"]) if values else 0
        if total == 0 or covered / total < minimum:
            errors.append(f"{module} M08.1 {counter.lower()} coverage below {minimum:.0%}")


def validate() -> list[str]:
    """Return every M08.1 architecture, scope and evidence violation."""
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    completed = state.get("completed_milestones", [])
    expected = [f"M{value:02d}" for value in range(len(completed))]
    if (state.get("active_milestone") not in (None, f"M{len(completed):02d}")
            or completed != expected or len(completed) < 9):
        errors.append("sequential milestone state must include the completed M08 baseline")
    if state.get("completed_hardening_milestones") != ["M08.1"]:
        errors.append("milestone state must record completed M08.1 hardening")

    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    materialized = {row["id"] for row in graph["materialized_build_modules"]}
    later = {"zbw-command-api", "zbw-command-paper", "zbw-ui-api", "zbw-ui-paper"}
    if materialized.intersection(later) and "M09" not in completed:
        errors.append("M08.1 must not materialize M09 presentation modules")
    if "M09" in completed and materialized.intersection(later) != later:
        errors.append("completed M09 must materialize all presentation modules")
    if "zbw-game" not in materialized or "zbw-arena" not in materialized:
        errors.append("M08.1 requires the existing arena and game modules")

    require_text("domain/zbw-domain/src/main/java/io/zartra/bedwars/domain/team/TeamLayoutLimits.java", (
        "MINIMUM_TEAM_COUNT = 2", "MAXIMUM_TEAM_COUNT = 64",
        "MAXIMUM_TEAM_CAPACITY = 64", "MAXIMUM_MATCH_PLAYERS = 256",
    ), errors)
    require_text("game/zbw-game/src/main/java/io/zartra/bedwars/game/model/MatchStateMachine.java", (
        "TeamLayoutLimits.MAXIMUM_TEAM_COUNT", "withVictory", "VictoryEvaluator",
    ), errors)
    require_text("arena/zbw-arena/src/main/java/io/zartra/bedwars/arena/validation/ArenaValidation.java", (
        "requiredSharedGeneratorTypes", "unsupported_team_size", "unsupported_mode",
    ), errors)
    require_text("game/zbw-game/src/main/java/io/zartra/bedwars/game/application/ArenaMatchAssembler.java", (
        "stale_definition", "ArenaDefinition.Status.ENABLED", "TeamDefinition.of",
    ), errors)

    arena_validation = (ROOT / "arena/zbw-arena/src/main/java/io/zartra/bedwars/arena/validation/"
                        "ArenaValidation.java").read_text(encoding="utf-8")
    if '.contains("diamond")' in arena_validation or '.contains("emerald")' in arena_validation:
        errors.append("generator validation must not use resource-name substrings")
    machine = (ROOT / "game/zbw-game/src/main/java/io/zartra/bedwars/game/model/"
               "MatchStateMachine.java").read_text(encoding="utf-8")
    if "teams.size() > 32" in machine:
        errors.append("the obsolete 32-team engine ceiling remains")

    paper_root = ROOT / "platform/paper/zbw-paper-modern/src/main/java"
    for path in paper_root.rglob("*.java"):
        content = path.read_text(encoding="utf-8")
        for policy in ("StandardVictoryEvaluator", "TeamLayoutLimits", "ArenaMatchAssembler"):
            if policy in content:
                errors.append(f"Paper production source owns M08.1 policy: {path.relative_to(ROOT)}")

    layout_test = "game/zbw-game/src/test/java/io/zartra/bedwars/game/TeamLayoutMatrixTest.java"
    require_text(layout_test, (
        "assemblesSoloEightByOne", "assemblesDoublesEightByTwo",
        "assemblesThreeByThreeByThreeByThree", "assemblesFourByFourByFourByFour",
        "assemblesFourByFour", "assemblesCustomTwelveByThreeWithoutPresetCeiling",
        "assemblesSharedMaximumSixtyFourByFour", "Red", "Blue", "Green", "Yellow",
        "Aqua", "White", "Pink", "Gray",
    ), errors)

    required = (
        "build/api-signature-baseline-m08-1.txt",
        "build/api-signature-baseline-m08-1-modern.txt",
        "docs/IMPLEMENTATION_M08_1.md", "docs/API_M08_1.md",
        "docs/TEAM_CONFIGURATION_M08_1.md",
        "docs/ARENA_VALIDATION_PROFILES_M08_1.md",
        "docs/LAYOUT_COMPATIBILITY_M08_1.md",
    )
    for relative in required:
        if not (ROOT / relative).is_file():
            errors.append(f"M08.1 evidence/document missing: {relative}")
    require_text("docs/MILESTONES.md", (
        "### M08.1 — Team configurability", "does not start or close any M09 or M10",
    ), errors)
    require_text("docs/REQUIREMENTS_TRACEABILITY.md", (
        "## M08.1 corrective hardening allocation", "ZBW-ADDON-156",
        "ZBW-ADDON-411/419", "ZBW-ADDON-421",
    ), errors)
    require_text("docs/ARCHITECTURE.md", (
        "### M08.1 configurable-layout hardening", "Paper continues to translate/project only",
    ), errors)
    for module in ("domain/zbw-domain", "arena/zbw-arena", "game/zbw-game"):
        coverage(module, errors)
    return errors


def main() -> int:
    """Report the deterministic M08.1 result."""
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print(
        "M08.1 architecture PASS: shared limits, exact validation, arena assembly and "
        "generic victory; no M09/M10 or Paper policy."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
