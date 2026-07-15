# Third-Party Notices

**Baseline date:** 2026-07-15<br>
**Release status:** M04 thin storage artifacts implemented locally; no Java library, plug-in, container image or creative asset is bundled

## Distributed third-party software and assets

None.

M04 resolves approved build, test and runtime libraries only into the local hash-locked cache/classpath. None enters a thin ZartraBedWars module JAR, and `docs/ASSET_PROVENANCE.md` contains zero approved packaged assets. Therefore the current artifacts distribute no third-party software/asset and have no product attribution, source-offer or modification notice to reproduce.

## Build and CI tooling

The current baseline uses 15 checksum- or commit-locked build/CI artifacts (the M01 set plus the M04 evidence-upload action). They are not product dependencies and are not redistributed in a ZartraBedWars artifact. Their exact identities, licences, rights and authoritative sources are generated at [build/THIRD_PARTY_BUILD_NOTICES.md](build/THIRD_PARTY_BUILD_NOTICES.md) from `build/dependency-lock.json`; the matching development SBOM is `build/sbom.cdx.json`.

M04 locks 192 Maven binary components and 604 exact JAR/POM files. Their generated notices are [build/M04_MAVEN_BUILD_NOTICES.md](build/M04_MAVEN_BUILD_NOTICES.md), their development SBOM is `build/maven-build-sbom.cdx.json`, and their integrity/licence source is `build/maven-dependency-lock.json`. Every component is classified `RUNTIME_OR_BUILD_NOT_BUNDLED`, with product redistribution, shading and modification disabled.

The RC-077 workflow uses MySQL `8.4.0` at OCI index digest `sha256:dab7049abafe3a0e12cbe5e49050cf149881c0cd9665c289e5808b9dad39c9e0` and MariaDB `11.4.2` at OCI index digest `sha256:e59ba8783bf7bc02a4779f103bb0d8751ac0e10f9471089709608377eded7aa8`. Their GPL-2.0-only official packaging provenance and immutable Linux/amd64 manifests are recorded in `build/m04-database-container-lock.json` and verified before execution. They are ephemeral CI-only test runtimes and are not copied, modified, bundled, cached as repository artifacts or redistributed by ZartraBedWars.

## External interoperability references

ZartraBedWars requirements mention external server platforms, APIs and optional plugins solely to describe planned interoperability. Their names and trademarks belong to their respective owners. Mention does not mean endorsement, inclusion or redistribution. Operators will install optional runtime products separately under their own licences/terms.

Exact certification selections and their compile-only/runtime/bundled policy are listed in `docs/DEPENDENCY_LICENSE_AUDIT.md`; the checksum/licence acquisition gate runs before first resolution. Proprietary plugin binaries—including any commercial anticheat or marketplace dependency—must never be committed, used as repository fixtures or included in release artifacts.

## Release generation rule

Before any product artifact is published, this document must be regenerated from the exact dependency lock/SBOM and approved asset manifest. It must include every required copyright, attribution, licence, modification and source-offer notice for approved bundled components. A mismatch blocks release under `ZBW-LICENSE-007`.
