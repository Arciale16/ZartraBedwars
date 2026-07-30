# M24 Release Qualification

**Status:** deterministic repository inputs qualified; release candidate pending
**Requirements:** `ZBW-GOV-007/011`, `ZBW-OPS-008`, `ZBW-CONTENT-001/011`,
`ZBW-LICENSE-001..007`, `ZBW-READY-001/002/005/006/008/012/017/019/020`

## Reproducibility and artifact verification

The root reactor fixes `project.build.outputTimestamp`. Build twice from clean, separately restored
checksum-locked repositories using the same certified JDK/profile. After each build run:

```text
python tools/validation/m24_qualification.py artifact-report --output target/m24-build-1.json
python tools/validation/m24_qualification.py artifact-report --output target/m24-build-2.json
python tools/validation/m24_qualification.py compare-artifacts --first target/m24-build-1.json --second target/m24-build-2.json
```

The report is sorted by module/path and records exact size and SHA-256. Any missing, additional or
different artifact fails comparison. Final signed checksums must be generated only from the
approved release candidate; `target/` reports are evidence outputs, not source governance.

## Release gates

| Gate | Current result |
|---|---|
| Maven/build dependency locks and checksums | verified; dynamic versions and product bundling prohibited |
| CycloneDX SBOM and generated notices | deterministic lock/SBOM counts verified |
| Repository binary and asset provenance scan | verified for tracked source; proprietary/provider/server binaries prohibited |
| Java 8/11/16/17/21 build and quality matrix | executed by locked toolchain CI; current environment may supply only a subset |
| API compatibility and strict JavaDoc | existing milestone gates retained |
| Compatibility report | M22 implementation recorded; exact runtime/client certification pending |
| Provider artifacts | resolution blocked until exact provenance, hashes, licence text and transitive graph are approved |
| Legal terms and release approval | owner/legal execution pending |

## No-claim rule

`build/m24-qualification.json` keeps `release_ready` and `support_claim_allowed` false. A release
tag, compatibility support statement or final 672-row compliance claim is prohibited until M22,
provider, toolchain, benchmark, security/recovery and legal gates have immutable evidence.
