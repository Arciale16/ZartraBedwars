#!/usr/bin/env python3
"""Validate the accepted RC-072 through RC-076 documentation baseline."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
PRD = ROOT / "docs" / "PRD" / "PRD.md"
TRACE = ROOT / "docs" / "REQUIREMENTS_TRACEABILITY.md"
ADDONS = ROOT / "docs" / "ADDON_FEATURE_CATALOG.md"
COVERAGE = ROOT / "docs" / "MASTER_PROMPT_COVERAGE.md"
RISKS = ROOT / "docs" / "RISKS_AND_CONFLICTS.md"

DECISION_IDS = [
    *(f"ZBW-CONTENT-{number:03d}" for number in range(1, 12)),
    *(f"ZBW-DISCORD-{number:03d}" for number in range(1, 9)),
    *(f"ZBW-COMPAT-{number:03d}" for number in range(1, 10)),
    *(f"ZBW-LICENSE-{number:03d}" for number in range(1, 8)),
]
ADDON_IDS = [f"ZBW-ADDON-{number:03d}" for number in range(1, 474)]

REQUIRED_DOCUMENTS = {
    "docs/ORIGINAL_STARTER_CATALOG.md": (
        "Shop balancing",
        "Game-mode balancing",
        "Quest starter catalogue",
        "Achievement starter catalogue",
        "Battle-pass starter season",
        "Cosmetic starter catalogue",
        "Private-game starter presets",
        "Sound and visual-effect starter catalogue",
        "Scarce",
        "Reduced",
        "Normal",
        "Abundant",
        "Extreme",
    ),
    "docs/ASSET_PROVENANCE.md": (
        "Asset ID",
        "Origin",
        "Author",
        "Licence",
        "Permitted use",
        "Redistribution status",
        "Modification status",
    ),
    "docs/DISCORD_ARCHITECTURE.md": (
        "Embedded webhook provider",
        "External Discord bot provider",
        "Custom provider API",
        "secure internal integration API",
        "event stream",
        "must not require",
        "Discord failures",
        "environment",
    ),
    "docs/COMPATIBILITY_FALLBACKS.md": (
        "Minecraft 1.8",
        "mandatory",
        "materials",
        "particles",
        "sounds",
        "entities",
        "packets",
        "GUI",
        "legacy",
        "purely decorative",
        "gameplay",
    ),
    "docs/DEPENDENCY_LICENSE_AUDIT.md": (
        "Name",
        "Version",
        "Authoritative source",
        "Licence",
        "Redistribution rights",
        "Shading permission",
        "Modification permission",
        "Required attribution",
        "Runtime-only or bundled",
        "Commercial-use compatibility",
        "UNSELECTED",
        "compile-only/provided",
        "proprietary plugin binaries",
    ),
    "THIRD_PARTY_NOTICES.md": (
        "no bundled third-party",
        "dependency",
        "asset",
        "notices",
    ),
}

ADRS = {
    "docs/DECISIONS/ADR-0001-resource-scarcity.md": "RC-072",
    "docs/DECISIONS/ADR-0002-original-content-provenance.md": "RC-073",
    "docs/DECISIONS/ADR-0003-discord-provider-topology.md": "RC-074",
    "docs/DECISIONS/ADR-0004-minecraft-1-8-fallbacks.md": "RC-075",
    "docs/DECISIONS/ADR-0005-dependency-licensing.md": "RC-076",
}


def read(relative_path: str | Path) -> str:
    path = ROOT / relative_path
    if not path.is_file():
        raise ValueError(f"missing required document: {path.relative_to(ROOT).as_posix()}")
    return path.read_text(encoding="utf-8-sig")


def require_literals(relative_path: str, literals: tuple[str, ...]) -> None:
    text = read(relative_path).casefold()
    missing = [literal for literal in literals if literal.casefold() not in text]
    if missing:
        raise ValueError(f"{relative_path} is missing required terms: {', '.join(missing)}")


def table_ids(text: str, pattern: str) -> list[str]:
    return re.findall(pattern, text, re.MULTILINE)


def validate() -> None:
    prd_text = read(PRD.relative_to(ROOT))
    trace_text = read(TRACE.relative_to(ROOT))
    addon_text = read(ADDONS.relative_to(ROOT))
    coverage_text = read(COVERAGE.relative_to(ROOT))
    risks_text = read(RISKS.relative_to(ROOT))

    prd_decisions = table_ids(prd_text, r"^\| (ZBW-(?:CONTENT|DISCORD|COMPAT|LICENSE)-\d{3}) \|")
    trace_decisions = table_ids(trace_text, r"^\| (ZBW-(?:CONTENT|DISCORD|COMPAT|LICENSE)-\d{3}) \|")
    if prd_decisions != DECISION_IDS:
        raise ValueError(f"PRD decision IDs are not the expected canonical 35 rows: {len(prd_decisions)} found")
    if trace_decisions != DECISION_IDS:
        raise ValueError(f"traceability decision IDs are not the expected canonical 35 rows: {len(trace_decisions)} found")

    prd_ids = table_ids(prd_text, r"^\| (ZBW-[A-Z]+-\d{3}) \|")
    if len(prd_ids) != 179 or len(set(prd_ids)) != 179:
        raise ValueError(f"expected 179 unique Part I PRD IDs, found {len(prd_ids)} rows/{len(set(prd_ids))} unique")

    addon_ids = table_ids(addon_text, r"^\| (ZBW-ADDON-\d{3}) \|")
    addon_ids_sorted = sorted(addon_ids, key=lambda value: int(value.rsplit("-", 1)[1]))
    if addon_ids_sorted != ADDON_IDS or len(addon_ids) != len(set(addon_ids)):
        raise ValueError(f"expected one ZBW-ADDON-001..473 row each, found {len(addon_ids)}")
    decision_addon_rows = "\n".join(
        line for line in addon_text.splitlines() if re.match(r"^\| ZBW-ADDON-(?:46[4-9]|47[0-3]) \|", line)
    )
    if decision_addon_rows.count("| COVERED |") != 10:
        raise ValueError("Resource Scarcity must have ten independently COVERED addon rows")
    resource_terms = (
        "original eleventh",
        "iron",
        "gold",
        "diamond",
        "emerald",
        "custom resource",
        "Scarce",
        "Reduced",
        "Normal",
        "Abundant",
        "Extreme",
        "GUI",
        "permission",
        "API",
        "PlaceholderAPI",
        "native and custom generators",
    )
    missing_resource_terms = [term for term in resource_terms if term.casefold() not in decision_addon_rows.casefold()]
    if missing_resource_terms:
        raise ValueError("Resource Scarcity rows are incomplete: " + ", ".join(missing_resource_terms))

    for relative_path, literals in REQUIRED_DOCUMENTS.items():
        require_literals(relative_path, literals)

    dependency_text = read("docs/DEPENDENCY_LICENSE_AUDIT.md")
    dependency_rows: list[list[str]] = []
    in_dependency_table = False
    for line in dependency_text.splitlines():
        if line.startswith("| Name | Version | Authoritative source |"):
            in_dependency_table = True
            continue
        if not in_dependency_table:
            continue
        if line.startswith("|---"):
            continue
        if not line.startswith("|"):
            break
        dependency_rows.append([cell.strip() for cell in line.strip("|").split("|")])
    if len(dependency_rows) < 35:
        raise ValueError(f"dependency audit candidate inventory is unexpectedly incomplete: {len(dependency_rows)} rows")
    invalid_dependency_rows = [
        row for row in dependency_rows if len(row) != 11 or any(not cell for cell in row)
    ]
    if invalid_dependency_rows:
        raise ValueError("every dependency row must populate all eleven owner-required audit columns")
    dependency_names = [row[0] for row in dependency_rows]
    if len(dependency_names) != len(set(dependency_names)):
        raise ValueError("dependency audit contains duplicate dependency names")

    for relative_path, conflict_id in ADRS.items():
        adr_text = read(relative_path)
        if "**Status:** Accepted" not in adr_text or f"**Resolves:** {conflict_id}" not in adr_text:
            raise ValueError(f"{relative_path} must be Accepted and resolve {conflict_id}")

    for conflict_number in range(72, 77):
        conflict_id = f"RC-{conflict_number:03d}"
        match = re.search(rf"^\| {conflict_id} \|.*?(?=^\| RC-|\Z)", risks_text, re.MULTILINE | re.DOTALL)
        if not match or "RESOLVED 2026-07-14" not in match.group(0):
            raise ValueError(f"{conflict_id} is not explicitly resolved in the risk register")

    if "**Requirement count:** 652" not in prd_text or "652 stable semantic requirement IDs" not in trace_text:
        raise ValueError("PRD and traceability must publish the final 652-requirement baseline")
    coverage_literals = (
        "652 stable semantic IDs",
        "6,946 / 6,946",
        "Overall functional coverage | **100.00%**",
        "RC-072 through RC-076 are resolved",
    )
    missing_coverage = [literal for literal in coverage_literals if literal not in coverage_text]
    if missing_coverage:
        raise ValueError("coverage report is stale or incomplete: " + ", ".join(missing_coverage))

    java_files = [path for path in ROOT.rglob("*.java") if ".git" not in path.parts]
    if java_files:
        preview = ", ".join(path.relative_to(ROOT).as_posix() for path in java_files[:5])
        raise ValueError(f"pre-implementation baseline contains Java files: {preview}")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="read-only validation; retained for command consistency")
    parser.parse_args()
    try:
        validate()
    except ValueError as error:
        print(f"pre-implementation decision validation failed: {error}", file=sys.stderr)
        return 1
    print("pre-implementation decisions verified: RC-072..RC-076, 35 decision IDs, 473 addon IDs, 652 total requirements")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
