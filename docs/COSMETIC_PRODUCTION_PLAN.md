# Cosmetic Production Plan

**Status:** Accepted
**Decision:** RC-017
**Requirements:** `ZBW-READY-003`, `ZBW-PROG-006..008`, `ZBW-CONTENT-001/007/009/011`

## Counting and quality rules

A built-in cosmetic counts only when it has a unique stable semantic ID; functional definition; original/licensed name, messages, sound/effect/model/texture references; category and rarity; preview; unlock/permission; compatibility fallback; packet/entity/tick budget; provenance; localization keys; and passing schema/golden/visual/cleanup tests. Recolouring one asset or changing only display metadata does not create another count unless the visual/effect composition and player-facing identity are materially distinct and pass review.

Every asset is created in-house or approved under `ASSET_PROVENANCE.md`. Hypixel, BedWars1058 and addon-author assets, exact names/messages/layouts/presets or extracted network content are prohibited. AI-assisted content, if used, records tool, model/version, prompt ownership, human author/editor, source inputs and commercial terms; unverified training/output rights block release.

## Five-batch catalogue

| Batch | Count | Required allocation | Exit gate |
|---|---:|---|---|
| C1 — Combat identity | 60 | 10 kill, 10 final-kill, 8 death-cry, 8 projectile/arrow, 8 footstep, 8 TNT/fireball, 8 message cosmetics | All combat events, visibility opt-out, cleanup and 1.8 fallbacks pass |
| C2 — Bed, island and match | 60 | 10 bed-destroy, 8 bed/wood skins, 8 island toppers, 8 sprays/glyphs, 8 generator/shop-purchase, 8 bridge/team-introduction, 10 spawn/respawn effects | No map collision, no gameplay hitbox change, all semantic fallback tests pass |
| C3 — Victory and spectacle | 60 | 12 victory dances, 8 dragon, 8 spectator, 8 elimination/victory message, 8 level-up, 8 cages, 8 emotes | Packet/entity limits, spectator isolation, cancel/reset cleanup and reduced-effects mode pass |
| C4 — Lobby and profile | 60 | 10 lobby gadgets, 8 waiting-lobby, 8 shopkeeper skins, 8 profile frames, 8 titles, 6 badges, 6 nameplate/chat, 6 scoreboard/tablist | Lobby-safe interaction, permission/privacy and Bedrock/legacy input alternatives pass |
| C5 — Seasonal and custom-system proof | 60 | 12 seasonal, 8 event, 8 exclusive reward, 8 custom-category examples, 8 multifunction compositions, 8 packet-only renderings, 8 accessibility/reduced-motion variants | Content-pack extension, custom rarity/category, provenance, no-copy and full-load gates pass |

Total: **300**. Custom operator definitions are additional and never used to satisfy the built-in count.

## Metadata and lifecycle

Each definition contains `id`, schema/content version, category, rarity, localized keys, icon and preview scene, renderer/effect graph, allowed contexts, duration/cooldown, view radius, maximum instances/packets, fallback semantic IDs, unlock sources, price/entitlement rules, permission, conflicts, reduced-effects behavior, asset IDs and provenance state. IDs never encode third-party brand names.

The registry supports configuration/API replacement and addition. An override must declare the replaced ID, migration, provenance and compatibility; deleting an owned cosmetic maps to a documented neutral fallback and preserves entitlement audit. Rarity/category registries accept custom IDs with explicit ordering/style/unlock policies.

## Performance budgets

- Per viewer: ≤20 cosmetic packets/tick sustained, burst 60; ≤32 visible effect instances; visibility radius default 32 blocks.
- Per arena: ≤200 cosmetic packets/tick sustained; gameplay packets/effects always take priority.
- Each effect callback: p99 ≤100 μs CPU and ≤2 KiB allocation on the owner thread; heavier preparation is async and immutable.
- Packet-only entities have deterministic destroy on world change, death, spectator change, disable and disconnect; zero leaked trackers after 100 lifecycle cycles.
- Reduced-effects mode caps particles at 25%, suppresses purely decorative audio/animation only, retains gameplay cues and uses non-color-only feedback.

## Production workflow and acceptance

Concept brief → originality search → provenance reservation → implementation definition/assets → peer content review → compatibility render (1.8.8/1.12.2/1.16.5/1.21.1/1.21.11 and Bedrock) → accessibility/reduced-effects review → performance/cleanup test → localization → final provenance approval. No batch starts final polish until the previous batch meets its gate; module foundations may be built in parallel.

Automated validators enforce 300 unique IDs, exact batch totals, no missing fields/assets/locales/fallbacks/tests, no duplicate asset hash masquerading as distinct content, permission/unlock reachability and provenance approval. Human review rejects derivative branding, low-quality clones, unreadable effects and gameplay-obscuring visuals. Release evidence lists all 300 items and their test/provenance records.
