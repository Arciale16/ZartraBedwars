#!/usr/bin/env python3
"""Generate strict deterministic M11 JavaDoc archives."""
from pathlib import Path
import api_docs

ROOT = Path(__file__).resolve().parents[2]
NEUTRAL_MODULES = api_docs.NEUTRAL_MODULES + (
    "arena/zbw-arena", "game/zbw-game", "command/zbw-command-api", "ui/zbw-ui-api",
    "scripting/zbw-scripting-api", "shop/zbw-shop", "content/zbw-content")
MODERN_MODULES = api_docs.MODERN_MODULES + (
    "command/zbw-command-paper", "ui/zbw-ui-paper", "platform/paper/zbw-paper-modern")

def main() -> int:
    neutral = api_docs.sources(NEUTRAL_MODULES)
    result = api_docs.generate(api_docs.executable("8", "1.8.0_442"), "8", neutral,
            ROOT / "target/apidocs-m11-neutral", [
                ROOT / ".m2/repository/com/zaxxer/HikariCP/4.0.3/HikariCP-4.0.3.jar",
                ROOT / ".m2/repository/com/github/ben-manes/caffeine/caffeine/2.9.3/caffeine-2.9.3.jar"])
    if result:
        return result
    api_docs.archive(ROOT / "target/apidocs-m11-neutral", ROOT / "target/zartrabedwars-m11-neutral-javadoc.zip")
    modern = api_docs.sources(MODERN_MODULES)
    result = api_docs.generate(api_docs.executable("21", "21.0.6"), "21", modern,
            ROOT / "target/apidocs-m11-modern", [
                ROOT / ".m2/repository/io/zartra/mirror/paper/paper-api/1.21.1-build133/paper-api-1.21.1-build133.jar",
                *api_docs.artifacts(NEUTRAL_MODULES)])
    if result:
        return result
    api_docs.archive(ROOT / "target/apidocs-m11-modern", ROOT / "target/zartrabedwars-m11-modern-javadoc.zip")
    print(f"M11 JavaDoc PASS: {len(neutral)} Java 8 and {len(modern)} Java 21 sources")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
