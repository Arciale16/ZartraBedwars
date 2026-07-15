#!/usr/bin/env python3
"""Run the test-artifact-only M07 arena/world scenario on checksum-locked Paper."""

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
HARNESS_CLASS = "io/zartra/bedwars/paper/m07/M07PaperCertificationPlugin"


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


def build_harness(arena_classes: Path, harness_classes: Path, destination: Path) -> None:
    """Package only M07 production classes plus the test certification entry point."""
    if not arena_classes.is_dir() or not harness_classes.is_dir():
        raise ValueError("compiled arena and Paper test classes are required")
    selected = sorted(harness_classes.glob(f"{HARNESS_CLASS}*.class"))
    if not selected:
        raise ValueError("compiled M07 Paper certification class is missing")
    with zipfile.ZipFile(destination, "w", zipfile.ZIP_DEFLATED) as bundle:
        bundle.writestr(
            "plugin.yml",
            "name: ZartraBedWarsM07Certification\n"
            "version: '0.1.0-SNAPSHOT'\n"
            "main: io.zartra.bedwars.paper.m07.M07PaperCertificationPlugin\n"
            "api-version: '1.21'\n"
            "load: POSTWORLD\n"
            "depend: [ZartraBedWars]\n",
        )
        for source in sorted(arena_classes.rglob("*.class")):
            bundle.write(source, source.relative_to(arena_classes).as_posix())
        for source in selected:
            bundle.write(source, source.relative_to(harness_classes).as_posix())


def write_properties(directory: Path) -> None:
    """Write isolated non-production server settings."""
    (directory / "eula.txt").write_text("eula=true\n", encoding="utf-8", newline="\n")
    (directory / "server.properties").write_text(
        "online-mode=false\n"
        "server-port=0\n"
        "max-players=1\n"
        "view-distance=2\n"
        "simulation-distance=2\n"
        "spawn-protection=0\n"
        "generate-structures=false\n"
        "level-name=m07_primary\n"
        "motd=ZartraBedWars M07 certification\n",
        encoding="utf-8",
        newline="\n",
    )


def validate_evidence(evidence: dict[str, object], server_sha256: str,
                      output: str, return_code: int) -> None:
    """Enforce every exact-runtime M07 assertion."""
    expected = {
        "runtime": "Paper 1.21.1 build 133",
        "server_sha256": server_sha256,
        "arena_validation": True,
        "setup_undo_redo": True,
        "archive_round_trip": True,
        "operations": 5,
        "filesystem_evidence_off_owner": True,
        "leak_free_after_unload": True,
        "success": True,
    }
    failures = [
        f"{key}: expected {value!r}, observed {evidence.get(key)!r}"
        for key, value in expected.items()
        if evidence.get(key) != value
    ]
    if return_code != 0:
        failures.append(f"Paper process returned {return_code}")
    for marker in (
        "M06 compatibility and world-provider foundation enabled",
        "M06 foundation shutdown initiated without owner-thread blocking",
    ):
        if marker not in output:
            failures.append(f"server log missing marker: {marker}")
    if failures:
        raise ValueError("M07 Paper certification failed:\n- " + "\n- ".join(failures))


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
    harness = plugins / "ZartraBedWarsM07Certification.jar"
    build_harness(arguments.arena_classes.resolve(), arguments.harness_classes.resolve(), harness)
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
    (run_directory / "m07-paper-console.log").write_text(
        output, encoding="utf-8", newline="\n"
    )
    source = plugins / "ZartraBedWarsM07Certification" / "m07-primary-certification.json"
    require_file(source, "M07 runtime evidence")
    evidence = json.loads(source.read_text(encoding="utf-8"))
    validate_evidence(evidence, expected_server, output, completed.returncode)
    evidence["primary_plugin_sha256"] = digest(plugin)
    evidence["test_harness_sha256"] = digest(harness)
    evidence["server_exit_code"] = completed.returncode
    evidence["certification_runner"] = "tools/validation/m07_paper_e2e.py"
    arguments.evidence_output.parent.mkdir(parents=True, exist_ok=True)
    arguments.evidence_output.write_text(
        json.dumps(evidence, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(
        "M07 Paper E2E PASS: Paper 1.21.1 build 133; arena validation; setup undo/redo; "
        "archive round trip; 5 owner/worker-affine world operations; leak-free unload"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--server", type=Path, required=True)
    parser.add_argument("--plugin", type=Path, required=True)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--arena-classes", type=Path,
                        default=ROOT / "arena/zbw-arena/target/classes")
    parser.add_argument("--harness-classes", type=Path,
                        default=ROOT / "platform/paper/zbw-paper-modern/target/test-classes")
    parser.add_argument("--run-directory", type=Path,
                        default=ROOT / "target/m07-paper-e2e")
    parser.add_argument("--evidence-output", type=Path,
                        default=ROOT / "build/evidence/m07-paper-primary.json")
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
