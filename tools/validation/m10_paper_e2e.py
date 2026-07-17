#!/usr/bin/env python3
"""Run the test-artifact-only M10 scenario on the locked Paper runtime."""

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
LOCK = ROOT / "build/m06-paper-runtime-lock.json"
HARNESS = "io/zartra/bedwars/paper/m10/M10PaperCertificationPlugin"

def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()

def require(path: Path, label: str) -> Path:
    path = path.resolve()
    if not path.is_file():
        raise ValueError(f"{label} missing: {path}")
    return path

def harness(classes: Path, output: Path) -> None:
    selected = sorted(classes.glob(f"{HARNESS}*.class"))
    if not selected:
        raise ValueError("compiled M10 certification class missing")
    with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as bundle:
        bundle.writestr("plugin.yml", "name: ZartraBedWarsM10Certification\nversion: '0.1.0-SNAPSHOT'\nmain: io.zartra.bedwars.paper.m10.M10PaperCertificationPlugin\napi-version: '1.21'\nload: POSTWORLD\ndepend: [ZartraBedWars]\ncommands:\n  zbw:\n    description: M10 certification command\n")
        for source in selected:
            bundle.write(source, source.relative_to(classes).as_posix())

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--server", type=Path, required=True)
    parser.add_argument("--plugin", type=Path, required=True)
    parser.add_argument("--java", type=Path, required=True)
    parser.add_argument("--harness-classes", type=Path, default=ROOT / "platform/paper/zbw-paper-modern/target/test-classes")
    parser.add_argument("--run-directory", type=Path, default=ROOT / "target/m10-paper-e2e")
    parser.add_argument("--evidence-output", type=Path, default=ROOT / "build/evidence/m10-paper-primary.json")
    parser.add_argument("--timeout", type=int, default=300)
    args = parser.parse_args()
    try:
        lock = json.loads(LOCK.read_text(encoding="utf-8"))
        expected = lock["paper"]["server"]["sha256"]
        server, plugin, java = require(args.server, "Paper"), require(args.plugin, "plugin"), require(args.java, "Java")
        if digest(server) != expected:
            raise ValueError("Paper digest differs from approved runtime lock")
        if args.run_directory.exists():
            shutil.rmtree(args.run_directory)
        plugins = args.run_directory / "plugins"
        plugins.mkdir(parents=True)
        shutil.copy2(server, args.run_directory / "paper-1.21.1-133.jar")
        shutil.copy2(plugin, plugins / "ZartraBedWars.jar")
        harness_jar = plugins / "ZartraBedWarsM10Certification.jar"
        harness(args.harness_classes.resolve(), harness_jar)
        (args.run_directory / "eula.txt").write_text("eula=true\n", encoding="utf-8", newline="\n")
        (args.run_directory / "server.properties").write_text("online-mode=false\nserver-port=0\nmax-players=1\nmax-tick-time=-1\nview-distance=2\nsimulation-distance=2\nspawn-protection=0\ngenerate-structures=false\nlevel-name=m10_primary\n", encoding="utf-8", newline="\n")
        completed = subprocess.run([str(java), "-Xms512M", "-Xmx1024M", "-Dcom.mojang.eula.agree=true", "-jar", "paper-1.21.1-133.jar", "--nogui"], cwd=args.run_directory, env=os.environ.copy(), text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=args.timeout, check=False)
        (args.run_directory / "m10-paper-console.log").write_text(completed.stdout, encoding="utf-8", newline="\n")
        source = require(plugins / "ZartraBedWarsM10Certification/m10-primary-certification.json", "M10 evidence")
        evidence = json.loads(source.read_text(encoding="utf-8"))
        keys = ("selector_rendering", "selector_click", "queue_command_dispatch", "gui_queue_action", "command_gui_parity", "owner_thread_projection", "bounded_off_owner_matching", "duplicate_action_prevented", "stale_view_rejected", "deterministic_cleanup", "success")
        failures = [key for key in keys if evidence.get(key) is not True]
        if evidence.get("runtime") != "Paper 1.21.1 build 133" or evidence.get("server_sha256") != expected or evidence.get("catalog_actions") != 115 or completed.returncode != 0:
            failures.append("runtime/digest/catalog/exit")
        if failures:
            raise ValueError("M10 Paper certification failed: " + ", ".join(failures))
        evidence.update({"primary_plugin_sha256": digest(plugin), "test_harness_sha256": digest(harness_jar), "server_exit_code": completed.returncode, "certification_runner": "tools/validation/m10_paper_e2e.py"})
        args.evidence_output.parent.mkdir(parents=True, exist_ok=True)
        args.evidence_output.write_text(json.dumps(evidence, indent=2, sort_keys=True) + "\n", encoding="utf-8", newline="\n")
        print("M10 Paper E2E PASS: selector, queue command/GUI, parity, owner thread, bounded worker, stale/duplicate/cleanup")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, subprocess.TimeoutExpired) as failure:
        print(f"ERROR: {failure}", file=sys.stderr)
        return 1

if __name__ == "__main__":
    raise SystemExit(main())
