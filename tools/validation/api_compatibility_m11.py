#!/usr/bin/env python3
"""Generate or verify additive M11 Java 8/21 API baselines."""
from __future__ import annotations
import argparse
from pathlib import Path
import api_compatibility

ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m11.txt"
MODERN_BASELINE = ROOT / "build/api-signature-baseline-m11-modern.txt"
PRIOR = ROOT / "build/api-signature-baseline-m10.txt"
PRIOR_MODERN = ROOT / "build/api-signature-baseline-m10-modern.txt"
MODULES = api_compatibility.NEUTRAL_MODULES + (
    "arena/zbw-arena", "game/zbw-game", "command/zbw-command-api", "ui/zbw-ui-api",
    "scripting/zbw-scripting-api", "scripting/zbw-scripting-engine", "shop/zbw-shop",
    "content/zbw-content", "storage/zbw-storage-sql")
MODERN_MODULES = api_compatibility.MODERN_MODULES + (
    "command/zbw-command-paper", "ui/zbw-ui-paper", "platform/paper/zbw-paper-modern")

def missing(prior: Path, current: str) -> list[str]:
    observed = set(current.splitlines())
    return [line for line in prior.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#") and line not in observed]

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    args = parser.parse_args()
    neutral = api_compatibility.observed(MODULES, 52, "M11 neutral")
    modern = api_compatibility.observed(MODERN_MODULES, 65, "M11 modern")
    if args.command == "generate":
        BASELINE.write_text(neutral, encoding="utf-8", newline="\n")
        MODERN_BASELINE.write_text(modern, encoding="utf-8", newline="\n")
        print(f"Generated M11 API baselines: {neutral.count('CLASS ')} neutral, {modern.count('CLASS ')} modern classes")
        return 0
    m11_missing = missing(BASELINE, neutral)
    m11_modern_missing = missing(MODERN_BASELINE, modern)
    prior_missing = missing(PRIOR, neutral)
    prior_modern_missing = missing(PRIOR_MODERN, modern)
    if m11_missing or m11_modern_missing or prior_missing or prior_modern_missing:
        print("ERROR: M11.1 removed an immutable M10/M11 signature")
        return 1
    print(f"M11.1 binary/API compatibility PASS: {neutral.count('CLASS ')} Java 8 and "
          f"{modern.count('CLASS ')} Java 21 classes; immutable M10/M11 baselines preserved")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
