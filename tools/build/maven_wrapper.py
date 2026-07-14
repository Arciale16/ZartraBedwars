#!/usr/bin/env python3
"""Checksum-locked Maven launcher for the Milestone 1 empty reactor."""

from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import urllib.request
import zipfile


ROOT = Path(__file__).resolve().parents[2]
PROPERTIES = ROOT / ".mvn" / "wrapper" / "maven-wrapper.properties"
TOOLCHAINS = ROOT / "build" / "toolchains.json"


def fail(message: str) -> "NoReturn":
    print(f"M1 wrapper error: {message}", file=sys.stderr)
    raise SystemExit(2)


def read_properties(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        key, separator, value = line.partition("=")
        if not separator or not key.strip() or not value.strip():
            fail(f"invalid property in {path}: {raw_line!r}")
        values[key.strip()] = value.strip()
    return values


def hash_file(path: Path, algorithm: str) -> str:
    digest = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def download(url: str, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "ZartraBedWars-M01/1"})
    with urllib.request.urlopen(request, timeout=60) as response:
        with tempfile.NamedTemporaryFile(dir=destination.parent, delete=False) as temporary:
            temporary_path = Path(temporary.name)
            shutil.copyfileobj(response, temporary, length=1024 * 1024)
    os.replace(temporary_path, destination)


def validate_archive(path: Path, properties: dict[str, str]) -> None:
    for algorithm, key in (("sha256", "distributionSha256Sum"), ("sha512", "distributionSha512Sum")):
        expected = properties[key].lower()
        actual = hash_file(path, algorithm)
        if actual != expected:
            path.unlink(missing_ok=True)
            fail(f"Maven archive {algorithm} mismatch: expected {expected}, received {actual}")


def safe_extract(archive: Path, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=destination.parent) as temporary_directory:
        temporary_root = Path(temporary_directory).resolve()
        with zipfile.ZipFile(archive) as bundle:
            for member in bundle.infolist():
                member_path = (temporary_root / member.filename).resolve()
                if temporary_root not in member_path.parents and member_path != temporary_root:
                    fail(f"unsafe Maven archive member: {member.filename}")
                unix_mode = member.external_attr >> 16
                if stat.S_ISLNK(unix_mode):
                    fail(f"symbolic link rejected in Maven archive: {member.filename}")
            bundle.extractall(temporary_root)
        extracted = temporary_root / destination.name
        if not extracted.is_dir():
            fail(f"Maven archive did not contain {destination.name}")
        if destination.exists():
            shutil.rmtree(destination)
        shutil.move(str(extracted), destination)


def java_executable() -> Path:
    java_home = os.environ.get("JAVA_HOME")
    executable_name = "java.exe" if os.name == "nt" else "java"
    if java_home:
        candidate = Path(java_home) / "bin" / executable_name
        if candidate.is_file():
            return candidate
    discovered = shutil.which("java")
    if discovered:
        return Path(discovered)
    fail("JAVA_HOME does not identify a JDK and java is not on PATH")


def verify_python(toolchains: dict[str, object]) -> None:
    expected = str(toolchains["python"]["version"])
    actual = platform.python_version()
    if actual != expected:
        fail(f"Python {expected} is required; current interpreter is {actual}")


def verify_java(toolchains: dict[str, object]) -> str:
    requested = os.environ.get("ZBW_COMPILE_JDK", "21")
    rows = {str(row["id"]): row for row in toolchains["jdks"]}
    if requested not in rows:
        fail(f"ZBW_COMPILE_JDK must be one of {', '.join(rows)}")
    java = java_executable()
    result = subprocess.run(
        [str(java), "-XshowSettings:properties", "-version"],
        cwd=ROOT,
        check=False,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
    )
    if result.returncode != 0:
        fail("java version inspection failed")
    version_match = re.search(r"^\s*java\.version\s*=\s*(\S+)\s*$", result.stdout, re.MULTILINE)
    vendor_match = re.search(r"^\s*java\.vendor\s*=\s*(.+?)\s*$", result.stdout, re.MULTILINE)
    if not version_match or not vendor_match:
        fail("unable to parse Java version/vendor properties")
    actual_version = version_match.group(1)
    actual_vendor = vendor_match.group(1)
    expected_version = str(rows[requested]["java_version"])
    if actual_version != expected_version:
        fail(f"JDK {requested} must report {expected_version}; current JDK reports {actual_version}")
    if "adoptium" not in actual_vendor.lower() and "temurin" not in result.stdout.lower():
        fail(f"JDK vendor must be Eclipse Temurin; current vendor is {actual_vendor}")
    return requested


def validate_arguments(arguments: list[str]) -> None:
    if "clean" in arguments:
        fail("the Maven clean plugin is not approved; use tools/build/clean_build.py")
    plugin_goals = [argument for argument in arguments if ":" in argument and not argument.startswith("-D")]
    if plugin_goals:
        fail(f"direct plugin goals are default-denied by the M1 lock: {', '.join(plugin_goals)}")


def main(arguments: list[str]) -> int:
    properties = read_properties(PROPERTIES)
    toolchains = json.loads(TOOLCHAINS.read_text(encoding="utf-8"))
    verify_python(toolchains)
    verified_jdk = verify_java(toolchains)
    validate_arguments(arguments)

    version = properties["mavenVersion"]
    archive = ROOT / ".tools" / "downloads" / f"apache-maven-{version}-bin.zip"
    distribution = ROOT / ".tools" / "maven" / f"apache-maven-{version}"
    if not archive.is_file():
        download(properties["distributionUrl"], archive)
    validate_archive(archive, properties)
    if not distribution.is_dir():
        safe_extract(archive, distribution)

    launcher = distribution / "bin" / ("mvn.cmd" if os.name == "nt" else "mvn")
    if not launcher.is_file():
        fail(f"Maven launcher missing after extraction: {launcher}")
    if os.name != "nt":
        launcher.chmod(launcher.stat().st_mode | stat.S_IXUSR)

    environment = os.environ.copy()
    environment["ZBW_VERIFIED_COMPILE_JDK"] = verified_jdk
    repository = ROOT / ".m2" / "repository"
    repository.mkdir(parents=True, exist_ok=True)
    command = [str(launcher), f"-Dmaven.repo.local={repository}", "--offline", *arguments]
    return subprocess.run(command, cwd=ROOT, env=environment, check=False).returncode


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
