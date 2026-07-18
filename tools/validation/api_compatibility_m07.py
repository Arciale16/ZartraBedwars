#!/usr/bin/env python3
"""Generate or verify the exact additive M07 Java 8 binary API baseline."""

from __future__ import annotations

import argparse
from pathlib import Path

import api_compatibility


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m07.txt"
M06_BASELINE = ROOT / "build/api-signature-baseline-m06.txt"
MODULES = api_compatibility.NEUTRAL_MODULES + ("arena/zbw-arena",)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    arguments = parser.parse_args()
    current = api_compatibility.observed(MODULES, 52, "M07 neutral")
    if arguments.command == "generate":
        BASELINE.write_text(current, encoding="utf-8", newline="\n")
        print(f"Generated M07 binary API baseline with {current.count('CLASS ')} public classes.")
        return 0
    if not BASELINE.is_file():
        print("ERROR: immutable M07 neutral binary API baseline is missing")
        return 1
    baseline_missing = api_compatibility.missing_signatures(
        BASELINE.read_text(encoding="utf-8"), current)
    if baseline_missing:
        print(f"ERROR: {len(baseline_missing)} immutable M07 signatures were removed or changed")
        return 1
    if not M06_BASELINE.is_file():
        print("ERROR: immutable M06 binary API baseline is missing")
        return 1
    signatures = set(current.splitlines())
    missing = [
        line for line in M06_BASELINE.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#") and line not in signatures
    ]
    if missing:
        print(f"ERROR: {len(missing)} M06 binary signatures were removed or changed")
        return 1
    print(
        "M07 binary/API compatibility PASS: "
        f"{current.count('CLASS ')} Java 8 public classes; M06 baseline preserved."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
