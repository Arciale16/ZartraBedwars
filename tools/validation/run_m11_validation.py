#!/usr/bin/env python3
"""Run deterministic M11 governance, inventory, API and documentation gates."""
from pathlib import Path
import platform
import subprocess
import sys
from foundation import validate_all

ROOT = Path(__file__).resolve().parents[2]

def run(label: str, command: list[str]) -> bool:
    print(f"== {label} ==", flush=True)
    return subprocess.run(command, cwd=ROOT, check=False).returncode == 0

def main() -> int:
    failures: list[str] = []
    if platform.python_version() != "3.12.13":
        failures.append(f"Python must be 3.12.13, found {platform.python_version()}")
    failures.extend(validate_all())
    commands = [
        ("governance tests", [sys.executable, "-m", "unittest", "discover", "-s", "tests/governance", "-v"]),
        ("M11 inventories", [sys.executable, "tools/validation/m11_inventories.py"]),
        ("feature dashboard", [sys.executable, "tools/validation/feature_dashboard.py"]),
        ("M11 binary API", [sys.executable, "tools/validation/api_compatibility_m11.py", "check"]),
        ("M11 strict JavaDoc", [sys.executable, "tools/validation/api_docs_m11.py"]),
        ("dependency licence SBOM", [sys.executable, "tools/dependencies/lock_dependencies.py", "validate"]),
        ("exact Maven repository", [sys.executable, "tools/dependencies/maven_lock.py", "validate", "--require-files"]),
        ("addon catalogue", [sys.executable, "tools/coverage/generate_addon_feature_catalog.py", "--check"]),
        ("Master Prompt coverage", [sys.executable, "tools/coverage/generate_master_prompt_coverage.py", "--check"]),
        ("pre-code decisions", [sys.executable, "tools/coverage/validate_preimplementation_decisions.py", "--check"]),
    ]
    for label, command in commands:
        if not run(label, command):
            failures.append(f"{label} failed")
    for failure in failures:
        print(f"ERROR: {failure}", file=sys.stderr)
    if failures:
        return 1
    print("Milestone 11 deterministic validation PASS.")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
