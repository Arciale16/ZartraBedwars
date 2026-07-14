# Declarative Scripting Security Specification

**Status:** Accepted
**Decision:** RC-018
**Requirement:** `ZBW-READY-004`

## Decision

ZartraBedWars does not execute JavaScript, Groovy, Kotlin, shell, JVM bytecode or arbitrary expressions from configuration. “Script” means a versioned declarative action graph interpreted by a project-owned engine. The engine is disabled by default and cannot become enabled through hot reload without `zartra.admin.scripts.enable`, typed confirmation and audit.

## Capability model

Allowed capabilities are narrow application commands such as `message.send_key`, `sound.play_semantic`, `effect.play_semantic`, `reward.request`, `stat.read_cached`, `match.read_snapshot`, `timer.schedule`, `condition.compare`, `random.choose_weighted` and `event.emit_custom`. Each definition declares owner, stable ID, trigger allowlist, subject/arena scope, maximum calls, permissions and expiry/version.

Forbidden in all profiles: filesystem, process execution, network/socket/HTTP, JDBC/Redis, reflection, classloader, native/JNI, environment/secrets, thread creation/control, JVM/system properties, arbitrary class/method access, dynamic dependency loading, raw packets/NMS, direct Bukkit objects, raw command dispatch, permission mutation and unrestricted economy/stat/case changes.

Rewards and mutations invoke authorized application use cases with idempotency and configured caps. A script cannot grant its caller more capability than the signed definition, trigger and host permission intersection.

## Resource limits

| Limit | Default hard maximum |
|---|---:|
| Parsed definition size | 64 KiB |
| Nodes/actions per graph | 100 |
| Nesting/call depth | 32 |
| Instructions per invocation | 10,000 |
| Mutable invocation state | 1 MiB |
| String value | 4 KiB |
| Collection entries | 256 |
| CPU budget | 5 ms measured worker CPU |
| Wall deadline | 250 ms including scheduled application calls |
| Concurrent invocations/server | 64; per arena 8; per player 2 |
| Emitted actions | 100/invocation and configured per-capability rate limit |
| Scheduled delay/lifetime | 24 hours; canceled on definition disable and scoped lifecycle end |

Evaluation and compilation use dedicated bounded executors, never the server tick thread. Live Bukkit/entity objects never enter the interpreter; immutable snapshots and typed IDs only. Applying an allowed result occurs through the owning scheduler and revalidates match/player/definition version.

## Definition lifecycle

Definitions are schema-validated, normalized, hashed and optionally signed. Import is a dry run showing capabilities, scopes, limits, changed IDs and provenance. Production activation requires an approved source, permission and confirmation. Invalid/over-budget definitions remain inactive; hot reload retains last-known-good. Disable/revoke cancels pending work and prevents new invocations.

Audit records definition/hash/version, actor, trigger, subjects as purpose-limited IDs, requested/allowed/denied capabilities, duration/instructions, result and correlation ID. It records no secret or arbitrary message content. Metrics use bounded definition/category labels.

## Failure behavior and tests

Timeout, cancellation, invalid state, denied capability, queue saturation or internal error produces a typed result, rate-limited diagnostic and no partial direct mutation. Transactional application actions either commit idempotently or do not apply. Gameplay continues with the configured non-script fallback.

Required tests include parser/property fuzzing; oversized/deep/cyclic graphs; instruction/memory/time/concurrency exhaustion; forbidden capability and confused-deputy attempts; reflection/classloader/path/network/command injection strings; signature/hash tampering; reward replay/duplicate triggers; lifecycle cancellation; audit redaction; tick-thread detector; and sustained `SHARED_40` load. Any escape, main-thread evaluation or unbounded allocation blocks release.
