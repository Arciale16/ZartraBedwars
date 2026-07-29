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
M11_ADDONS = tuple(range(10, 26)) + tuple(range(61, 71)) + tuple(range(141, 148)) + tuple(range(184, 202)) + tuple(range(236, 245)) + tuple(range(300, 323)) + tuple(range(341, 350)) + tuple(range(363, 369)) + tuple(range(379, 398)) + tuple(range(438, 453))
M12_ADDONS = tuple(range(174, 184)) + tuple(range(210, 217)) + tuple(range(245, 252)) + tuple(range(266, 283))
M12_REQUIREMENTS = {
    "ZBW-PROG-001", "ZBW-PROG-002", "ZBW-PROG-003",
    "ZBW-PROG-004", "ZBW-PROG-005", "ZBW-PROG-011",
}
M13_ADDONS = tuple(range(81, 92))
M13_REQUIREMENTS = {
    "ZBW-PROG-009", "ZBW-PROG-010", "ZBW-PROG-012", "ZBW-PROG-013",
    "ZBW-CONTENT-004", "ZBW-CONTENT-005", "ZBW-CONTENT-006",
}
M14_ADDONS = tuple(range(26, 41)) + tuple(range(274, 283)) + tuple(range(369, 379))
M14_REQUIREMENTS = {
    "ZBW-PROG-006", "ZBW-PROG-007", "ZBW-PROG-008", "ZBW-PROG-014",
    "ZBW-CONTENT-007", "ZBW-CONTENT-009", "ZBW-CONTENT-011",
}
M15_REQUIREMENTS = {
    "ZBW-STATS-001", "ZBW-STATS-002", "ZBW-STATS-003", "ZBW-STATS-004",
    "ZBW-STATS-005", "ZBW-STATS-006", "ZBW-STATS-007", "ZBW-STATS-008",
}

M17_REQUIREMENTS = {
    "ZBW-REPLAY-001", "ZBW-REPLAY-002", "ZBW-REPLAY-003", "ZBW-REPLAY-004",
    "ZBW-REPLAY-005", "ZBW-REPLAY-006", "ZBW-REPLAY-007", "ZBW-REPLAY-008",
    "ZBW-REPLAY-009", "ZBW-REPLAY-010",
}

M19_REQUIREMENTS = {
    "ZBW-DEPLOY-006", "ZBW-DEPLOY-008", "ZBW-DEPLOY-009",
}

M20_REQUIREMENTS = {
    "ZBW-DEPLOY-002", "ZBW-DEPLOY-003", "ZBW-DEPLOY-004",
}

M18_REQUIREMENTS = {
    "ZBW-ATLAS-001", "ZBW-ATLAS-003", "ZBW-ATLAS-004",
    "ZBW-ATLAS-005", "ZBW-ATLAS-006", "ZBW-ATLAS-011",
    "ZBW-ATLAS-002", "ZBW-ATLAS-007", "ZBW-ATLAS-008",
    "ZBW-ATLAS-009", "ZBW-ATLAS-010", "ZBW-ATLAS-012", "ZBW-ATLAS-013",
}


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


def m11_requirement(identifier: str) -> bool:
    """Return whether completed M11 supplies an implemented requirement portion."""
    if identifier in {
        "ZBW-GAME-004", "ZBW-GAME-005", "ZBW-SHOP-001", "ZBW-SHOP-002",
        "ZBW-SHOP-003", "ZBW-SHOP-004", "ZBW-SHOP-005", "ZBW-SHOP-006",
        "ZBW-SHOP-007", "ZBW-CONTENT-002", "ZBW-CONTENT-003", "ZBW-READY-004",
        "ZBW-READY-015",
    }:
        return True
    match = re.match(r"ZBW-ADDON-(\d{3})$", identifier)
    return bool(match and int(match.group(1)) in M11_ADDONS)


def m12_requirement(identifier: str) -> bool:
    """Return whether completed M12 supplies an implemented requirement portion."""
    if identifier in M12_REQUIREMENTS:
        return True
    match = re.match(r"ZBW-ADDON-(\d{3})$", identifier)
    return bool(match and int(match.group(1)) in M12_ADDONS)


def m13_requirement(identifier: str) -> bool:
    """Return whether completed M13 supplies a verified requirement portion."""
    if identifier in M13_REQUIREMENTS:
        return True
    match = re.match(r"ZBW-ADDON-(\d{3})$", identifier)
    return bool(match and int(match.group(1)) in M13_ADDONS)


def m14_requirement(identifier: str) -> bool:
    """Return whether M14 Phase 1 supplies a foundation portion."""
    if identifier in M14_REQUIREMENTS:
        return True
    match = re.match(r"ZBW-ADDON-(\d{3})$", identifier)
    return bool(match and int(match.group(1)) in M14_ADDONS)


def m15_requirement(identifier: str) -> bool:
    return identifier in M15_REQUIREMENTS


def state(identifier: str, planned: str) -> tuple[str, str]:
    if identifier in {"ZBW-UX-001", "ZBW-UX-002", "ZBW-UX-003", "ZBW-UX-006"}:
        return "VERIFIED", "M09 implementation, tests, documentation and Paper evidence complete"
    if presentation_requirement(identifier):
        return "PARTIAL", "M09 presentation portion verified; later requirement allocations remain visible"
    if m10_requirement(identifier):
        return "PARTIAL", "M10 shared-server framework verified; M11/M15/M16/M17/M20/M22 allocations remain"
    if m11_requirement(identifier):
        return "PARTIAL", "M11-owned portion verified; later allocations remain visible in traceability"
    if identifier in M12_REQUIREMENTS:
        return "VERIFIED", "M12-owned progression, persistence, reward and presentation scope is complete; later owners remain explicit"
    if m12_requirement(identifier):
        return "PARTIAL", "M12-owned portion verified; M13+ integrations remain visible in traceability"
    if m13_requirement(identifier):
        return "VERIFIED", "M13 objectives, runtime, persistence and primary presentation are complete; later owners remain explicit"
    if m14_requirement(identifier):
        return "PARTIAL", "M14 Phase 1 neutral models, validation and storage/service ports implemented; runtime/content/presentation remain"
    if m15_requirement(identifier):
        return "VERIFIED", "M15 statistics/runtime/leaderboard/adapters implementation is complete; later owners remain visible"
    if identifier in M17_REQUIREMENTS:
        return "PARTIAL", "M17 replay allocation is complete; provider, distributed, compatibility and release qualification remain"
    if identifier == "ZBW-DEPLOY-006":
        return "VERIFIED", "M19 Redis contracts, adapter, security, bridges, recovery, performance and operations evidence complete"
    if identifier in M19_REQUIREMENTS:
        return "PARTIAL", "M19 Redis allocation complete; M20 proxy topology and delivery allocation remains"
    if identifier in M20_REQUIREMENTS:
        return "PARTIAL", "M20 proxy networking allocation complete; provider, compatibility and release qualification remain"
    if identifier in M18_REQUIREMENTS:
        return "PARTIAL", "M18 Atlas allocation is complete; M19 distributed and M21 provider/release qualification remain"
    match = re.match(r"ZBW-ADDON-(\d{3})$", identifier)
    if match and (41 <= int(match.group(1)) <= 60 or 102 <= int(match.group(1)) <= 107 or 252 <= int(match.group(1)) <= 259 or 291 <= int(match.group(1)) <= 299 or int(match.group(1)) == 387 or 464 <= int(match.group(1)) <= 473):
        return "PARTIAL", "M20 proxy coordination allocation complete; owner-side feature and M21/M22 provider/compatibility work remain"
    if match and 323 <= int(match.group(1)) <= 333:
        return "VERIFIED", "M18 guarded staff operation, permission, confirmation, audit and rollback evidence complete"
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
    m11 = m11_requirement(identifier)
    m12 = m12_requirement(identifier)
    m13 = m13_requirement(identifier)
    m14 = m14_requirement(identifier)
    return {
        "Feature": feature.replace("|", "\\|"), "Requirement ID": identifier,
        "Category": category, "Planned milestone": planned, "Current status": status,
        "Core implementation": "M14 cosmetic/profile/calendar foundation" if m14 else ("M13 objective/content runtime and persistence" if m13 else ("M12 progression/reward implementation" if m12 else ("M11 checkpoint implementation" if m11 else ("M10 typed framework" if m10 else ("M07/M08 typed use case" if m09 else "See traceability"))))),
        "Paper implementation": "M13 owner-thread Paper projection verified" if m13 else ("M12 primary Paper projection certified" if m12 else ("M11 primary Paper projection certified" if m11 else ("M10 primary projection" if m10 else ("M09 primary adapter verified" if m09 else "See traceability")))),
        "Command": "Generated M13 action path where allocated" if m13 else ("Generated M12 action path where allocated" if m12 else ("Generated M11 action path where allocated" if m11 else ("Generated M10 action path" if m10 else ("Generated action path" if m09 else "See traceability")))),
        "GUI": "Generated M13 parity page where allocated" if m13 else ("Generated M12 parity page where allocated" if m12 else ("Generated M11 parity page where allocated" if m11 else ("Generated M10 parity page" if m10 else ("Generated parity page" if m09 else "See traceability")))),
        "Permission": "M03 central authorization and M13 execution revalidation" if m13 else ("M03 central authorization and execution revalidation" if m12 else ("Central authorization contract" if m11 else ("M03 execution revalidation + M10 node" if m10 else ("M03 revalidation + granular node" if m09 else "See traceability")))),
        "Tests": "M14 immutable-model/invalid-state/catalogue/API tests" if m14 else ("M13 model/runtime/recovery/presentation/Paper evidence" if m13 else ("M12 unit/integration/recovery/Paper certification evidence" if m12 else ("M11 unit/integration/security/Paper certification evidence" if m11 else ("M10 unit/quality/Paper evidence" if m10 else ("M09 unit/parity/Paper E2E" if m09 else "Milestone evidence"))))),
        "Documentation": "M14 Phase 1 implementation/API evidence" if m14 else ("M13 Phase 1-3 implementation/API/command/GUI evidence" if m13 else ("M12 phase implementation/API and closure evidence" if m12 else ("M11 and M11.1 implementation/API evidence" if m11 else ("M10 guides and inventories" if m10 else ("M09 framework/inventories" if m09 else "PRD + traceability"))))),
        "Configurable or hardcoded": "Typed immutable versioned M14 definitions" if m14 else ("Typed immutable versioned definitions" if m13 else ("Typed versioned progression/reward policy" if m12 else ("Typed replaceable catalog/policy" if m11 else ("Typed replaceable policy" if m10 else ("Typed/configurable; no adapter policy" if m09 else "Per requirement"))))),
        "Blocker or deferred dependency": blocker,
        "Notes": "M14 Phase 1 only; runtime/content/presentation and M15+ owners are not claimed" if m14 else ("M13-owned scope verified; M15 statistics, M16 PlaceholderAPI, M17 replay and later owners are not claimed" if m13 else ("M12-owned portion verified; M13+ and M15/M16/later integrations are not claimed" if m12 else ("M11-owned portion verified; later owners are not claimed" if m11 else ("Framework only; named-mode gameplay is not claimed" if m10 else ("M09 baseline retained" if m09 else "Scope is not advanced beyond completed milestones"))))),
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
        "identifies remaining ownership. Completed M13 scope never implies completion of later allocations.",
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
        "M00–M20 and hardening M08.1 are complete; M21 is next and remains unstarted.",
        "M10 extends `zbw-game`, M09 presentation and primary Paper projection without a new module,",
        "with deterministic 115-action inventories and strict quality/API/runtime evidence.",
        "Merged PR #17 supplies M11 Phases 1-4; squash-merged PR #18 supplies M11.1 corrective",
        "implementation and successful mandatory remote certification. M12 Phases 1–5 complete progression,",
        "persistence, rewards and primary presentation. M13 Phases 1–3 complete neutral objective/content",
        "contracts, deterministic runtime, SQL persistence and M09/Paper presentation. M14 Phase 1 adds",
        "neutral cosmetic/profile/calendar models and ports. M15/M16 are complete. M17 closes replay",
        "contracts, ingestion, SQL persistence, playback, bounded Paper viewer/visuals and staff tools;",
        "provider/distributed/compatibility and final release qualification remain with M21-M24. M19 closes Redis coordination; M20 closes proxy routing, cross-server flows, failure/security testing and operations without taking domain ownership.",
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
