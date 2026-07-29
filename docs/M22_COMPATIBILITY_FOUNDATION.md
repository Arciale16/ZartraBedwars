# M22 compatibility foundation

**Status:** Phase 1 governance and build boundaries only
**Requirements:** `ZBW-ARC-002`, `ZBW-INT-004`, `ZBW-INT-010`,
`ZBW-COMPAT-001..009`, `ZBW-READY-001`, `ZBW-READY-002`,
`ZBW-READY-005`, `ZBW-READY-006`, `ZBW-LICENSE-001/002/005`

## Scope

M22 is active. This checkpoint materializes eleven empty Maven artifact
boundaries and a deterministic compatibility model. It adds no Java source,
version mapping, NMS access, server API dependency, legacy Paper bootstrap or
support certification. Paper 1.21.1 build 133 retains only its completed M06
foundation evidence.

The canonical machine model is
`build/m22-compatibility-matrix.json`. It binds all 22 exact server fixtures to
nine artifact families, five exact JDK toolchains and five independent client
paths. Its 45 server-family/client-path cells remain uncertified.

## Module grouping

| Reactor profile | JDK | Compatibility boundaries | Paper boundary |
|---|---:|---|---|
| `legacy-paper-platform` | 8 | `zbw-compat-v1_8`, `zbw-compat-v1_9`, `zbw-compat-v1_10`, `zbw-compat-v1_11` | `zbw-paper-legacy` |
| `j11-paper-platform` | 11 | `zbw-compat-v1_12-v1_16_4` | `zbw-paper-j11` |
| `j16-paper-platform` | 16 | `zbw-compat-v1_16_5` | `zbw-paper-j16` |
| `j17-paper-platform` | 17 | `zbw-compat-v1_17-v1_19` | `zbw-paper-j17` |
| `modern-paper-platform` | 21 | `zbw-compat-v1_20-v1_21` | `zbw-paper-modern` |

Each compatibility boundary depends only on `zbw-compat-api`. Each new Paper
assembly boundary depends only on `zbw-application` and its matching
compatibility boundary. The legacy assemblies cannot link the Java 21 command,
GUI or modern Paper artifacts. Domain, API and application modules have no
reverse dependency on a compatibility or Paper adapter.

The broad `1.12-1.16.4` and `1.17-1.19` build boundaries are allocation units,
not proof that one server API binary safely serves every included minor. Phase
2 must prove public-API linkage per exact fixture or split a boundary before
introducing platform source. Grouping convenience cannot override the exact
fixture matrix.

## Provider acquisition gate

`build/m22-provider-lock-requirements.json` records the exact selected versions,
provided-only scope, SPDX licence and upstream provenance for ProtocolLib,
ViaVersion, ViaBackwards, ViaRewind, Geyser and Floodgate. Artifact and exact
licence-text SHA-256 values are deliberately unset and marked
`REQUIRED_BEFORE_RESOLUTION`; Maven declarations and downloads remain forbidden
until those values, exact coordinates/sources and the complete transitive graph
are captured through the existing immutable lock/SBOM process.

This is not a dependency waiver. A null digest blocks resolution and Phase 2;
it cannot be interpreted as approval, substituted with another version or
converted into a floating dependency.

## Validation contract

`tools/validation/m22_foundation.py` rejects module/POM drift, Java sources in
the Phase 1 modules, core-to-adapter dependencies, fixture or JDK drift,
non-deterministic matrix dimensions, changed provider versions/licences and any
attempt to enable provider resolution before the exact lock is complete.

Subsequent phases must add adapter behavior and certification without changing
M08-M21 ownership. M22 translates existing neutral semantics only.
