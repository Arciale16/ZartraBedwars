#!/usr/bin/env python3
"""Run every deterministic Milestone 8 governance and API validation gate."""

from __future__ import annotations

from pathlib import Path
import platform
import subprocess
import sys

from foundation import validate_all
from m02_architecture import validate as validate_m02_architecture
from m03_architecture import validate as validate_m03_architecture
from m04_architecture import validate as validate_m04_architecture
from m05_architecture import validate as validate_m05_architecture
from m06_architecture import validate as validate_m06_architecture
from m06_m22_allocation import validate as validate_m06_m22_allocation
from m07_architecture import validate as validate_m07_architecture
from m07_m09_allocation import validate as validate_m07_m09_allocation
from m08_architecture import validate as validate_m08_architecture
from m08_m09_allocation import validate as validate_m08_m09_allocation


ROOT = Path(__file__).resolve().parents[2]


def run(label: str, command: list[str]) -> bool:
    """Run one deterministic child gate."""
    print(f"== {label} ==", flush=True)
    return subprocess.run(command, cwd=ROOT, check=False).returncode == 0


def main() -> int:
    """Run the full M00-M08 validation chain."""
    failures: list[str] = []
    if platform.python_version() != "3.12.13":
        failures.append(f"Python runtime must be 3.12.13, found {platform.python_version()}")
    failures.extend(validate_all())
    failures.extend(validate_m02_architecture())
    failures.extend(validate_m03_architecture())
    failures.extend(validate_m04_architecture())
    failures.extend(validate_m05_architecture())
    failures.extend(validate_m06_m22_allocation())
    failures.extend(validate_m06_architecture())
    failures.extend(validate_m07_m09_allocation())
    failures.extend(validate_m07_architecture())
    failures.extend(validate_m08_m09_allocation())
    failures.extend(validate_m08_architecture())
    commands = [
        ("governance tests", [
            sys.executable, "-m", "unittest", "discover", "-s", "tests/governance", "-v",
        ]),
        ("dependency/licence/SBOM", [
            sys.executable, "tools/dependencies/lock_dependencies.py", "validate",
        ]),
        ("exact Maven repository", [
            sys.executable, "tools/dependencies/maven_lock.py", "validate", "--require-files",
        ]),
        ("Paper API acquisition lock", [
            sys.executable, "tools/dependencies/acquire_m06_paper.py", "verify",
        ]),
        ("database image lock", [
            sys.executable, "tools/ci/m04_database_images.py", "validate",
        ]),
        ("M02-M06 binary/API compatibility", [
            sys.executable, "tools/validation/api_compatibility.py", "check",
        ]),
        ("M07 binary/API compatibility", [
            sys.executable, "tools/validation/api_compatibility_m07.py", "check",
        ]),
        ("M08 binary/API compatibility", [
            sys.executable, "tools/validation/api_compatibility_m08.py", "check",
        ]),
        ("M08 strict JavaDoc", [sys.executable, "tools/validation/api_docs_m08.py"]),
        ("addon catalogue", [
            sys.executable, "tools/coverage/generate_addon_feature_catalog.py", "--check",
        ]),
        ("Master Prompt coverage", [
            sys.executable, "tools/coverage/generate_master_prompt_coverage.py", "--check",
        ]),
        ("pre-code decisions", [
            sys.executable, "tools/coverage/validate_preimplementation_decisions.py", "--check",
        ]),
    ]
    for label, command in commands:
        if not run(label, command):
            failures.append(f"{label} failed")
    if failures:
        for failure in failures:
            print(f"ERROR: {failure}", file=sys.stderr)
        return 1
    print("Milestone 8 deterministic validation PASS.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
