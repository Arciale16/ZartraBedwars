# M11 Phase 4 — Utility-item action API

Phase 4 implements the Java 8-neutral portion of `ZBW-SHOP-007` and allocated local addon
mechanics in `io.zartra.bedwars.shop.item`.

`UtilityItemDefinition` and `UtilityItemCatalog` provide immutable identities, semantic inventory
items, exact permission nodes, match-resource prices, cooldowns, limits, target rules and bounded
parameters. `AddonMechanics.starterCatalog()` supplies original configurable definitions for
Pop-up Tower, Rush, Ultimate, BedSteal, Voidless, Sponge, generator actions and Item Rotation.

`ItemActionService` consumes M08 `MatchSnapshot` state and never recreates match/session lifecycle.
It validates the active session, authorization, server-authoritative target facts, cooldown, limit
and idempotency key before invoking an atomic transaction port. Successful actions produce a typed
semantic effect for an owner-thread adapter. Cleanup removes match-owned effects and closes the
runtime.

`ItemActionPorts.Transaction` owns all-or-nothing match-resource debit and inventory consumption.
It must compensate a committed debit when effect application rejects before publication.
`ItemActionPorts.Effect` translates validated effects only and contains no action policy. No
Bukkit, Paper, NMS, persistent progression, statistics, PlaceholderAPI, distributed coordination,
external provider or legacy compatibility type is exposed.

This is not a general-purpose scripting platform. The declarative sandbox remains disabled by
default and final interpreter/security certification belongs to final M11 integration.
