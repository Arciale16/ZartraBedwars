# ADR-0018: Typed configuration documents and adapter boundary

**Status:** Accepted<br>
**Date:** 2026-07-15<br>
**Requirements:** `ZBW-OPS-001`, `ZBW-OPS-003`, `ZBW-OPS-004`, `ZBW-UX-005`, `ZBW-DISCORD-008`

## Context

M03 must define and test all initial configuration semantics while remaining independent of filesystem access, YAML libraries, Minecraft lifecycle and runtime scheduling. Selecting a concrete parser or persistence adapter now would add an unaudited dependency and mix M04/M05 runtime concerns into the configuration foundation.

## Decision

The canonical boundary is an immutable, versioned `ConfigurationModel.Document` containing validated `ConfigurationKey` to scalar-source values. Each of the 36 logical files has a typed `Schema`; parsing, filesystem and runtime configuration providers are adapters that must map into this model before validation. M03 supplies deterministic reference generation, strict validation, pure consecutive migration and transactional in-memory reload coordination without reading or writing a file.

Secrets never become document values. Documents contain only `SecretRef` identities; injected sources resolve characters into closeable zeroizing leases. Localization import/export likewise uses caller-provided `Reader`/string boundaries and does not select persistence.

## Alternatives considered

| Alternative | Benefit | Cost/risk | Reason not selected |
|---|---|---|---|
| Add a YAML implementation in M03 | Immediate file loading | Adds a dependency before its runtime owner and couples schemas to parser behavior | Deferred to the assigned adapter milestone |
| Use raw nested maps throughout | Minimal type definitions | Weak validation, unstable metadata and implementation leakage | Rejected in favor of explicit immutable types |
| Let each feature parse its own file | Local ownership | Duplicate policy, partial reload and inconsistent secret handling | Rejected; schemas and validation are centralized |

## Consequences and controls

Runtime source/sink adapters must preserve unknown-key rejection, schema versions, safe error codes, backup-first migration and last-known-good reload semantics. An adapter choice requires dependency/licence approval and compatibility tests but cannot change public M03 semantics. This decision does not implement storage, scheduling, commands, GUIs or Minecraft adapters.
