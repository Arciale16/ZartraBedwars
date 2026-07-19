# M11.1 governance-only merge exception record

**Record:** `M11.1-MERGE-EXCEPTION-001`  
**Date:** 2026-07-19  
**Pull request:** #18  
**Requirements affected:** `ZBW-GAME-004`, `ZBW-GAME-005`, `ZBW-SHOP-001..007`,
`ZBW-CONTENT-002..003`, `ZBW-READY-004`, `ZBW-READY-015` and the M11-owned addon
allocations recorded in `docs/IMPLEMENTATION_M11_1.md`.

## Purpose and limits

This record permits an owner to evaluate a governance-only merge exception while GitHub-hosted
Actions cannot start because of an external account spending/payment restriction. It is not evidence
that GitHub Actions passed, does not waive any M11 exit criterion, does not close RC-087, does not
certify the remote Java/Paper matrices and does not authorize M12 to start.

No production defect was found during the available local verification. M11.1 is technically
implemented, but final remote certification remains pending. Any merge performed under this record
must retain M11 as active and M12 as blocked until the immutable merged commit completes every
mandatory remote workflow successfully.

## Recorded local evidence

| Gate | Local result |
|---|---|
| Java 8 reactor and tests | PASS — 346 tests, 0 failures, 0 errors, 0 skipped |
| Governance suite | PASS — 36/36 |
| Addon catalogue | PASS — 49 addons, 473 mappings, 100% coverage |
| Feature dashboard | PASS — 672 deterministic rows |
| Dependency, licence and SBOM validation | PASS |
| Strict JavaDoc source validation | PASS |
| M11.1 Phase 1 API compatibility | PASS — 29 Java 8 classes |
| M11.1 Phase 2 API compatibility | PASS — 22 Java 8 classes |
| Working tree | CLEAN at validation completion |

The approved local JDK installations for Java 8, 11, 16, 17 and 21 were present. In the local
sandbox, Java 11+ child compiler processes could not read the managed Maven cache, so the complete
multi-JDK quality reactor, modern API baseline and Paper certification were not recertified locally.
This environment limitation is not recorded as a passing result.

## Mandatory post-restriction action

When GitHub Actions can start again, rerun PR #18 (or the immutable merged commit) through the full
Java 8/11/16/17/21 matrix, Java 21 reactor, Checkstyle, SpotBugs, JaCoCo, strict JavaDoc, complete API
compatibility, dependency/licence/SBOM, governance, catalogue, dashboard and locked Paper 1.21.1
certification. A skipped, unavailable or incomplete mandatory job is not success. RC-087 and M11 may
close only after all required jobs pass; only then may M12 become eligible to start.

