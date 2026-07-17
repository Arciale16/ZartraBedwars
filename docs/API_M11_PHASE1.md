# M11 Phase 1 API — shop and content foundations

**Status:** implemented and verified Phase 1 boundary; M11 remains active
**Requirements:** `ZBW-SHOP-001..004`, foundational portions of `ZBW-CONTENT-002/010`, `ZBW-READY-004`, and contract portions of `ZBW-ADDON-380..388`

## Modules and packages

- `zbw-shop` exports `io.zartra.bedwars.shop.api` and keeps orchestration in
  `io.zartra.bedwars.shop.application`.
- `zbw-content` exports `io.zartra.bedwars.content.shop` with original starter definitions.
- `zbw-scripting-api` exports `io.zartra.bedwars.scripting.api` references only.
- `zbw-scripting-engine` is not materialized: Phase 1 does not execute declarative actions.

All three modules target Java 8 bytecode and contain no Bukkit, Paper, NMS, JDBC, filesystem,
Redis, proxy or runtime-configuration imports.

## Catalog and tender model

`ShopIds` supplies type-distinct catalog, category, item and rotation IDs. `ShopCatalog` is an
immutable revisioned snapshot containing ordered categories, items, semantic item/icon IDs,
localized message keys, mode/arena/group/team scope, visibility, permissions, conditions,
multi-resource prices, bulk rules, cooldowns and player/team/arena/inventory limits.

`TenderRegistry` resolves each priced `ResourceId` to one explicit provider. The built-in registry
contains iron, gold, diamond and emerald. Extension-defined match resources use the same atomic
transaction boundary; persistent/virtual and Vault providers remain M12 and M21 respectively.

## Quote and atomic purchase contract

`PurchaseService.quote` performs exact catalog/scope checks, central authorization through
`AuthorizationService`, availability, bulk/confirmation, tender, balance, capacity, ownership,
limit, cooldown and registered custom-condition validation. Expected denials are
`PurchaseFailure` values rather than exceptions.

`PurchaseService.execute` revalidates every required central-authorization node immediately before
it invokes `PurchaseTransactionPort.commit` exactly once. The immutable quote carries the sorted,
deduplicated authorization set. The port contract requires revision and idempotency revalidation,
all resource debits, the complete item grant,
player/team/arena counters, cooldown state and purchase history to commit as one indivisible
owner-thread mutation. Any rejection must leave all state unchanged. The port must never perform
database, filesystem or network I/O on the Minecraft owner thread.

## Quick Buy, favourites, history and rotation

`ShopUserData` defines bounded immutable Quick Buy, favourite, optimistic preference and newest-
first history values plus asynchronous persistence ports. `RotationContracts` defines timezone,
period, start/end, weighted pool, slot, no-repeat, cooldown, permission, active snapshot, local
optimistic persistence and non-blocking event contracts. Runtime selection, administrative
overrides and distributed synchronization are not claimed in Phase 1.

## Compatibility and evolution

Catalog revisions and exact script schema versions fence incompatible changes. Additive optional
metadata may evolve within the API major version; semantic changes require a new stable content ID,
catalog revision or API major version. Platform item stacks and visual fallbacks remain adapter
responsibilities, with full legacy certification owned by M22.
