#!/usr/bin/env python3
"""Generate strict deterministic Java 8 documentation for M21 provider boundaries."""

from pathlib import Path

import api_docs


ROOT = Path(__file__).resolve().parents[2]
ADAPTER_MODULES = (
    "integrations/vault/zbw-integration-vault",
    "integrations/luckperms/zbw-integration-luckperms",
    "integrations/citizens/zbw-integration-citizens",
    "integrations/znpcs/zbw-integration-znpcsplus",
    "integrations/hologram/zbw-integration-decentholograms",
    "integrations/party/zbw-integration-alessiopdp",
    "integrations/anticheat/zbw-integration-grim",
    "integrations/anticheat/zbw-integration-vulcan",
    "cloud/zbw-cloudnet",
)


def main() -> int:
    """Document provider SPI, native party, and isolated adapter boundaries."""
    modules = ("api/zbw-api", "observability/zbw-observability",
               "party/zbw-party", "party/zbw-party-sql", *ADAPTER_MODULES)
    sources = api_docs.sources(modules)
    output = ROOT / "target/apidocs-m21-party-provider-java8"
    classpath = api_docs.artifacts((
        "api/zbw-api",
        "storage/zbw-storage-api",
        "application/zbw-application",
        "observability/zbw-observability",
        "redis/zbw-redis-api",
        "proxy/zbw-proxy-api",
        "party/zbw-party",
        "party/zbw-party-sql",
        *ADAPTER_MODULES,
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
