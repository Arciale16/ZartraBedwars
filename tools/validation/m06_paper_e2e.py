#!/usr/bin/env python3
"""Run and certify the checksum-locked M06 Paper primary runtime."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[2]
LOCK_PATH = ROOT / "build" / "m06-paper-runtime-lock.json"


def digest(path: Path, algorithm: str) -> str:
    """Return a streaming digest for one evidence input."""
    value = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def require_file(path: Path, label: str) -> Path:
    """Fail deterministically when an input artifact is absent."""
    resolved = path.resolve()
    if not resolved.is_file():
        raise ValueError(f"{label} does not exist: {resolved}")
    return resolved


def write_properties(directory: Path) -> None:
    """Write the isolated, non-production certification server settings."""
    (directory / "eula.txt").write_text("eula=true\n", encoding="utf-8", newline="\n")
    (directory / "server.properties").write_text(
        "online-mode=false\n"
        "server-port=0\n"
        "max-players=1\n"
        "view-distance=2\n"
        "simulation-distance=2\n"
        "spawn-protection=0\n"
        "generate-structures=false\n"
        "level-name=m06_primary\n"
        "motd=ZartraBedWars M06 certification\n",
        encoding="utf-8",
        newline="\n",
    )


def validate_evidence(evidence: dict[str, object], expected_server: str,
                      process_output: str, return_code: int) -> None:
    """Enforce every primary-runtime certification assertion."""
    expected = {
        "runtime": "Paper 1.21.1 build 133",
        "server_sha256": expected_server,
        "operations": 5,
        "filesystem_evidence_off_owner": True,
        "leak_free_after_unload": True,
        "worker_shutdown": True,
        "success": True,
    }
    failures = [
        f"{key}: expected {value!r}, observed {evidence.get(key)!r}"
        for key, value in expected.items() if evidence.get(key) != value
    ]
    if return_code != 0:
        failures.append(f"Paper process returned {return_code}")
    for marker in (
        "M06 compatibility and world-provider foundation enabled",
        "M06 foundation shutdown initiated without owner-thread blocking",
    ):
        if marker not in process_output:
            failures.append(f"server log missing marker: {marker}")
    if "certification operation failed" in process_output:
        failures.append("server log reports a certification operation failure")
    if failures:
        raise ValueError("M06 Paper certification failed:\n- " + "\n- ".join(failures))


def run(arguments: argparse.Namespace) -> int:
    """Execute the isolated server and publish validated evidence."""
    lock = json.loads(LOCK_PATH.read_text(encoding="utf-8"))
    expected_server = lock["paper"]["server"]["sha256"]
    server = require_file(arguments.server, "Paper server")
    plugin = require_file(arguments.plugin, "M06 plugin")
    java = require_file(arguments.java, "Java launcher")
    observed_server = digest(server, "sha256")
    if observed_server != expected_server:
        raise ValueError(
            f"Paper server sha256 mismatch: expected {expected_server}, observed {observed_server}"
        )

    run_directory = arguments.run_directory.resolve()
    if run_directory.exists():
        shutil.rmtree(run_directory)
    (run_directory / "plugins").mkdir(parents=True)
    shutil.copy2(server, run_directory / "paper-1.21.1-133.jar")
    shutil.copy2(plugin, run_directory / "plugins" / "ZartraBedWars.jar")
    write_properties(run_directory)

    environment = os.environ.copy()
    environment["ZBW_M06_CERTIFY"] = "true"
    command = [
        str(java), "-Xms512M", "-Xmx1024M", "-Dcom.mojang.eula.agree=true",
        "-jar", "paper-1.21.1-133.jar", "--nogui",
    ]
    try:
        completed = subprocess.run(
            command, cwd=run_directory, env=environment, text=True,
            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
            timeout=arguments.timeout, check=False,
        )
    except subprocess.TimeoutExpired as failure:
        output = failure.stdout or ""
        if isinstance(output, bytes):
            output = output.decode("utf-8", errors="replace")
        (run_directory / "m06-paper-console.log").write_text(
            output, encoding="utf-8", newline="\n"
        )
        raise ValueError(f"Paper certification exceeded {arguments.timeout} seconds") from failure

    output = completed.stdout
    (run_directory / "m06-paper-console.log").write_text(
        output, encoding="utf-8", newline="\n"
    )
    source_evidence = run_directory / "plugins" / "ZartraBedWars" \
        / "m06-primary-certification.json"
    require_file(source_evidence, "runtime certification evidence")
    evidence = json.loads(source_evidence.read_text(encoding="utf-8"))
    validate_evidence(evidence, expected_server, output, completed.returncode)
    evidence["plugin_sha256"] = digest(plugin, "sha256")
    evidence["server_exit_code"] = completed.returncode
    evidence["certification_runner"] = "tools/validation/m06_paper_e2e.py"
    arguments.evidence_output.parent.mkdir(parents=True, exist_ok=True)
    arguments.evidence_output.write_text(
        json.dumps(evidence, indent=2, sort_keys=True) + "\n",
        encoding="utf-8", newline="\n",
    )
    print(
        "M06 Paper E2E PASS: Paper 1.21.1 build 133; "
        "5 operations; off-owner filesystem guard; owner-thread platform mutation; leak-free unload"
    )
    return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--server", type=Path, required=True)
    parser.add_argument("--plugin", type=Path, required=True)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--run-directory", type=Path,
                        default=ROOT / "target" / "m06-paper-e2e")
    parser.add_argument("--evidence-output", type=Path,
                        default=ROOT / "build" / "evidence" / "m06-paper-primary.json")
    parser.add_argument("--timeout", type=int, default=300)
    arguments = parser.parse_args()
    if arguments.timeout < 30 or arguments.timeout > 900:
        parser.error("--timeout must be between 30 and 900 seconds")
    try:
        return run(arguments)
    except (OSError, ValueError, json.JSONDecodeError) as failure:
        print(f"ERROR: {failure}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
