#!/usr/bin/env python3
"""Install one checksum-locked Linux Temurin toolchain for CI."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import tempfile
import urllib.request


ROOT = Path(__file__).resolve().parents[2]
ACQUISITION = ROOT / "build" / "dependency-acquisition.json"
TOOLCHAINS = ROOT / "build" / "toolchains.json"


def digest(path: Path) -> str:
    value = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "ZartraBedWars-M01/1"})
    with urllib.request.urlopen(request, timeout=90) as response:
        with tempfile.NamedTemporaryFile(dir=destination.parent, delete=False) as temporary:
            temporary_path = Path(temporary.name)
            shutil.copyfileobj(response, temporary, length=1024 * 1024)
    os.replace(temporary_path, destination)


def install(jdk_id: str) -> Path:
    acquisition = json.loads(ACQUISITION.read_text(encoding="utf-8"))
    toolchains = json.loads(TOOLCHAINS.read_text(encoding="utf-8"))
    rows = {str(row["id"]): row for row in toolchains["jdks"]}
    if jdk_id not in rows:
        raise SystemExit(f"Unknown JDK id {jdk_id!r}")
    prefix = f"tool:temurin:{rows[jdk_id]['release']}:linux-x64"
    matches = [artifact for artifact in acquisition["artifacts"] if artifact["id"] == prefix]
    if len(matches) != 1:
        raise SystemExit(f"Expected one acquisition row for {prefix}")
    artifact = matches[0]
    archive = ROOT / ".tools" / "downloads" / artifact["cache_filename"]
    if not archive.is_file():
        download(artifact["source"], archive)
    actual = digest(archive)
    if actual != artifact["sha256"]:
        archive.unlink(missing_ok=True)
        raise SystemExit(f"JDK {jdk_id} checksum mismatch: {actual}")

    destination = ROOT / ".tools" / "jdks" / f"linux-{jdk_id}"
    marker = destination / ".verified-sha256"
    if not marker.is_file() or marker.read_text(encoding="ascii").strip() != actual:
        if destination.exists():
            shutil.rmtree(destination)
        destination.parent.mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(dir=destination.parent) as temporary_directory:
            temporary_root = Path(temporary_directory).resolve()
            with tarfile.open(archive, "r:gz") as bundle:
                bundle.extractall(temporary_root, filter="data")
            roots = [item for item in temporary_root.iterdir() if item.is_dir()]
            if len(roots) != 1:
                raise SystemExit(f"JDK {jdk_id} archive must contain exactly one root directory")
            shutil.move(str(roots[0]), destination)
        marker.write_text(actual + "\n", encoding="ascii")

    java = destination / "bin" / "java"
    if not java.is_file():
        raise SystemExit(f"JDK {jdk_id} is missing bin/java")
    java.chmod(java.stat().st_mode | 0o100)
    result = subprocess.run([str(java), "-version"], text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
    if result.returncode != 0 or rows[jdk_id]["java_version"] not in result.stdout or "Temurin" not in result.stdout:
        raise SystemExit(f"JDK {jdk_id} identity verification failed: {result.stdout.strip()}")
    return destination.resolve()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--id", required=True, choices=("8", "11", "16", "17", "21"))
    parser.add_argument("--github-env", type=Path)
    parser.add_argument("--github-path", type=Path)
    arguments = parser.parse_args()
    java_home = install(arguments.id)
    if arguments.github_env:
        with arguments.github_env.open("a", encoding="utf-8") as stream:
            stream.write(f"JAVA_HOME={java_home}\n")
            stream.write(f"ZBW_COMPILE_JDK={arguments.id}\n")
    if arguments.github_path:
        with arguments.github_path.open("a", encoding="utf-8") as stream:
            stream.write(f"{java_home / 'bin'}\n")
    print(java_home)
    return 0


if __name__ == "__main__":
    sys.exit(main())
