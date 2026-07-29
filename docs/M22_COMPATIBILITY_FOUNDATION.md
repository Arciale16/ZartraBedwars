# M22 compatibility foundation

**Status:** Phase 3 client translation and feature parity implemented; exact runtime/provider certification pending
**Requirements:** `ZBW-ARC-002`, `ZBW-INT-004`, `ZBW-INT-010`,
`ZBW-COMPAT-001..009`, `ZBW-READY-001`, `ZBW-READY-002`,
`ZBW-READY-005`, `ZBW-READY-006`, `ZBW-LICENSE-001/002/005`

## Scope and ownership

M22 remains active. Phase 2 supplied exact-runtime server adapters and fail-closed
Java 8/11/16/17 bootstraps. Phase 3 adds the Java 8
`zbw-compat-client` adapter boundary. Client translation is evaluated only after
an exact server `CompatibilityAdapter` has been selected; it cannot wrap,
replace or mutate that adapter. Domain, game, shop, progression, replay, Atlas,
Redis, proxy and provider ownership remain unchanged.

The canonical machine model is `build/m22-compatibility-matrix.json`. It binds
22 exact server fixtures to nine server families, five JDK toolchains and five
independent client paths. Phase 3 implements deterministic parity decisions for
all ten required client surfaces. The 45 runtime/client cells remain
certification-pending until the exact private/public server fixtures and locked
provider binaries can be exercised.

## Client adapter boundary

`zbw-compat-client` depends only on `zbw-compat-api` and compiles for Java 8.
It contains no ViaVersion, ViaBackwards, ViaRewind, Geyser, Floodgate, Bukkit,
Paper, NMS or packet implementation import. An operator runtime supplies the
nonblocking `ClientTranslationGateway` against independently installed plugins.
The gateway returns only an opaque session key, protocol number, client edition,
input family and a complete provider inventory; no player name, address, device
identifier or vendor object crosses the boundary.

Provider paths require these exact chains:

| Client path | Exact prerequisites | Behavior when absent, incompatible or duplicate |
|---|---|---|
| Native Java | none | remains available when optional providers are absent |
| ViaVersion | ViaVersion 5.4.2 | translated path blocked; native path unaffected |
| ViaBackwards | ViaVersion 5.4.2 + ViaBackwards 5.4.2 | translated path blocked |
| ViaRewind | ViaVersion 5.4.2 + ViaBackwards 5.4.2 + ViaRewind 4.0.6 | legacy translated path blocked |
| Bedrock | Geyser 2.7.0 + Floodgate 2.2.4 | Bedrock path blocked |

Discovery, inspection and cleanup are asynchronous. Sessions are bounded to
4,096, duplicate opens are idempotent, every close releases its gateway state
and shutdown drains all tracked sessions. A discovery or inspection failure
fails closed without blocking an owner/tick thread.

## Feature parity policy

Every report contains exactly one outcome for GUI, shop, spectator, replay
access, hotbar, text, sound, particles, entity display and input. Each outcome
is native, translated, equivalent fallback, explicitly degraded decoration or
blocked. Activation is permitted only when every gameplay-information surface
is preserved. A decorative reduction is legal only with an equivalent visible
or textual cue and an explicit diagnostic reason.

The evaluator first proves that the already-selected server adapter exposes the
required semantic kinds. A missing server capability blocks the affected client
feature; a client translator can never manufacture or conceal an unsupported
server semantic. `docs/COMPATIBILITY_FALLBACKS.md` owns the complete Phase 3
fallback rows.

## Dependency acquisition gate

`build/m22-provider-lock-requirements.json` still blocks Maven declarations and
artifact downloads for ProtocolLib, ViaVersion, ViaBackwards, ViaRewind, Geyser
and Floodgate. Exact upstream coordinates, artifact and licence-text SHA-256,
provenance and complete transitive graphs remain prerequisites. Phase 3 therefore
uses vendor-neutral gateways and introduces no unlocked dependency, repository,
binary, lock row or SBOM component. This is an enforced acquisition gate, not a
waiver or support claim.

## Phase 3 evidence

`ClientModelTest`, `ClientAdapterMatrixTest` and
`ClientCompatibilityLayerTest` cover immutable/privacy-safe models, exact
provider chains, absence, incompatibility, duplicates, all five client paths,
all ten feature surfaces, packet/input semantic requirements, decorative
fallbacks, missing server capability, classloading isolation, bounded sessions,
idempotent open, failed discovery/inspection and cleanup.

This checkpoint implements the client-adapter and deterministic parity portions
of `ZBW-INT-010` and `ZBW-COMPAT-004..008`. Exact provider binary linkage,
server startup/gameplay execution and the full 45-cell certification remain M22
closure gates. No release support claim is made.