#!/usr/bin/env python3
"""Generate or verify the exact Java 8 M18 Atlas API checkpoint."""
from __future__ import annotations

import argparse
from pathlib import Path
import sys

import api_compatibility


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m18-atlas-api.txt"
CLASS_ROOT = ROOT / "atlas/zbw-atlas-api/target/classes"


def observed() -> str:
    """Return the deterministic public Atlas API with Java 8 class-major enforcement."""
    if not CLASS_ROOT.is_dir():
        raise ValueError(f"Missing M18 compiled classes: {CLASS_ROOT}")
    signatures: list[str] = []
    count = 0
    for path in sorted(CLASS_ROOT.rglob("*.class")):
        lines = api_compatibility.signature(path, 52)
        if lines:
            count += 1
            signatures.extend(lines)
    if not count:
        raise ValueError("No M18 Atlas public classes found")
    return "\n".join((
        "# ZartraBedWars M18 Atlas Java 8 API baseline",
        "# class-major=52",
        *sorted(signatures),
        "",
    ))


def main() -> int:
    """Generate or compare the immutable Atlas API checkpoint."""
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    command = parser.parse_args().command
    current = observed()
    if command == "generate":
        BASELINE.write_text(current, encoding="utf-8", newline="\n")
        print(f"Generated M18 Atlas API baseline: {current.count('CLASS ')} Java 8 classes")
        return 0
    if not BASELINE.is_file() or BASELINE.read_text(encoding="utf-8") != current:
        print("ERROR: M18 Atlas API differs from its immutable checkpoint", file=sys.stderr)
        return 1
    print(f"M18 Atlas API compatibility PASS: {current.count('CLASS ')} Java 8 classes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
