# M03 public API reference

## Compatibility contract

All M03 public types compile to Java 8 bytecode, reject `null` at public boundaries unless JavaDoc explicitly states otherwise and expose immutable values or immutable snapshots. Failures expected during normal operation use typed decisions, issues or results rather than generic exceptions. Callers must not assume a Minecraft owner thread, blocking I/O or background execution: every M03 service is synchronous and performs only bounded in-memory work. Runtime adapters remain responsible for thread dispatch and I/O.

The append-only M03 signature baseline is `build/api-signature-baseline-m03.txt`. Validation requires the complete M02 baseline to remain present and rejects any unreviewed M03 surface drift. Deprecation requires JavaDoc replacement guidance and at least one public API compatibility window; removal requires a major API version and an accepted ADR.

## Authorization

| Type | Contract |
|---|---|
| `PermissionNode` | Validated canonical permission identity. |
| `AuthorizationSubject` | Typed subject identity and immutable exact grants. |
| `AuthorizationRequest` | Subject, action node and optional typed target. |
| `AuthorizationDecision` | Explicit allow/deny outcome with stable reason. |
| `AuthorizationService` | Central authorization port; implementations must audit sensitive decisions without leaking secrets. |

`DefaultAuthorizationService` is the sole M03 implementation. It supports exact grants and one-hop deprecated aliases; it does not infer roles, parents or wildcard grants.

## Configuration

| Type | Contract |
|---|---|
| `ConfigurationKey` | Validated dotted option identity. |
| `ConfigurationVersion` | Positive schema version with deterministic ordering. |
| `ReloadTarget` | Stable targeted-reload identity. |

`ConfigurationModel` supplies immutable schemas, documents, option metadata, validation reports and the deterministic reference generator. `ConfigurationMigrationService` owns pure consecutive migrations. `TransactionalReloadService` owns atomic prepare/apply/rollback coordination. `ConfigurationValidationService` composes schema and injected external checks without performing I/O itself.

## Localization

| Type | Contract |
|---|---|
| `LocaleId` | Normalized language/region identity. |
| `MessageKey` | Validated stable message identity. |
| `LocalizationService` | Catalog lookup using typed parameters and explicit locale selection. |
| `LocalizationService.Parameters` | Immutable parameter collection with duplicate-name rejection. |
| `LocalizationService.LocalizedMessage` | Escaped neutral message plus selected locale and fallback metadata. |

`DefaultLocalizationService` exposes immutable catalog replacement, per-player and server locale selection, completeness reports and deterministic line-format import/export. Minecraft component rendering and legacy fallbacks are adapter concerns assigned to M22.

## Secrets

`SecretRef` identifies `PROVIDER`, `ENVIRONMENT` or `PROTECTED_FILE` lookups without containing secret material. `SecretServices` owns injected resolution ports, a zeroizable lease, exact-value redaction and diagnostic allowlisting. Consumers must close leases promptly, never log resolved characters and never serialize a resolved secret.

## Discord optionality

`DisabledDiscordProvider` implements the existing provider-neutral Discord contract with an empty capability set, disabled health, stopped lifecycle and typed policy rejection. It creates no thread and performs no network operation. Enabling webhook, external-bot or custom providers remains M16.

## Extension lifecycle

Extensions may consume these stable API contracts through the M02 extension/provider lifecycle. They must declare compatible API versions and capabilities, validate configuration before activation, unregister resources during stop and treat all configuration/localization snapshots as immutable. Extension code must not depend on `zbw-config` implementation packages.
