#!/usr/bin/env python3
"""Run every deterministic Milestone 1 governance check."""

from __future__ import annotations

import argparse
from pathlib import Path
import platform
import subprocess
import sys

from foundation import validate_all


ROOT = Path(__file__).resolve().parents[2]


def run(label: str, command: list[str]) -> bool:
    print(f"== {label} ==", flush=True)
    result = subprocess.run(command, cwd=ROOT, check=False)
    return result.returncode == 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--skip-tests", action="store_true")
    arguments = parser.parse_args()
    failures: list[str] = []
    if platform.python_version() != "3.12.13":
        failures.append(f"Python runtime must be 3.12.13, found {platform.python_version()}")

    structural_errors = validate_all()
    if structural_errors:
        failures.extend(structural_errors)
    else:
        print("Foundation/static gate PASS: module graph, bytecode, fixtures, assets, quality and CI.")

    commands = [
        ("dependency/licence/SBOM gate", [sys.executable, "tools/dependencies/lock_dependencies.py", "validate"]),
        ("addon catalogue", [sys.executable, "tools/coverage/generate_addon_feature_catalog.py", "--check"]),
        ("Master Prompt coverage", [sys.executable, "tools/coverage/generate_master_prompt_coverage.py", "--check"]),
        ("pre-code decisions", [sys.executable, "tools/coverage/validate_preimplementation_decisions.py", "--check"]),
    ]
    if not arguments.skip_tests:
        commands.insert(0, ("governance unit tests", [sys.executable, "-m", "unittest", "discover", "-s", "tests/governance", "-v"]))
    for label, command in commands:
        if not run(label, command):
            failures.append(f"{label} failed")

    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        return 1
    print("Milestone 1 deterministic validation PASS.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
