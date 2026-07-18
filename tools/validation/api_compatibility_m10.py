#!/usr/bin/env python3
"""Generate or verify additive M10 Java 8/21 API baselines."""

from __future__ import annotations

import argparse
from pathlib import Path
import api_compatibility

ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m10.txt"
MODERN_BASELINE = ROOT / "build/api-signature-baseline-m10-modern.txt"
PRIOR = ROOT / "build/api-signature-baseline-m09.txt"
PRIOR_MODERN = ROOT / "build/api-signature-baseline-m09-modern.txt"
MODULES = api_compatibility.NEUTRAL_MODULES + ("arena/zbw-arena", "game/zbw-game", "command/zbw-command-api", "ui/zbw-ui-api")
MODERN_MODULES = api_compatibility.MODERN_MODULES + ("command/zbw-command-paper", "ui/zbw-ui-paper", "platform/paper/zbw-paper-modern")

def missing(prior: Path, current: str) -> list[str]:
    observed = set(current.splitlines())
    return [line for line in prior.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#") and line not in observed]

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    args = parser.parse_args()
    neutral = api_compatibility.observed(MODULES, 52, "M10 neutral")
    modern = api_compatibility.observed(MODERN_MODULES, 65, "M10 modern")
    if args.command == "generate":
        BASELINE.write_text(neutral, encoding="utf-8", newline="\n")
        MODERN_BASELINE.write_text(modern, encoding="utf-8", newline="\n")
        print(f"Generated M10 API baselines: {neutral.count('CLASS ')} neutral, {modern.count('CLASS ')} modern classes")
        return 0
    baseline_missing = missing(BASELINE, neutral)
    modern_baseline_missing = missing(MODERN_BASELINE, modern)
    if baseline_missing or modern_baseline_missing:
        print(f"ERROR: removed {len(baseline_missing)} neutral and "
              f"{len(modern_baseline_missing)} modern immutable M10 signatures")
        return 1
    prior_missing = missing(PRIOR, neutral)
    modern_missing = missing(PRIOR_MODERN, modern)
    if prior_missing or modern_missing:
        print(f"ERROR: removed {len(prior_missing)} neutral and {len(modern_missing)} modern M09 signatures")
        return 1
    print(f"M10 binary/API compatibility PASS: immutable M10/M09 signatures preserved "
          f"within {neutral.count('CLASS ')} Java 8 and {modern.count('CLASS ')} Java 21 classes")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
