#!/usr/bin/env python3
"""Acquire checksum-locked M06 Paper artifacts without redistributing them."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import shutil
import urllib.request


ROOT = Path(__file__).resolve().parents[2]
LOCK = ROOT / "build" / "m06-paper-runtime-lock.json"
REPOSITORY = ROOT / ".m2" / "repository"


def digest(path: Path, algorithm: str) -> str:
    """Return a streaming hexadecimal digest."""
    value = hashlib.new(algorithm)
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            value.update(block)
    return value.hexdigest()


def download(url: str, destination: Path, algorithm: str, expected: str) -> None:
    """Download to a temporary file and atomically publish only an exact match."""
    destination.parent.mkdir(parents=True, exist_ok=True)
    temporary = destination.with_suffix(destination.suffix + ".part")
    request = urllib.request.Request(
        url, headers={"User-Agent": "ZartraBedWars-M06-Certification/1.0"}
    )
    with urllib.request.urlopen(request, timeout=60) as response, temporary.open("wb") as output:
        shutil.copyfileobj(response, output)
    actual = digest(temporary, algorithm)
    if actual != expected:
        temporary.unlink(missing_ok=True)
        raise ValueError(f"{destination.name}: {algorithm} mismatch: {actual}")
    temporary.replace(destination)


def acquire_api(lock: dict[str, object]) -> Path:
    """Install the exact API under the immutable, non-SNAPSHOT local mirror coordinate."""
    api = lock["paper"]["api"]
    version = api["mirror_coordinate"].split(":")[2]
    directory = REPOSITORY / "io" / "zartra" / "mirror" / "paper" / "paper-api" / version
    jar = directory / f"paper-api-{version}.jar"
    if not jar.is_file() or digest(jar, "sha512") != api["sha512"]:
        download(api["url"], jar, "sha512", api["sha512"])
    pom = directory / f"paper-api-{version}.pom"
    pom.write_text(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n"
        "  <modelVersion>4.0.0</modelVersion>\n"
        "  <groupId>io.zartra.mirror.paper</groupId>\n"
        "  <artifactId>paper-api</artifactId>\n"
        f"  <version>{version}</version>\n"
        "  <name>Paper API build 133 immutable compile-only mirror</name>\n"
        "  <licenses><license><name>GNU General Public License v3.0 only</name>"
        "<url>https://www.gnu.org/licenses/gpl-3.0-standalone.html</url>"
        "<distribution>repo</distribution></license></licenses>\n"
        "</project>\n",
        encoding="utf-8",
        newline="\n",
    )
    (directory / "_remote.repositories").write_text(
        f"paper-api-{version}.jar>=\npaper-api-{version}.pom>=\n",
        encoding="utf-8",
        newline="\n",
    )
    return jar


def acquire_server(lock: dict[str, object], destination: Path) -> Path:
    """Acquire the exact non-redistributed server fixture."""
    server = lock["paper"]["server"]
    if not destination.is_file() or digest(destination, "sha256") != server["sha256"]:
        download(server["url"], destination, "sha256", server["sha256"])
    return destination


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("artifact", choices=("api", "server", "verify"))
    parser.add_argument("--server-destination", type=Path,
                        default=ROOT / ".tools" / "paper" / "paper-1.21.1-133.jar")
    arguments = parser.parse_args()
    lock = json.loads(LOCK.read_text(encoding="utf-8"))
    if arguments.artifact in {"api", "verify"}:
        jar = acquire_api(lock)
        print(f"Paper API PASS: {jar} sha512={digest(jar, 'sha512')}")
    if arguments.artifact == "server":
        server = acquire_server(lock, arguments.server_destination)
        print(f"Paper server PASS: {server} sha256={digest(server, 'sha256')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
