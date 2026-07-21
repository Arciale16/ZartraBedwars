#!/usr/bin/env python3
"""Generate or verify additive M13 Phase 2 progression API compatibility."""
from __future__ import annotations

import argparse
from pathlib import Path
import sys

import api_compatibility_m12


ROOT = Path(__file__).resolve().parents[2]
BASELINE = ROOT / "build/api-signature-baseline-m13-phase2.txt"
PRIOR = ROOT / "build/api-signature-baseline-m13-phase1.txt"


def missing(baseline: Path, current: str) -> list[str]:
    """Return immutable prior signatures absent from the current API."""
    observed = set(current.splitlines())
    return [
        line for line in baseline.read_text(encoding="utf-8").splitlines()
        if line and not line.startswith("#") and line not in observed
    ]


def current() -> str:
    """Return the exact M13 Phase 2 progression checkpoint."""
    observed = api_compatibility_m12.observed()
    lines = observed.splitlines()
    lines[0] = "# ZartraBedWars M13 Phase 2 progression JVM binary API baseline"
    return "\n".join(lines) + "\n"


def main() -> int:
    """Generate the checkpoint or verify it preserves every Phase 1 signature."""
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("generate", "check"))
    command = parser.parse_args().command
    observed = current()
    if command == "generate":
        BASELINE.write_text(observed, encoding="utf-8", newline="\n")
        print(f"Generated M13 Phase 2 API baseline: {observed.count('CLASS ')} Java 8 classes")
        return 0
    if not BASELINE.is_file() or BASELINE.read_text(encoding="utf-8") != observed:
        print("ERROR: M13 Phase 2 progression API differs from its immutable checkpoint", file=sys.stderr)
        return 1
    prior_missing = missing(PRIOR, observed)
    if prior_missing:
        print("ERROR: M13 Phase 2 removed an immutable Phase 1 signature", file=sys.stderr)
        return 1
    print(
        f"M13 Phase 2 binary/API compatibility PASS: {observed.count('CLASS ')} Java 8 classes; "
        "immutable Phase 1 baseline preserved"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
