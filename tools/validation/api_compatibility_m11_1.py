#!/usr/bin/env python3
"""Generate or verify the additive M11.1 Phase 1 Java 8 API surface."""
from __future__ import annotations

import argparse
from pathlib import Path
import sys

import api_compatibility


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m11-1.txt"
CLASS_ROOTS = (
    ROOT / "scripting/zbw-scripting-engine/target/classes/io/zartra/bedwars/scripting/engine",
    ROOT / "configuration/zbw-config/target/classes/io/zartra/bedwars/config/m11",
    ROOT / "content/zbw-content/target/classes/io/zartra/bedwars/content/mode",
)


def observed() -> str:
    """Return the exact public API introduced by this corrective phase."""
    signatures: list[str] = []
    count = 0
    for root in CLASS_ROOTS:
        if not root.is_dir():
            raise ValueError(f"Missing M11.1 compiled classes: {root}")
        for path in sorted(root.rglob("*.class")):
            lines = api_compatibility.signature(path, 52)
            if lines:
                count += 1
                signatures.extend(lines)
    if not count:
        raise ValueError("No M11.1 public classes found")
    return "\n".join((
        "# ZartraBedWars M11.1 Phase 1 additive JVM binary API baseline",
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
        print(f"Generated M11.1 API baseline: {current.count('CLASS ')} classes")
        return 0
    if BASELINE.read_text(encoding="utf-8") != current:
        print("ERROR: M11.1 Phase 1 API differs from exact additive baseline", file=sys.stderr)
        return 1
    print(f"M11.1 binary/API compatibility PASS: {current.count('CLASS ')} Java 8 classes")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
