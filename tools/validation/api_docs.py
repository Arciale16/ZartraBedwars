#!/usr/bin/env python3
"""Generate strict Java 8/21 M06 JavaDoc and deterministic archives."""

from __future__ import annotations

import os
from pathlib import Path
import shutil
import subprocess
import zipfile


ROOT = Path(__file__).resolve().parents[2]
NEUTRAL_OUTPUT = ROOT / "target" / "apidocs-neutral"
MODERN_OUTPUT = ROOT / "target" / "apidocs-modern"
NEUTRAL_ARCHIVE = ROOT / "target" / "zartrabedwars-m06-neutral-javadoc.zip"
MODERN_ARCHIVE = ROOT / "target" / "zartrabedwars-m06-modern-javadoc.zip"
LEGACY_M05_ARCHIVE = ROOT / "target" / "zartrabedwars-m05-javadoc.zip"
NEUTRAL_MODULES = (
    "api/zbw-api", "domain/zbw-domain", "application/zbw-application", "sdk/zbw-sdk",
    "integrations/discord/zbw-integration-discord-api", "configuration/zbw-config",
    "storage/zbw-storage-api", "storage/zbw-storage-sql", "observability/zbw-observability",
    "compatibility/zbw-compat-api", "world/zbw-world",
)
MODERN_MODULES = (
    "compatibility/zbw-compat-v1_20-v1_21",
    "platform/paper/zbw-paper-modern",
)
MODERN_CLASSPATH_MODULES = NEUTRAL_MODULES + (
    "arena/zbw-arena",
    "game/zbw-game",
    "scripting/zbw-scripting-api",
    "shop/zbw-shop",
    "content/zbw-content",
    "command/zbw-command-api",
    "ui/zbw-ui-api",
)


def executable(jdk: str, expected: str) -> Path:
    """Find the exact locked JavaDoc executable for one artifact boundary."""
    name = "javadoc.exe" if os.name == "nt" else "javadoc"
    variables = (f"JAVA{jdk}_HOME", "JAVA_HOME")
    candidates: list[Path] = []
    for variable in variables:
        home = os.environ.get(variable)
        if home:
            candidates.append(Path(home) / "bin" / name)
    candidates.extend(sorted((ROOT / ".tools" / "jdks").rglob(name)))
    for candidate in candidates:
        if not candidate.is_file():
            continue
        version = subprocess.run(
            [str(candidate), "-J-version"], text=True, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, check=False).stdout
        if expected in version:
            return candidate
    raise SystemExit(f"Exact Temurin {jdk} JavaDoc executable containing {expected} is required")


def sources(modules: tuple[str, ...]) -> list[Path]:
    """Return all production sources in deterministic path order."""
    result: list[Path] = []
    for module in modules:
        result.extend(sorted((ROOT / module / "src" / "main" / "java").rglob("*.java")))
    return result


def artifacts(modules: tuple[str, ...]) -> list[Path]:
    """Return exact reactor artifacts used as the documentation class path."""
    result: list[Path] = []
    for module in modules:
        artifact_id = Path(module).name
        artifact = ROOT / module / "target" / f"{artifact_id}-0.1.0-SNAPSHOT.jar"
        if not artifact.is_file():
            raise SystemExit(f"Missing reactor artifact for strict JavaDoc: {artifact}")
        result.append(artifact)
    return result


def archive(output: Path, destination: Path) -> None:
    """Create a timestamp-stable JavaDoc ZIP."""
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED,
                         compresslevel=9) as bundle:
        for path in sorted(output.rglob("*")):
            if not path.is_file():
                continue
            info = zipfile.ZipInfo(path.relative_to(output).as_posix(), (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = 0o100644 << 16
            bundle.writestr(info, path.read_bytes())


def generate(javadoc: Path, source: str, source_paths: list[Path], output: Path,
             classpath: list[Path]) -> int:
    """Run strict JavaDoc for one bytecode family."""
    if output.exists():
        shutil.rmtree(output)
    output.mkdir(parents=True)
    arguments = [
        "-quiet", "-Xdoclint:all,-missing", "-notimestamp",
        "-encoding", "UTF-8", "-source", source,
        "-classpath", os.pathsep.join(str(path) for path in classpath),
        "-d", str(output), *[str(path) for path in source_paths],
    ]
    argument_file = output.parent / f".{output.name}-javadoc.args"
    serialized = []
    for argument in arguments:
        normalized = argument.replace("\\", "/")
        serialized.append('"' + normalized.replace('"', '\\"') + '"')
    argument_file.write_text("\n".join(serialized) + "\n", encoding="utf-8")
    try:
        return subprocess.run(
            [str(javadoc), f"@{argument_file}"], cwd=ROOT, check=False).returncode
    finally:
        argument_file.unlink(missing_ok=True)


def main() -> int:
    neutral_sources = sources(NEUTRAL_MODULES)
    neutral_classpath = [
        ROOT / ".m2/repository/com/zaxxer/HikariCP/4.0.3/HikariCP-4.0.3.jar",
        ROOT / ".m2/repository/com/github/ben-manes/caffeine/caffeine/2.9.3/caffeine-2.9.3.jar",
    ]
    result = generate(executable("8", "1.8.0_442"), "8", neutral_sources,
                      NEUTRAL_OUTPUT, neutral_classpath)
    if result:
        return result
    archive(NEUTRAL_OUTPUT, NEUTRAL_ARCHIVE)
    shutil.copyfile(NEUTRAL_ARCHIVE, LEGACY_M05_ARCHIVE)

    modern_sources = sources(MODERN_MODULES)
    modern_classpath = [
        ROOT / ".m2/repository/io/zartra/mirror/paper/paper-api/1.21.1-build133/"
        "paper-api-1.21.1-build133.jar",
        *artifacts(MODERN_CLASSPATH_MODULES),
    ]
    result = generate(executable("21", "21.0.6"), "21", modern_sources,
                      MODERN_OUTPUT, modern_classpath)
    if result:
        return result
    archive(MODERN_OUTPUT, MODERN_ARCHIVE)
    print(
        f"JavaDoc PASS: {len(neutral_sources)} Java 8 and {len(modern_sources)} Java 21 sources; "
        f"archives {NEUTRAL_ARCHIVE.name}, {MODERN_ARCHIVE.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
