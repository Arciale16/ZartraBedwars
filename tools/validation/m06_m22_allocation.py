#!/usr/bin/env python3
"""Validate M06/M22 boundaries while allowing milestone-qualified later composition."""

from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
LEGACY_MODULES = {
    "zbw-compat-v1_8",
    "zbw-compat-v1_9",
    "zbw-compat-v1_10",
    "zbw-compat-v1_11",
    "zbw-compat-v1_12-v1_16_4",
    "zbw-compat-v1_16_5",
    "zbw-compat-v1_17-v1_19",
    "zbw-paper-legacy",
    "zbw-paper-j11",
    "zbw-paper-j16",
    "zbw-paper-j17",
}


def read(relative: str) -> str:
    """Read one normative UTF-8 governance file."""
    return (ROOT / relative).read_text(encoding="utf-8")


def _normalize(text: str) -> str:
    """Normalize mojibake/UTF-8 variant punctuation for stable checks."""
    return (
        text
        .replace("â€“", "–")
        .replace("â€”", "—")
        .replace("â†’", "→")
    )


def require(text: str, token: str, location: str, errors: list[str]) -> None:
    """Record a missing normative assertion."""
    if _normalize(token) not in _normalize(text):
        errors.append(f"{location}: missing allocation assertion: {token}")


def validate() -> list[str]:
    """Return all M06/M22 allocation inconsistencies."""
    errors: list[str] = []
    graph = json.loads(read("build/module-graph.json"))
    modules = {row["id"]: row for row in graph["planned_production_modules"]}

    expected = {
        "zbw-compat-api": (8, "M06", ["zbw-api"]),
        "zbw-world": (8, "M06", ["zbw-api", "zbw-application"]),
        "zbw-compat-v1_20-v1_21": (21, "M06", ["zbw-compat-api"]),
        "zbw-paper-modern": (
            21,
            "M06",
            ["zbw-application", "zbw-world", "zbw-compat-v1_20-v1_21", "zbw-game",
             "zbw-command-paper", "zbw-ui-paper", "zbw-shop", "zbw-content",
             "zbw-scripting-api", "zbw-scripting-engine", "zbw-progression", "zbw-integration-placeholderapi"],
        ),
    }
    for identifier, (bytecode, milestone, dependencies) in expected.items():
        row = modules.get(identifier)
        if row is None:
            errors.append(f"missing planned module: {identifier}")
            continue
        actual = (row["bytecode"], row["first_milestone"], row["depends_on"])
        if actual != (bytecode, milestone, dependencies):
            errors.append(f"{identifier}: incorrect M06 allocation {actual}")
    paper = modules.get("zbw-paper-modern", {})
    if paper.get("dependency_since") != {
            "zbw-game": "M08",
            "zbw-command-paper": "M09",
            "zbw-ui-paper": "M09",
            "zbw-shop": "M11",
            "zbw-content": "M11",
            "zbw-scripting-api": "M11",
            "zbw-scripting-engine": "M11",
            "zbw-progression": "M12",
            "zbw-integration-placeholderapi": "M16",
    }:
        errors.append(
            "zbw-paper-modern: later dependencies must activate only in their "
            "declared M08/M09/M11/M12 milestones")

    for identifier in sorted(LEGACY_MODULES):
        row = modules.get(identifier)
        if row is None or row["first_milestone"] != "M22":
            errors.append(f"{identifier}: legacy/intermediate module must remain M22")

    milestone = lambda value: int(value[1:])
    for identifier, row in modules.items():
        if row["first_milestone"] != "M06":
            continue
        for dependency in row["depends_on"]:
            activation = row.get("dependency_since", {}).get(
                dependency, row["first_milestone"])
            if milestone(activation) > 6:
                continue
            dependency_row = modules.get(dependency)
            if dependency_row is None:
                continue
            if milestone(dependency_row["first_milestone"]) > 6:
                errors.append(
                    f"{identifier}: M06 depends on later {dependency} "
                    f"({dependency_row['first_milestone']})")

    state = json.loads(read("build/milestone-state.json"))
    completed = state["completed_milestones"]
    expected_completed = [f"M{value:02d}" for value in range(len(completed))]
    active = state["active_milestone"]
    if (completed != expected_completed or len(completed) < 6
            or active not in (None, f"M{len(completed):02d}")):
        errors.append("milestone state must represent active M06 or completed M06 closure")

    milestones = read("docs/MILESTONES.md")
    require(milestones, "M06 primary certification never closes the M22 release gate", "milestones", errors)
    require(milestones, "The M06 certification target is only Paper 1.21.1 build 133", "milestones", errors)
    require(milestones, "`zbw-compat-v1_8` and every legacy/intermediate adapter", "milestones", errors)
    require(milestones, "Full 1.8–1.21.x certification remains a release gate", "milestones", errors)

    traceability = read("docs/REQUIREMENTS_TRACEABILITY.md")
    require(traceability, "the narrow `zbw-compat-v1_8` adapter is delivered exclusively in M22", "traceability", errors)
    require(traceability, "M06 neutral capability contracts and Paper 1.21.1 primary modern adapter only", "traceability", errors)
    require(traceability, "## M06 foundational allocation for continuing requirements", "traceability", errors)

    architecture = read("docs/ARCHITECTURE.md")
    require(architecture, "### M06 allocation boundary", "architecture", errors)
    require(architecture, "No M06 module may depend on an artifact whose first milestone is M22", "architecture", errors)
    require(architecture, "Primary M06 evidence cannot be used as a full-family or 1.8 support claim", "architecture", errors)

    matrix = read("docs/RUNTIME_COMPATIBILITY_MATRIX.md")
    require(matrix, "**Only M06 row:**", "runtime matrix", errors)
    fixture_section = matrix.split("## Mandatory server-runtime fixtures", 1)[1].split(
        "## Client protocol compatibility", 1)[0]
    m06_rows = [
        line for line in fixture_section.splitlines()
        if line.startswith("| ") and "M06" in line
    ]
    if len(m06_rows) != 1 or not m06_rows[0].startswith("| 1.21.1 |"):
        errors.append("runtime matrix must assign only the 1.21.1 fixture row to M06")

    fallbacks = read("docs/COMPATIBILITY_FALLBACKS.md")
    require(fallbacks, "It does not create or certify a legacy adapter", "fallbacks", errors)
    require(fallbacks, "`zbw-compat-v1_8` is implemented only in M22", "fallbacks", errors)
    require(fallbacks, "Full 1.8–1.21.x certification remains an M22 release gate", "fallbacks", errors)

    risks = read("docs/RISKS_AND_CONFLICTS.md")
    require(risks, "| RC-079 | Fact — **RESOLVED 2026-07-15**", "risks", errors)
    return errors


def main() -> int:
    """Run the deterministic allocation check."""
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("M06/M22 allocation PASS: primary M06 foundation isolated; legacy/full matrix remains M22.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())