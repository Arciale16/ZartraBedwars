#!/usr/bin/env python3
"""Deterministically validate the complete ZartraBedWars pre-code decision baseline."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]

DECISION_IDS = [
    *(f"ZBW-CONTENT-{number:03d}" for number in range(1, 12)),
    *(f"ZBW-DISCORD-{number:03d}" for number in range(1, 9)),
    *(f"ZBW-COMPAT-{number:03d}" for number in range(1, 10)),
    *(f"ZBW-LICENSE-{number:03d}" for number in range(1, 8)),
    *(f"ZBW-READY-{number:03d}" for number in range(1, 21)),
]
ADDON_IDS = [f"ZBW-ADDON-{number:03d}" for number in range(1, 474)]
RESOLVED_RISKS = (3, 4, 17, 18, 21, 22, 24, 27, 29, 40, 41, 43, 46, 50, 59, 61, 62, 65, 66, 71, 72, 73, 74, 75, 76)

REQUIRED_DOCUMENTS = {
    "docs/PRE_CODE_DECISIONS.md": ("RC-003", "RC-071", "ZBW-READY-001", "ZBW-READY-020", "External facts", "Measurable closure evidence"),
    "docs/PRE_CODE_READINESS_REPORT.md": ("PRE-CODE READY", "672", "6,966", "100.00%", "No Java"),
    "docs/RUNTIME_COMPATIBILITY_MATRIX.md": ("1.8.8", "1.21.11", "Java 8", "Java 21", "SHA-256", "server-runtime", "Client protocol"),
    "docs/BENCHMARK_BASELINE.md": ("SMALL", "SHARED_40", "PROXY_4", "TPS", "MSPT", "Redis", "Replay", "PlaceholderAPI", "memory"),
    "docs/PRIVACY_AND_RETENTION.md": ("chat", "30 days", "90 days", "180 days", "365 days", "legal hold", "pseudonym"),
    "docs/NETWORK_SECURITY.md": ("HMAC-SHA-256", "TLS", "128-bit nonce", "dedupe", "SQLite", "MySQL", "outbox"),
    "docs/SCRIPTING_SECURITY.md": ("disabled by default", "10,000", "1 MiB", "5 ms", "filesystem", "reflection", "tick thread"),
    "docs/PROJECT_LICENSING_RECOMMENDATION.md": ("proprietary", "all rights reserved", "Apache License 2.0", "premium", "addons", "public API"),
    "docs/COSMETIC_PRODUCTION_PLAN.md": ("five", "60", "300", "provenance", "fallback", "performance"),
    "docs/BALANCING_BASELINE.md": ("zbw:standard-v1", "iron", "gold", "diamond", "emerald", "golden simulations"),
    "docs/OPERATIONAL_DEFAULTS.md": ("RPO", "RTO", "15 min", "30 min", "quarterly", "degradation"),
    "docs/QUALITY_GATES.md": ("90%", "85%", "80%", "critical/high", "Mutation", "TODO"),
    "docs/DEPENDENCY_LICENSE_AUDIT.md": ("Exact version", "SHA-256", "compile-only", "provided", "commercial", "proprietary", "pre-resolution"),
    "docs/COMPATIBILITY_FALLBACKS.md": ("Minecraft 1.8", "materials", "particles", "sounds", "entities", "packets", "GUI", "gameplay"),
    "docs/ASSET_PROVENANCE.md": ("Asset ID", "Origin", "Author", "Licence", "Redistribution status", "Modification status"),
    "THIRD_PARTY_NOTICES.md": ("dependency", "asset", "notice"),
    "README.md": ("PRE-CODE READY", "Documentation"),
    "docs/README.md": ("PRE_CODE_DECISIONS", "REQUIREMENTS_TRACEABILITY", "RUNTIME_COMPATIBILITY_MATRIX"),
}

ADRS = {
    "ADR-0001-resource-scarcity.md": (72,),
    "ADR-0002-original-content-provenance.md": (73,),
    "ADR-0003-discord-provider-topology.md": (74,),
    "ADR-0004-minecraft-1-8-fallbacks.md": (75,),
    "ADR-0005-dependency-licensing.md": (76,),
    "ADR-0006-runtime-artifacts-and-matrix.md": (3, 4, 22),
    "ADR-0007-dependency-and-provider-baseline.md": (21, 24, 27),
    "ADR-0008-declarative-scripting-sandbox.md": (18,),
    "ADR-0009-performance-and-quality-gates.md": (29, 62),
    "ADR-0010-privacy-retention-and-visibility.md": (40, 41, 65),
    "ADR-0011-network-security-and-authority.md": (46, 50),
    "ADR-0012-cosmetic-production.md": (17,),
    "ADR-0013-clean-room-addon-provenance.md": (43, 71),
    "ADR-0014-original-balance-baseline.md": (59,),
    "ADR-0015-operational-recovery-defaults.md": (61,),
    "ADR-0016-project-licensing-model.md": (66,),
}


def read(relative_path: str) -> str:
    path = ROOT / relative_path
    if not path.is_file():
        raise ValueError(f"missing required document: {relative_path}")
    return path.read_text(encoding="utf-8-sig")


def require_literals(relative_path: str, literals: tuple[str, ...]) -> None:
    folded = read(relative_path).casefold()
    missing = [literal for literal in literals if literal.casefold() not in folded]
    if missing:
        raise ValueError(f"{relative_path} is missing: {', '.join(missing)}")


def ids(text: str, pattern: str) -> list[str]:
    return re.findall(pattern, text, re.MULTILINE)


def validate() -> None:
    prd = read("docs/PRD/PRD.md")
    trace = read("docs/REQUIREMENTS_TRACEABILITY.md")
    addons = read("docs/ADDON_FEATURE_CATALOG.md")
    coverage = read("docs/MASTER_PROMPT_COVERAGE.md")
    risks = read("docs/RISKS_AND_CONFLICTS.md")

    prd_ids = ids(prd, r"^\| (ZBW-[A-Z]+-\d{3}) \|")
    trace_ids = ids(trace, r"^\| (ZBW-[A-Z]+-\d{3}) \|")
    if len(prd_ids) != 199 or len(set(prd_ids)) != 199:
        raise ValueError(f"expected 199 unique Part I PRD IDs, found {len(prd_ids)}/{len(set(prd_ids))}")
    if len(trace_ids) != 199 or len(set(trace_ids)) != 199 or set(trace_ids) != set(prd_ids):
        raise ValueError("traceability must contain exactly the same 199 Part I IDs as the PRD")
    if [item for item in prd_ids if item in set(DECISION_IDS)] != DECISION_IDS:
        raise ValueError("PRD decision IDs are not the canonical 55-row sequence")
    if [item for item in trace_ids if item in set(DECISION_IDS)] != DECISION_IDS:
        raise ValueError("traceability decision IDs are not the canonical 55-row sequence")

    addon_ids = ids(addons, r"^\| (ZBW-ADDON-\d{3}) \|")
    if sorted(addon_ids, key=lambda value: int(value.rsplit("-", 1)[1])) != ADDON_IDS or len(addon_ids) != len(set(addon_ids)):
        raise ValueError("expected one append-only ZBW-ADDON-001..473 row each")
    covered_addon_rows = sum(
        1 for line in addons.splitlines()
        if re.match(r"^\| ZBW-ADDON-\d{3} \|", line) and line.rstrip().endswith("| COVERED |")
    )
    if covered_addon_rows != 473:
        raise ValueError("all 473 addon requirements must be COVERED")

    for relative_path, literals in REQUIRED_DOCUMENTS.items():
        require_literals(relative_path, literals)

    for ready_id in (f"ZBW-READY-{number:03d}" for number in range(1, 21)):
        for relative_path in ("docs/PRE_CODE_DECISIONS.md", "docs/MILESTONES.md"):
            short_id = ready_id.removeprefix("ZBW-")
            if ready_id not in read(relative_path) and short_id not in read(relative_path):
                raise ValueError(f"orphan decision requirement {ready_id}: absent from {relative_path}")

    for filename, risk_numbers in ADRS.items():
        text = read(f"docs/DECISIONS/{filename}")
        if "**Status:** Accepted" not in text:
            raise ValueError(f"ADR not accepted: {filename}")
        for number in risk_numbers:
            if f"RC-{number:03d}" not in text:
                raise ValueError(f"{filename} does not resolve RC-{number:03d}")

    for number in RESOLVED_RISKS:
        risk_id = f"RC-{number:03d}"
        line = next((line for line in risks.splitlines() if line.startswith(f"| {risk_id} |")), "")
        if "RESOLVED 2026-07-14" not in line:
            raise ValueError(f"{risk_id} is not explicitly resolved")

    expected_coverage = ("672 stable semantic IDs", "6,966 / 6,966", "Overall functional coverage | **100.00%**", "PRE-CODE READY")
    missing = [literal for literal in expected_coverage if literal not in coverage]
    if missing:
        raise ValueError("coverage report is stale: " + ", ".join(missing))
    if "**Requirement count:** 672" not in prd or "672 stable semantic requirement IDs" not in trace:
        raise ValueError("PRD/traceability counts are stale")

    excluded_build_paths = {".git", ".tools", ".m2", "target"}
    java_files = [
        path
        for path in ROOT.rglob("*.java")
        if not any(part in excluded_build_paths for part in path.parts)
    ]
    if java_files:
        raise ValueError("pre-code baseline contains Java files: " + ", ".join(str(path.relative_to(ROOT)) for path in java_files[:5]))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    parser.parse_args()
    try:
        validate()
    except ValueError as error:
        print(f"pre-code decision validation failed: {error}", file=sys.stderr)
        return 1
    print("PRE-CODE READY verified: 25 resolved decisions, 55 decision IDs, 199 Part I IDs, 473 addon IDs, 672 total requirements")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
