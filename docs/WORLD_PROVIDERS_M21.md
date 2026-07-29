# M21 optional world-provider integrations

Scope: `ZBW-INT-005`, `ZBW-ARENA-005`, `ZBW-ARC-007`.

## Boundary

`zbw-integration-world-providers` is a Java 8 adapter module over the existing M06 `WorldProvider` and M21 `Provider` contracts. M06 retains native world orchestration and fallback ownership; M07/M08 retain arena and game lifecycle ownership. The module contains no Bukkit, Paper, NMS, storage, Redis, proxy or vendor imports and performs no direct world mutation.

Each adapter receives an operator-supplied `WorldProviderGateway`. Plan creation is side-effect free; the M06 orchestrator remains responsible for affinity, execution, rollback and bounded scheduling. A gateway result that changes operation identity, target or type is rejected and the native provider is used.

## Certified adapters

| Adapter identity | Operator runtime | Exact baseline | Capability projection |
|---|---|---:|---|
| `zartra:worldedit` | WorldEdit | `7.3.16` | edit and selection planning |
| `zartra:fawe` | FastAsyncWorldEdit | `2.15.1` | fast edit and reset planning |
| `zartra:worldguard` | WorldGuard | `7.0.17` | region-protection planning |
| `zartra:slimeworldmanager` | AdvancedSlimePaper/SlimeWorldManager | `5.1.0` | loading and snapshot planning |
| `zartra:multiverse-core` | Multiverse-Core | `5.3.3` | loading and registry planning |

These are compatibility identities, not bundled dependencies. Vendor binaries remain operator-supplied under `DEPENDENCY_LICENSE_AUDIT.md`; the Maven lock and SBOM are unchanged.

## Lifecycle and degradation

Paper declares all five plugins through `softdepend`. The generic M21 provider runtime starts registered adapters asynchronously, rejects duplicate canonical IDs and drains/stops them during disable. The compatibility probe has four safe outcomes:

- available and compatible: adapter becomes `RUNNING` and may delegate plans/snapshots;
- absent: adapter is `DISABLED` and native fallback remains active;
- incompatible or indeterminate version: adapter is `UNAVAILABLE` and native fallback remains active;
- gateway failure or malformed result: the individual operation falls back without changing arena, game or storage ownership.

No compatibility probe blocks the Paper owner thread. Gateway implementations must keep vendor callbacks outside that thread and must not execute plans during capability detection.

## Certification

`WorldProviderAdaptersTest` covers all five identities and pinned versions, asynchronous compatibility, absent/incompatible/indeterminate providers, restart and cleanup lifecycle, duplicate identity rejection, vendor-class isolation, malformed plans, gateway failure and native fallback. Paper runtime tests cover generic registration, duplicate rejection, absent-provider startup and reverse cleanup. Plugin Doctor inventories all fourteen M21 providers without exposing secrets or world data.

M21 closes only the provider integration allocation. Full 1.8–1.21.x runtime/provider compatibility remains M22.
