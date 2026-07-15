#!/usr/bin/env python3
"""Generate strict Java 8 JavaDoc and a deterministic current documentation archive."""

from __future__ import annotations

import os
from pathlib import Path
import shutil
import subprocess
import zipfile


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "target" / "apidocs"
ARCHIVE = ROOT / "target" / "zartrabedwars-m03-javadoc.zip"
MODULES = (
    "api/zbw-api", "domain/zbw-domain", "application/zbw-application", "sdk/zbw-sdk",
    "integrations/discord/zbw-integration-discord-api", "configuration/zbw-config",
)


def executable() -> Path:
    name = "javadoc.exe" if os.name == "nt" else "javadoc"
    for variable in ("JAVA8_HOME", "JAVA_HOME"):
        home = os.environ.get(variable)
        if home and (Path(home) / "bin" / name).is_file():
            candidate = Path(home) / "bin" / name
            version = subprocess.run(
                [str(candidate), "-J-version"], text=True, stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT, check=False).stdout
            if '1.8.0_442' in version:
                return candidate
    candidates = sorted((ROOT / ".tools" / "jdks").rglob(name))
    for candidate in candidates:
        version = subprocess.run(
            [str(candidate), "-J-version"], text=True, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, check=False).stdout
        if '1.8.0_442' in version:
            return candidate
    raise SystemExit("Exact Temurin 8u442 JavaDoc executable is required")


def main() -> int:
    sources = []
    for module in MODULES:
        sources.extend(sorted((ROOT / module / "src" / "main" / "java").rglob("*.java")))
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    OUTPUT.mkdir(parents=True)
    command = [
        str(executable()), "-quiet", "-Xdoclint:all,-missing", "-notimestamp", "-encoding", "UTF-8",
        "-source", "8", "-d", str(OUTPUT), *[str(path) for path in sources],
    ]
    result = subprocess.run(command, cwd=ROOT, check=False)
    if result.returncode:
        return result.returncode
    with zipfile.ZipFile(ARCHIVE, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for path in sorted(OUTPUT.rglob("*")):
            if not path.is_file():
                continue
            info = zipfile.ZipInfo(path.relative_to(OUTPUT).as_posix(), (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            archive.writestr(info, path.read_bytes())
    print(f"JavaDoc PASS: {len(sources)} sources; archive {ARCHIVE.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
