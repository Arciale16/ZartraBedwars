# ADR-0017: Extension metadata format and compatibility validation

**Status:** Accepted<br>
**Date:** 2026-07-14<br>
**Requirements:** `ZBW-ARC-003`, `ZBW-ARC-010`, `ZBW-ECO-002`, `ZBW-ECO-003`, `ZBW-CONTENT-010`, `ZBW-DISCORD-005`

## Context

M02 requires deterministic extension metadata validation without adding a runtime configuration, JSON/YAML parser, filesystem dependency or class-loading path before M03/M23. Metadata must remain Java 8 compatible, safe for untrusted input and replaceable without exposing internal implementation classes.

## Decision

Use a schema-versioned UTF-8 `key=value` exchange format read only from a caller-provided `Reader`. Schema 1 has a fixed allowlist of scalar and repeated keys for identity, version ranges, entrypoint, Minecraft range, dependencies, capabilities, permissions and configuration keys. Unknown, duplicate, missing and malformed keys are errors. The reader performs no filesystem access and no class loading.

Validation is a separate deterministic contract. It checks target API/product/Minecraft compatibility, catalogue-unique extension IDs, dependency presence/ranges, self/duplicate dependencies and duplicate permission/configuration declarations. Issues are typed and sorted independently of input iteration order. Runtime discovery and activation are not part of this ADR or M02.

## Alternatives considered

| Alternative | Benefit | Cost/risk | Reason not selected |
|---|---|---|---|
| JSON/YAML library in M02 | Familiar nested syntax | Adds an unneeded runtime/parser dependency and preempts M03 configuration architecture | Deferred to a future schema adapter if needed |
| Java properties API | Built in | Implicit escaping/default charset and duplicate-key behavior weaken deterministic rejection | Project reader keeps explicit UTF-8 and duplicate checks |
| Annotation/class scanning | Convenient discovery | Loads untrusted code and couples metadata to runtime/classpath behavior | Forbidden before validation |

## Consequences and controls

The initial syntax is deliberately small and not a runtime configuration language. Future schemas or JSON/YAML adapters may map into the same immutable `ExtensionMetadata` model after an API-compatible decision. Schema/API incompatibility fails validation before activation. Fixtures, malformed-input tests, deterministic issue ordering, JavaDoc and the binary signature baseline are acceptance evidence.
