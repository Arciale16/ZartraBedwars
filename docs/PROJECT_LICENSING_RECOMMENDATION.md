# Project Licensing Recommendation

**Status:** Accepted internal licensing model; public legal instruments must be executed before distribution
**Decision:** RC-066
**Requirement:** `ZBW-READY-019`

This document records the project owner's concrete licensing model and release gates. It is not a substitute for jurisdiction-specific legal advice.

## Binding model

| Deliverable | Pre-publication status | Recommended publication instrument | Rights granted only after execution |
|---|---|---|---|
| ZartraBedWars core and standard modules | Private proprietary; all rights reserved; no copying, redistribution, modification or commercial use by third parties | Commercial EULA/subscription licence | Run purchased instances/seats; backup; expressly documented configuration/customization only |
| Premium/enterprise modules and hosted services | Same proprietary status | Commercial addendum/SLA/data-processing terms | Entitled features, support and service use within purchased limits |
| Original maps, textures, models, sounds, messages, cosmetic/effect content and branding | Proprietary unless an asset manifest row states a compatible third-party licence | Asset/content licence incorporated into EULA | Runtime use only; no extraction/resale/repackaging unless explicitly granted |
| Public API binary, JavaDoc and SDK examples | Private until separately published | Apache License 2.0 is the approved recommendation for owner/legal execution | API/SDK use, modification and redistribution under Apache-2.0 notices/patent terms; no rights to proprietary implementation/assets/marks |
| Third-party addons built by external developers | Author chooses code licence, subject to SDK terms and dependency licences | SDK/addon agreement plus chosen addon licence | Use public API; no use/redistribution of Zartra proprietary assets, internals or marks beyond nominative compatibility |
| Configuration/content packs authored by operators | Operator-owned inputs; project receives only rights needed to process them | EULA input-content clause | Load/transform/store at operator direction; operator warrants rights |
| Documentation | Proprietary except API/SDK documentation explicitly released with SDK | Commercial documentation terms or Apache-2.0 SDK notice | Use with licensed product; redistribute only when expressly allowed |

No open-source licence is silently attached to the repository. “Apache-2.0 recommended” is an approved future publication choice, not a grant until the owner adds the exact licence text and copyright notice to the released SDK/API artifact.

## Public API and addon boundary

- The public API exposes immutable contracts, events and provider SPIs only. Internal packages and implementation artifacts are not part of the SDK grant.
- SDK terms prohibit implying endorsement, using ZartraBedWars trademarks in a confusing product name, redistributing proprietary binaries/assets and bypassing entitlement controls.
- Addons may be free or commercial under their author's lawful terms. Compatibility must be described factually (“for ZartraBedWars”) and cannot copy built-in premium functionality/source/assets.
- API deprecation spans at least one major product version. A public API licence change is prospective and cannot revoke already granted Apache-2.0 rights to an old released SDK.

## Dependency and asset compatibility

Every distribution is generated only from `APPROVED-BUNDLED` dependency rows and approved asset-provenance rows. Compile-only/provided GPL APIs do not enter the proprietary JAR. Any dependency whose terms impose source disclosure, network copyleft, field-of-use restriction, non-commercial restriction or incompatible attribution on the proprietary product is not bundled unless owner/legal approves a compatible architecture and obligations.

Third-party notices, source offers and asset credits are product/version-specific. The project name/mark registry and asset manifest must distinguish owner marks from nominative third-party references.

## Release gate

Public, marketplace, customer or commercial distribution is blocked until all of the following exist and agree with the release artifact:

1. executed proprietary code/EULA and, where needed, premium/SLA terms;
2. privacy notice, retention policy, processor/subprocessor list and data-processing terms appropriate to deployment;
3. exact public API/SDK licence if those artifacts are published;
4. trademark/brand usage rules and copyright notices;
5. dependency SBOM, `THIRD_PARTY_NOTICES.md`, source-offer obligations and asset provenance;
6. contributor/employee/contractor rights assignments or inbound licence evidence;
7. lawful migration/import and external-provider terms;
8. owner/legal approval recorded against the immutable release checksum.

Until this gate passes, development may proceed privately but no artifact or protected content may be distributed. A licence decision may be refined by counsel without removing a functional requirement; incompatible packaging is resolved through provided/runtime separation or original replacement.
