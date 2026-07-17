#!/usr/bin/env python3
"""Generate and verify the owner-supplied native addon feature catalogue."""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / "docs" / "ADDON_FEATURE_CATALOG.md"
PRD = ROOT / "docs" / "PRD" / "PRD.md"
WIKI = "https://wiki.andrei1058.com/docs/BedWars1058/addons/"
M11_STANDARD_MILESTONE = (
    "M11 mechanics/config/API/feature presentation/primary Paper; "
    "M16 placeholders; M22 full compatibility"
)
M11_STATISTICS_MILESTONE = (
    "M11 mechanics/config/API/feature presentation/primary Paper; "
    "M15 statistics; M16 placeholders; M22 full compatibility"
)
M11_ROTATION_MILESTONE = (
    "M11 local rotation mechanics/persistence/API/feature presentation/primary Paper; "
    "M16 placeholders; M19 Redis coordination; M20 proxy/server synchronization; "
    "M22 full compatibility"
)
M11_STATISTICS_IDS = {
    "ZBW-ADDON-010", "ZBW-ADDON-061", "ZBW-ADDON-300",
    "ZBW-ADDON-315", "ZBW-ADDON-341", "ZBW-ADDON-438",
}


@dataclass(frozen=True)
class Addon:
    key: str
    tier: str
    name: str
    purpose: str
    overlaps: str
    milestone: str
    module: str
    config: str
    gui: str
    commands: str
    permissions: str
    api: str
    papi: str
    performance: str
    security: str
    tests: str
    docs: str
    features: tuple[str, ...]
    source: str = WIKI


def addon(
    key: str,
    tier: str,
    name: str,
    purpose: str,
    overlaps: str,
    milestone: str,
    module: str,
    features: Iterable[str],
    *,
    config: str | None = None,
    gui: str | None = None,
    commands: str | None = None,
    permissions: str | None = None,
    api: str | None = None,
    papi: str | None = None,
    performance: str = "Bounded main-thread work; cached reads; configurable update cadence and limits",
    security: str = "Validate state and ownership; deny-by-default administrative mutations; audit privileged actions",
    tests: str | None = None,
    docs: str | None = None,
    source: str = WIKI,
) -> Addon:
    label = key.replace("_", "-")
    token = label.replace("-", "_")
    permission_path = label.replace("-", ".")
    return Addon(
        key=key,
        tier=tier,
        name=name,
        purpose=purpose,
        overlaps=overlaps,
        milestone=milestone,
        module=module,
        config=config or f"addons/{label}.yml (enabled, defaults, per-mode/arena/group overrides)",
        gui=gui or f"{name} player surface plus validated admin editor/preview where state is configurable",
        commands=commands or f"/zbw {label} …; /zbw admin {label} reload|validate|inspect",
        permissions=permissions or f"zartrabedwars.{permission_path}.use; zartrabedwars.admin.{permission_path}.*",
        api=api or f"{name} service/query API; cancellable pre-event and immutable post-event",
        papi=papi or f"zartra_{token}_* player/state placeholders with documented null/offline fallbacks",
        performance=performance,
        security=security,
        tests=tests or f"Unit rules; Paper test-harness lifecycle/GUI/permission tests; cross-version and negative-path tests for {name}",
        docs=docs or f"Player, operator, configuration, command/permission, API/event and migration reference for {name}",
        features=tuple(features),
        source=source,
    )


def m08_addon(
    key: str,
    tier: str,
    name: str,
    purpose: str,
    overlaps: str,
    modules: str,
    features: Iterable[str],
    *,
    later_owners: str = "",
    config: str | None = None,
    commands: str | None = None,
    permissions: str | None = None,
    api: str | None = None,
    papi: str | None = None,
    source: str = WIKI,
) -> Addon:
    """Create one M08 capability with explicit continuing presentation ownership."""
    label = key.replace("_", "-")
    token = label.replace("-", "_")
    permission_path = label.replace("-", ".")
    command_surface = commands or (
        f"/zbw {label} …; /zbw admin {label} reload|validate|inspect"
    )
    permission_surface = permissions or (
        f"zartrabedwars.{permission_path}.use; "
        f"zartrabedwars.admin.{permission_path}.*"
    )
    api_surface = api or (
        f"{name} service/query API; cancellable pre-event and immutable post-event"
    )
    papi_surface = papi or (
        f"zartra_{token}_* player/state placeholders with documented null/offline fallbacks"
    )
    later = f"; {later_owners}" if later_owners else ""
    return addon(
        key,
        tier,
        name,
        purpose,
        overlaps,
        "M08 core/primary Paper; M09 final command/GUI/editor presentation; "
        f"M16 placeholders{later}; M22 full compatibility",
        f"{modules}, zbw-command-api, zbw-command-paper, zbw-ui-api, zbw-ui-paper",
        features,
        config=f"M08 core: {config or f'addons/{label}.yml with validated defaults and per-mode/arena/group overrides'}",
        gui=(
            f"M08 primary Paper: closed feature-specific {name} player feedback/projection only; "
            "M09 final: unified admin editor, preview and confirmation presentation"
        ),
        commands=f"M09 final presentation: {command_surface}",
        permissions=(
            f"M08 use cases enforce {permission_surface}; "
            "M09 command/GUI adapters revalidate the same nodes"
        ),
        api=f"M08 core/application: {api_surface}",
        papi=f"M16: {papi_surface}",
        tests=(
            "M08 unit/application and closed Paper 1.21.1 projection tests; "
            "M09 command/GUI/editor/confirmation tests; M16 placeholder tests; "
            "M22 full cross-version tests"
        ),
        docs=(
            f"M08 {name} application/primary-Paper reference; M09 command/permission/GUI reference; "
            "M16 placeholder and M22 compatibility reference"
        ),
        source=source,
    )


ADDONS: tuple[Addon, ...] = (
    m08_addon(
        "hotbar-manager", "Premium", "HotbarManager", "Native, state-aware and fully configurable player hotbars.",
        "ZBW-GAME-006, ZBW-GAME-008, ZBW-UX-001, ZBW-UX-006", "zbw-game, zbw-paper-modern, zbw-compat-api",
        (
            "Define independent hotbar loadouts for lobby, waiting, countdown, playing, spectator and post-game states",
            "Configure each hotbar slot with material, amount, name, lore, enchant glint and version-safe item metadata",
            "Bind a hotbar item to a native action such as join, leave, team select, shop, tracker, spectate or play again",
            "Apply conditional visibility and action availability by permission, game state, mode, arena and player context",
            "Apply deterministic global, group, mode and arena override precedence to hotbar definitions",
            "Provide a paginated admin editor with slot movement, action selection, validation and preview",
            "Reload valid hotbar definitions atomically while retaining the last-known-good configuration on failure",
            "Replace and restore state hotbars without duplication, item loss or contamination of saved inventories",
            "Expose hotbar state, selected action and rebuild operations through API/events and placeholders",
        ),
    ),
    addon(
        "armed-mode", "Premium", "Armed Mode Addon", "A gun-based BedWars mode with original weapon content and packet-safe presentation.",
        "ZBW-GAME-004, ZBW-SHOP-003, ZBW-SHOP-007, ZBW-ARC-004", "M11", "zbw-game, zbw-shop, zbw-compat-api",
        (
            "Enable Armed as an independently selectable mode with per-arena eligibility and isolated statistics",
            "Provide a registry for original ranged-weapon definitions and their version-neutral behavior",
            "Ship an original close-range rapid-fire weapon archetype",
            "Ship an original medium-range automatic weapon archetype",
            "Ship an original precision single-shot weapon archetype",
            "Ship an original high-impact slow-fire weapon archetype",
            "Ship an original spread-projectile weapon archetype",
            "Let players buy ranged weapons and ammunition through validated shop entries",
            "Support configurable weapon upgrade tiers and preserve upgrades through valid respawns",
            "Track magazines, reserve ammunition, reload timing and reload cancellation",
            "Apply configurable projectile damage, range, accuracy, falloff and headshot multipliers",
            "Enforce per-weapon fire cooldowns, purchase limits and anti-macro rate checks",
            "Allow the normal ranged shop category to be disabled while Armed replacements remain available",
            "Make ranged combat compatible with automatic bed defenses and protected-region rules",
            "Offer an optional original resource pack with packet-based models/sounds and a vanilla fallback",
            "Provide per-player language feedback plus admin weapon validation, preview and live diagnostics",
        ),
        source="https://voxel.shop/resource/bedwars1058-armed-mode.2394",
        performance="Pool projectile calculations; cap active shots; packet work only on the owning event loop/main thread",
        security="Server-authoritative ammo, cadence, hit and damage validation; signed/allowlisted resource-pack URL",
    ),
    addon(
        "cosmetics-premium", "Premium", "Cosmetics", "A native cosmetic platform with 300+ original built-in entries and custom content.",
        "ZBW-PROG-006, ZBW-PROG-007, ZBW-PROG-008", "M14", "zbw-progression, zbw-ui-paper, zbw-compat-api",
        (
            "Provide at least 300 distinct built-in cosmetic entries made from original or lawfully licensed content",
            "Cover every PRD cosmetic category with explicit category registration and browsing",
            "Create, edit, validate, enable and disable custom cosmetics without source-code changes",
            "Create and assign custom rarity definitions with display, sort order and acquisition metadata",
            "Support cosmetics that combine multiple triggers, visuals, sounds and state-safe actions",
            "Render supported cosmetics through packet-based viewers without persistent world pollution",
            "Provide server-side fallback rendering when packet features are unavailable on a client version",
            "Browse, filter, search, preview, equip, unequip and favorite cosmetics through GUIs",
            "Enforce ownership, rarity, permission, season, mode and mutual-exclusion rules before activation",
            "Persist equipped slots, favorites and ownership across reconnects and proxy nodes",
            "Grant and revoke cosmetic ownership through audited commands and API operations",
            "Expose catalogue, ownership, equipped state, rarity and activation lifecycle through public API/events",
            "Expose cosmetic counts, selected entries, rarity and unlock progress through PlaceholderAPI",
            "Budget, batch and cull cosmetic packets/effects per player, arena and tick",
            "Provide content-pack schema, validation report, migration guide and legal provenance manifest",
        ),
        security="Validate custom action allowlists and assets; prevent spoofed ownership; audit grants/revocations",
    ),
    addon(
        "private-games", "Premium", "Private Games", "Party-hosted private matches with isolated access, modifiers and policy.",
        "ZBW-GAME-002, ZBW-GAME-005, ZBW-STATS-006, ZBW-DEPLOY-002", "M20", "zbw-game, zbw-proxy-api, zbw-statistics",
        (
            "Let an eligible party leader create and own a private game reservation",
            "Restrict private-game admission to the host party and explicitly invited players",
            "Provide a host settings GUI and item with validated pre-start modifier changes",
            "Allow the host to start, cancel and transfer ownership under configurable policy",
            "Apply a one-hit-kill combat modifier",
            "Apply a configurable player-health multiplier modifier",
            "Apply a low-gravity movement modifier",
            "Apply a player-speed multiplier modifier",
            "Apply a respawn-time modifier",
            "Apply a game-event timing modifier",
            "Apply a no-emerald-generator modifier",
            "Apply a no-diamond-generator modifier",
            "Apply a block-protection-disabled modifier without bypassing world or server safety boundaries",
            "Apply an instant-bed-break modifier",
            "Apply a maximum team-upgrade tier modifier",
            "Register additional original private modifiers through a validated public extension API",
            "Configure whether private matches write normal, separate or no progression/statistics",
            "Persist host, roster, settings and rejoin tokens across shared servers and proxy transfers",
            "Provide audited staff inspect, join, transfer, terminate and recovery controls",
            "Expose private-state, host and active-modifier queries/events/placeholders with privacy-safe fallbacks",
        ),
        source="https://polymart.org/resource/1620/",
        config="addons/private-games.yml: modifiers, modifiers.resource-scarcity, presets and per-mode/arena/group overrides",
        gui="Private Game host settings GUI with modifier permissions, Resource Scarcity preset/custom multiplier editor, preview, reset and lock state",
        commands="/zbw private modifier resource-scarcity preset|set|reset|show; /zbw admin private-games reload|validate|inspect",
        permissions="zartrabedwars.private.games.host; zartrabedwars.private.games.modifier.resource_scarcity; zartrabedwars.admin.private.games.*",
        api="PrivateGameModifierRegistry and ResourceScarcityPolicy API; cancellable pre-change and immutable applied/failed events",
        papi="zartra_private_game_modifier_resource_scarcity, zartra_private_resource_<id>_multiplier and preset placeholders",
        tests="Unit multiplier/preset rules; native/custom generator contract tests; GUI/permission/rejoin/proxy and mid-game mutation E2E",
        docs="Private Games and Resource Scarcity player/operator/configuration/command/permission/API/event/placeholder reference",
        security="Signed reservations; invitation/party authorization; staff audit; no public leakage of private rosters",
    ),
    addon(
        "luckyblock-ntd", "Premium", "LuckyBlock NTD", "A native Lucky Block mode driven by safe, weighted and extensible outcomes.",
        "ZBW-GAME-004, ZBW-SHOP-006, ZBW-SHOP-007", "M11", "zbw-game, zbw-shop, zbw-arena",
        (
            "Enable LuckyBlock NTD as a selectable mode with per-arena eligibility and isolated statistics",
            "Generate lucky blocks through configurable generator tiers, intervals and spawn limits",
            "Validate lucky-block placement, pickup, ownership and opening inside an active arena",
            "Select outcomes from weighted pools with conditions, cooldowns and deterministic test seeds",
            "Provide original beneficial item, resource and temporary-buff outcomes",
            "Provide original hostile, trap and temporary-debuff outcomes with hard safety limits",
            "Provide original entity, structure, particle and sound outcomes with cleanup ownership",
            "Expose a safe outcome registry and cancellable open/result events for extensions",
            "Provide admin pool editing, simulation, probability validation and live diagnostics",
            "Cap spawned entities/blocks/effects and roll back every owned outcome at arena reset",
        ),
    ),
    addon(
        "discord-stats-premium", "Premium", "DiscordStats for BedWars1058", "Privacy-aware BedWars statistics and leaderboards delivered through Discord.",
        "ZBW-STATS-007, ZBW-INT-002, ZBW-OPS-007", "M16", "zbw-integration-discord, zbw-statistics",
        (
            "Look up an online or offline player's allowed BedWars statistics from Discord",
            "Render configurable player statistic cards/embeds with locale-aware formatting",
            "Render configurable global, mode, arena and group leaderboards",
            "Link Discord identities to Minecraft UUIDs through a verified account-link flow",
            "Register permission-aware slash commands with guild and channel allowlists",
            "Schedule opt-in leaderboard/statistic message refreshes without duplicate posts",
            "Cache and coalesce database queries while preserving a documented freshness bound",
            "Enforce token secrecy, API rate limits, privacy/consent, redaction and deletion policy",
            "Provide audited admin diagnostics, resync and message-repair operations",
            "Expose link, query, publish, failure and rate-limit lifecycle through API/events and metrics",
        ),
        config="integrations/discord-stats.yml plus secret environment variables; never plaintext tokens in exports",
        gui="In-game link/privacy/settings GUI; Discord slash-command embeds; admin diagnostics view",
        commands="Discord /bw profile|stats|leaderboard|link; /zbw discordstats diagnose|resync",
        permissions="zartrabedwars.discordstats.use|leaderboard|link; zartrabedwars.admin.discordstats.*",
        papi="Uses the canonical zartra_stats_*, zartra_rank_* and zartra_leaderboard_* categories in Discord templates",
        security="Least-privilege bot scopes; secret redaction; verified linking; rate limiting; consent and erasure controls",
    ),
    addon(
        "quests-addon", "Premium", "Quests", "Configurable daily, weekly, seasonal and custom BedWars objectives and rewards.",
        "ZBW-PROG-009, ZBW-PROG-010, ZBW-PROG-011, ZBW-PROG-012", "M13", "zbw-progression, zbw-ui-paper",
        (
            "Define daily, weekly, seasonal and administrator-authored quest schedules",
            "Define typed objectives over supported gameplay, economy, social and mode events",
            "Evaluate objective filters for mode, arena, group, team, item, victim and match validity",
            "Accumulate monotonic, idempotent quest progress and survive reconnect/proxy transfer",
            "Present active, locked, completed, expired and claimed quests in a paginated GUI",
            "Notify progress and completion through configurable chat, title, sound and action-bar channels",
            "Grant one or more validated rewards exactly once and recover interrupted deliveries",
            "Support explicit claim and policy-controlled auto-claim flows",
            "Provide audited admin CRUD, assign, progress, complete, reset, reroll and simulation controls",
            "Expose definitions, progress, completion and claim lifecycle through API/events/placeholders",
            "Archive/rotate expired instances and batch persistence without blocking the server thread",
        ),
    ),
    addon(
        "spectator-options", "Premium", "Spectator Options", "Per-session spectator viewing, movement and visibility controls.",
        "ZBW-GAME-009, ZBW-GAME-010, ZBW-UX-003",
        "M10 core/primary Paper/command/GUI; M16 placeholders; M22 full compatibility",
        "zbw-game, zbw-command-api, zbw-command-paper, zbw-ui-api, zbw-ui-paper, zbw-compat-api",
        (
            "Open a spectator-options menu from the spectator hotbar",
            "Select a configurable spectator flight-speed level",
            "Toggle spectator night vision without leaking effects after exit",
            "Automatically follow or teleport between valid living targets",
            "Enter and leave a target's first-person spectator camera safely",
            "Toggle visibility of other spectators independently of living players",
            "Persist allowed preferences while resetting target-bound session state on arena exit",
            "Enforce visibility, vanished-player, staff, private-game and target-consent policy",
            "Provide localized admin configuration, preview, reset and diagnostics",
            "Expose spectator options and target transitions through API/events/placeholders",
        ),
        source="https://www.spigotmc.org/resources/bedwars-spectator-options.133455/",
    ),
    addon(
        "spectate", "Free", "Spectate Addon", "A safe `/spectate <player>` entry point for local and distributed matches.",
        "ZBW-GAME-009, ZBW-GAME-010, ZBW-DEPLOY-002, ZBW-UX-004", "M20", "zbw-game, zbw-command-api, zbw-command-paper, zbw-proxy-api",
        (
            "Resolve `/spectate <player>` by UUID across local and proxy-registered arenas",
            "Validate target state, arena spectating policy, privacy, vanish and caller eligibility",
            "Reserve and transfer the caller to a remote target arena when required",
            "Create an isolated spectator session without joining a team or affecting match statistics",
            "Provide safe spectator exit, original-location return and failure recovery",
            "Rate-limit lookups and expose permission-aware suggestions, API queries, events and status placeholders",
        ),
        commands="/spectate <player>; /spectate leave; /zbw admin spectate inspect|toggle",
        permissions="zartrabedwars.command.spectate; zartrabedwars.spectate.bypass; zartrabedwars.admin.spectate.*",
    ),
    m08_addon(
        "deposit", "Free", "Deposit Addon", "Deposit eligible held items and resources into a player's Ender Chest.",
        "ZBW-GAME-008, ZBW-SHOP-006, ZBW-UX-006", "zbw-game, zbw-paper-modern",
        (
            "Trigger a deposit through a configured item action or command during eligible game states",
            "Deposit the held eligible stack into the invoking player's Ender Chest",
            "Deposit configured BedWars resource types from inventory with explicit quantity selection",
            "Apply allowlists, denylists, mode/arena rules, cooldowns and per-match limits",
            "Handle partial capacity and overflow atomically without deleting or duplicating items",
            "Keep Ender Chest ownership private and prevent access to protected or synthetic items",
            "Provide localized result feedback plus audited admin configuration, API events and diagnostics",
        ),
        commands="/deposit [hand|resources|all]; /zbw admin deposit reload|inspect",
        permissions="zartrabedwars.deposit.use|all; zartrabedwars.admin.deposit.*",
        papi="zartra_deposit_cooldown, zartra_deposit_last_amount and eligibility placeholders",
    ),
    addon(
        "spectator-playagain-menu", "Free", "Spectator & Play-Again Addon Menu", "A spectator navigation and replay-routing menu after elimination or match end.",
        "ZBW-GAME-003, ZBW-GAME-009, ZBW-UX-003",
        "M10 local shared-server behavior/command/GUI; M16 placeholders; M20 proxy routing; M22 full compatibility",
        "zbw-game, zbw-command-api, zbw-command-paper, zbw-ui-api, zbw-ui-paper, zbw-proxy-api",
        (
            "Open the combined spectator/play-again menu from a hotbar item and command",
            "List valid living players with team, health, distance and privacy-safe status",
            "Teleport/follow a selected valid spectator target",
            "Queue play again for the same mode through the canonical reservation service",
            "Offer an eligible same-map preference without promising unavailable capacity",
            "Offer mode/map selector navigation and lobby/leave actions",
            "Auto-open the menu after configurable elimination or game-end delay",
            "Handle target death, arena shutdown, queue failure and stale clicks with recoverable feedback",
            "Expose menu state and selections through permission-aware admin configuration, API/events and placeholders",
        ),
    ),
    m08_addon(
        "arena-start-message", "Free", "Arena Start Message", "Localized join announcements for arenas approaching a start.",
        "ZBW-GAME-006, ZBW-UX-005, ZBW-DEPLOY-002", "zbw-game, zbw-paper-modern, zbw-proxy-api",
        (
            "Emit one arena-start announcement when configured waiting/countdown thresholds are crossed",
            "Render arena, mode, group, player count, capacity and countdown in localized message templates",
            "Provide a clickable/interactive join action routed through eligibility and reservation checks",
            "Target configurable local, server-group, proxy-network or permission-filtered audiences",
            "Apply per-arena deduplication, cooldowns and cancellation when start conditions regress",
            "Support configurable sound/title/action-bar companions with a text-only fallback",
            "Provide admin preview/reload/diagnostics plus cancellable publish and join-click events",
        ),
        later_owners="M20 proxy delivery",
    ),
    addon(
        "compass", "Free", "Compass", "Team tracking and localized quick team communication.",
        "ZBW-GAME-002, ZBW-GAME-008, ZBW-PAPI-001",
        "M10 core/primary Paper/command/GUI; M16 placeholders; M22 full compatibility",
        "zbw-game, zbw-command-api, zbw-command-paper, zbw-ui-api, zbw-ui-paper, zbw-compat-api",
        (
            "Provide a state-safe tracker/communications compass hotbar item",
            "Select a living enemy target manually from a permission-safe GUI",
            "Select the nearest eligible enemy target automatically",
            "Update direction and distance at a bounded configurable cadence",
            "Respect invisibility, vanish, spectator, private-game and tracking-range rules",
            "Open a quick-communications wheel/menu from the compass",
            "Send predefined team-only callouts for attack, defend, resources, danger and regroup",
            "Attach safe target/location/team context to callouts without leaking hidden players",
            "Localize every tracker and callout message per sender/recipient with fallbacks",
            "Apply anti-spam cooldowns, mute preferences and API/events/placeholders for selections and messages",
        ),
    ),
    addon(
        "sponge-effects", "Free", "Sponge placement effects", "Animated, reversible effects when game sponges are placed.",
        "ZBW-SHOP-007, ZBW-GAME-002, ZBW-ARC-004", "M11", "zbw-game, zbw-arena, zbw-compat-api",
        (
            "Recognize configured game sponge placement only inside an active owned arena",
            "Validate placement against region, build, cooldown and item-ownership rules",
            "Run an expanding particle animation with configurable shape, radius, duration and density",
            "Run localized sound and optional safe knockback/status effects with hard bounds",
            "Map particles, sounds and materials through version capability adapters with fallbacks",
            "Cancel tasks and remove every owned temporary effect on reset, shutdown or arena transition",
            "Provide admin preview/configuration plus cancellable placement/effect API events",
        ),
    ),
    m08_addon(
        "anti-drop", "Free", "AntiDrop", "Recover eligible drops/resources that would otherwise be lost to the void.",
        "ZBW-GAME-002, ZBW-GAME-003, ZBW-SHOP-006", "zbw-game, zbw-paper-modern",
        (
            "Detect eligible owned item entities crossing an arena's configured void/loss boundary",
            "Capture an eligible stack exactly once before world removal",
            "Resolve the recipient by configurable owner, killer, team or nearest-eligible policy",
            "Filter resources/items by allowlist, source, game state, age and ownership metadata",
            "Deliver captured items atomically with overflow routing or a recoverable pending grant",
            "Resolve death, disconnect, simultaneous pickup and arena-reset races without duplication",
            "Provide localized recovery feedback plus admin inspect/configuration and capture API events",
        ),
    ),
    addon(
        "team-selector", "Free", "Team Selector", "Pre-game team selection with capacity, party and balance enforcement.",
        "ZBW-GAME-002, ZBW-GAME-007, ZBW-UX-003",
        "M10 core/primary Paper/command/GUI; M16 placeholders; M22 full compatibility",
        "zbw-game, zbw-command-api, zbw-command-paper, zbw-ui-api, zbw-ui-paper, zbw-compat-api",
        (
            "Open a team-selector GUI from a waiting-state hotbar item and command",
            "Display every configured team, supporting at least twelve teams without a hard-coded ceiling",
            "Show team colour, occupancy, capacity, selected state and join restrictions",
            "Select, change or clear a preferred team before the assignment lock point",
            "Reject full, disabled or permission-restricted team selections with localized feedback",
            "Keep parties together where possible and expose explicit split/deny policy when impossible",
            "Auto-assign unselected players using deterministic capacity and balance rules",
            "Restore valid selections on rejoin and discard stale selections after arena reset",
            "Provide admin configuration plus selection/assignment API events and team placeholders",
        ),
        source="https://www.spigotmc.org/resources/addon-team-selector-for-bedwars1058.60438/",
    ),
    addon(
        "bedwars-proxy", "Free", "BedWarsProxy", "A scalable proxy-wide arena selector, reservation and transfer plane.",
        "ZBW-DEPLOY-002, ZBW-DEPLOY-003, ZBW-DEPLOY-004, ZBW-ARC-007", "M20", "zbw-proxy-api, zbw-velocity, zbw-bungeecord, zbw-redis",
        (
            "Register arena servers, modes, groups, capacity and lifecycle status in the distributed registry",
            "Display a proxy-wide arena selector with live status and bounded-staleness indicators",
            "Filter and sort arenas by mode, group, map, availability, region and permission",
            "Create an atomic capacity reservation before any cross-server transfer",
            "Route direct join, quick join, map selection, spectate and play-again through one transfer contract",
            "Support multiple proxy nodes without duplicate joins, split-brain occupancy or local-only assumptions",
            "Expire leases, detect unhealthy servers and fail over/requeue without stranding players",
            "Sign and consume one-time transfer context containing UUID, reservation and intended action",
            "Provide player and staff selector/diagnostic commands, GUI, API/events and status placeholders",
            "Publish transfer latency, registry freshness, rejection and failover metrics with alert thresholds",
        ),
        performance="Redis-backed indexed registry; coalesced updates; bounded selector refresh and reservation latency budgets",
        security="Signed one-time transfer tokens; replay protection; server allowlist; least-privilege Redis credentials",
    ),
    addon(
        "reward-commands", "Free", "Reward Commands", "Validated, exactly-once command rewards bound to game outcomes.",
        "ZBW-PROG-011, ZBW-OPS-004, ZBW-INT-005", "M12", "zbw-progression, zbw-command-api, zbw-command-paper",
        (
            "Execute configured rewards for a valid game win",
            "Execute configured rewards for the killer on a valid final kill",
            "Execute configured rewards for the victim on a valid final death",
            "Bind additional reward definitions to allowlisted canonical domain events",
            "Execute an allowlisted command as console with safely escaped typed variables",
            "Execute an allowlisted command as the eligible player with explicit online requirements",
            "Evaluate permission, mode, arena, group, chance, streak and placement conditions",
            "Support bounded delay and ordered command batches that cancel safely on shutdown",
            "Guarantee idempotency across event replay, retry, reconnect and proxy failover",
            "Provide audited admin validate/simulate/reload controls plus reward execution API events",
        ),
        security="Deny arbitrary operator elevation; allowlist command roots/variables; redact secrets; audit every execution",
    ),
    addon(
        "popup-towers", "Free", "Pop-up Towers", "Purchasable, animated and reversible defensive structures.",
        "ZBW-SHOP-007, ZBW-GAME-002, ZBW-ARENA-003", "M11", "zbw-game, zbw-arena, zbw-shop",
        (
            "Provide a purchasable pop-up-tower item with configurable price, limits and shop placement",
            "Validate activation location, orientation, owner/team, build region and collision volume",
            "Construct the tower through a bounded animated block-placement sequence",
            "Load original configurable tower templates through the canonical schematic abstraction",
            "Recolour supported template blocks to the owner's team through version-safe mappings",
            "Prevent replacement of protected, player-placed or non-owned blocks according to policy",
            "Register placed blocks with arena ownership, protection and rollback services",
            "Enforce per-player/team cooldowns, concurrent tower caps and material limits",
            "Remove or persist the tower according to destruction and arena-reset rules without residue",
            "Provide admin preview/validate/editor operations plus placement/completion API events and metrics",
        ),
    ),
    addon(
        "generator-split", "Free", "Generator Split", "Fair, configurable sharing of generator drops among nearby teammates.",
        "ZBW-GAME-002, ZBW-SHOP-006", "M11", "zbw-game, zbw-shop",
        (
            "Detect eligible teammates within a configurable generator-sharing radius at spawn time",
            "Split or replicate generated resources according to an explicit configurable policy",
            "Distribute indivisible amounts deterministically and fairly across repeated drops",
            "Deliver shares directly or as owned drops while preserving inventory overflow",
            "Restrict sharing by team, alive state, distance, AFK policy and generator/resource type",
            "Prevent duplicated payouts during pickup, chunk unload, reconnect and generator reschedule races",
            "Configure behavior per arena, mode, group, resource and generator tier with live-safe reload",
            "Expose split recipients/amounts through feedback, API events, placeholders and performance metrics",
        ),
    ),
    addon(
        "discord-corebot", "Free", "Discord Stats / Corebot Addon", "A native adapter for Discord/Corebot-style stat lookup and leaderboard publishing.",
        "ZBW-STATS-007, ZBW-INT-002, ZBW-OPS-007", "M16", "zbw-integration-discord, zbw-statistics",
        (
            "Provide Discord player-stat lookup through the canonical statistics query service",
            "Provide Discord global, mode, arena and group leaderboard lookup",
            "Publish configurable statistic/leaderboard embeds to allowlisted channels",
            "Integrate through a vendor-neutral bot adapter rather than proprietary Corebot code",
            "Optionally link Discord and Minecraft identities through the shared verified link service",
            "Cache, paginate and rate-limit queries with visible freshness/error status",
            "Enforce consent, privacy, token secrecy, deletion and staff audit policy",
            "Provide admin adapter diagnostics/resync plus API events and metrics",
        ),
        config="integrations/discord-corebot.yml plus secret environment variables and vendor-neutral adapter selection",
        gui="Discord command/embed surfaces; in-game link/privacy GUI; operator diagnostics",
        commands="Configured Discord stats/leaderboard commands; /zbw discord adapter diagnose|resync",
        permissions="zartrabedwars.discord.use|link; zartrabedwars.admin.discord.*",
        security="No proprietary bot code; minimal bot scopes; verified links; redacted secrets; rate and privacy controls",
    ),
    addon(
        "golden-gg", "Free", "Golden GG", "Permission-aware post-game GG formatting and optional auto-message.",
        "ZBW-GAME-003, ZBW-UX-005", "M12", "zbw-game, zbw-ui-paper",
        (
            "Recognize an exact configurable GG phrase during the valid post-game window",
            "Render eligible GG text with configured gold styling and a plain fallback",
            "Require an explicit player permission for gold styling",
            "Optionally send one automatic GG after a completed eligible match",
            "Prevent duplicate, late, cross-arena and spam-triggered GG messages",
            "Localize phrases and configure window, format, channels and auto-message policy",
            "Provide admin reload/preview plus cancellable formatting and auto-send API events",
        ),
        source="https://www.spigotmc.org/threads/addon-golden-gg-for-bedwars.521196/",
    ),
    addon(
        "winstreak", "Free", "Winstreak", "Current/best streak tracking, rewards and public queries.",
        "ZBW-STATS-004, ZBW-STATS-006, ZBW-PROG-011", "M15", "zbw-statistics, zbw-progression",
        (
            "Increment a current winstreak exactly once after an eligible win",
            "Reset or preserve a current winstreak after losses, draws, invalid games and disconnects by policy",
            "Persist the best historical winstreak monotonically",
            "Track streak dimensions for global, mode, arena and group without conflating them",
            "Exclude or separately track private, test and administratively invalidated matches",
            "Trigger configured milestone announcements and exactly-once rewards",
            "Expose current/best streaks through profile GUI, commands and PlaceholderAPI",
            "Provide audited admin inspect/set/reset/recalculate/import operations",
            "Publish streak-change events and keep cross-server updates atomic and cache-coherent",
        ),
    ),
    addon(
        "cloudnet", "Free", "CloudNet Support", "CloudNet service discovery and elastic BedWars arena lifecycle support.",
        "ZBW-DEPLOY-003, ZBW-DEPLOY-005, ZBW-OPS-006", "M21", "zbw-cloudnet, zbw-proxy-api, zbw-game",
        (
            "Discover eligible CloudNet arena services and register their canonical metadata",
            "Publish arena mode, group, map, lifecycle, capacity and health as service properties",
            "Start arena services from configured CloudNet tasks/templates on capacity demand",
            "Maintain configurable warm capacity and autoscaling thresholds with hysteresis",
            "Drain a service before planned shutdown and reject new reservations during drain",
            "Replace unhealthy/crashed services and reconcile their expired reservations",
            "Support ephemeral private-game and replay-viewer service requests",
            "Coordinate service lifecycle without treating CloudNet callbacks as the Minecraft main thread",
            "Provide audited scale/drain/reconcile diagnostics through commands, API/events and metrics",
            "Secure CloudNet credentials/permissions and document standalone fallback behavior",
        ),
    ),
    addon(
        "swappage", "Free", "Swappage", "A mode that atomically swaps teams and their owned match state.",
        "ZBW-GAME-002, ZBW-GAME-003, ZBW-GAME-004",
        "M10 mode registration/selection; M11 gameplay mechanics; M15 statistics; M16 placeholders; M22 full compatibility",
        "zbw-game, zbw-arena, zbw-shop, zbw-statistics, zbw-compat-api",
        (
            "Enable Swappage per mode/arena with isolated statistics and explicit activation rules",
            "Select valid swap pairs or rotations deterministically for any supported team count",
            "Move living players to the corresponding new team and safe spawn atomically",
            "Transfer team-owned bed state, upgrades, traps, generator state and protected ownership consistently",
            "Apply swap triggers at configured time/event boundaries with countdown warnings",
            "Preserve pending swap state and correct team assignment across disconnect/rejoin",
            "Localize swap announcements, titles, sounds and per-player transition feedback",
            "Expose current/next swap, team mapping and lifecycle through API/events/placeholders",
            "Bound transition work and roll back/recover the whole swap if validation or mutation fails",
        ),
        source="https://www.spigotmc.org/resources/swappage-bedwars1058-addon.102551/",
    ),
    addon(
        "xp-bar", "Free", "XP Bar Addon", "Use the client XP bar as a reversible BedWars level/progress display.",
        "ZBW-PROG-002, ZBW-GAME-008, ZBW-PAPI-001", "M12", "zbw-progression, zbw-ui-paper, zbw-compat-api",
        (
            "Display the BedWars level as the client XP level in configured game/lobby states",
            "Display fractional progress toward the next BedWars level on the XP bar",
            "Update the display after authoritative XP/level changes and at a bounded fallback cadence",
            "Preserve and restore the player's real server XP state on every enter/exit/failure path",
            "Allow per-player display opt-out and state-specific operator configuration",
            "Use version adapters for XP packets with an API fallback and no cross-player leakage",
            "Expose displayed level/progress/next threshold through API events and placeholders",
        ),
    ),
    addon(
        "map-selector", "Free", "MapSelector Addon", "Join a selected eligible map/arena through a live, scalable selector.",
        "ZBW-GAME-007, ZBW-ARENA-006, ZBW-DEPLOY-002", "M20", "zbw-ui-paper, zbw-proxy-api, zbw-game",
        (
            "Open the map selector through a configured hotbar item, NPC, GUI link or command",
            "List eligible maps/arenas with mode, group, capacity, lifecycle and bounded-staleness status",
            "Filter maps by mode/group and paginate or search large catalogues",
            "Select a specific available arena through atomic reservation and transfer",
            "Offer an explicit fallback/queue choice when the selected arena is unavailable",
            "Respect permission, party, private, maintenance, cooldown and capacity eligibility",
            "Provide admin layout/filter configuration, preview, validation and diagnostics",
            "Expose map selection, availability and transfer outcome through API/events/placeholders",
        ),
    ),
    addon(
        "ratio-placeholders", "Free", "KDR, FKDR and WLR Addon", "Canonical derived-ratio calculations and PlaceholderAPI exposure.",
        "ZBW-STATS-003, ZBW-PAPI-001", "M15", "zbw-statistics, zbw-integration-placeholderapi",
        (
            "Calculate kill/death ratio from authoritative kills and deaths",
            "Calculate final-kill/final-death ratio from authoritative final eliminations",
            "Calculate win/loss ratio from authoritative completed-match outcomes",
            "Apply documented zero-denominator, precision, rounding, invalid-match and offline-player rules",
            "Register global, mode, arena, group and target-player ratio placeholders with stable names",
            "Cache derived reads, invalidate on stat changes and provide admin debug/conformance tests",
        ),
        gui="Statistics/profile GUIs consume ratios; no standalone GUI required",
        commands="/bw stats [player]; /zbw admin stats ratio-debug <player>",
        permissions="zartrabedwars.command.stats; zartrabedwars.admin.stats.debug",
        papi="zartra_kdr, zartra_fkdr, zartra_wlr plus mode/arena/group/target variants",
    ),
    addon(
        "reward-summary", "Free", "RewardSummary Addon", "End-of-match reward aggregation, delivery and explanation.",
        "ZBW-GAME-003, ZBW-PROG-011, ZBW-PROG-012", "M12", "zbw-progression, zbw-ui-paper",
        (
            "Aggregate every reward caused by one match under a stable reward-summary identifier",
            "Break down base, win, placement, kill, final-kill, bed, quest, challenge, pass and multiplier rewards",
            "Show earned currency, XP, items, commands and unlocks with before/after totals",
            "Present summaries through configurable chat and an optional paginated GUI",
            "Support title/action-bar/sound notification channels without replacing the detailed record",
            "Mark delivered, pending, failed and retried components without double granting",
            "Persist summaries for offline/cross-server delivery and configurable recent-history access",
            "Expose summary queries/delivery events/placeholders plus audited staff inspect/retry controls",
        ),
    ),
    addon(
        "holiday-reward", "Free", "HolidayReward Addon", "Calendar-windowed, idempotent seasonal reward claims.",
        "ZBW-PROG-011, ZBW-PROG-014, ZBW-ECO-001", "M12", "zbw-progression, zbw-ui-paper",
        (
            "Define holiday reward campaigns with stable IDs, timezone and start/end instants",
            "Define per-day, milestone and campaign-completion reward entries",
            "Evaluate permission, account age, play, region and prior-claim eligibility",
            "Present an accessible calendar/claim GUI with locked, available, claimed and missed states",
            "Claim an available reward explicitly and guarantee exactly-once delivery",
            "Optionally auto-grant eligible rewards under a visible configurable policy",
            "Apply per-account/device/network abuse controls without silently denying legitimate users",
            "Provide audited admin create/edit/preview/grant/revoke/reconcile commands",
            "Expose campaign/claim state and lifecycle through API/events/placeholders and migration-safe storage",
        ),
    ),
    addon(
        "layout-migrator", "Free", "Hypixel Migrator", "A lawful, dry-run-first importer for externally supplied BedWars layout data.",
        "ZBW-SHOP-001, ZBW-ECO-001, ZBW-OPS-004", "M23", "zbw-application, zbw-shop, zbw-config",
        (
            "Import only lawfully obtained user-supplied layout files through a documented neutral source adapter",
            "Parse and validate source categories, slots, prices, items, actions and metadata without executing content",
            "Map supported source concepts to native ZartraBedWars shop and hotbar schemas",
            "Produce a dry-run report of mapped, conflicting, lossy and unsupported records",
            "Resolve naming, material, version and slot conflicts through explicit operator policies",
            "Create a backup and transaction journal before applying any migration",
            "Roll back a failed or rejected migration without modifying the source export",
            "Provide audited CLI/command workflow, fixture tests, migration guide and provenance acknowledgement",
        ),
        gui="Optional migration review/diff GUI; headless dry-run report is mandatory",
        commands="/zbw admin migrate-layout validate|plan|apply|rollback|report",
        permissions="zartrabedwars.admin.migrate-layout.*",
        papi="— (migration tooling has no player runtime scalar)",
        security="Treat inputs as untrusted; path confinement, size/depth limits, no code execution; preserve provenance",
    ),
    addon(
        "play-again", "Free", "Play Again Addon", "Reliable post-match requeue and reservation flow.",
        "ZBW-GAME-003, ZBW-DEPLOY-002, ZBW-UX-004", "M20", "zbw-game, zbw-proxy-api, zbw-ui-paper",
        (
            "Offer play-again from the post-game and spectator hotbar/menu",
            "Requeue for the same mode using the canonical mode routing policy",
            "Request the same map/arena when eligible and available",
            "Offer next-available or explicit selector fallback when the preferred arena is unavailable",
            "Create one atomic queue/reservation request across shared-server and proxy deployments",
            "Keep a party together and require the configured leader/member confirmation policy",
            "Validate cooldown, ban, maintenance, private-game and match-completion eligibility",
            "Recover from transfer timeout, stale reservation, arena shutdown and duplicate clicks",
            "Expose request/status/result through GUI, command, API/events and placeholders",
        ),
    ),
    addon(
        "ultimate-mode", "Free", "Ultimate Mode", "A mode with seven original selectable class-like abilities.",
        "ZBW-GAME-004, ZBW-SHOP-003, ZBW-SHOP-007", "M11", "zbw-game, zbw-shop, zbw-ui-paper",
        (
            "Enable Ultimate as a selectable mode with per-arena eligibility and isolated statistics",
            "Select and change an ultimate through a waiting-state GUI and configured shop category",
            "Provide an original Kangaroo mobility/leap ultimate with cooldown and boundary checks",
            "Provide an original Swordsman dash/recall ultimate with collision and damage checks",
            "Provide an original Healer area-support ultimate with team-only targets and capped healing",
            "Provide an original Frozo control ultimate with bounded slow/freeze behavior",
            "Provide an original Builder rapid-construction ultimate using owned reversible blocks",
            "Provide an original Demolition explosive ultimate respecting bed/region protection",
            "Provide an original Gatherer resource-oriented ultimate with duplication-safe grants",
            "Track cooldown, charges, activation and cancellation server-authoritatively",
            "Allow mode-specific fall-damage behavior to be configured explicitly",
            "Persist selection/cooldowns across valid rejoin and clear all state on arena reset",
            "Provide admin ability configuration, validation, preview and balance diagnostics",
            "Expose selection, readiness, activation and result through API/events/placeholders",
            "Cap ability blocks/entities/particles and version-map every platform effect",
        ),
        source="https://www.spigotmc.org/resources/bedwars1058-ultimate-mode.133463/",
    ),
    addon(
        "voidless", "Free", "Voidless Addon", "Voidless arenas with automatic bed defense and explicit fall handling.",
        "ZBW-GAME-004, ZBW-ARENA-003, ZBW-SHOP-007", "M11", "zbw-game, zbw-arena",
        (
            "Enable Voidless as a selectable mode with compatible arena validation and isolated statistics",
            "Apply configurable voidless fall/low-boundary damage and recovery rules",
            "Create an automatic original defense around each configured team bed at game start",
            "Load defense shape, layers, materials and team recolouring from validated configuration",
            "Register defense blocks with team ownership, protection and break/drop rules",
            "Avoid collisions with arena structures and report invalid defense placements before start",
            "Roll back every automatic defense block during arena reset and crash recovery",
            "Provide admin preview/configuration plus defense/fall lifecycle API events and placeholders",
        ),
    ),
    addon(
        "admin-addon", "Free", "AdminAddon", "Audited administration and tightly controlled staff testing/troll tools.",
        "ZBW-GAME-009, ZBW-OPS-004, ZBW-OPS-005, ZBW-UX-002", "M18", "zbw-command-api, zbw-command-paper, zbw-game, zbw-ui-paper",
        (
            "Force-join an eligible player to an arena with explicit override scope",
            "Revive an eliminated player through a state-valid administrative recovery path",
            "Move a player to another team while reconciling spawn, inventory and team ownership",
            "Set, destroy or restore a team's bed state through guarded controls",
            "Advance or skip a canonical arena event with validation and warning",
            "Give/use a staff toy-stick interaction with an allowlisted effect set",
            "Place/remove a temporary player cage using owned reversible blocks",
            "Open a staff control GUI showing targets, arena state, confirmations and outcomes",
            "Require granular permissions, target immunity, reason, confirmation and optional dual control",
            "Audit actor, target, before/after state and outcome and provide rollback/recovery where meaningful",
            "Expose controlled operations through public staff API/events without raw state mutation",
        ),
        commands="/zbw admin game forcejoin|revive|setteam|bed|skipevent|tool|cage|rollback|inspect",
        permissions="zartrabedwars.admin.game.<operation>; zartrabedwars.staff.immune; no wildcard granted by default",
        papi="zartra_staff_session_* and arena diagnostics only; no private audit payload placeholders",
        security="Deny-by-default granular RBAC; target immunity; reasons/confirmations; tamper-evident audit; safe rollback",
    ),
    m08_addon(
        "leave-delay", "Free", "LeaveDelay Addon", "Cancelable countdown before a player leaves an active arena.",
        "ZBW-GAME-003, ZBW-GAME-008, ZBW-UX-005", "zbw-game, zbw-paper-modern",
        (
            "Start a configurable leave countdown instead of immediately leaving an eligible arena",
            "Show remaining delay through localized chat/title/action-bar/bossbar feedback",
            "Cancel the countdown on configured movement, damage, combat, death, state change or command",
            "Configure delays and cancellation policy per lobby/waiting/playing/spectator state",
            "Provide a granular bypass permission and audited staff immediate-leave operation",
            "Complete leave exactly once and restore inventory/location/session even across disconnect races",
            "Expose active delay, remaining time, cancellation and completion through API/events/placeholders",
        ),
    ),
    addon(
        "rush-mode", "Free", "RushMode", "A fast-paced mode with accelerated resources, defenses and bridge mechanics.",
        "ZBW-GAME-004, ZBW-SHOP-006, ZBW-SHOP-007", "M11", "zbw-game, zbw-shop, zbw-arena",
        (
            "Enable Rush as a selectable mode with compatible arenas and isolated statistics",
            "Start configured generators at accelerated/max tiers and timing",
            "Create an original automatic bed-defense template with team ownership and rollback",
            "Provide an original expanding-bridge item/action with direction and boundary validation",
            "Animate expanding bridges with per-tick block budgets and collision/protection rules",
            "Apply explicit Rush shop, pricing, event-time and respawn balance overrides",
            "Preserve mode state across valid rejoin and clear every temporary object on reset",
            "Provide admin configuration, preview, validation and balance diagnostics",
            "Expose mode, bridge, defense and timing state through API/events/placeholders",
        ),
    ),
    addon(
        "group-stats", "Free", "GroupStats", "Statistics, rankings and leaderboards partitioned by configured arena group.",
        "ZBW-STATS-002, ZBW-STATS-005, ZBW-STATS-006", "M15", "zbw-statistics, zbw-ui-paper",
        (
            "Persist each eligible match statistic under its immutable arena-group dimension",
            "Query a player's statistics for one group without mixing global or other-group totals",
            "Rank players and build leaderboards independently for each group",
            "Display group statistics through profile/leaderboard GUIs and commands",
            "Expose group statistic, rank and leaderboard PlaceholderAPI families",
            "Provide audited admin inspect/recalculate/import/merge controls with dry-run reports",
            "Keep group caches and cross-server updates atomic, bounded and invalidated by canonical events",
        ),
    ),
    addon(
        "per-group-stats", "Free", "Per Group Stats", "Dynamic PlaceholderAPI access to statistics for any configured group.",
        "ZBW-STATS-002, ZBW-PAPI-001", "M16", "zbw-statistics, zbw-integration-placeholderapi",
        (
            "Register a stable placeholder namespace accepting a validated group identifier",
            "Expose every canonical base statistic for the requested group",
            "Expose canonical KDR, FKDR, WLR and other derived values for the requested group",
            "Resolve current-player, explicit-target and offline-player group queries consistently",
            "Return documented unknown-group, absent-value and unavailable-storage fallbacks",
            "Cache/invalidate group placeholder reads and provide admin list/debug/conformance commands",
        ),
        gui="Consumed by any configured GUI; no standalone GUI required",
        commands="/zbw placeholders groups|test|debug",
        permissions="zartrabedwars.placeholders.use; zartrabedwars.admin.placeholders.debug",
        papi="zartra_group_<group>_<stat>, zartra_group_<group>_kdr|fkdr|wlr|rank plus target variants",
    ),
    addon(
        "per-arena-gen", "Free", "PerArenaGen", "Validated generator timing and tier behavior per arena.",
        "ZBW-ARENA-004, ZBW-SHOP-006, ZBW-OPS-003", "M11", "zbw-game, zbw-arena, zbw-config",
        (
            "Configure generator spawn intervals independently for each arena",
            "Configure intervals by resource type, generator type and upgrade tier",
            "Edit values through setup/admin commands and a validated configuration surface",
            "Validate nonzero bounds, supported resources, tier ordering and incompatible overrides",
            "Apply live-safe interval changes by rescheduling without duplicate or skipped ownership",
            "Expose effective intervals/tier/next spawn through API/events/placeholders and timing metrics",
        ),
    ),
    addon(
        "cosmetics-network", "Free", "Cosmetics inspired by advanced BedWars networks", "Original, network-grade cosmetic content using the canonical cosmetics platform.",
        "ZBW-PROG-006, ZBW-PROG-007, ZBW-PROG-008", "M14", "zbw-progression, zbw-ui-paper, zbw-compat-api",
        (
            "Ship an original curated cosmetic content pack without copying network names, art, sounds or layouts",
            "Register explicit kill, final-kill, bed-destroy, win, projectile, shopkeeper and island effect categories",
            "Register remaining PRD cosmetic categories through the same canonical category registry",
            "Browse, preview, equip and unequip content through the canonical cosmetics GUI",
            "Enforce ownership, rarity, permission, state and mutual-exclusion rules",
            "Support custom operator-created content through validated schema and safe action allowlists",
            "Render supported effects with packet isolation and version-safe server fallback",
            "Expose ownership/equipped/activation through API/events and PlaceholderAPI",
            "Apply per-tick/per-player effect, entity, block and packet budgets with cleanup",
            "Maintain a provenance manifest and automated check forbidding copied proprietary assets/branding",
        ),
        security="Safe action allowlist, ownership enforcement and content provenance; no proprietary code/assets/branding",
    ),
    addon(
        "item-rotation", "Free", "Item Rotation", "Scheduled shop rotations of original configurable items and abilities.",
        "ZBW-SHOP-003, ZBW-SHOP-007, ZBW-OPS-003", "M11", "zbw-shop, zbw-game, zbw-ui-paper",
        (
            "Provide a dedicated configurable rotation shop category",
            "Define rotation periods, timezone/clock source, start/end and deterministic schedule IDs",
            "Define eligible item pools, weights, slots and no-repeat/cooldown constraints",
            "Register original rotated items through the safe shop item/action API",
            "Validate price, currency, quantity, limits, state and inventory capacity on purchase",
            "Display current and upcoming rotation with countdown and localized item details",
            "Override pools/schedules by mode, arena, group and event under explicit precedence",
            "Provide audited admin CRUD/validate/simulate/force/rollback controls",
            "Persist active rotation and synchronize it atomically across proxy/server nodes",
            "Expose rotation, item, purchase and transition through API/events/placeholders and metrics",
        ),
        source="https://www.spigotmc.org/resources/item-rotation-bw1058.107016/",
    ),
    addon(
        "color-changer", "Free", "Color Changer Addon", "Team-colour normalization for supported blocks/items in game contexts.",
        "ZBW-GAME-002, ZBW-SHOP-007, ZBW-ARC-004", "M11", "zbw-game, zbw-compat-api",
        (
            "Convert supported player-placed colourable blocks to the placing player's team colour",
            "Convert supported colourable inventory items to the owning player's team colour",
            "Convert supported colourable items stored in arena-owned chests to the owning team colour",
            "Map legacy data values and modern materials through a version-neutral colour capability registry",
            "Disable or override conversion per arena, mode, material and item source",
            "Exclude protected, custom-model, signed, cosmetic and explicitly denylisted items",
            "Preserve amount, name, lore, enchantments, tags and canonical item identity during conversion",
            "Provide localized feedback plus admin reload/inspect/configuration and conversion API events",
            "Expose team colour/mapping state through placeholders and batch conversions within tick budgets",
        ),
        source="https://www.spigotmc.org/resources/bedwars1058-color-changer-addon.104501/",
    ),
    m08_addon(
        "tab-sorter", "Free", "TabSorter", "Arena-aware tab ordering, grouping and templated header/footer.",
        "ZBW-GAME-002, ZBW-UX-005, ZBW-PAPI-001", "zbw-game, zbw-paper-modern, zbw-compat-api, zbw-integration-placeholderapi",
        (
            "Sort playing players by configurable team order",
            "Sort players deterministically within a team by configured rank/name policy",
            "Place spectators, staff and lobby players in explicit configurable sections",
            "Render team, status, prefix and allowed statistic fields in tab entries",
            "Render configurable localized tab header",
            "Render configurable localized tab footer",
            "Expand PlaceholderAPI values with timeouts/fallbacks and no main-thread remote query",
            "Refresh only changed entries at a bounded cadence and respect client packet limits",
            "Restore/clear tab state after game exit and interoperate through an explicit scoreboard-owner policy",
            "Provide admin reload/preview/diagnostics and tab-render API events",
        ),
        source="https://www.spigotmc.org/resources/bedwars1058-tabsorter.100842/",
    ),
    addon(
        "arena-setup-addon", "Free", "ArenaSetup", "Guided, validated in-game arena construction and map-management tools.",
        "ZBW-ARENA-002, ZBW-ARENA-004, ZBW-ARENA-007, ZBW-UX-002",
        "M07 core; M09 command/GUI presentation",
        "zbw-arena, zbw-ui-api, zbw-ui-paper, zbw-command-api, zbw-command-paper",
        (
            "Enter and exit an isolated arena setup session through an admin command",
            "Provide configurable setup hotbar items with action, slot and localized metadata",
            "Set and validate the waiting-lobby spawn",
            "Create/edit teams and set each team spawn",
            "Set and validate each team's bed location and facing",
            "Set and validate each team's base/resource generator",
            "Create, orient and remove team shop NPC locations",
            "Create, orient and remove team-upgrade NPC locations",
            "Create and validate diamond generator locations",
            "Create and validate emerald generator locations",
            "Auto-discover eligible diamond/emerald generator markers with confirmation before import",
            "Configure arena mode, group, team count, team size, bounds and world adapter",
            "Provide setup GUIs for team, generator, NPC and arena-property editing",
            "Validate completeness, collisions, unsafe spawns, missing regions and mode-specific prerequisites",
            "Preview, undo, redo, confirm and atomically save setup changes with last-known-good rollback",
            "Expose setup sessions/validation through granular commands, permissions and API events",
        ),
        source="https://www.spigotmc.org/resources/addon-bedwars1058-arenasetup-1-8-1-21.97709/",
        config="M07 core: addons/arena-setup-addon.yml with validated defaults and per-mode/arena/group overrides",
        gui="M09 presentation: unified setup wizard/editor/preview/validation/confirmation pages invoking M07 use cases",
        commands="M09 presentation: /zbw setup <arena>; /zbw setup waiting|team|spawn|bed|generator|shop|upgrade|validate|save|undo|exit",
        permissions="M07 enforces zartrabedwars.admin.setup.<operation> with world/arena scope; M09 revalidates the same nodes at adapters",
        api="M07 core: ArenaSetup service/query API with cancellable pre-events and immutable post-events",
        papi="M16: zartra_setup_arena, zartra_setup_step, zartra_setup_errors for authorized staff only",
        tests="M07 unit, typed-harness lifecycle, validation, persistence, rollback and permission tests; M09 command/GUI/editor/confirmation tests; M22 cross-version tests",
        docs="M07 application/configuration/API documentation; M09 command/permission/GUI reference; M16 placeholder and M22 compatibility reference",
    ),
    m08_addon(
        "bossbar", "Free", "BossBar", "State-aware, localized BedWars boss bars with version fallbacks.",
        "ZBW-GAME-006, ZBW-UX-005, ZBW-ARC-004", "zbw-game, zbw-paper-modern, zbw-compat-api",
        (
            "Display a waiting/countdown boss bar with state and player/capacity progress",
            "Display a playing boss bar with next event, bed/team or mode-specific progress",
            "Display a post-game boss bar with winner and return/requeue countdown",
            "Transition bars atomically on state changes without stale viewers or duplicate bars",
            "Configure template, colour, style, progress source and cadence per mode/arena/group",
            "Respect per-player visibility preferences, locale and vanished/private information rules",
            "Use native modern boss bars and a documented legacy-version compatibility fallback",
            "Provide admin reload/preview/diagnostics plus bossbar render API/events/placeholders",
        ),
    ),
    m08_addon(
        "adventure-mode", "Free", "AdventureMode", "Correct player game-mode transitions around waiting and active matches.",
        "ZBW-GAME-003, ZBW-GAME-006, ZBW-GAME-009", "zbw-game, zbw-paper-modern",
        (
            "Place players in adventure mode when joining an eligible waiting arena",
            "Place active team players in survival mode when gameplay starts",
            "Place eliminated viewers in the configured spectator-compatible game mode",
            "Restore the pre-arena or configured lobby game mode on every exit/failure path",
            "Configure behavior per arena/mode/state without granting creative privileges",
            "Recover correct mode on reconnect and expose transition API events/state placeholders",
        ),
        source="https://polymart.org/resource/3296/",
    ),
    addon(
        "bed-steal", "Free", "BedSteal", "A native BedWars/lifesteal hybrid mode with bed upgrades and original combat items.",
        "ZBW-GAME-004, ZBW-SHOP-003, ZBW-SHOP-007", "M11", "zbw-game, zbw-shop, zbw-arena",
        (
            "Enable BedSteal as a selectable mode with compatible arenas and isolated statistics",
            "Grant one signed bed-upgrade token for an eligible enemy-bed destruction",
            "Consume a bed-upgrade token by interacting with the player's own active team bed",
            "Increase the team bed level and the eligible player's maximum hearts by configured increments",
            "Protect the upgraded bed within a configurable radius while its level has remaining protection",
            "Decrease/resolve bed protection levels under explicit enemy-break rules",
            "Grant redstone currency from eligible bed-destruction outcomes",
            "Spawn original special middle-area mobs that can drop redstone currency",
            "Configure mob type, attributes, spawn areas, cadence, caps, drops and cleanup",
            "Provide a BedSteal-specific item-rotation shop category using redstone currency",
            "Provide an original poison-inflicting sword item with bounded effect/cooldown",
            "Provide an original shuriken-like projectile item with server-authoritative hit validation",
            "Provide audited admin token/currency/item give, mob spawn, reset and diagnostics controls",
            "Persist/reconcile bed levels, health, currency and cooldowns across valid rejoin",
            "Expose BedSteal state through API/events/placeholders and cap mobs/projectiles/effects per arena",
        ),
        source="https://voxel.shop/resource/3575/",
    ),
    addon(
        "discord-utils", "Free", "DiscordUtils", "Configurable Discord statistic profiles backed by PlaceholderAPI and verified linking.",
        "ZBW-INT-002, ZBW-STATS-007, ZBW-PAPI-001", "M16", "zbw-integration-discord, zbw-integration-placeholderapi",
        (
            "Define and render up to at least twenty-five statistic fields per Discord profile",
            "Use PlaceholderAPI values in profile titles, field names and field values with safe fallbacks",
            "Link Discord users to Minecraft UUIDs through the DiscordSRV-compatible verified link abstraction",
            "Strip or translate Minecraft colour/style codes for Discord-safe output",
            "Add a profile through a permission-aware Discord slash command",
            "Edit an owned/authorized profile through a Discord slash command",
            "Remove an owned/authorized profile through a Discord slash command",
            "Apply administrator-defined profile presets through a Discord slash command",
            "Refresh configured profile messages without creating uncontrolled duplicates",
            "Protect bot tokens, identity links and private fields with rate limits, consent and deletion support",
            "Provide in-game/operator configuration, diagnostics, API events and delivery metrics",
        ),
        source="https://modrinth.com/plugin/discordutils",
        config="integrations/discord-utils.yml plus secret environment variables; schema for profiles, fields and presets",
        gui="Discord profile embeds/slash commands; in-game link/privacy GUI; operator diagnostics",
        commands="Discord /profile add|edit|remove|preset; /zbw discordutils link|privacy|diagnose|resync",
        permissions="zartrabedwars.discordutils.profile.*; zartrabedwars.admin.discordutils.*",
        papi="Consumes allowlisted PlaceholderAPI categories and exposes link/profile publication status",
        security="Least-privilege bot scopes; verified links; field allowlists; secret redaction; rate and privacy controls",
    ),
)


# Append-only amendments preserve every pre-existing stable ID. Do not move these
# features into an addon's base tuple: doing so would renumber later requirements.
APPENDED_FEATURES: tuple[tuple[str, str], ...] = (
    ("private-games", "Register RESOURCE SCARCITY as the original eleventh built-in Private Games modifier"),
    ("private-games", "Configure and validate the RESOURCE SCARCITY iron generation multiplier independently"),
    ("private-games", "Configure and validate the RESOURCE SCARCITY gold generation multiplier independently"),
    ("private-games", "Configure and validate the RESOURCE SCARCITY diamond generation multiplier independently"),
    ("private-games", "Configure and validate the RESOURCE SCARCITY emerald generation multiplier independently"),
    ("private-games", "Configure RESOURCE SCARCITY multipliers independently for every registered custom resource ID"),
    ("private-games", "Provide original Scarce, Reduced, Normal, Abundant and Extreme RESOURCE SCARCITY presets with versioned values"),
    ("private-games", "Let authorized hosts select a preset or edit each multiplier through a validated GUI with preview, reset and lock feedback"),
    ("private-games", "Expose RESOURCE SCARCITY configuration, commands, granular permissions, API events and PlaceholderAPI state"),
    ("private-games", "Apply RESOURCE SCARCITY deterministically to native and custom generators without duplicate scheduling, item loss or unsafe mid-game mutation"),
)


def esc(value: str) -> str:
    return value.replace("|", "\\|").replace("\n", " ")


def identifiers() -> list[tuple[str, Addon, str]]:
    rows: list[tuple[str, Addon, str]] = []
    for entry in ADDONS:
        for feature in entry.features:
            rows.append((f"ZBW-ADDON-{len(rows) + 1:03d}", entry, feature))
    if len(rows) != 463:
        raise ValueError(f"Stable base must remain exactly ZBW-ADDON-001..463, found {len(rows)} rows")
    addons_by_key = {entry.key: entry for entry in ADDONS}
    for key, feature in APPENDED_FEATURES:
        if key not in addons_by_key:
            raise ValueError(f"Unknown append-only addon key: {key}")
        rows.append((f"ZBW-ADDON-{len(rows) + 1:03d}", addons_by_key[key], feature))
    return rows


def compact_id_spans(rows: list[tuple[str, Addon, str]]) -> str:
    numbers = [int(requirement_id.rsplit("-", 1)[1]) for requirement_id, _, _ in rows]
    groups: list[tuple[int, int]] = []
    start = previous = numbers[0]
    for number in numbers[1:]:
        if number == previous + 1:
            previous = number
            continue
        groups.append((start, previous))
        start = previous = number
    groups.append((start, previous))
    return "; ".join(
        f"ZBW-ADDON-{start:03d}" if start == end else f"ZBW-ADDON-{start:03d}–ZBW-ADDON-{end:03d}"
        for start, end in groups
    )


def atomic_milestone(requirement_id: str, entry: Addon) -> str:
    """Return the reconciled ownership cell without changing a stable addon requirement."""
    if entry.milestone != "M11":
        return entry.milestone
    if requirement_id in M11_STATISTICS_IDS:
        return M11_STATISTICS_MILESTONE
    if requirement_id == "ZBW-ADDON-387":
        return M11_ROTATION_MILESTONE
    return M11_STANDARD_MILESTONE


def inventory_milestone(rows: list[tuple[str, Addon, str]], entry: Addon) -> str:
    """Summarize all explicit owners represented by an addon's atomic rows."""
    if entry.milestone != "M11":
        return entry.milestone
    identifiers_for_addon = {requirement_id for requirement_id, _, _ in rows}
    if "ZBW-ADDON-387" in identifiers_for_addon:
        return M11_ROTATION_MILESTONE
    if identifiers_for_addon.intersection(M11_STATISTICS_IDS):
        return M11_STATISTICS_MILESTONE
    return M11_STANDARD_MILESTONE


def validate() -> None:
    if len(ADDONS) != 49:
        raise ValueError(f"Expected 49 addons, found {len(ADDONS)}")
    tiers = {"Premium": 0, "Free": 0}
    keys: set[str] = set()
    names: set[str] = set()
    import re

    core_ids = set(re.findall(r"^\| (ZBW-[A-Z]+-\d{3}) \|", PRD.read_text(encoding="utf-8-sig"), re.MULTILINE))
    if len(core_ids) != 199:
        raise ValueError(f"Expected 199 Part I PRD IDs for overlap validation, found {len(core_ids)}")
    architecture_modules = {
        "zbw-application", "zbw-arena", "zbw-bungeecord", "zbw-cloudnet",
        "zbw-command-api", "zbw-command-paper", "zbw-compat-api", "zbw-config",
        "zbw-game", "zbw-integration-discord", "zbw-integration-placeholderapi",
        "zbw-paper-modern", "zbw-progression", "zbw-proxy-api", "zbw-redis", "zbw-shop",
        "zbw-statistics", "zbw-ui-api", "zbw-ui-paper", "zbw-velocity",
    }
    for entry in ADDONS:
        tiers[entry.tier] += 1
        if entry.key in keys or entry.name in names:
            raise ValueError(f"Duplicate addon key/name: {entry.key} / {entry.name}")
        keys.add(entry.key)
        names.add(entry.name)
        overlap_ids = {value.strip() for value in entry.overlaps.split(",")}
        unknown_overlaps = overlap_ids - core_ids
        if unknown_overlaps:
            raise ValueError(f"Unknown core overlap IDs for {entry.name}: {sorted(unknown_overlaps)}")
        unknown_modules = {value.strip() for value in entry.module.split(",")} - architecture_modules
        if unknown_modules:
            raise ValueError(f"Unknown architecture modules for {entry.name}: {sorted(unknown_modules)}")
        if not entry.features or len(set(entry.features)) != len(entry.features):
            raise ValueError(f"Missing or duplicate atomic features for {entry.name}")
        for field in (
            entry.purpose, entry.overlaps, entry.milestone, entry.module, entry.config, entry.gui,
            entry.commands, entry.permissions, entry.api, entry.papi, entry.performance, entry.security,
            entry.tests, entry.docs, entry.source,
        ):
            if not field.strip():
                raise ValueError(f"Incomplete mapping surface for {entry.name}")
    if tiers != {"Premium": 8, "Free": 41}:
        raise ValueError(f"Expected 8 premium and 41 free addons, found {tiers}")
    ids = [row[0] for row in identifiers()]
    if len(ids) != len(set(ids)):
        raise ValueError("Duplicate requirement IDs")
    if ids != [f"ZBW-ADDON-{index:03d}" for index in range(1, 474)]:
        raise ValueError("Addon IDs must be the append-only range ZBW-ADDON-001..473")


def render() -> str:
    validate()
    rows = identifiers()
    by_key: dict[str, list[tuple[str, Addon, str]]] = {}
    for row in rows:
        by_key.setdefault(row[1].key, []).append(row)

    lines: list[str] = [
        "# Native Addon Feature Catalogue",
        "",
        "**Status:** normative owner-supplied scope supplement<br>",
        "**Baseline date:** 2026-07-14<br>",
        f"**Inventory:** {len(ADDONS)} addons (8 premium references, 41 free references)<br>",
        f"**Atomic addon requirements:** {len(rows)} (`{rows[0][0]}` through `{rows[-1][0]}`)<br>",
        "**Functional coverage:** 100% — every row below is `COVERED` by a stable requirement and complete planning mapping; this is specification coverage, not implementation status.",
        "",
        "## 1. Normative interpretation and originality rules",
        "",
        "The project owner supplied the addon names and functional scope after the original Master Prompt audit. This catalogue is therefore a normative supplement to `MASTER_PROMPT.md`, the PRD and the traceability matrix. The premium/free labels describe the source catalogue only; they do not import a third party's commercial terms into ZartraBedWars.",
        "",
        "Each `ZBW-ADDON-*` row is an atomic, independently verifiable `MUST` requirement. Addon headings are organisational labels, never substitute requirements. An overlap means the native feature also satisfies or extends an existing core requirement; it does not merge away this explicit addon requirement.",
        "",
        "Only clean-room, original equivalents may be implemented. Do not copy proprietary source, bytecode, configuration text, messages, artwork, models, sounds, maps, balance tables, GUI layouts, trademarks or branding. Public pages are used only to identify functional behaviour. Every shipped asset/content entry requires an original or lawfully licensed provenance record. Ambiguity is resolved by preserving the user-facing capability through the neutral contracts below, not by cloning undocumented implementation details.",
        "",
        "A mapping cell containing `—` is permitted only when it includes a reason. Configuration keys, command names, permission nodes, API type names and placeholders shown here are canonical planned interfaces; ADR review may refine spelling only with migration aliases and traceability updates.",
        "",
        "## 2. Universal acceptance and verification contract",
        "",
        "Every atomic row is accepted only when its stated behaviour works in shared-server and applicable proxy deployment, observes the module/threading contracts in `docs/ARCHITECTURE.md`, supports Minecraft 1.8 through 1.21.x through compatibility providers, rejects invalid/unauthorised state without partial mutation, cleans up owned runtime objects, emits the mapped API events, and is documented. Tests listed in a row are minimum suites and include positive, boundary, permission-denied, stale-state, reconnect/reset, and relevant cross-version cases.",
        "",
        "All configuration mutations require schema validation, last-known-good rollback and documented precedence. All GUI actions must revalidate on click. All commands require localized feedback and permission-aware help/completion. All privileged operations require granular permissions and audit. Placeholder reads must be side-effect free, null-safe and non-blocking. No network, database, Redis, Discord or filesystem wait may block the Minecraft main thread.",
        "",
        "For the M11 ranges, the milestone cell is an explicit ownership split rather than a claim that M11 implements every surface in the row. M11 owns atomic mechanics, configuration, feature-specific reuse of M09 command/GUI infrastructure, API/events and primary Paper 1.21.1 behavior. M15 owns every explicitly named statistics projection, M16 owns every PlaceholderAPI cell, M19/M20 own the Redis/proxy synchronization portion of ZBW-ADDON-387, M21 owns concrete Vault/NPC/hologram providers where a row consumes them, and M22 owns legacy fallbacks and full compatibility. The Swappage rows preserve M10 registration/selection, M11 mechanics, M15 statistics, M16 placeholders and M22 compatibility. A row remains one stable atomic requirement and reaches final completion only after all listed milestone portions pass.",
        "",
        "## 3. Inventory summary",
        "",
        "| # | Tier | Addon reference | Functional purpose | Atomic IDs | Count | Existing overlap | Milestone | Native module(s) | Coverage |",
        "|---:|---|---|---|---|---:|---|---|---|---|",
    ]
    for index, entry in enumerate(ADDONS, 1):
        addon_rows = by_key[entry.key]
        span = compact_id_spans(addon_rows)
        lines.append(
            f"| {index} | {entry.tier} | {esc(entry.name)} | {esc(entry.purpose)} | {span} | {len(addon_rows)} | {esc(entry.overlaps)} | {inventory_milestone(addon_rows, entry)} | {esc(entry.module)} | COVERED |"
        )

    lines += [
        "",
        "## 4. Atomic feature mappings",
        "",
        "The `Trace entry` column is the canonical Part III traceability-matrix row. `PRD` always includes PRD §4.17 and §8.9 plus the listed core overlaps. The per-addon legal note inherits the global originality rule above.",
        "",
    ]
    for index, entry in enumerate(ADDONS, 1):
        addon_rows = by_key[entry.key]
        lines += [
            f"### 4.{index} {entry.name} ({entry.tier})",
            "",
            f"**Purpose:** {entry.purpose}<br>",
            f"**Functional reference:** [{entry.source}]({entry.source})<br>",
            f"**Originality/licensing:** original native equivalent only; no third-party source, assets, protected branding, copied text/layout or proprietary balance data.<br>",
            f"**Existing overlap retained:** {entry.overlaps}.",
            "",
            "| Requirement ID | Priority | Atomic implementable capability | PRD | Trace entry | Milestone | Module | Configuration | GUI | Commands | Permissions | API/events | PlaceholderAPI | Performance | Security | Tests | Documentation | Status |",
            "|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|",
        ]
        for req_id, _, feature in addon_rows:
            values = (
                req_id,
                "MUST",
                feature,
                f"§4.17, §8.9; overlaps {entry.overlaps}",
                f"Part III / {req_id}; Part I / {entry.overlaps}",
                atomic_milestone(req_id, entry),
                entry.module,
                entry.config,
                entry.gui,
                entry.commands,
                entry.permissions,
                entry.api,
                entry.papi,
                entry.performance,
                entry.security,
                entry.tests,
                entry.docs,
                "COVERED",
            )
            lines.append("| " + " | ".join(esc(value) for value in values) + " |")
        lines.append("")

    lines += [
        "## 5. Coverage and conformance controls",
        "",
        f"- Addon references catalogued: **{len(ADDONS)}/{len(ADDONS)}**.",
        "- Premium references catalogued: **8/8**.",
        "- Free references catalogued: **41/41**.",
        f"- Atomic addon features mapped: **{len(rows)}/{len(rows)}**.",
        "- Atomic features with `PARTIALLY COVERED` status: **0**.",
        "- Atomic features with `MISSING` status: **0**.",
        "- Addon functional coverage: **100%**.",
        "",
        "`python tools/coverage/generate_addon_feature_catalog.py --check` is the required drift check. It validates inventory cardinality, tier split, unique append-only ID allocation, nonempty atomic feature lists, complete mapping surfaces and byte-for-byte generated output. `python tools/coverage/generate_master_prompt_coverage.py --check` validates the combined Master Prompt plus owner-supplied-addon baseline. `python tools/coverage/validate_preimplementation_decisions.py --check` validates the RC-072..076 IDs, ADRs, required catalogues, published totals and no-Java gate.",
        "",
        "### M09 verified presentation overlay",
        "",
        "The 77 catalogue rows in `ZBW-ADDON-001..009`, `108..114`, `124..130`, `148..154`, `334..340`, `398..407` and `408..437` retain every atomic behavior and mapping above. Their M09 command, GUI, editor and confirmation cells are implemented by the 87-action shared catalogue, machine-readable inventories and exact Paper evidence in `build/evidence/m09-paper-primary.json`. M07/M08 remain the only owners of feature rules. PlaceholderAPI, distributed, provider, legacy compatibility and other explicitly later cells remain open in their assigned milestones; this overlay does not merge or weaken a catalogue row.",
        "",
        "## 6. Resolved policy decisions and remaining execution gates",
        "",
        "1. **Private Games modifier:** RC-072 is resolved by the original RESOURCE SCARCITY requirements `ZBW-ADDON-464..473`, including independent native/custom resource multipliers and five presets.",
        "2. **Original content:** RC-073 is resolved by `ZBW-CONTENT-001..011`, `docs/ORIGINAL_STARTER_CATALOG.md` and `docs/ASSET_PROVENANCE.md`; production content still requires provenance approval before packaging.",
        "3. **Discord topology:** RC-074 is resolved by the provider architecture in `docs/DISCORD_ARCHITECTURE.md`; Discord remains optional and failure-isolated.",
        "4. **Legacy fallbacks:** RC-075 is resolved by `docs/COMPATIBILITY_FALLBACKS.md`; every implemented fallback still requires its compatibility fixture before the affected milestone exits.",
        "5. **Trademark/runtime naming:** addon and network names remain requirements-source references. Original runtime names and migration aliases require legal/product review without reducing behavior.",
        "6. **Dependency selection:** RC-076's default-deny licensing policy is resolved. Exact versions remain blocked under RC-021/024/027 until their audit rows are approved; no unselected candidate may enter a build.",
        "",
        "## 7. Source and provenance notes",
        "",
        "The owner-supplied list is authoritative for inclusion. The BedWars1058 wiki is used for the premium/free split and public purpose summaries. Linked public product/resource pages provide supplementary behavior descriptions where available. They are references, not implementation dependencies and not authorization to copy. The repository must retain a release-time dependency/content SBOM and provenance manifest.",
        "",
    ]
    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="fail if the generated catalogue is stale")
    args = parser.parse_args()
    content = render()
    if args.check:
        if not OUTPUT.exists() or OUTPUT.read_text(encoding="utf-8") != content:
            print(f"STALE: {OUTPUT.relative_to(ROOT)}")
            return 1
        print(f"OK: 49 addons, {len(identifiers())} atomic requirements, 100% addon coverage")
        return 0
    OUTPUT.write_text(content, encoding="utf-8")
    print(f"WROTE: {OUTPUT.relative_to(ROOT)} ({len(identifiers())} atomic requirements)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
