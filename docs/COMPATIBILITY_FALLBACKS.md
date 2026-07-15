# Minecraft 1.8 Compatibility Fallback Matrix

**Status:** accepted mandatory compatibility policy<br>
**Decision:** RC-075<br>
**Requirements:** `ZBW-COMPAT-001..009`<br>
**Implementation status:** M01 governance/fixture manifest VERIFIED; M06/M22 allocation reconciled; compatibility adapters NOT STARTED

The exact server distributions, build hashes, JDKs, artifact families and client-protocol certification dimensions are normative in `RUNTIME_COMPATIBILITY_MATRIX.md`. This file owns semantic fallbacks; the runtime matrix owns which exact environments must prove them.

## 1. Compatibility guarantee

Minecraft server runtime 1.8 is a mandatory supported target, not merely a modern server accepting 1.8 clients through a protocol translator. Shared gameplay/domain semantics remain version-neutral. Separate bootstrap/toolchain and narrow compatibility artifacts may be used because a Java 21-only artifact cannot safely run on legacy JVM/server baselines.

For an unavailable 1.8 capability, ZartraBedWars must provide a safe visual and functional fallback. Gameplay functionality is preserved. Only a purely decorative sub-effect may be suppressed, and only when no safe equivalent exists; suppression must be declared in this matrix, configurable, observable and tested.

## 2. Adapter boundary

Core, domain, application and feature modules never import versioned Bukkit/NMS classes. They use `zbw-compat-api` representations for materials, item metadata, sounds, particles, text, inventory/UI capabilities, entities, packets, scheduler context and client capability.

M06 defines `zbw-compat-api`, typed fallback/degradation contracts and primary Paper 1.21.1 mappings in the Java-21 `zbw-compat-v1_20-v1_21` adapter. It does not create or certify a legacy adapter.

`zbw-compat-v1_8` is implemented only in M22 and owns all 1.8 mappings and packet/platform calls. Startup selects one tested adapter and rejects an incompatible server before feature activation. Missing mappings return typed unsupported/fallback results; they never leak a modern enum/class name to legacy runtime code.

## 3. Mandatory fallback matrix

| Capability | Modern intent | 1.8 fallback | Configuration | Gameplay preservation | Decorative suppression allowed | Required tests |
|---|---|---|---|---|---|---|
| Materials and block states | Namespaced modern materials/block data | Validated legacy material plus data value from adapter registry | `compatibility.materials.<semantic-id>.v1_8` | Item cost/action/ownership remains identical | Only cosmetic appearance when no safe material exists | Every referenced material resolves and round-trips |
| Item identity/PDC | PersistentDataContainer/components | Namespaced legacy NBT through isolated adapter plus signed server-side identity record | `compatibility.item-metadata.v1_8` | Purchase, cooldown, ownership and anti-forgery remain | No | Tamper, clone, restart and migration tests |
| Custom model data | Resource-pack model predicate | Configurable legacy material/data, lore and optional enchant glint | Per-item `legacy-appearance` | Action and price remain identical | Model only | Pack absent/declined and vanilla-client tests |
| RGB/gradient text | MiniMessage/RGB component | Nearest configured legacy named colours with style-safe truncation | `compatibility.text.v1_8` | Meaning, warnings and actionable text remain | Gradient only | Colour, length, Unicode and injection fixtures |
| Click/hover components | Rich interactive component | Plain localized command hint plus equivalent command/GUI action | Per-message `legacy-action` | User can perform the same action | Hover decoration only | Task-equivalence usability tests |
| Titles/subtitles | Modern title API | Validated 1.8 title packet where supported, otherwise action bar/chat | `compatibility.feedback.title-order` | Countdown/result information remains visible | Animation only | Protocol/client capability matrix |
| Boss bars | Native boss-bar API | Ordered scoreboard/action-bar/title/chat presentation; no fake entity unless adapter tests prove safety | `compatibility.bossbar.v1_8` | Countdown/event/progress data remains available | Bar animation/style only | Join/leave/world-change cleanup and packet tests |
| Action bar | Modern audience API | Legacy chat packet position where safe, otherwise scoreboard/chat | `compatibility.actionbar.v1_8` | Status information remains | Fade animation | Packet and fallback-order tests |
| Toasts/advancements | Advancement toast | Localized title/action-bar/chat plus configured sound | `compatibility.toast.v1_8` | Achievement/reward notification remains | Toast artwork | Notification equivalence tests |
| Particles | Modern particle keys/data | Semantic mapping to a supported legacy particle with bounded density | `compatibility.particles.<semantic-id>.v1_8` | Telegraphs retain a non-colour-only cue | Pure ambience only | Mapping, packet-size and effect-budget tests |
| Sounds | Modern sound keys/categories | Supported legacy sound and adjusted pitch/volume, with text cue if none is safe | `compatibility.sounds.<semantic-id>.v1_8` | Alerts retain visible cue | Non-informational sound only | Mapping, invalid-key and volume-bound tests |
| Potion/effect types | Modern status effects | Equivalent supported effect or bounded native policy implementation | `compatibility.effects.<semantic-id>.v1_8` | Combat outcome remains specified | No | Duration/amplifier/combat golden tests |
| Entity types/poses | Modern display/entity capability | Safe legacy ArmorStand/NPC/hologram provider or static block/text representation | `compatibility.entities.<semantic-id>.v1_8` | Interaction target/action remains available | Animation/model only | Spawn/remove/chunk/reset/provider tests |
| Packet-only cosmetics | Modern per-viewer packets | Supported legacy packet recipe or server-visible bounded effect with viewer filtering | Per-cosmetic `legacy-renderer` | Cosmetic ownership/equip system remains | Visual activation if no safe renderer | Viewer isolation and packet-budget tests |
| Inventory GUI components | Modern item components/custom slots | 1.8 inventory size/slot-safe layout, legacy item metadata and paginated navigation | `compatibility.gui.v1_8` | Every critical action has a reachable slot/command | Decorative separators only | Every GUI path, stale click and pagination E2E |
| Off-hand actions | Off-hand slot/input | Main-hand item or explicit GUI/command action | Per-action `legacy-input` | Same action and cooldown | Off-hand animation | Input equivalence and duplicate-action tests |
| Spectator camera | Modern spectator target/camera APIs | Safe teleport/follow; first-person packet mode only when adapter capability passes | `compatibility.spectator.v1_8` | Target selection, follow and safe exit remain | First-person effect if unsafe | Death/disconnect/world-change/exit tests |
| Glowing/outline | Modern glowing state | Enchant glint, particles, nameplate/text marker or team-colour cue | `compatibility.highlight.v1_8` | Target/state remains distinguishable | Outline animation | Visibility/privacy and colour-blind cue tests |
| Resource-pack prompt/status | Modern prompt/status API | Legacy resource-pack send/status capability with explicit vanilla fallback | `compatibility.resource-pack.v1_8` | Optional assets never gate gameplay | Pack-only visuals/sounds | Accept/decline/fail/timeout tests |
| Scheduler/region ownership | Modern scheduler abstraction | Legacy main-thread scheduler adapter with the same owner-thread contract | `compatibility.scheduler.v1_8` | State transitions remain deterministic | No | Thread guards and shutdown cancellation tests |
| Namespaced keys | Modern namespaced registry APIs | Internal namespaced strings mapped by adapter without loading modern classes | Schema registry | IDs and migrations remain stable | No | Serialization and cross-version migration tests |

## 4. Starter semantic mappings

Exact Bukkit/NMS enum names are implementation data and belong only in the version adapter. The following are configuration-level semantic defaults:

| Semantic intent | Preferred 1.8 presentation | Safe final fallback |
|---|---|---|
| Confirmation | Legacy click-like sound + green text | Green text |
| Rejection | Legacy low note + red text | Red text with reason |
| Countdown | Supported note sound + title/action bar | Chat/scoreboard countdown |
| Team colour block | Legacy wool/clay material and data mapping | Configured safe team-colour block |
| Dust/colour particle | Supported redstone/spell particle mapping | Team-coloured text/title cue |
| Generator upgrade | Bounded particle ring + sound | Generator label/message update |
| Native bossbar progress | Action bar or scoreboard progress | Localized chat on change |
| Custom model item | Legacy material/lore/glint | Legacy material and localized lore |

Operators may override a visual mapping only with a value validated as supported by the active adapter. Invalid override data prevents the affected definition from enabling and retains the last-known-good mapping.

## 5. Matrix maintenance

Every new material, sound, particle, entity, packet, GUI component, text feature or input path must either reuse an existing semantic mapping or append a row before implementation. The compatibility validator fails on an unmapped semantic ID. The public API exposes semantic capabilities and fallback classification, never version-specific platform types.

The generated compatibility report records exact server artifact/build/SHA-256/JDK, adapter version, client-protocol path, feature, preferred renderer, actual renderer, suppressed decorative portion, configuration key, test fixture and known limitation.

## 6. Acceptance

RC-075 is resolved at policy/design level when this matrix is normative and traceable. M06 establishes only the Java-8-neutral fallback contracts and Paper 1.21.1 primary mappings and may certify only that foundation scope. M22 implements `zbw-compat-v1_8` and every other deferred legacy/intermediate fallback, then runs the complete startup, gameplay, GUI, item, packet, cleanup and migration matrix. A crash, unsupported enum/class exposure or loss of gameplay behavior is never an accepted fallback.

M03 contributes only the strict `compatibility.yml` schema declaration and per-option compatibility metadata used by the generated reference. No Minecraft material, particle, sound, text, entity, packet or GUI adapter is present, and M03 therefore makes no new runtime-support claim. M06/M22 must consume these version-neutral declarations behind their adapters and preserve every fallback rule above. Full 1.8–1.21.x certification remains an M22 release gate; an M06 primary-row result is never evidence of 1.8 or complete 1.20–1.21 support.
