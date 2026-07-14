# ZartraBedWars Original Starter Catalogue

**Status:** normative pre-implementation content baseline<br>
**Decision:** RC-073<br>
**Requirements:** `ZBW-CONTENT-001..010`, `ZBW-ADDON-464..473`<br>
**Implementation status:** NOT STARTED

## 1. Originality and evolution contract

Every ID, name, description and numerical preset in this document was authored for ZartraBedWars. It is a functional starting point, not copied balance or protected content from Hypixel, BedWars1058 or any addon author. Shipped code, messages, sounds, models, textures and effects must be original or have an approved row in `docs/ASSET_PROVENANCE.md`.

All definitions use stable namespaced IDs, schema versions and typed values. Operators may replace or extend them through validated configuration. Extensions may register new definitions through public APIs; they may not overwrite a built-in ID unless an explicit versioned override policy permits it. Reload uses validate-then-swap and retains the last-known-good catalogue on failure.

## 2. Shop balancing starter profiles

Multipliers apply after the item's versioned base definition. Prices round up to the smallest valid currency unit. A zero or negative calculated price is invalid.

| Stable profile ID | Display name | Price multiplier | Cooldown multiplier | Team stock multiplier | Personal limit multiplier | Purpose |
|---|---|---:|---:|---:|---:|---|
| `zbw:shop/standard_foundry` | Standard Foundry | 1.00 | 1.00 | 1.00 | 1.00 | Default balanced public play |
| `zbw:shop/lean_kit` | Lean Kit | 0.85 | 1.10 | 0.85 | 0.85 | Lower prices with tighter availability |
| `zbw:shop/tactical_reserve` | Tactical Reserve | 1.15 | 0.90 | 1.25 | 1.00 | Higher cost with broader team stock |
| `zbw:shop/rapid_exchange` | Rapid Exchange | 0.95 | 0.65 | 1.00 | 1.25 | Faster item cycling for accelerated modes |

Every shop profile requires price, tender, cooldown, stock, purchase-limit and invalid-rounding golden tests before M11 exits.

## 3. Game-mode balancing starter profiles

The values below are original relative multipliers. `Event time` below 1.00 makes scheduled events occur earlier. Mode-specific mechanics remain governed by their own typed definitions.

| Stable profile ID | Mode | Generator rate | Event time | Respawn time | Damage | Special policy |
|---|---|---:|---:|---:|---:|---|
| `zbw:mode/standard` | Standard/custom teams | 1.00 | 1.00 | 1.00 | 1.00 | Canonical neutral baseline |
| `zbw:mode/rush` | Rush | 1.75 | 0.65 | 0.75 | 1.00 | Owned expanding bridges and original auto-defense enabled |
| `zbw:mode/ultimate` | Ultimate | 1.00 | 0.90 | 0.90 | 1.00 | Ability cooldown profile `zbw:ability/standard` |
| `zbw:mode/armed` | Armed | 1.10 | 0.90 | 1.00 | 0.90 | Server-authoritative weapon falloff and ammunition enabled |
| `zbw:mode/voidless` | Voidless | 1.00 | 1.00 | 0.90 | 1.00 | Low-boundary recovery and original auto-defense enabled |
| `zbw:mode/lucky_block` | LuckyBlock | 1.00 | 0.85 | 1.00 | 1.00 | Outcome pool `zbw:lucky/starter_safe` |
| `zbw:mode/bedsteal` | BedSteal | 0.95 | 1.00 | 1.00 | 1.00 | Redstone/mob/bed-level caps from typed mode config |
| `zbw:mode/swappage` | Swappage | 1.00 | 0.90 | 1.00 | 1.00 | First swap cannot occur before the configured safe-start bound |
| `zbw:mode/adventure` | Adventure | 1.00 | 1.00 | 1.00 | 1.00 | Waiting-state adventure and playing-state survival transitions |

## 4. Quest starter catalogue

| Stable quest ID | Original name | Schedule | Objective | Target | Reward seed |
|---|---|---|---|---:|---|
| `zbw:quest/bridgewright` | Bridgewright | Daily | Place valid player-owned blocks in completed games | 96 | 250 XP |
| `zbw:quest/bedwatch` | Bedwatch | Daily | Complete games while the player's bed survives | 2 | 180 XP + 75 coins |
| `zbw:quest/final_signal` | Final Signal | Daily | Record valid final kills | 4 | 220 XP |
| `zbw:quest/market_route` | Market Route | Daily | Buy items from three distinct shop categories | 3 | 160 XP |
| `zbw:quest/emerald_circuit` | Emerald Circuit | Weekly | Collect emeralds in completed public games | 24 | 900 XP + 300 coins |
| `zbw:quest/team_lantern` | Team Lantern | Weekly | Send useful quick-team callouts in games that complete | 20 | 650 XP |
| `zbw:quest/mode_cartographer` | Mode Cartographer | Weekly | Complete games in distinct enabled modes | 4 | 1,000 XP |
| `zbw:quest/replay_scholar` | Replay Scholar | Weekly | Watch eligible replay evidence for a minimum configured duration | 3 | 500 XP |

Progress is idempotent and excludes invalidated, test and policy-excluded private matches.

## 5. Achievement starter catalogue

| Stable achievement ID | Original name | Criterion | Tier targets | Points |
|---|---|---|---|---|
| `zbw:achievement/first_light` | First Light | Complete public wins | 1 / 25 / 100 | 5 / 20 / 50 |
| `zbw:achievement/anchor_line` | Anchor Line | Break enemy beds | 10 / 100 / 500 | 5 / 25 / 75 |
| `zbw:achievement/last_beacon` | Last Beacon | Record final kills | 25 / 250 / 1,000 | 5 / 30 / 90 |
| `zbw:achievement/quiet_foundry` | Quiet Foundry | Win without losing the team bed | 5 / 25 / 100 | 10 / 35 / 100 |
| `zbw:achievement/full_spectrum` | Full Spectrum | Win in distinct enabled modes | 3 / 6 / 9 | 10 / 30 / 70 |
| `zbw:achievement/resource_keeper` | Resource Keeper | Deposit eligible resources safely | 100 / 1,000 / 10,000 | 5 / 25 / 80 |
| `zbw:achievement/steady_signal` | Steady Signal | Reach a public winstreak | 3 / 10 / 25 | 10 / 40 / 120 |
| `zbw:achievement/review_compass` | Review Compass | Submit accurate Atlas verdicts after eligibility | 10 / 100 / 500 | 10 / 50 / 150 |

## 6. Battle-pass starter season

The original starter season ID is `zbw:season/first_constellation`, with ten seed tiers used to prove free/premium track mechanics. The full configured season may extend the tier count without changing these IDs.

| Tier | Free-track seed | Premium-track seed |
|---:|---|---|
| 1 | 100 coins | Title `Trailfinder` |
| 2 | 250 XP | Cosmetic token |
| 3 | Quest reroll token | 300 coins |
| 4 | Profile badge `Copper Star` | Original projectile trail `Dawn Thread` |
| 5 | 500 XP | 750 XP |
| 6 | 300 coins | Original shopkeeper skin entitlement `Field Scribe` |
| 7 | Quest reroll token | Cosmetic token ×2 |
| 8 | Profile frame `Open Orbit` | 600 coins |
| 9 | 900 XP | Original victory effect entitlement `Skyline Pulse` |
| 10 | Title `Constellation I` | Badge `First Constellation` and 1,200 XP |

Premium entitlement is an abstract provider grant; ZartraBedWars handles no payment-card data.

## 7. Cosmetic starter catalogue

These seed entries establish original naming and category coverage. M14 still requires at least 300 approved built-in definitions under `ZBW-PROG-006` and `ZBW-ADDON-026`; a seed entry is not a substitute for that count.

| Stable cosmetic ID | Original name | Category | Rarity | Asset class |
|---|---|---|---|---|
| `zbw:cosmetic/dawn_thread` | Dawn Thread | Projectile trail | Uncommon | Vanilla particle recipe |
| `zbw:cosmetic/skyline_pulse` | Skyline Pulse | Victory effect | Rare | Vanilla particle/sound recipe |
| `zbw:cosmetic/copper_star` | Copper Star | Profile badge | Common | Text/vector-original required |
| `zbw:cosmetic/open_orbit` | Open Orbit | Profile frame | Rare | Texture-original required |
| `zbw:cosmetic/field_scribe` | Field Scribe | Shopkeeper skin | Epic | Skin-original/licensed required |
| `zbw:cosmetic/ember_index` | Ember Index | Kill effect | Rare | Vanilla particle recipe |
| `zbw:cosmetic/falling_signal` | Falling Signal | Final-kill effect | Epic | Packet/particle recipe |
| `zbw:cosmetic/lantern_break` | Lantern Break | Bed-destroy effect | Rare | Particle/sound recipe |
| `zbw:cosmetic/silver_glyph` | Silver Glyph | Glyph | Uncommon | Texture-original required |
| `zbw:cosmetic/echo_chime` | Echo Chime | Death cry | Rare | Sound-original/licensed required |
| `zbw:cosmetic/cloud_archive` | Cloud Archive | Island topper | Epic | Model-original required |
| `zbw:cosmetic/blueprint_spray` | Blueprint Spray | Spray | Uncommon | Texture-original required |
| `zbw:cosmetic/oak_constellation` | Oak Constellation | Wood skin | Rare | Texture-original required |
| `zbw:cosmetic/starweave` | Starweave | Bed skin | Epic | Texture/model-original required |
| `zbw:cosmetic/wayfinder_cage` | Wayfinder Cage | Waiting cage | Rare | Vanilla block recipe |
| `zbw:cosmetic/quiet_applause` | Quiet Applause | Emote | Uncommon | Packet/animation recipe |
| `zbw:cosmetic/arrival_beacon` | Arrival Beacon | Join effect | Rare | Particle recipe |
| `zbw:cosmetic/returning_comet` | Returning Comet | Respawn effect | Epic | Particle/sound recipe |
| `zbw:cosmetic/market_spark` | Market Spark | Shop-purchase effect | Uncommon | Particle/sound recipe |
| `zbw:cosmetic/bridge_ribbon` | Bridge Ribbon | Bridge effect | Rare | Bounded packet recipe |

## 8. Private-game starter presets

### 8.1 RESOURCE SCARCITY presets

Multipliers are independent typed values with an allowed default range of `0.10` through `5.00`. A custom resource inherits `customDefault` until the host supplies an override for its stable resource ID.

| Preset ID | Display name | Iron | Gold | Diamond | Emerald | Custom default |
|---|---|---:|---:|---:|---:|---:|
| `zbw:private/resource_scarce` | Scarce | 0.50 | 0.50 | 0.50 | 0.50 | 0.50 |
| `zbw:private/resource_reduced` | Reduced | 0.75 | 0.75 | 0.75 | 0.75 | 0.75 |
| `zbw:private/resource_normal` | Normal | 1.00 | 1.00 | 1.00 | 1.00 | 1.00 |
| `zbw:private/resource_abundant` | Abundant | 1.50 | 1.50 | 1.50 | 1.50 | 1.50 |
| `zbw:private/resource_extreme` | Extreme | 2.50 | 2.50 | 2.50 | 2.50 | 2.50 |

The host may override any column independently. The effective interval is calculated by the generator policy without creating a second scheduler. Changes lock at countdown start by default; an operator may allow transactional mid-game changes only when every active generator provider advertises `DYNAMIC_RATE_SAFE`.

### 8.2 Bundle presets

| Preset ID | Original name | Included modifier choices |
|---|---|---|
| `zbw:private/steady_field` | Steady Field | Normal resources, normal health/speed/gravity and standard event times |
| `zbw:private/lean_skirmish` | Lean Skirmish | Reduced resources, faster events and shorter respawn |
| `zbw:private/overflow_trial` | Overflow Trial | Abundant resources, maximum-upgrade modifier and standard combat |

## 9. Sound and visual-effect starter catalogue

These semantic IDs describe intent. Platform adapters choose original/licensed pack assets or safe vanilla mappings; 1.8 fallbacks are normative in `docs/COMPATIBILITY_FALLBACKS.md`.

| Semantic ID | Original name | Trigger | Required fallback class |
|---|---|---|---|
| `zbw:feedback/confirm_soft` | Soft Confirm | Valid non-destructive action | Legacy click sound or text cue |
| `zbw:feedback/reject_low` | Low Reject | Validation/permission denial | Legacy bass sound plus text cue |
| `zbw:feedback/countdown_mark` | Countdown Mark | Countdown step | Legacy note sound and title/chat |
| `zbw:feedback/bed_alarm` | Bed Alarm | Own-bed destruction | Legacy wither-like alert plus text |
| `zbw:feedback/final_signal` | Final Signal | Final elimination | Legacy thunder-like cue plus message |
| `zbw:feedback/reward_rise` | Reward Rise | Reward committed | Legacy level-up cue plus summary |
| `zbw:effect/resource_arc` | Resource Arc | Generator upgrade | Bounded legacy particle ring |
| `zbw:effect/team_intro` | Team Introduction | Match start | Team-colour particles or title cue |
| `zbw:effect/bridge_ribbon` | Bridge Ribbon | Owned bridge expansion | Bounded team-colour trail |
| `zbw:effect/skyline_pulse` | Skyline Pulse | Victory | Bounded particle/sound sequence |

## 10. Acceptance

The starter catalogue is accepted at specification level when all IDs are unique, every numeric value validates, all referenced rewards/categories exist or are declared in the same content pack, every asset-bearing entry has provenance before packaging, every 1.8 fallback has a compatibility row, and extension/config round-trip tests preserve stable IDs. Runtime implementation remains NOT STARTED.
