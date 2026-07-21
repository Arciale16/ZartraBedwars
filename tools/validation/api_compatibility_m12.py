#!/usr/bin/env python3
"""Generate or verify the exact Java 8 M12 progression API checkpoint."""
from __future__ import annotations

import argparse
from pathlib import Path
import sys

import api_compatibility


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m12.txt"
CLASS_ROOT = ROOT / "progression/zbw-progression/target/classes"


def observed() -> str:
    """Return the deterministic public progression API with Java 8 class-major enforcement."""
    if not CLASS_ROOT.is_dir():
        raise ValueError(f"Missing M12 compiled classes: {CLASS_ROOT}")
    signatures: list[str] = []
    count = 0
    for path in sorted(CLASS_ROOT.rglob("*.class")):
        lines = api_compatibility.signature(path, 52)
        if lines:
            count += 1
            signatures.extend(lines)
    if not count:
        raise ValueError("No M12 public classes found")
    return "\n".join((
        "# ZartraBedWars M12 progression JVM binary API baseline",
        "# class-major=52",
        *sorted(signatures),
        "",
    ))


def main() -> int:
    """Generate or compare the immutable checkpoint."""
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    command = parser.parse_args().command
    current = observed()
    if command == "generate":
        BASELINE.write_text(current, encoding="utf-8", newline="\n")
        print(f"Generated M12 API baseline: {current.count('CLASS ')} Java 8 classes")
        return 0
    if not BASELINE.is_file() or BASELINE.read_text(encoding="utf-8") != current:
        print("ERROR: M12 progression API differs from its immutable checkpoint", file=sys.stderr)
        return 1
    print(f"M12 binary/API compatibility PASS: {current.count('CLASS ')} Java 8 classes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
