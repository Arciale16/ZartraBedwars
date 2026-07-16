# Arena validation profiles

`ArenaValidationProfile` replaces generator-name substring recognition with exact
`GeneratorTypeId` requirements.

The original starter profile has ID `zartra:arena-validation/standard`, requires exact
shared types `zartra:diamond` and `zartra:emerald`, and requires one owned generator,
shop NPC and upgrade NPC for every configured team. A type such as
`example:diamond-compatible` does not satisfy `zartra:diamond`.

A custom profile supplies:

- a stable profile ID;
- zero to 64 exact shared generator type IDs;
- explicit team-generator, shop-NPC and upgrade-NPC requirements.

Custom resources are unrestricted by name. The active generator registry remains the
later runtime authority; M08.1 validates only typed arena prerequisites and does not
implement M10 generator gameplay.

The default validator also reports group mismatch, unsupported arena modes, team sizes
outside the map range, insufficient aggregate capacity, missing world/spawns/beds,
unsafe bounds, broken references and collisions. Reports remain deterministic,
localized by stable message key and block enable/assembly on every error.
