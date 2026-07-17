#!/usr/bin/env python3
"""Generate or verify the additive M09 Java 8/21 binary API baselines."""

from __future__ import annotations

import argparse
from pathlib import Path

import api_compatibility


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m09.txt"
MODERN_BASELINE = ROOT / "build/api-signature-baseline-m09-modern.txt"
PRIOR = ROOT / "build/api-signature-baseline-m08-1.txt"
PRIOR_MODERN = ROOT / "build/api-signature-baseline-m08-1-modern.txt"
MODULES = api_compatibility.NEUTRAL_MODULES + (
    "arena/zbw-arena", "game/zbw-game", "command/zbw-command-api", "ui/zbw-ui-api",
)
MODERN_MODULES = api_compatibility.MODERN_MODULES + (
    "command/zbw-command-paper", "ui/zbw-ui-paper",
)


def prior_missing(prior: Path, current: str) -> list[str]:
    signatures = set(current.splitlines())
    return [line for line in prior.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#") and line not in signatures]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    arguments = parser.parse_args()
    current = api_compatibility.observed(MODULES, 52, "M09 neutral")
    modern = api_compatibility.observed(MODERN_MODULES, 65, "M09 modern")
    if arguments.command == "generate":
        BASELINE.write_text(current, encoding="utf-8", newline="\n")
        MODERN_BASELINE.write_text(modern, encoding="utf-8", newline="\n")
        print(f"Generated M09 API baselines: {current.count('CLASS ')} neutral, "
              f"{modern.count('CLASS ')} modern classes")
        return 0
    for path in (BASELINE, MODERN_BASELINE, PRIOR, PRIOR_MODERN):
        if not path.is_file():
            print(f"ERROR: required API baseline missing: {path.name}")
            return 1
    if BASELINE.read_text(encoding="utf-8") != current:
        print("ERROR: M09 neutral API differs from its exact baseline")
        return 1
    if MODERN_BASELINE.read_text(encoding="utf-8") != modern:
        print("ERROR: M09 modern API differs from its exact baseline")
        return 1
    missing = prior_missing(PRIOR, current)
    missing_modern = prior_missing(PRIOR_MODERN, modern)
    if missing or missing_modern:
        print(f"ERROR: M09 removed {len(missing)} neutral and "
              f"{len(missing_modern)} modern prior signatures")
        return 1
    print(f"M09 binary/API compatibility PASS: {current.count('CLASS ')} Java 8 and "
          f"{modern.count('CLASS ')} Java 21 classes; all M08.1 signatures preserved")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
