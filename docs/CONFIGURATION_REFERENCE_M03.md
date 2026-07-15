# M03 configuration reference

## Model and generation

`ConfigurationModel.InitialCatalog` is the canonical M03 schema catalogue. `ReferenceGenerator` deterministically emits a fully commented reference document and a Markdown option table from the same typed definitions, so documentation and validation cannot drift. Every option records purpose, typed default, accepted values/range, example, dependencies, incompatibilities, performance and security impact, reload target, restart requirement, compatibility, deprecation and migration guidance.

M03 deliberately models logical documents without selecting a YAML library or performing filesystem access. Runtime source/sink adapters may be added only in their assigned milestone and must first produce the same immutable `Document` representation selected by ADR-0018.

## Logical files

The initial version-1 catalogue contains exactly these 36 paths:

`config.yml`, `deployment.yml`, `database.yml`, `redis.yml`, `proxy.yml`, `cloudnet.yml`, `arenas.yml`, `maps.yml`, `modes.yml`, `shops.yml`, `upgrades.yml`, `generators.yml`, `items.yml`, `quests.yml`, `achievements.yml`, `challenges.yml`, `battlepass.yml`, `cosmetics.yml`, `content.yml`, `rewards.yml`, `statistics.yml`, `placeholders.yml`, `replay.yml`, `atlas.yml`, `anticheat.yml`, `parties.yml`, `npcs.yml`, `holograms.yml`, `gui.yml`, `messages.yml`, `permissions.yml`, `compatibility.yml`, `performance.yml`, `security.yml`, `integrations.yml` and `integrations/discord.yml`.

Every document contains `meta.schema-version`. Feature-specific schemas add only foundation settings assigned to M03; generic files retain typed version/reload metadata until their owning feature milestone adds its schema.

## Strict validation

Unknown keys, missing required values, malformed types, out-of-range values, unmet dependencies and incompatible options are errors. `ConfigurationValidationService` validates every document, rejects duplicate or missing logical files, limits a document to 10,000 options and runs injected external and cross-document checks without doing I/O. Error details contain configuration keys and safe explanations, never resolved secrets.

The initial cross-document rules require enabled database/Redis/proxy/CloudNet/Discord providers to have a corresponding `SecretRef`. Provenance copying remains disabled. Minimum and maximum generator multiplier bounds must be coherent.

## Migration and reload

Migrations are registered as consecutive `N -> N+1` pure transformations. Backup acknowledgement is mandatory before a migration runs. Failure returns a typed report and preserves the original document.

Reload is serialized by the caller and uses an immutable plan:

1. validate all candidate documents;
2. prepare every affected participant;
3. apply each prepared change;
4. on failure, roll back applied participants in reverse order;
5. publish the candidate as last-known-good only after complete success.

Restart-only changes are reported and never live-applied. Supported targets are represented by `ReloadTarget`; the generated reference identifies the target of every option.

## RESOURCE SCARCITY

The original eleventh Private Games modifier is configured in `modes.yml`. Each preset independently defines `iron`, `gold`, `diamond`, `emerald`, `custom-default` and a namespaced `custom-overrides` map. Values are constrained to `0.10..5.00` and must also satisfy the bounds in `generators.yml`.

| Preset | Default multiplier |
|---|---:|
| Scarce | 0.50 |
| Reduced | 0.75 |
| Normal | 1.00 |
| Abundant | 1.50 |
| Extreme | 2.50 |

`private-games.resource-scarcity.change-policy` defaults to `countdown-locked`; `dynamic-rate-safe` is accepted only for a later runtime provider that advertises safe live-rate mutation. GUI editing, command surfaces, PlaceholderAPI and generator application remain M20.

## Security and compatibility foundations

Provider credentials are `SecretRef` values, not token strings. Discord defaults disabled. Content provenance must be approved and proprietary copying must remain false. Scripting defaults disabled with capability authorization. Replay/Atlas retention and privacy defaults, network authority, visibility, balance-profile IDs and semantic compatibility fallback policy are schema declarations only; their runtime owners remain M04 and later milestones.
