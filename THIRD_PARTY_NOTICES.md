# Third-Party Notices

**Baseline date:** 2026-07-14<br>
**Release status:** documentation-only; exact selections exist, but no Java build has resolved or bundled any third-party dependency or asset

## Distributed third-party software and assets

None.

The dependency selection policy is fixed, but no artifact has entered a build and `docs/ASSET_PROVENANCE.md` contains zero approved packaged assets. Therefore this documentation baseline distributes no third-party software/asset and has no artifact-specific attribution, source offer or modification notice to reproduce.

## External interoperability references

ZartraBedWars requirements mention external server platforms, APIs and optional plugins solely to describe planned interoperability. Their names and trademarks belong to their respective owners. Mention does not mean endorsement, inclusion or redistribution. Operators will install optional runtime products separately under their own licences/terms.

Exact certification selections and their compile-only/runtime/bundled policy are listed in `docs/DEPENDENCY_LICENSE_AUDIT.md`; the checksum/licence acquisition gate runs before first resolution. Proprietary plugin binaries—including any commercial anticheat or marketplace dependency—must never be committed, used as repository fixtures or included in release artifacts.

## Release generation rule

Before any artifact is published, this document must be regenerated from the exact dependency lock/SBOM and approved asset manifest. It must include every required copyright, attribution, licence, modification and source-offer notice for approved bundled components. A mismatch blocks release under `ZBW-LICENSE-007`.
