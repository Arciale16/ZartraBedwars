#!/usr/bin/env python3
"""Deterministic architecture and source-governance checks for Milestone 2."""

from __future__ import annotations

from pathlib import Path
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
PRODUCTION_MODULES = (
    "api/zbw-api",
    "domain/zbw-domain",
    "application/zbw-application",
    "sdk/zbw-sdk",
    "integrations/discord/zbw-integration-discord-api",
)
CORE_MODULES = PRODUCTION_MODULES[:3]
MAVEN = {"m": "http://maven.apache.org/POM/4.0.0"}
FORBIDDEN_CORE_IMPORTS = (
    "org.bukkit", "io.papermc", "net.minecraft", "com.velocitypowered",
    "net.md_5.bungee", "redis.clients", "io.lettuce", "java.sql", "javax.sql",
    "java.io", "java.nio.file",
)


def java_sources(module: str) -> list[Path]:
    return sorted((ROOT / module / "src" / "main" / "java").rglob("*.java"))


def validate() -> list[str]:
    errors: list[str] = []
    marker = re.compile(
        r"\b(?:TODO|FIXME)\b|UnsupportedOperationException|\bstub\b|fake implementation",
        re.IGNORECASE)
    mutable_global = re.compile(r"\bstatic\s+(?!final\b)[^;=()]+(?:=|;)")
    service_locator = re.compile(r"\b(?:ServiceLocator|GlobalRegistry|getInstance\s*\()")

    for module in PRODUCTION_MODULES:
        source_root = ROOT / module / "src" / "main" / "java"
        if not source_root.is_dir():
            errors.append(f"missing M02 source root: {module}")
            continue
        packages = {path.parent for path in java_sources(module) if path.name != "package-info.java"}
        for package in sorted(packages):
            if not (package / "package-info.java").is_file():
                errors.append(f"exported source package lacks package-info.java: {package.relative_to(ROOT)}")
        for path in java_sources(module):
            content = path.read_text(encoding="utf-8")
            if marker.search(content):
                errors.append(f"prohibited incomplete implementation marker: {path.relative_to(ROOT)}")
            if mutable_global.search(content):
                errors.append(f"global mutable state is prohibited: {path.relative_to(ROOT)}")
            if service_locator.search(content):
                errors.append(f"service locator pattern is prohibited: {path.relative_to(ROOT)}")

    for module in CORE_MODULES:
        for path in java_sources(module):
            for imported in re.findall(r"^import\s+([^;]+);", path.read_text(encoding="utf-8"), re.MULTILINE):
                if imported.startswith(FORBIDDEN_CORE_IMPORTS):
                    errors.append(
                        f"platform/storage/filesystem import in core: {path.relative_to(ROOT)}: {imported}")

    expected_dependencies = {
        "api/zbw-api": set(),
        "domain/zbw-domain": {"zbw-api"},
        "application/zbw-application": {"zbw-api", "zbw-domain"},
        "sdk/zbw-sdk": {"zbw-api"},
        "integrations/discord/zbw-integration-discord-api": {"zbw-api"},
    }
    for module, expected in expected_dependencies.items():
        root = ET.parse(ROOT / module / "pom.xml").getroot()
        observed = set()
        for dependency in root.findall("m:dependencies/m:dependency", MAVEN):
            group = dependency.findtext("m:groupId", default="", namespaces=MAVEN)
            scope = dependency.findtext("m:scope", default="compile", namespaces=MAVEN)
            if group == "io.zartra.bedwars" and scope != "test":
                observed.add(dependency.findtext("m:artifactId", default="", namespaces=MAVEN))
        if observed != expected:
            errors.append(f"{module}: internal dependencies {sorted(observed)} != {sorted(expected)}")

    fixture_root = ROOT / "sdk" / "zbw-sdk" / "src" / "test" / "resources" / "extensions"
    fixtures = {path.relative_to(fixture_root).as_posix() for path in fixture_root.rglob("*.properties")}
    expected_fixtures = {
        "valid/example.properties", "valid/optional-dependency.properties",
        "invalid/unsupported-api.properties", "invalid/malformed.properties",
        "invalid/required-missing.properties", "invalid/duplicate-dependency.properties",
    }
    if fixtures != expected_fixtures:
        errors.append("extension metadata fixture inventory drift")

    prohibited_m03_paths = (
        ROOT / "configuration", ROOT / "localization", ROOT / "permissions",
    )
    for path in prohibited_m03_paths:
        if path.exists():
            errors.append(f"M03 path materialized during M02: {path.relative_to(ROOT)}")
    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("M02 architecture PASS: five bounded modules; core is platform/storage/filesystem independent.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
