# Team layout compatibility matrix

| Layout | Teams | Capacity each | Maximum players | Deterministic test |
|---|---:|---:|---:|---|
| Solo | 8 | 1 | 8 | `assemblesSoloEightByOne` |
| Doubles | 8 | 2 | 16 | `assemblesDoublesEightByTwo` |
| 3v3v3v3 | 4 | 3 | 12 | `assemblesThreeByThreeByThreeByThree` |
| 4v4v4v4 | 4 | 4 | 16 | `assemblesFourByFourByFourByFour` |
| 4v4 | 2 | 4 | 8 | `assemblesFourByFour` |
| Custom | 12 | 3 | 36 | `assemblesCustomTwelveByThreeWithoutPresetCeiling` |
| Shared safety maximum | 64 | 4 | 256 | `assemblesSharedMaximumSixtyFourByFour` |

The eight-team fixtures explicitly preserve Red, Blue, Green, Yellow, Aqua, White,
Pink and Gray identities, labels and semantic colors. They are test/configuration data,
not engine constants. Custom IDs, labels, colors, counts and capacities use the same
assembler and state machine.

For every row the suite verifies arena-to-match assembly, lookup by stable ID,
configured metadata/capacity, preference-aware assignment and admission. The 2-team
lifecycle additionally verifies reconnect, bed/team elimination, generic victory
intent, duplicate retry, exactly-once completion, restoration, reset and recovery.
Fail-closed tests cover disabled, stale and cross-definition-inconsistent arenas.

This matrix certifies configurability of the M08 foundation. It does not claim later
shops, generators, modes, matchmaking, selectors or full gameplay acceptance assigned
to M10 and subsequent milestones.
