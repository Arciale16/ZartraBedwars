# M09 implementation evidence

## Scope and requirements

Milestone 9 implements `ZBW-UX-001`, `ZBW-UX-002`, `ZBW-UX-003` and `ZBW-UX-006`,
and completes the presentation allocations of `ZBW-ARENA-001..009`,
`ZBW-GAME-001..003/006/008/010`, `ZBW-ADDON-001..009`, `108..114`, `124..130`,
`148..154`, `334..340`, `398..407` and `408..437`. It does not implement selectors,
matchmaking, modes or the spectator framework assigned to M10.

The four new modules are `zbw-command-api` and `zbw-ui-api` at Java 8, plus
`zbw-command-paper` and `zbw-ui-paper` at Java 21. The neutral modules have no
Bukkit, Paper, NMS, storage or filesystem dependency. Paper adapters translate only;
the exact `PresentationActions.Registry` fails startup for missing, duplicate or unknown
M07/M08 use-case bindings.

## Delivered behavior

- An immutable typed command tree provides aliases, validated typed arguments, sender rules,
  completion, localized help/usage/errors, cooldowns, cancellation, deadlines, M03 authorization
  revalidation, bounded execution and immutable audit records.
- A bounded GUI framework provides page/component/session IDs, navigation, back/close,
  pagination, search/filter/sort, loading/empty/error states, retained query state, refresh,
  stale-view rejection, click nonce deduplication, expiry and accessibility/fallback metadata.
- Confirmation intents bind actor, action, target, revision, expiry and nonce; they are single-use,
  reject replay/stale/mismatched input, reauthorize before consumption and audit issue/consume/reject.
- The generic editor owns draft/revision mechanics only: validate, preview, apply, cancel, undo/redo,
  import/export, duplicate/reset, migration, conflict and expiry. Arena/game rules stay in M07/M08.
- The shared 87-action catalogue generates both command and GUI mappings, granular permissions,
  documentation and parity evidence. Destructive command and GUI paths use the same confirmation
  service and the same application binding.

## Tests and evidence

M09 contributes 24 zero-skip unit/adapter tests: 8 command API, 10 UI API, 5 Paper command
and 1 Paper GUI test. They cover parsing, authorization, cooldown, cancellation/deadline,
inventory completeness, bounded execution, GUI lifecycle/state/expiry/deduplication,
confirmation attacks, editor lifecycle/conflicts/migration, dashboard behavior and all 87 parity
mappings. Exact Paper 1.21.1 build 133 evidence is in
`build/evidence/m09-paper-primary.json`; it proves real command dispatch, real inventory API
rendering, parity, off-owner bounded work, no duplicate execution and clean exit.

Required machine-readable outputs are `build/m09-command-inventory.json`,
`build/m09-permission-inventory.json` and the generated 672-row feature dashboard.

## Final verification

| Gate | Result |
|---|---|
| Java matrix | Java 8/11/16/17 neutral reactors: 235 tests each; Java 21 full 22-module reactor: 258 tests; zero failures/errors/skips |
| M09 tests | 24: command API 8, UI API 10, Paper command 5, Paper UI 1 |
| M09 JaCoCo | command API 86.69% line / 71.30% branch; UI API 92.17% / 70.36%; Paper command 90.14% / 75.38%; Paper UI 95.33% / 75.93% |
| Static analysis | Full quality reactor PASS; zero Checkstyle violations and zero SpotBugs findings |
| Binary/API | PASS for 514 Java 8 and 25 Java 21 classes; every M08.1 signature preserved |
| Strict JavaDoc | PASS for 275 Java 8 and 27 Java 21 sources; neutral and modern archives generated |
| Dependencies/licences/SBOM | 15 build/CI artifacts and 212 Maven components locked; 650 exact repository files; zero product binaries bundled |
| Governance and coverage | 32 governance tests PASS; 49 addons/473 addon requirements; 6,438 Master Prompt assertions and 6,966 combined atomic items; 672 requirements; 100% documentation coverage |
| Paper certification | Exact Paper 1.21.1 build 133 (`39bd8c00…287b9`) PASS with exit code 0 for dispatch, inventory rendering, parity, off-owner bounded work and duplicate prevention |
| Inventories/dashboard | 87 commands, 87 granular permissions, 88 GUI pages; 672 deterministic dashboard rows |

The isolated Paper runner sets `max-tick-time=-1` so slow first-world generation on CI or Windows
cannot trigger Paper's watchdog before certification completes. This setting exists only in the
temporary test server; production runtime defaults are not changed.

## Exit decision

M09 is verified only after the clean Java matrix, Checkstyle, SpotBugs, JaCoCo, strict JavaDoc,
binary/API, dependency/licence/provenance/SBOM, governance, documentation and exact Paper gates
all pass. Later compatibility certification remains M22; M10 remains unstarted.
