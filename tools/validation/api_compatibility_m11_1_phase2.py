#!/usr/bin/env python3
"""Generate or verify the additive M11.1 Phase 2 Java 8 API surface."""
from __future__ import annotations

import argparse
from pathlib import Path
import sys

import api_compatibility

ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m11-1-phase2.txt"
CLASS_ROOT = ROOT / "shop/zbw-shop/target/classes/io/zartra/bedwars/shop/mode"


def observed() -> str:
    """Return the exact public API introduced by Phase 2."""
    if not CLASS_ROOT.is_dir():
        raise ValueError(f"Missing M11.1 Phase 2 compiled classes: {CLASS_ROOT}")
    signatures: list[str] = []
    count = 0
    for path in sorted(CLASS_ROOT.rglob("*.class")):
        lines = api_compatibility.signature(path, 52)
        if lines:
            count += 1
            signatures.extend(lines)
    if not count:
        raise ValueError("No M11.1 Phase 2 public classes found")
    return "\n".join((
        "# ZartraBedWars M11.1 Phase 2 additive JVM binary API baseline",
        "# class-major=52",
        *sorted(signatures),
        "",
    ))


def main() -> int:
    """Generate or verify the exact additive baseline."""
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    command = parser.parse_args().command
    current = observed()
    if command == "generate":
        BASELINE.write_text(current, encoding="utf-8", newline="\n")
        print(f"Generated M11.1 Phase 2 API baseline: {current.count('CLASS ')} classes")
        return 0
    if BASELINE.read_text(encoding="utf-8") != current:
        print("ERROR: M11.1 Phase 2 API differs from exact additive baseline", file=sys.stderr)
        return 1
    print(f"M11.1 Phase 2 binary/API compatibility PASS: "
          f"{current.count('CLASS ')} Java 8 classes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
