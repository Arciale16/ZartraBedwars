# Milestone 3 implementation evidence

## Scope

M03 implements the configuration, localization, authorization and validation foundations allocated by `docs/MILESTONES.md`. The direct Requirement IDs are `ZBW-OPS-001..004`, `ZBW-UX-004`, `ZBW-UX-005`, `ZBW-DISCORD-006` and `ZBW-DISCORD-008`. It also supplies only the M03 schema/configuration portions of `ZBW-ADDON-464..473`, `ZBW-CONTENT-001..011`, `ZBW-COMPAT-001..009` and `ZBW-READY-003/004/010/011/013/014/015/018`.

This milestone does not add a Minecraft bootstrap, storage, a scheduler, commands, GUIs, gameplay, version adapters, content packs or M04 production paths. Continuing requirements retain their later milestone owners.

## Materialized modules and boundaries

`configuration/zbw-config` is the only new reactor module. It targets Java 8 bytecode and has production dependencies only on `zbw-api` and `zbw-application`. Architecture validation rejects Bukkit/Paper/NMS, proxy, Redis, SQL, filesystem, scheduler and implementation-provider dependencies. The existing Discord API artifact gains the disabled provider required for optional operation.

The public API adds these exported packages:

- `io.zartra.bedwars.api.authorization`
- `io.zartra.bedwars.api.configuration`
- `io.zartra.bedwars.api.localization`
- `io.zartra.bedwars.api.secret`

The implementation module owns schema, migration, reload, authorization, localization, secret-resolution and cross-document validation packages. No public API exposes an implementation class.

## Delivered behavior

- An immutable, versioned model covers the 36 logical configuration files required by `ZBW-OPS-001`.
- Every initial option has purpose, default, accepted values, example, dependency, incompatibility, performance, security, reload/restart, compatibility, deprecation and migration metadata.
- Strict validation rejects unknown, malformed, missing-dependency and cross-document-invalid values with stable issue codes and safe messages.
- Pure consecutive migrations require backup success before conversion and retain the source document after failure.
- Targeted reload uses prepare/apply/rollback phases, reports restart-only changes and retains the last-known-good snapshot; an apply failure cannot leave a partially applied configuration.
- Authorization uses immutable exact grants, target scoping, one-hop aliases, 33 canonical actions and a sanitized audit port. It has no wildcard, parent-node or role-label authority.
- Localization supports deterministic catalogs, typed escaped parameters, per-player/server/fallback locale selection, live locale changes, completeness reporting and deterministic import/export. Platform text rendering remains M22.
- Secrets are represented by `SecretRef`, resolved only through injected provider/environment/protected-file ports, returned through zeroizable leases and removed from diagnostics by exact redaction and allowlist export.
- The disabled Discord provider performs no network I/O, owns no thread and advertises no capability; Discord is not required for startup or gameplay.
- RESOURCE SCARCITY has independent iron, gold, diamond, emerald, custom-default and per-custom-resource multipliers plus the original `Scarce`, `Reduced`, `Normal`, `Abundant` and `Extreme` presets. Host GUI/runtime application remains M20.

## Deterministic evidence

The M03 validator entry point is `tools/validation/run_m03_validation.py`. Its checks include configuration boundaries, the exact logical-file and permission-action inventories, JavaDoc, immutable M02 API preservation, the M03 API signature baseline, dependency/licence locks, governance tests and the three documentation coverage validators.

The clean build and quality profile create the Java-8 module JARs, aggregate source/Javadoc archives, `target/zartrabedwars-m03-javadoc.zip`, JaCoCo reports, SpotBugs reports and Checkstyle reports. The exact final commands and results are recorded in the pull request and the M03 exit-evidence row of `docs/MILESTONES.md`.

## Deferred owners

M04 owns persistence, migrations backed by a database, outbox/inbox and caches. M05 owns executors, scheduling and health runtime. M09 owns commands and GUIs. M16 owns concrete Discord providers. M20 owns the Private Games host surfaces and live generator mutation. M22 owns client/version rendering adapters. These are exclusions, not reductions of their requirements.
