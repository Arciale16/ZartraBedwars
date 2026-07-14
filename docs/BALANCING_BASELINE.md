# Original Balancing Baseline

**Status:** Accepted starter profile `zbw:standard-v1`
**Decision:** RC-059
**Requirement:** `ZBW-READY-015`

All names and numeric presets are original project defaults and configurable. They are not claimed to reproduce another network. Stable semantic IDs let APIs/content packs replace or extend values without copying protected content.

## Core rules and resources

| Rule | Standard | Solo/Doubles | Teams 3/4 | Rush | Ultimate | Voidless/Adventure |
|---|---:|---:|---:|---:|---:|---:|
| Pre-game countdown | 20 s | 20 s | 25 s | 10 s | 20 s | 20 s |
| Respawn delay while bed alive | 5 s | 5 s | 6 s | 3 s | 5 s | 5 s |
| Post-bed final elimination | immediate on death | same | same | same | same | same |
| Match soft limit | 35 min | 32 min | 40 min | 20 min | 35 min | 40 min |
| Sudden-death start | 30 min | 27 min | 35 min | 17 min | 30 min | 35 min |
| Build height above island reference | 32 blocks | 32 | 36 | 40 | 36 | 28 |
| Rejoin reservation | 180 s | 180 | 240 | 90 | 180 | 240 |

Native base-generator intervals per resource item are iron 1.00 s, gold 4.00 s, diamond 30.00 s and emerald 55.00 s. Tier upgrades multiply interval by `0.80`, `0.62`, then `0.48`; minimum interval is 0.20 s. Generator Split divides one due yield fairly among eligible teammates within 1.75 blocks using rotating remainder, never duplicates total yield.

Resource Scarcity multiplies generation **rate** (not interval), matching the accepted original starter catalogue: Scarce `0.50`, Reduced `0.75`, Normal `1.00`, Abundant `1.50`, Extreme `2.50`. Custom resources default to Normal and may define their own validated 0.05–10.00 multiplier. Composition with tier rate is multiplicative and capped by the minimum interval.

## Starter shop

| Semantic item | Price | Limits/cooldown | Purpose |
|---|---:|---|---|
| 16 wool blocks | 4 iron | stack cap 64 | Basic bridging |
| 12 hardened clay blocks | 12 iron | stack cap 48 | Early defense |
| 4 blast-resistant glass | 12 iron | stack cap 32 | Explosive counter |
| 16 end-stone blocks | 28 iron | stack cap 64 | Durable defense |
| 4 timber blocks | 4 gold | stack cap 32 | Flexible defense |
| Stone sword | 12 iron | one active | Early combat |
| Iron sword | 8 gold | one active | Mid combat |
| Diamond sword | 5 emerald | one active | Late combat |
| Permanent chain armor | 36 iron | once/player | Early armor |
| Permanent iron armor | 10 gold | once/player | Mid armor |
| Permanent diamond armor | 7 emerald | once/player | Late armor |
| Shears | 24 iron | one; persistent | Block tool |
| Pickaxe tier 1 | 12 iron | downgrade one tier on death | Defense breaker |
| Axe tier 1 | 12 iron | downgrade one tier on death | Defense breaker |
| Bow | 14 gold | one active | Ranged pressure |
| 8 arrows | 3 gold | inventory cap 64 | Bow ammunition |
| Fire charge | 48 iron | 1.25 s cooldown | Knockback/bridge control |
| TNT | 5 gold | 2.5 s cooldown | Bed defense pressure |
| Water bucket | 4 gold | inventory cap 2 | Defense/mobility |
| Bridge egg equivalent | 2 emerald | 5 s cooldown | Guided bridge creation |
| Warp pearl | 4 emerald | 3 s cooldown | Late mobility |
| Rescue capsule | 5 gold | inventory cap 1; 90 s cooldown | Void rescue, AntiDrop synergy |
| Pop-up tower | 22 iron | 8 s cooldown; one constructing/player | Temporary structure |
| Sponge burst | 4 gold | 3 s cooldown | Water clear + semantic effect |

Every item has semantic material/effect fallbacks, maximum count, cooldown and placement/region validation. Economy transactions are atomic and idempotent.

## Team upgrades

| Upgrade | Cost/progression |
|---|---|
| Weapon sharpness | 5 diamonds; one level |
| Armor reinforcement | 2/4/8/14 diamonds for four levels |
| Tool speed | 3/6 diamonds for two levels |
| Base generator | 2/4/8 diamonds for three levels |
| Base regeneration | 4 diamonds; one level |
| Trap slots | 1/2/4/6 diamonds; FIFO up to four queued |

Costs scale by team-size factor: solo `0.85`, doubles `1.00`, 3-player `1.15`, 4-player `1.25`, rounded up. Rush halves generator upgrade costs and disables the match soft-limit extension; Ultimate keeps prices and adds its ability cooldown profile. Voidless/Adventure never changes economy implicitly; each has its own explicit override pack.

## Rewards and change control

Base completion XP is 120 plus 2/minute played (cap 50), win +90, final elimination +12 (cap 120), bed break +30 (cap 90), team objective contribution +20. Coins: participation 25, win 50, final elimination 5 (cap 50), bed break 15 (cap 45). Quest/achievement/battle-pass rewards use the original starter catalogue and the transactional reward ledger; duplicate event IDs produce no reward.

Profiles are immutable-versioned definitions. A change requires new version, rationale, simulation comparing win duration/resource flow/item purchase/role impact, migration/rollback and owner approval. Golden simulations fix seeded scenarios for solo, doubles, teams, Rush, Ultimate, Voidless and custom generator/scarcity composition. Telemetry may recommend changes but never modifies live balance automatically.
