#!/usr/bin/env python3
"""Generate strict deterministic M09 JavaDoc archives."""

from __future__ import annotations

from pathlib import Path

import api_docs


ROOT = Path(__file__).resolve().parents[2]
NEUTRAL_MODULES = api_docs.NEUTRAL_MODULES + (
    "arena/zbw-arena", "game/zbw-game", "command/zbw-command-api", "ui/zbw-ui-api",
)
MODERN_MODULES = api_docs.MODERN_MODULES + (
    "command/zbw-command-paper", "ui/zbw-ui-paper",
)
CURRENT_CLASSPATH_MODULES = NEUTRAL_MODULES + (
    "scripting/zbw-scripting-api", "shop/zbw-shop", "content/zbw-content")
NEUTRAL_OUTPUT = ROOT / "target/apidocs-m09-neutral"
MODERN_OUTPUT = ROOT / "target/apidocs-m09-modern"
NEUTRAL_ARCHIVE = ROOT / "target/zartrabedwars-m09-neutral-javadoc.zip"
MODERN_ARCHIVE = ROOT / "target/zartrabedwars-m09-modern-javadoc.zip"


def main() -> int:
    neutral_sources = api_docs.sources(NEUTRAL_MODULES)
    neutral_classpath = [
        ROOT / ".m2/repository/com/zaxxer/HikariCP/4.0.3/HikariCP-4.0.3.jar",
        ROOT / ".m2/repository/com/github/ben-manes/caffeine/caffeine/2.9.3/caffeine-2.9.3.jar",
    ]
    result = api_docs.generate(api_docs.executable("8", "1.8.0_442"), "8",
                               neutral_sources, NEUTRAL_OUTPUT, neutral_classpath)
    if result:
        return result
    api_docs.archive(NEUTRAL_OUTPUT, NEUTRAL_ARCHIVE)
    modern_sources = api_docs.sources(MODERN_MODULES)
    modern_classpath = [
        ROOT / ".m2/repository/io/zartra/mirror/paper/paper-api/1.21.1-build133/"
        "paper-api-1.21.1-build133.jar",
        *api_docs.artifacts(CURRENT_CLASSPATH_MODULES),
    ]
    result = api_docs.generate(api_docs.executable("21", "21.0.6"), "21",
                               modern_sources, MODERN_OUTPUT, modern_classpath)
    if result:
        return result
    api_docs.archive(MODERN_OUTPUT, MODERN_ARCHIVE)
    print(f"M09 JavaDoc PASS: {len(neutral_sources)} Java 8 and "
          f"{len(modern_sources)} Java 21 sources")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
