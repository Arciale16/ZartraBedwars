#!/usr/bin/env python3
"""Perform a clean M1 build without resolving an unapproved clean plugin."""

from __future__ import annotations

import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[2]


def remove_build_outputs() -> int:
    removed = 0
    for target in sorted(ROOT.rglob("target")):
        if not target.is_dir():
            continue
        resolved = target.resolve()
        if ROOT.resolve() not in resolved.parents:
            raise SystemExit(f"Refusing to remove target outside repository: {resolved}")
        if ".tools" in target.parts or ".git" in target.parts:
            raise SystemExit(f"Refusing to remove protected path: {resolved}")
        shutil.rmtree(resolved)
        removed += 1
    return removed


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--jdk", default="21", choices=("8", "11", "16", "17", "21"))
    arguments = parser.parse_args()
    removed = remove_build_outputs()
    environment = os.environ.copy()
    environment["ZBW_COMPILE_JDK"] = arguments.jdk
    command = [sys.executable, str(ROOT / "tools" / "build" / "maven_wrapper.py"), "verify"]
    print(f"Removed {removed} Maven target director{'y' if removed == 1 else 'ies'}.")
    return subprocess.run(command, cwd=ROOT, env=environment, check=False).returncode


if __name__ == "__main__":
    raise SystemExit(main())
