#!/usr/bin/env python3
"""Validate the reconciled M10 framework and later-mechanics allocation."""

from __future__ import annotations

import json
from pathlib import Path
import re


ROOT = Path(__file__).resolve().parents[2]


def _require(path: str, assertions: tuple[str, ...], errors: list[str]) -> None:
    text = (ROOT / path).read_text(encoding="utf-8")
    for assertion in assertions:
        if assertion not in text:
            errors.append(f"{path} lacks reconciled M10 assertion: {assertion}")


def _catalogue_milestones() -> dict[int, str]:
    rows: dict[int, str] = {}
    for line in (ROOT / "docs/ADDON_FEATURE_CATALOG.md").read_text(
            encoding="utf-8").splitlines():
        match = re.match(r"^\| ZBW-ADDON-(\d{3}) \|", line)
        if match:
            rows[int(match.group(1))] = line.strip("|").split("|")[5].strip()
    return rows


def validate() -> list[str]:
    """Return deterministic reconciliation errors, or an empty list."""
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    completed = state["completed_milestones"]
    expected = [f"M{value:02d}" for value in range(len(completed))]
    if completed != expected or len(completed) < 10:
        errors.append("M10 requires an ordered completed M00..M09 baseline")
    active = state["active_milestone"]
    if "M10" not in completed and active != "M10":
        errors.append("machine state must record M10 active or completed")

    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    if any(row["first_milestone"] == "M10" for row in graph["planned_production_modules"]):
        errors.append("reconciled M10 must not materialize a new production module")

    _require("docs/MILESTONES.md", (
        "M10 — Selectors, matchmaking, mode selection and spectator framework",
        "registration/selection portions only of ZBW-ADDON-236..244",
        "All named-mode gameplay mechanics, including Swappage, and mode-specific",
        "completion portions of ZBW-ADDON-236..244",
    ), errors)
    _require("docs/ARCHITECTURE.md", (
        "M10 adds no new Maven module.",
        "M10 registers typed deferred bindings for named modes, including",
        "Swappage, but M11 implements their gameplay mechanics.",
    ), errors)
    _require("docs/REQUIREMENTS_TRACEABILITY.md", (
        "## M10 reconciled allocation for continuing requirements",
        "M10 / ZBW-GAME-004/005",
        "M10 / ZBW-ADDON-236..244",
        "M11 complete swap gameplay and owned-component transfer",
    ), errors)

    milestones = _catalogue_milestones()
    allocations = {
        tuple(range(92, 102)): {"M10", "M16", "M22"},
        tuple(range(115, 124)): {"M10", "M16", "M20", "M22"},
        tuple(range(131, 141)): {"M10", "M16", "M22"},
        tuple(range(155, 164)): {"M10", "M16", "M22"},
        tuple(range(236, 245)): {"M10", "M11", "M15", "M16", "M22"},
    }
    for identifiers, expected_owners in allocations.items():
        for identifier in identifiers:
            actual = set(re.findall(r"M\d{2}", milestones.get(identifier, "")))
            if actual != expected_owners:
                errors.append(
                    f"ZBW-ADDON-{identifier:03d} owners {sorted(actual)} != "
                    f"{sorted(expected_owners)}")
    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("M10 allocation PASS: framework ownership is isolated from later mode mechanics/providers.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
