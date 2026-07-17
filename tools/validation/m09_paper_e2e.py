#!/usr/bin/env python3
"""Run the test-artifact-only M09 command/GUI scenario on locked Paper."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import zipfile


ROOT = Path(__file__).resolve().parents[2]
LOCK_PATH = ROOT / "build" / "m06-paper-runtime-lock.json"
HARNESS_CLASS = "io/zartra/bedwars/paper/m09/M09PaperCertificationPlugin"


def digest(path: Path) -> str:
    """Return a streaming SHA-256 digest."""
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def require_file(path: Path, label: str) -> Path:
    """Resolve and require one deterministic input file."""
    resolved = path.resolve()
    if not resolved.is_file():
        raise ValueError(f"{label} does not exist: {resolved}")
    return resolved


def build_harness(harness_classes: Path, destination: Path) -> None:
    """Package only the M09 test certification entry point."""
    if not harness_classes.is_dir():
        raise ValueError("compiled Paper test classes are required")
    selected = sorted(harness_classes.glob(f"{HARNESS_CLASS}*.class"))
    if not selected:
        raise ValueError("compiled M09 Paper certification class is missing")
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED) as bundle:
        bundle.writestr(
            "plugin.yml",
            "name: ZartraBedWarsM09Certification\n"
            "version: '0.1.0-SNAPSHOT'\n"
            "main: io.zartra.bedwars.paper.m09.M09PaperCertificationPlugin\n"
            "api-version: '1.21'\n"
            "load: POSTWORLD\n"
            "depend: [ZartraBedWars]\n"
            "commands:\n"
            "  zbw:\n"
            "    description: M09 certification command\n"
            "    usage: /zbw help\n",
        )
        for source in selected:
            bundle.write(source, source.relative_to(harness_classes).as_posix())


def write_properties(directory: Path) -> None:
    """Write isolated non-production server settings."""
    (directory / "eula.txt").write_text("eula=true\n", encoding="utf-8", newline="\n")
    (directory / "server.properties").write_text(
        "online-mode=false\nserver-port=0\nmax-players=1\nmax-tick-time=-1\nview-distance=2\n"
        "simulation-distance=2\nspawn-protection=0\ngenerate-structures=false\n"
        "level-name=m09_primary\nmotd=ZartraBedWars M09 certification\n",
        encoding="utf-8",
        newline="\n",
    )


def validate_evidence(evidence: dict[str, object], server_sha256: str,
                      output: str, return_code: int) -> None:
    """Enforce every exact-runtime M09 assertion."""
    expected = {
        "runtime": "Paper 1.21.1 build 133",
        "server_sha256": server_sha256,
        "command_dispatch": True,
        "inventory_rendering": True,
        "command_gui_parity": True,
        "async_off_owner": True,
        "duplicate_action_prevented": True,
        "evidence_written_off_owner": True,
        "catalog_actions": 87,
        "success": True,
    }
    failures = [
        f"{key}: expected {value!r}, observed {evidence.get(key)!r}"
        for key, value in expected.items()
        if evidence.get(key) != value
    ]
    if return_code != 0:
        failures.append(f"Paper process returned {return_code}")
    if "M06 compatibility and world-provider foundation enabled" not in output:
        failures.append("server log missing primary foundation enable marker")
    if failures:
        raise ValueError("M09 Paper certification failed:\n- " + "\n- ".join(failures))


def run(arguments: argparse.Namespace) -> int:
    """Build the excluded harness, run Paper, and publish certified evidence."""
    lock = json.loads(LOCK_PATH.read_text(encoding="utf-8"))
    expected_server = lock["paper"]["server"]["sha256"]
    server = require_file(arguments.server, "Paper server")
    plugin = require_file(arguments.plugin, "primary plugin")
    java = require_file(arguments.java, "Java launcher")
    if digest(server) != expected_server:
        raise ValueError("Paper server SHA-256 differs from the approved runtime lock")
    run_directory = arguments.run_directory.resolve()
    if run_directory.exists():
        shutil.rmtree(run_directory)
    plugins = run_directory / "plugins"
    plugins.mkdir(parents=True)
    shutil.copy2(server, run_directory / "paper-1.21.1-133.jar")
    shutil.copy2(plugin, plugins / "ZartraBedWars.jar")
    harness = plugins / "ZartraBedWarsM09Certification.jar"
    build_harness(arguments.harness_classes.resolve(), harness)
    write_properties(run_directory)
    completed = subprocess.run(
        [str(java), "-Xms512M", "-Xmx1024M", "-Dcom.mojang.eula.agree=true",
         "-jar", "paper-1.21.1-133.jar", "--nogui"],
        cwd=run_directory,
        env=os.environ.copy(),
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        timeout=arguments.timeout,
        check=False,
    )
    output = completed.stdout
    (run_directory / "m09-paper-console.log").write_text(
        output, encoding="utf-8", newline="\n"
    )
    source = plugins / "ZartraBedWarsM09Certification" / "m09-primary-certification.json"
    require_file(source, "M09 runtime evidence")
    evidence = json.loads(source.read_text(encoding="utf-8"))
    validate_evidence(evidence, expected_server, output, completed.returncode)
    evidence["primary_plugin_sha256"] = digest(plugin)
    evidence["test_harness_sha256"] = digest(harness)
    evidence["server_exit_code"] = completed.returncode
    evidence["certification_runner"] = "tools/validation/m09_paper_e2e.py"
    arguments.evidence_output.parent.mkdir(parents=True, exist_ok=True)
    arguments.evidence_output.write_text(
        json.dumps(evidence, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print("M09 Paper E2E PASS: command dispatch; inventory rendering; "
          "command/GUI parity; bounded off-owner execution; duplicate prevention")
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--server", type=Path, required=True)
    parser.add_argument("--plugin", type=Path, required=True)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--harness-classes", type=Path,
                        default=ROOT / "platform/paper/zbw-paper-modern/target/test-classes")
    parser.add_argument("--run-directory", type=Path,
                        default=ROOT / "target/m09-paper-e2e")
    parser.add_argument("--evidence-output", type=Path,
                        default=ROOT / "build/evidence/m09-paper-primary.json")
    parser.add_argument("--timeout", type=int, default=300)
    arguments = parser.parse_args()
    if arguments.timeout < 30 or arguments.timeout > 900:
        parser.error("--timeout must be between 30 and 900 seconds")
    try:
        return run(arguments)
    except (OSError, ValueError, json.JSONDecodeError, subprocess.TimeoutExpired) as failure:
        print(f"ERROR: {failure}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
