#!/usr/bin/env python3
"""Generate strict deterministic Java 8 documentation for the M18 Atlas API."""

from pathlib import Path

import api_docs


ROOT = Path(__file__).resolve().parents[2]
ATLAS_MODULES = ("atlas/zbw-atlas-api",)
ATLAS_CLASSPATH_MODULES = ("api/zbw-api", "replay/zbw-replay-api")


def main() -> int:
    """Document the Atlas API against only its neutral public dependencies."""
    sources = api_docs.sources(ATLAS_MODULES)
    output = ROOT / "target/apidocs-m18-atlas-api"
    result = api_docs.generate(
        api_docs.executable("8", "1.8.0_442"),
        "8",
        sources,
        output,
        api_docs.artifacts(ATLAS_CLASSPATH_MODULES),
    )
    if result:
        return result
    api_docs.archive(output, ROOT / "target/zartrabedwars-m18-atlas-api-javadoc.zip")
    print(f"M18 Atlas JavaDoc PASS: {len(sources)} Java 8 sources")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
