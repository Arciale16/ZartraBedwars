#!/usr/bin/env python3
"""Deterministic M09 architecture, scope and evidence validation."""

from __future__ import annotations

import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


ROOT = Path(__file__).resolve().parents[2]
MODULES = {
    "zbw-command-api": "command/zbw-command-api",
    "zbw-ui-api": "ui/zbw-ui-api",
    "zbw-command-paper": "command/zbw-command-paper",
    "zbw-ui-paper": "ui/zbw-ui-paper",
}


def require(relative: str, values: tuple[str, ...], errors: list[str]) -> None:
    path = ROOT / relative
    if not path.is_file():
        errors.append(f"M09 required file missing: {relative}")
        return
    text = path.read_text(encoding="utf-8")
    for value in values:
        if value not in text:
            errors.append(f"{relative} lacks M09 assertion: {value}")


def validate() -> list[str]:
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    expected_completed = [f"M{value:02d}" for value in range(10)]
    if state.get("active_milestone") is not None:
        errors.append("M09 completion must leave no active milestone")
    if state.get("completed_milestones") != expected_completed:
        errors.append("M09 completion must record the ordered M00..M09 closure")
    graph = json.loads((ROOT / "build/module-graph.json").read_text(encoding="utf-8"))
    materialized = {row["id"]: row for row in graph["materialized_build_modules"]}
    planned = {row["id"]: row for row in graph["planned_production_modules"]}
    for module, path in MODULES.items():
        if module not in materialized or materialized[module]["path"] != f"{path}/pom.xml":
            errors.append(f"M09 module graph missing materialized {module}")
    expected = {
        "zbw-command-api": {"zbw-api", "zbw-application"},
        "zbw-ui-api": {"zbw-api", "zbw-application", "zbw-command-api"},
        "zbw-command-paper": {"zbw-command-api", "zbw-ui-api", "zbw-arena", "zbw-game"},
        "zbw-ui-paper": {"zbw-ui-api", "zbw-command-api", "zbw-arena", "zbw-game", "zbw-compat-api"},
    }
    for module, dependencies in expected.items():
        if set(planned[module]["depends_on"]) != dependencies:
            errors.append(f"M09 dependency mismatch for {module}")
    for module in ("zbw-command-api", "zbw-ui-api"):
        if planned[module]["bytecode"] != 8:
            errors.append(f"{module} must remain Java 8")
        for source in (ROOT / MODULES[module] / "src/main/java").rglob("*.java"):
            text = source.read_text(encoding="utf-8")
            if re.search(r"\b(org\.bukkit|io\.papermc|net\.minecraft|java\.sql|java\.nio\.file)\b", text):
                errors.append(f"platform/storage type leaked into {source.relative_to(ROOT)}")
    for module in ("zbw-command-paper", "zbw-ui-paper"):
        if planned[module]["bytecode"] != 21:
            errors.append(f"{module} must remain Java 21")
    require("build/m09-command-inventory.json", ('"count": 87', '"commands"'), errors)
    require("build/m09-permission-inventory.json", ('"count": 87', '"permissions"'), errors)
    require("docs/FEATURE_IMPLEMENTATION_STATUS.md", ("Total requirements: **672**", "M09"), errors)
    require("build/evidence/m09-paper-primary.json", (
        '"command_dispatch": true', '"inventory_rendering": true',
        '"command_gui_parity": true', '"duplicate_action_prevented": true',
        '"success": true',
    ), errors)
    for relative in (
        "docs/IMPLEMENTATION_M09.md", "docs/API_M09.md", "docs/COMMAND_FRAMEWORK_M09.md",
        "docs/GUI_FRAMEWORK_M09.md", "docs/CONFIRMATION_FRAMEWORK_M09.md",
        "docs/EDITOR_FRAMEWORK_M09.md", "docs/COMMANDS.md", "docs/PERMISSIONS.md",
        "docs/ADMIN_INTERACTION_GUIDE_M09.md", "docs/ACCESSIBILITY_FALLBACK_M09.md",
        "docs/PRESENTATION_EXTENSION_GUIDE_M09.md",
    ):
        if not (ROOT / relative).is_file():
            errors.append(f"M09 documentation missing: {relative}")
    command_inventory = json.loads((ROOT / "build/m09-command-inventory.json").read_text(encoding="utf-8"))
    if len(command_inventory.get("commands", [])) != 87:
        errors.append("M09 command inventory is not 87 actions")
    dashboard = (ROOT / "docs/FEATURE_IMPLEMENTATION_STATUS.md").read_text(encoding="utf-8")
    if len(re.findall(r"^\| .* \| ZBW-[A-Z]+-\d{3} \|", dashboard, re.MULTILINE)) != 672:
        errors.append("M09 feature dashboard is not 672 rows")
    for module in MODULES.values():
        report = ROOT / module / "target/site/jacoco/jacoco.xml"
        if report.is_file():
            counters = {row.attrib["type"]: row.attrib for row in ET.parse(report).getroot().findall("counter")}
            for counter, minimum in (("LINE", .80), ("BRANCH", .70)):
                data = counters.get(counter, {})
                covered, missed = int(data.get("covered", 0)), int(data.get("missed", 0))
                if not covered + missed or covered / (covered + missed) < minimum:
                    errors.append(f"{module} {counter.lower()} coverage below {minimum:.0%}")
    m10_paths = ("matchmaking", "spectator", "selector", "ultimate-mode", "rush-mode")
    for path in ROOT.rglob("*.java"):
        normalized = path.as_posix().lower()
        if any(value in normalized for value in m10_paths):
            errors.append(f"M10 source materialized during M09: {path.relative_to(ROOT)}")
    return errors


def main() -> int:
    errors = validate()
    if errors:
        for error in errors:
            print(f"ERROR: {error}")
        return 1
    print("M09 architecture PASS: four modules, Java boundaries, 87-action parity, "
          "672-row dashboard, exact Paper evidence; no M10 source")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
