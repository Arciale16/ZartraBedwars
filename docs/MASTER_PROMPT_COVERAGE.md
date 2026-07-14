# MASTER_PROMPT complete coverage report

**Status:** Final post-remediation audit — 100% functional coverage; Java implementation remains not started.

**Authoritative source:** `MASTER_PROMPT.md` (6,470 physical lines; SHA-256 `afce1250079945a7543f027bb23df14cedee7913ac52f8cb0775da784b280afa`).

**Atomic inventory:** 6,438 non-empty source assertions. This is a lossless upper-bound catalogue: it intentionally includes headings and governance statements as well as every functional child, so a parsing heuristic cannot discard a requested feature.

**Requirement baseline:** 144 stable `ZBW-*` IDs. Every atomic assertion maps to at least one of them and to its own stable baseline ID `MP-L####`.

**Verifier runtime:** Python 3.11 or newer, standard library only. M01 shall pin the exact CI patch version alongside the build dependency matrix.

## Coverage result

| Measure | Result |
|---|---|
| Source assertions catalogued | 6,438 / 6,438 |
| COVERED | 6,438 |
| PARTIALLY COVERED | 0 |
| MISSING | 0 |
| Overall functional coverage | **100.00%** |
| Java precondition | **PASS for coverage only**; the independent ADR decision gate in `docs/RISKS_AND_CONFLICTS.md` remains in force |

The initial requirement-level matrix was only partially sufficient because its source audit used broad line ranges. ZBW-GOV-011 and ZBW-QA-007, the Part II atomic rows below, architecture/milestone/risk updates and the deterministic verifier remediate that structural gap. The final report therefore contains no partial or missing item.

## Requested-category summary

| Requested category | Explicit source assertions | Status | Notes |
|---|---:|---|---|
| BedWars1058 core-feature references | 3 | **COVERED** | Three direct references exist (setup usability, migration layouts/data, statistics compatibility); no BedWars1058 core-feature catalogue appears in the source. |
| Premium addon features | 0 | **COVERED** | No feature catalogue is explicitly labelled premium addons. Premium battle-pass tracks and future premium-module licensing are preserved in their actual categories; see RC-067. |
| Free addon features | 0 | **COVERED** | No feature catalogue is explicitly labelled free addons. No features were invented or reclassified; see RC-067. |
| Core gameplay | 39 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Game modes | 26 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Lobby features | 42 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Setup and map management | 219 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Shop, upgrades, generators and gameplay items | 263 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Cosmetic categories and system features | 202 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Quests, achievements, challenges, rewards and battle pass | 913 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Replay and Atlas | 1,209 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Statistics and leaderboards | 333 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| GUIs | 619 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Player commands | 167 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Staff commands | 107 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Admin commands | 212 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Permissions | 315 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Public APIs | 408 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Integrations | 768 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| PlaceholderAPI | 461 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Documentation deliverables | 332 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Performance requirements | 279 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Security requirements | 414 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Migration requirements | 116 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |
| Testing requirements | 253 | **COVERED** | All source assertions tagged to this category are listed verbatim in the annexes. |

## Method and acceptance rules

1. `MP-L####` uses the physical source line number in this content-addressed baseline; blank and divider-only lines receive no row.
2. Each row carries the original source text (only separator dashes are omitted), category tags, stable PRD ID mappings, PRD sections, its Part I/Part II trace entry, status and notes.
3. Duplicate text remains in separate rows and is cross-noted. Broad requirement parents are acceptable only because the exact child text is preserved normatively in its Part II row.
4. `COVERED` means the source child is explicitly preserved by this PRD/traceability baseline. It does not claim runtime implementation; all implementation requirements remain `NOT STARTED`.
5. Any source, PRD or matrix edit must regenerate the report and pass `python tools/coverage/generate_master_prompt_coverage.py --check` before Java work.
6. Facts, assumptions and recommendations for unresolved ambiguity/conflict remain distinguished in `docs/RISKS_AND_CONFLICTS.md`; mapping a conflict does not silently resolve it.

## Normative atomic annexes

| Annex | Atomic range | Rows | Status |
|---|---|---:|---|
| [MASTER_PROMPT_COVERAGE_PART_01.md](coverage/MASTER_PROMPT_COVERAGE_PART_01.md) | MP-L0001–MP-L0756 | 750 | **COVERED** |
| [MASTER_PROMPT_COVERAGE_PART_02.md](coverage/MASTER_PROMPT_COVERAGE_PART_02.md) | MP-L0757–MP-L1509 | 750 | **COVERED** |
| [MASTER_PROMPT_COVERAGE_PART_03.md](coverage/MASTER_PROMPT_COVERAGE_PART_03.md) | MP-L1510–MP-L2262 | 750 | **COVERED** |
| [MASTER_PROMPT_COVERAGE_PART_04.md](coverage/MASTER_PROMPT_COVERAGE_PART_04.md) | MP-L2263–MP-L3014 | 750 | **COVERED** |
| [MASTER_PROMPT_COVERAGE_PART_05.md](coverage/MASTER_PROMPT_COVERAGE_PART_05.md) | MP-L3015–MP-L3765 | 750 | **COVERED** |
| [MASTER_PROMPT_COVERAGE_PART_06.md](coverage/MASTER_PROMPT_COVERAGE_PART_06.md) | MP-L3766–MP-L4520 | 750 | **COVERED** |
| [MASTER_PROMPT_COVERAGE_PART_07.md](coverage/MASTER_PROMPT_COVERAGE_PART_07.md) | MP-L4521–MP-L5274 | 750 | **COVERED** |
| [MASTER_PROMPT_COVERAGE_PART_08.md](coverage/MASTER_PROMPT_COVERAGE_PART_08.md) | MP-L5275–MP-L6028 | 750 | **COVERED** |
| [MASTER_PROMPT_COVERAGE_PART_09.md](coverage/MASTER_PROMPT_COVERAGE_PART_09.md) | MP-L6029–MP-L6470 | 438 | **COVERED** |

## Remaining decisions

Coverage is complete, but coverage is not the same as product-decision closure. The blocking pre-code ADR/policy decisions listed in `docs/RISKS_AND_CONFLICTS.md` remain unresolved. RC-067 additionally records that the source provides no premium/free addon catalogues and no BedWars1058 core-feature catalogue; a future owner-supplied catalogue would be a source change requiring regeneration.

No Java implementation was created or started by this audit.
