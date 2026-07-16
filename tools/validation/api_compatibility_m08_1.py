#!/usr/bin/env python3
"""Generate or verify the additive M08.1 Java 8 binary API baseline."""

from __future__ import annotations

import argparse
from pathlib import Path

import api_compatibility


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m08-1.txt"
MODERN_BASELINE = ROOT / "build/api-signature-baseline-m08-1-modern.txt"
M08_BASELINE = ROOT / "build/api-signature-baseline-m08.txt"
M08_MODERN_BASELINE = ROOT / "build/api-signature-baseline-m08-modern.txt"
MODULES = api_compatibility.NEUTRAL_MODULES + ("arena/zbw-arena", "game/zbw-game")
MODERN_MODULES = api_compatibility.MODERN_MODULES


def prior_missing(prior: Path, current: str) -> list[str]:
    """Return visible prior signatures absent from the current build."""
    signatures = set(current.splitlines())
    return [
        line for line in prior.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#") and line not in signatures
    ]


def main() -> int:
    """Generate a new exact baseline or verify it and all prior signatures."""
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    arguments = parser.parse_args()
    current = api_compatibility.observed(MODULES, 52, "M08.1 neutral")
    modern = api_compatibility.observed(MODERN_MODULES, 65, "M08.1 modern")
    if arguments.command == "generate":
        BASELINE.write_text(current, encoding="utf-8", newline="\n")
        MODERN_BASELINE.write_text(modern, encoding="utf-8", newline="\n")
        print(
            "Generated M08.1 binary API baselines with "
            f"{current.count('CLASS ')} neutral and {modern.count('CLASS ')} modern classes."
        )
        return 0
    for path in (BASELINE, MODERN_BASELINE, M08_BASELINE, M08_MODERN_BASELINE):
        if not path.is_file():
            print(f"ERROR: required binary API baseline is missing: {path.name}")
            return 1
    if BASELINE.read_text(encoding="utf-8") != current:
        print("ERROR: M08.1 neutral binary API differs from its exact baseline")
        return 1
    if MODERN_BASELINE.read_text(encoding="utf-8") != modern:
        print("ERROR: M08.1 modern binary API differs from its exact baseline")
        return 1
    missing = prior_missing(M08_BASELINE, current)
    missing_modern = prior_missing(M08_MODERN_BASELINE, modern)
    if missing or missing_modern:
        print(
            "ERROR: M08.1 removed or changed "
            f"{len(missing)} neutral and {len(missing_modern)} modern M08 signatures"
        )
        return 1
    print(
        "M08.1 binary/API compatibility PASS: "
        f"{current.count('CLASS ')} Java 8 and {modern.count('CLASS ')} Java 21 classes; "
        "all M08 signatures preserved."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
