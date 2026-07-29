#!/usr/bin/env python3
"""Generate strict deterministic Java 8 documentation for M21 Phase 1."""

from pathlib import Path

import api_docs


ROOT = Path(__file__).resolve().parents[2]


def main() -> int:
    """Document the provider SPI and native party core/SQL boundary."""
    modules = ("api/zbw-api", "party/zbw-party", "party/zbw-party-sql")
    sources = api_docs.sources(modules)
    output = ROOT / "target/apidocs-m21-party-provider-java8"
    classpath = api_docs.artifacts((
        "api/zbw-api",
        "storage/zbw-storage-api",
        "party/zbw-party",
        "party/zbw-party-sql",
    ))
    result = api_docs.generate(
        api_docs.executable("8", "1.8.0_442"), "8", sources, output, classpath)
    if result:
        return result
    api_docs.archive(
        output, ROOT / "target/zartrabedwars-m21-party-provider-javadoc.zip")
    print(f"M21 JavaDoc PASS: {len(sources)} Java 8 sources")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
