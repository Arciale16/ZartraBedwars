#!/usr/bin/env python3
"""Generate and validate the authoritative 672-row feature dashboard."""

from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path
import re
import sys


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "docs" / "FEATURE_IMPLEMENTATION_STATUS.md"
VALID = {"NOT_STARTED", "IN_PROGRESS", "PARTIAL", "IMPLEMENTED", "VERIFIED", "BLOCKED", "DEFERRED"}
M09_ADDONS = tuple(range(1, 10)) + tuple(range(108, 115)) + tuple(range(124, 131)) + tuple(range(148, 155)) + tuple(range(334, 341)) + tuple(range(398, 438))
M10_ADDONS = tuple(range(92, 102)) + tuple(range(115, 124)) + tuple(range(131, 141)) + tuple(range(155, 164)) + tuple(range(236, 245))


def cells(line: str) -> list[str]:
    return [value.strip() for value in line.strip().strip("|").split("|")]


def trace_rows() -> dict[str, str]:
    result: dict[str, str] = {}
    for line in (ROOT / "docs/REQUIREMENTS_TRACEABILITY.md").read_text(encoding="utf-8").splitlines():
        if re.match(r"^\| ZBW-[A-Z]+-\d{3} \|", line):
            result[cells(line)[0]] = line
    return result


def milestones(text: str) -> str:
    values = []
    for raw in re.findall(r"\bM(?:0?[0-9]|1[0-9]|2[0-4])\b", text):
        value = f"M{int(raw[1:]):02d}"
        if value not in values:
            values.append(value)
    return "/".join(values) if values else "Cross-milestone"


def milestone_numbers(value: str) -> list[int]:
    return [int(number) for number in re.findall(r"M(\d{2})", value)]


def presentation_requirement(identifier: str) -> bool:
    if identifier in {"ZBW-UX-001", "ZBW-UX-002", "ZBW-UX-003", "ZBW-UX-006"}:
        return True
    if re.match(r"ZBW-ARENA-00[1-9]$", identifier):
        return True
    if identifier in {"ZBW-GAME-001", "ZBW-GAME-002", "ZBW-GAME-003", "ZBW-GAME-006", "ZBW-GAME-008", "ZBW-GAME-010"}:
        return True
    match = re.match(r"ZBW-ADDON-(\d{3})$", identifier)
    return bool(match and int(match.group(1)) in M09_ADDONS)


def m10_requirement(identifier: str) -> bool:
    if identifier in {"ZBW-GAME-004", "ZBW-GAME-005", "ZBW-GAME-007", "ZBW-GAME-009", "ZBW-CONTENT-003"}:
        return True
    match = re.match(r"ZBW-ADDON-(\d{3})$", identifier)
    return bool(match and int(match.group(1)) in M10_ADDONS)


def m11_phase1_requirement(identifier: str) -> bool:
    """Return whether the current M11 checkpoint supplies an implemented portion."""
    return identifier in {
        "ZBW-SHOP-001", "ZBW-SHOP-002", "ZBW-SHOP-003", "ZBW-SHOP-004",
        "ZBW-CONTENT-002", "ZBW-READY-004",
    }


def state(identifier: str, planned: str) -> tuple[str, str]:
    if identifier in {"ZBW-UX-001", "ZBW-UX-002", "ZBW-UX-003", "ZBW-UX-006"}:
        return "VERIFIED", "M09 implementation, tests, documentation and Paper evidence complete"
    if presentation_requirement(identifier):
        return "PARTIAL", "M09 presentation portion verified; later requirement allocations remain visible"
    if m10_requirement(identifier):
        return "PARTIAL", "M10 shared-server framework verified; M11/M15/M16/M17/M20/M22 allocations remain"
    if m11_phase1_requirement(identifier):
        return "PARTIAL", "M11 Phase 1 foundation implemented; later M11 and retained milestone allocations remain"
    numbers = milestone_numbers(planned)
    if numbers and min(numbers) >= 10:
        return "DEFERRED", f"Owned by {planned}"
    if numbers and max(numbers) <= 8 and len(numbers) == 1:
        return "VERIFIED", f"Verified by completed {planned} evidence"
    return "PARTIAL", "Foundation exists; remaining allocations are recorded in traceability"


def prd_features(trace: dict[str, str]) -> list[dict[str, str]]:
    rows = []
    for line in (ROOT / "docs/PRD/PRD.md").read_text(encoding="utf-8").splitlines():
        if not re.match(r"^\| ZBW-[A-Z]+-\d{3} \|", line):
            continue
        values = cells(line)
        identifier, feature = values[0], values[2]
        prefix = identifier.split("-")[1]
        planned = milestones(trace.get(identifier, ""))
        status, blocker = state(identifier, planned)
        category = {
            "ARENA": "BW1058 core parity", "GAME": "BW1058/Hypixel parity",
            "SHOP": "BW1058 core parity", "ECO": "BW1058 core parity",
            "PAPI": "PlaceholderAPI", "INT": "Integrations", "DISCORD": "Integrations",
            "REPLAY": "Replay", "ATLAS": "Atlas", "COMPAT": "Compatibility",
            "DEPLOY": "Database/deployment", "OPS": "Performance and operations",
            "QA": "Documentation/testing", "GOV": "Documentation/governance",
            "LICENSE": "Documentation/licensing", "UX": "Commands/GUI/permissions",
        }.get(prefix, prefix.title())
        rows.append(row(feature, identifier, category, planned, status, blocker))
    return rows


def addon_features() -> list[dict[str, str]]:
    rows = []
    addon = "Addon"
    tier = "Addon"
    for line in (ROOT / "docs/ADDON_FEATURE_CATALOG.md").read_text(encoding="utf-8").splitlines():
        heading = re.match(r"^### 4\.\d+ (.+) \((Premium|Free)\)$", line)
        if heading:
            addon, tier = heading.group(1), heading.group(2)
        if not re.match(r"^\| ZBW-ADDON-\d{3} \|", line):
            continue
        values = cells(line)
        identifier = values[0]
        planned = values[5] if len(values) > 5 else "Cross-milestone"
        status, blocker = state(identifier, milestones(planned))
        rows.append(row(f"{addon} — {values[2]}", identifier, f"{tier} addons",
                        milestones(planned), status, blocker))
    return rows


def row(feature: str, identifier: str, category: str, planned: str,
        status: str, blocker: str) -> dict[str, str]:
    m09 = presentation_requirement(identifier)
    m10 = m10_requirement(identifier)
    m11 = m11_phase1_requirement(identifier)
    return {
        "Feature": feature.replace("|", "\\|"), "Requirement ID": identifier,
        "Category": category, "Planned milestone": planned, "Current status": status,
        "Core implementation": "M11 Phase 1 typed foundation" if m11 else ("M10 typed framework" if m10 else ("M07/M08 typed use case" if m09 else "See traceability")),
        "Paper implementation": "Deferred within M11" if m11 else ("M10 primary projection" if m10 else ("M09 primary adapter verified" if m09 else "See traceability")),
        "Command": "M09 framework; feature actions later M11" if m11 else ("Generated M10 action path" if m10 else ("Generated action path" if m09 else "See traceability")),
        "GUI": "M09 framework; feature pages later M11" if m11 else ("Generated M10 parity page" if m10 else ("Generated parity page" if m09 else "See traceability")),
        "Permission": "Central authorization contract" if m11 else ("M03 execution revalidation + M10 node" if m10 else ("M03 revalidation + granular node" if m09 else "See traceability")),
        "Tests": "M11 Phase 1 contract/unit evidence" if m11 else ("M10 unit/quality/Paper evidence" if m10 else ("M09 unit/parity/Paper E2E" if m09 else "Milestone evidence")),
        "Documentation": "M11 Phase 1 implementation/API guides" if m11 else ("M10 guides and inventories" if m10 else ("M09 framework/inventories" if m09 else "PRD + traceability")),
        "Configurable or hardcoded": "Typed replaceable catalog/policy" if m11 else ("Typed replaceable policy" if m10 else ("Typed/configurable; no adapter policy" if m09 else "Per requirement")),
        "Blocker or deferred dependency": blocker,
        "Notes": "Phase 1 only; later M11 behavior is not claimed" if m11 else ("Framework only; named-mode gameplay is not claimed" if m10 else ("M09 baseline retained" if m09 else "Scope is not advanced beyond completed milestones")),
    }


def render() -> str:
    rows = prd_features(trace_rows()) + addon_features()
    identifiers = [entry["Requirement ID"] for entry in rows]
    if len(rows) != 672 or len(set(identifiers)) != 672:
        raise ValueError(f"dashboard requires 672 unique requirements; observed {len(rows)}/{len(set(identifiers))}")
    totals = Counter(entry["Current status"] for entry in rows)
    if not set(totals).issubset(VALID):
        raise ValueError("invalid dashboard status")
    categories = Counter(entry["Category"] for entry in rows)
    lines = [
        "# Feature implementation status",
        "",
        "This generated file is the authoritative human-readable project dashboard. Run",
        "`python tools/validation/feature_dashboard.py` to reject stale or contradictory rows.",
        "A requirement can remain `PARTIAL` after one allocated portion is verified; its blocker column",
        "identifies remaining ownership. M10 framework and M11 Phase 1 verification never imply later mechanics.",
        "",
        "## Project totals",
        "",
        "| Status | Count |",
        "|---|---:|",
    ]
    for status in sorted(VALID):
        lines.append(f"| {status} | {totals.get(status, 0)} |")
    lines.extend(["", "Total requirements: **672**.", "", "## Required coverage views", "",
                  "| View | Rows |", "|---|---:|"])
    required_views = (
        "BW1058 core parity", "BW1058/Hypixel parity", "Free addons", "Premium addons",
        "Commands/GUI/permissions", "PlaceholderAPI", "Integrations", "Database/deployment",
        "Replay", "Atlas", "Compatibility", "Documentation/testing",
        "Documentation/governance", "Documentation/licensing", "Performance and operations",
    )
    for category in required_views:
        lines.append(f"| {category} | {categories.get(category, 0)} |")
    lines.extend([
        "", "## Milestone and evidence summary", "",
        "M00–M10 and hardening M08.1 are recorded complete in `build/milestone-state.json`.",
        "M10 extends `zbw-game`, M09 presentation and primary Paper projection without a new module,",
        "with deterministic 115-action inventories and strict quality/API/runtime evidence.",
        "M11 is active at its Phase 1 checkpoint: three neutral foundation modules are materialized,",
        "while the scripting engine, runtime mechanics, presentation and later ownership remain deferred.",
        "", "## Feature rows", "",
    ])
    columns = list(rows[0])
    lines.append("| " + " | ".join(columns) + " |")
    lines.append("|" + "|".join("---" for _ in columns) + "|")
    for entry in rows:
        lines.append("| " + " | ".join(entry[column] for column in columns) + " |")
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    arguments = parser.parse_args()
    try:
        expected = render()
    except ValueError as failure:
        print(f"ERROR: {failure}", file=sys.stderr)
        return 1
    if arguments.write:
        OUTPUT.write_text(expected, encoding="utf-8", newline="\n")
    elif not OUTPUT.is_file() or OUTPUT.read_text(encoding="utf-8") != expected:
        print("ERROR: docs/FEATURE_IMPLEMENTATION_STATUS.md is stale", file=sys.stderr)
        return 1
    print("Feature dashboard PASS: 672 unique rows, allowed statuses, deterministic output")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
