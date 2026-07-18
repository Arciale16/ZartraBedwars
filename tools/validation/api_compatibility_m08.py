#!/usr/bin/env python3
"""Generate or verify the exact additive M08 Java 8 binary API baseline."""

from __future__ import annotations

import argparse
from pathlib import Path

import api_compatibility


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m08.txt"
MODERN_BASELINE = ROOT / "build/api-signature-baseline-m08-modern.txt"
M07_BASELINE = ROOT / "build/api-signature-baseline-m07.txt"
M06_MODERN_BASELINE = ROOT / "build/api-signature-baseline-m06-modern.txt"
MODULES = api_compatibility.NEUTRAL_MODULES + ("arena/zbw-arena", "game/zbw-game")
MODERN_MODULES = api_compatibility.MODERN_MODULES


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    arguments = parser.parse_args()
    current = api_compatibility.observed(MODULES, 52, "M08 neutral")
    modern = api_compatibility.observed(MODERN_MODULES, 65, "M08 modern")
    if arguments.command == "generate":
        BASELINE.write_text(current, encoding="utf-8", newline="\n")
        MODERN_BASELINE.write_text(modern, encoding="utf-8", newline="\n")
        print(
            "Generated M08 binary API baselines with "
            f"{current.count('CLASS ')} neutral and {modern.count('CLASS ')} modern public classes."
        )
        return 0
    if not BASELINE.is_file() or api_compatibility.missing_signatures(
            BASELINE.read_text(encoding="utf-8"), current):
        print("ERROR: immutable M08 neutral binary API signatures were removed or changed")
        return 1
    if not M07_BASELINE.is_file():
        print("ERROR: immutable M07 binary API baseline is missing")
        return 1
    if not MODERN_BASELINE.is_file() or api_compatibility.missing_signatures(
            MODERN_BASELINE.read_text(encoding="utf-8"), modern):
        print("ERROR: immutable M08 modern binary API signatures were removed or changed")
        return 1
    if not M06_MODERN_BASELINE.is_file():
        print("ERROR: immutable M06 modern binary API baseline is missing")
        return 1
    signatures = set(current.splitlines())
    missing = [
        line for line in M07_BASELINE.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#") and line not in signatures
    ]
    if missing:
        print(f"ERROR: {len(missing)} M07 binary signatures were removed or changed")
        return 1
    modern_signatures = set(modern.splitlines())
    missing_modern = [
        line for line in M06_MODERN_BASELINE.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#") and line not in modern_signatures
    ]
    if missing_modern:
        print(f"ERROR: {len(missing_modern)} M06 modern binary signatures were removed or changed")
        return 1
    print(
        "M08 binary/API compatibility PASS: "
        f"{current.count('CLASS ')} Java 8 and {modern.count('CLASS ')} Java 21 public classes; "
        "M07 neutral and M06 modern baselines preserved."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
