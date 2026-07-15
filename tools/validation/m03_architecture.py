#!/usr/bin/env python3
"""Deterministic architecture and source-governance checks for Milestone 3."""

from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
CONFIG_MODULE = "configuration/zbw-config"
PRODUCTION_MODULES = (
    "api/zbw-api",
    "domain/zbw-domain",
    "application/zbw-application",
    CONFIG_MODULE,
    "sdk/zbw-sdk",
    "integrations/discord/zbw-integration-discord-api",
)
MAVEN = {"m": "http://maven.apache.org/POM/4.0.0"}
FORBIDDEN_CONFIG_IMPORTS = (
    "org.bukkit", "io.papermc", "net.minecraft", "com.velocitypowered",
    "net.md_5.bungee", "redis.clients", "io.lettuce", "java.sql", "javax.sql",
    "java.io", "java.nio.file",
)
EXPECTED_LOGICAL_FILES = {
    "config.yml", "deployment.yml", "database.yml", "redis.yml", "proxy.yml",
    "cloudnet.yml", "arenas.yml", "maps.yml", "modes.yml", "shops.yml",
    "upgrades.yml", "generators.yml", "items.yml", "quests.yml", "achievements.yml",
    "challenges.yml", "battlepass.yml", "cosmetics.yml", "content.yml", "rewards.yml",
    "statistics.yml", "placeholders.yml", "replay.yml", "atlas.yml", "anticheat.yml",
    "parties.yml", "npcs.yml", "holograms.yml", "gui.yml", "messages.yml",
    "permissions.yml", "compatibility.yml", "performance.yml", "security.yml",
    "integrations.yml", "integrations/discord.yml",
}
CANONICAL_ACTIONS = {
    "view", "use", "create", "edit", "delete", "duplicate", "import", "export",
    "enable", "disable", "start", "stop", "force", "reload", "reset", "backup",
    "restore", "migrate", "inspect", "debug", "bypass", "manage", "grant", "revoke",
    "set", "add", "remove", "approve", "reject", "override", "view.identity",
    "view.hidden", "view.private",
}


def sources(module: str) -> list[Path]:
    return sorted((ROOT / module / "src" / "main" / "java").rglob("*.java"))


def validate() -> list[str]:
    errors: list[str] = []
    state = json.loads((ROOT / "build" / "milestone-state.json").read_text(encoding="utf-8"))
    if state["active_milestone"] not in {"M03", "M04", "M05", "M06", "M07", None}:
        errors.append("active milestone must preserve the M03 baseline")
    completed = state["completed_milestones"]
    if completed not in (["M00", "M01", "M02"], ["M00", "M01", "M02", "M03"],
                         ["M00", "M01", "M02", "M03", "M04"],
                         ["M00", "M01", "M02", "M03", "M04", "M05"],
                         ["M00", "M01", "M02", "M03", "M04", "M05", "M06"],
                         ["M00", "M01", "M02", "M03", "M04", "M05", "M06", "M07"]):
        errors.append("completed milestones must be the ordered M03 implementation or closure set")

    marker = re.compile(
        r"\b(?:TODO|FIXME)\b|UnsupportedOperationException|\bstub\b|fake implementation",
        re.IGNORECASE,
    )
    mutable_global = re.compile(r"\bstatic\s+(?!final\b)[^;=()]+(?:=|;)")
    service_locator = re.compile(r"\b(?:ServiceLocator|GlobalRegistry|getInstance\s*\()")
    for module in PRODUCTION_MODULES:
        source_root = ROOT / module / "src" / "main" / "java"
        if not source_root.is_dir():
            errors.append(f"missing production source root: {module}")
            continue
        packages = {path.parent for path in sources(module) if path.name != "package-info.java"}
        for package in sorted(packages):
            if not (package / "package-info.java").is_file():
                errors.append(f"exported source package lacks package-info.java: {package.relative_to(ROOT)}")
        for path in sources(module):
            content = path.read_text(encoding="utf-8")
            if marker.search(content):
                errors.append(f"prohibited incomplete implementation marker: {path.relative_to(ROOT)}")
            if mutable_global.search(content):
                errors.append(f"global mutable state is prohibited: {path.relative_to(ROOT)}")
            if service_locator.search(content):
                errors.append(f"service locator pattern is prohibited: {path.relative_to(ROOT)}")

    for path in sources(CONFIG_MODULE):
        content = path.read_text(encoding="utf-8")
        for imported in re.findall(r"^import\s+([^;]+);", content, re.MULTILINE):
            if imported.startswith(FORBIDDEN_CONFIG_IMPORTS):
                errors.append(f"platform/storage/filesystem import in config: {path.relative_to(ROOT)}: {imported}")
        for forbidden in ("System.getenv", "Files.", "Paths.", "new Thread", "ExecutorService",
                          "ScheduledExecutor", "TimerTask"):
            if forbidden in content:
                errors.append(f"runtime I/O or scheduler leaked into M03 config: {path.relative_to(ROOT)}: {forbidden}")

    pom = ET.parse(ROOT / CONFIG_MODULE / "pom.xml").getroot()
    internal: set[str] = set()
    external_compile: set[str] = set()
    for dependency in pom.findall("m:dependencies/m:dependency", MAVEN):
        group = dependency.findtext("m:groupId", default="", namespaces=MAVEN)
        artifact = dependency.findtext("m:artifactId", default="", namespaces=MAVEN)
        scope = dependency.findtext("m:scope", default="compile", namespaces=MAVEN)
        if group == "io.zartra.bedwars" and scope != "test":
            internal.add(artifact)
        elif scope != "test":
            external_compile.add(f"{group}:{artifact}")
    if internal != {"zbw-api", "zbw-application"}:
        errors.append(f"zbw-config internal dependencies drifted: {sorted(internal)}")
    if external_compile:
        errors.append(f"zbw-config has unapproved implementation dependencies: {sorted(external_compile)}")

    schema_source = (ROOT / CONFIG_MODULE / "src/main/java/io/zartra/bedwars/config/schema/ConfigurationModel.java").read_text(encoding="utf-8")
    observed_files = set(re.findall(r'\("([^"\n]+\.yml)",\s*ReloadTarget\.', schema_source))
    if observed_files != EXPECTED_LOGICAL_FILES:
        errors.append("logical configuration file inventory drift")
    auth_source = (ROOT / CONFIG_MODULE / "src/main/java/io/zartra/bedwars/config/authorization/DefaultAuthorizationService.java").read_text(encoding="utf-8")
    observed_actions = set(re.findall(r'\("([a-z.]+)"\)', auth_source)) & CANONICAL_ACTIONS
    if observed_actions != CANONICAL_ACTIONS:
        errors.append("canonical authorization action inventory drift")
    authorization_implementations = sum(
        path.read_text(encoding="utf-8").count("implements AuthorizationService")
        for path in sources(CONFIG_MODULE)
    )
    if authorization_implementations != 1:
        errors.append("authorization checks must have one centralized M03 implementation")

    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    materialized = {row["id"] for row in graph["materialized_build_modules"]}
    if "zbw-config" not in materialized:
        errors.append("zbw-config is absent from materialized module graph")
    if state["active_milestone"] == "M03":
        for later_path in ("storage", "platform", "proxy", "gameplay", "replay", "atlas"):
            if (ROOT / later_path).exists():
                errors.append(f"post-M03 production path materialized: {later_path}")
        if list(ROOT.rglob("plugin.yml")) or list(ROOT.rglob("paper-plugin.yml")):
            errors.append("Minecraft runtime descriptor is forbidden in M03")
    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("M03 architecture PASS: platform-free config module, 36 schemas, centralized authorization, no M04 paths.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
