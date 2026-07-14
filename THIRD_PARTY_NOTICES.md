# Third-Party Notices

**Baseline date:** 2026-07-14<br>
**Release status:** M02 API/domain artifacts verified; no Java library, plug-in, product dependency or creative asset is bundled

## Distributed third-party software and assets

None.

M02 resolves approved build/test libraries and Maven plug-ins only into the local hash-locked cache. None enters a product artifact, and `docs/ASSET_PROVENANCE.md` contains zero approved packaged assets. Therefore ZartraBedWars currently distributes no third-party software/asset and has no product attribution, source-offer or modification notice to reproduce.

## Build and CI tooling

M01 uses 14 checksum- or commit-locked build/CI artifacts. They are not product dependencies and are not redistributed in a ZartraBedWars artifact. Their exact identities, licences, rights and authoritative sources are generated at [build/THIRD_PARTY_BUILD_NOTICES.md](build/THIRD_PARTY_BUILD_NOTICES.md) from `build/dependency-lock.json`; the matching development SBOM is `build/sbom.cdx.json`.

M02 additionally locks 170 Maven build/test components and 535 exact JAR/POM files. Their generated notices are [build/M02_MAVEN_BUILD_NOTICES.md](build/M02_MAVEN_BUILD_NOTICES.md), their development SBOM is `build/maven-build-sbom.cdx.json`, and their integrity/licence source is `build/maven-dependency-lock.json`. Every component is classified `BUILD_OR_TEST_ONLY`, with product redistribution, shading and modification disabled.

## External interoperability references

ZartraBedWars requirements mention external server platforms, APIs and optional plugins solely to describe planned interoperability. Their names and trademarks belong to their respective owners. Mention does not mean endorsement, inclusion or redistribution. Operators will install optional runtime products separately under their own licences/terms.

Exact certification selections and their compile-only/runtime/bundled policy are listed in `docs/DEPENDENCY_LICENSE_AUDIT.md`; the checksum/licence acquisition gate runs before first resolution. Proprietary plugin binaries—including any commercial anticheat or marketplace dependency—must never be committed, used as repository fixtures or included in release artifacts.

## Release generation rule

Before any product artifact is published, this document must be regenerated from the exact dependency lock/SBOM and approved asset manifest. It must include every required copyright, attribution, licence, modification and source-offer notice for approved bundled components. A mismatch blocks release under `ZBW-LICENSE-007`.
