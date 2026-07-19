#!/usr/bin/env python3
"""Validate M10 scope, boundaries and generated evidence."""

from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[2]

def validate() -> list[str]:
    errors: list[str] = []
    state = json.loads((ROOT / "build/milestone-state.json").read_text(encoding="utf-8"))
    completed = state.get("completed_milestones", [])
    if (state.get("active_milestone") not in (None, "M11", "M12")
            or completed[:11] != [f"M{value:02d}" for value in range(11)]):
        errors.append("M10 must be closed in ordered milestone state")
    source = ROOT / "game/zbw-game/src/main/java/io/zartra/bedwars/game"
    for package in ("mode", "selector", "matchmaking", "spectator"):
        if not (source / package / "package-info.java").is_file():
            errors.append(f"missing neutral M10 package {package}")
    for path in source.rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        if re.search(r"\b(org\.bukkit|io\.papermc|net\.minecraft|java\.sql|java\.nio\.file)\b", text):
            errors.append(f"platform/storage leak: {path.relative_to(ROOT)}")
    command = json.loads((ROOT / "build/m10-command-inventory.json").read_text(encoding="utf-8"))
    permissions = json.loads((ROOT / "build/m10-permission-inventory.json").read_text(encoding="utf-8"))
    if command.get("count") != 115 or command.get("baseline") != 87 or command.get("additive") != 28:
        errors.append("M10 command inventory totals are not 87+28=115")
    if permissions.get("count") != 115:
        errors.append("M10 permission inventory is not 115")
    for relative in ("docs/IMPLEMENTATION_M10.md", "docs/API_M10.md", "docs/MATCHMAKING_FRAMEWORK_M10.md",
                     "docs/SELECTORS_M10.md", "docs/MODE_FRAMEWORK_M10.md", "docs/SPECTATOR_FRAMEWORK_M10.md",
                     "docs/MATCHMAKING_ADMIN_GUIDE_M10.md", "docs/MATCHMAKING_EXTENSION_GUIDE_M10.md"):
        if not (ROOT / relative).is_file():
            errors.append(f"missing M10 documentation: {relative}")
    dashboard = (ROOT / "docs/FEATURE_IMPLEMENTATION_STATUS.md").read_text(encoding="utf-8")
    if len(re.findall(r"^\| .* \| ZBW-[A-Z]+-\d{3} \|", dashboard, re.MULTILINE)) != 672:
        errors.append("dashboard must retain 672 rows")
    forbidden = ("UltimateMode", "RushMode", "ArmedMode", "LuckyBlockMode", "SwappageMode")
    for path in ROOT.rglob("*.java"):
        if any(value in path.name for value in forbidden):
            errors.append(f"M11 named-mode mechanics materialized in M10: {path.relative_to(ROOT)}")
    return errors

def main() -> int:
    errors = validate()
    for error in errors:
        print(f"ERROR: {error}")
    if errors:
        return 1
    print("M10 architecture PASS: neutral boundaries, 87+28 actions, 672 rows, no M11 mechanics")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
