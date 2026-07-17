# M10 implementation evidence

## Authoritative scope

M10 completes the shared-server framework portions of `ZBW-GAME-004`, `ZBW-GAME-005`,
`ZBW-GAME-007`, `ZBW-GAME-009` and `ZBW-CONTENT-003`; the M10 portions of
`ZBW-ADDON-092..101`, `115..123`, `131..140`, `155..163`; and registration/selection
only for `ZBW-ADDON-236..244`. Named-mode mechanics remain M11, statistics M15,
PlaceholderAPI M16, staff evidence M17, proxy-wide orchestration M20 and full compatibility M22.

## Delivered boundaries

- `zbw-game` adds Java 8 neutral `mode`, `selector`, `matchmaking` and `spectator` packages.
- Existing M09 command/UI modules expose 28 additive actions over the same use-case bindings;
  the M09 87-action baseline remains unchanged.
- `zbw-paper-modern` adds owner-thread-checked projection and a bounded off-owner executor.
  No Bukkit/Paper/NMS type enters a neutral package.
- No new Maven module, storage migration, proxy protocol, Redis behavior or named-mode mechanic
  is introduced.

## Verification record

The exact final record is:

| Gate | Result |
|---|---|
| Java 8 / 11 / 16 / 17 neutral reactors | PASS; 273 tests per toolchain, zero failures/errors/skips |
| Java 21 complete 22-module reactor | PASS; 300 tests, zero failures/errors/skips |
| M10-specific JUnit tests | 42: 37 neutral game, 2 command/parity and 3 Paper projection tests |
| Game quality | 89 tests; 93.69% lines, 85.21% branches |
| Affected-module coverage | command API 87.60%/72.76%; command Paper 90.14%/75.38%; UI API 92.17%/70.36%; UI Paper 95.33%/75.93%; Paper modern 85.77%/76.92% (line/branch) |
| Static analysis | zero Checkstyle violations; zero SpotBugs findings across all 22 modules |
| Binary/API compatibility | 582 Java 8 and 45 Java 21 classes; every M09 signature retained |
| Strict JavaDoc | 284 Java 8 and 47 Java 21 sources; both deterministic archives generated |
| Governance | 35 tests PASS; 6,438 source assertions, 672 requirements and 473 addon IDs remain 100% covered |
| Dependencies/licences/SBOM | 15 build/CI artifacts and 212 Maven components locked; 650 exact repository files; zero product binaries |
| Presentation inventories | 115 commands/actions, 115 permissions and 116 dashboard-inclusive GUI pages |
| Dashboard | 672 unique rows: 10 VERIFIED, 228 PARTIAL, 434 DEFERRED and zero BLOCKED/IMPLEMENTED/IN_PROGRESS/NOT_STARTED |

The exact Paper evidence at `build/evidence/m10-paper-primary.json` certifies Paper 1.21.1 build
133 (`SHA-256 39bd8c00b9e18de91dcabd3cc3dcfa5328685a53b7187a2f63280c22e2d287b9`). It proves
selector rendering/click translation, queue command and GUI dispatch, parity, owner-thread
projection, bounded off-owner matching, duplicate prevention, stale-view rejection and
deterministic cleanup. The M10 validation tools and CI regenerate these release artifacts.

## Layout and behavior evidence

`ModeSelectorM10Test` applies the same typed semantic-team definition to Solo 8x1, Doubles 8x2,
4x3, 4x4, 2x4, custom 12x3 and high-team-count 64x4 layouts. `MatchmakingM10Test` proves atomic
party admission, leader and revision validation, deterministic aging/fairness, bounded capacity,
idempotent retries, no party splitting, revision-bound single-owner reservations and no
overbooking. `SpectatorAddonM10Test` proves lifecycle, navigation, restrictions, reconnect and
cleanup. The M10 Paper certification proves presentation translation and thread ownership; it
does not claim later named-mode mechanics.

## Recovery and shutdown

Shared-server queues and reservations are bounded process state. Admission is revision-fenced,
idempotent and fail-closed; shutdown stops admission and drains bounded work. A restart discards
uncertified local intent rather than reconstructing an assignment. Durable/distributed matching
and proxy-wide recovery remain explicitly owned by M20.
