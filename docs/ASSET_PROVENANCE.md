# ZartraBedWars Asset Provenance Manifest

**Status:** normative empty-at-code-baseline manifest<br>
**Decision:** RC-073<br>
**Requirement:** `ZBW-CONTENT-011`<br>
**Last reviewed:** 2026-07-14

## 1. Current result

The repository contains no production textures, models, sounds, skins, fonts, maps or other distributable binary creative assets at this documentation-only baseline. Therefore the approved-asset inventory is correctly empty. No future asset may be packaged merely because it is mentioned in `docs/ORIGINAL_STARTER_CATALOG.md`.

Data definitions and original names in the starter catalogue are specifications, not evidence that a binary asset has been created or licensed.

## 2. Mandatory manifest fields

Every asset row must contain all owner-required fields:

| Field | Required meaning and acceptance |
|---|---|
| Asset ID | Immutable namespaced identifier; never reused for different content |
| Origin | `ORIGINAL`, `COMMISSIONED`, `LICENSED`, `PUBLIC_DOMAIN` or `GENERATED_WITH_APPROVED_TERMS`, plus source location |
| Author | Legal person/entity or documented project contributor identity |
| Licence | Exact licence name/SPDX identifier or signed contract reference and version/date |
| Permitted use | Exact allowed product, platform, commercial and derivative-use scope |
| Redistribution status | `APPROVED`, `PROHIBITED` or `CONDITIONAL`, with the applicable condition |
| Modification status | Whether derivatives are allowed and whether modifications were made |

The repository also requires source URL/contract reference, source hash, repository path, acquisition date, approver, notice path and current lifecycle status. These extra fields do not replace any owner-required field.

## 3. Approved asset inventory

| Asset ID | Origin | Author | Licence | Permitted use | Redistribution status | Modification status | Source/contract and hash | Repository path | Approval/status |
|---|---|---|---|---|---|---|---|---|---|

There are **0 approved distributable asset rows** and **0 packaged asset files** at this baseline.

## 4. Lifecycle and controls

1. An asset begins as `PROPOSED` outside release packaging.
2. The contributor supplies the original source or lawful licence/contract, author identity and cryptographic hash.
3. Legal/content review records permitted commercial use, redistribution, derivatives, attribution and platform limits.
4. Only an `APPROVED` row may be copied into a production resource pack or release artifact.
5. Modification creates a new source hash and updates modification status; a materially different work receives a new asset ID.
6. Licence, author, origin or file drift fails the provenance validator and release build.
7. Revoked/expired rights mark the row `RETIRED`, remove the asset from packaging and activate a configured original/vanilla fallback without deleting the feature.

## 5. Prohibited sources

Do not extract, trace, recolour or adapt proprietary assets, exact protected content, messages, sounds, models, textures, maps, skins, GUI layouts or branding from Hypixel, BedWars1058, addon JARs, marketplace downloads or third-party servers. Possession or technical accessibility is not permission. Functional inspiration must be independently expressed through the native requirements and original content process.

## 6. Verification

M01 establishes an asset scanner that inventories production resource paths and compares every file hash with this manifest. M14 requires the 300+ cosmetic definition/content audit, and M24 requires a clean provenance report and matching `THIRD_PARTY_NOTICES.md`. Until then, no asset-bearing requirement may be marked implemented or distributable.
