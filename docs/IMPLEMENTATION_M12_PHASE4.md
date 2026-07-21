# M12 Phase 4 — presentation and primary Paper integration

## Scope

This checkpoint implements the presentation portions of `ZBW-PROG-001..005` and
`ZBW-PROG-011`, plus the M12 presentation cells for `ZBW-ADDON-174..183`,
`ZBW-ADDON-210..216`, `ZBW-ADDON-245..251` and `ZBW-ADDON-266..282`.
It reuses M03 authorization and localization, M09 command/UI/confirmation machinery and the
committed M12 application operations. It does not complete or certify M12.

## Delivered

- Seventeen additive command/GUI parity actions cover player progression, level, XP, prestige,
  persistent balances, rewards and unlocks, plus administration inspection, mutation, ledger and
  recovery diagnostics.
- Every action has one granular permission and one M09 page ID. Mutations are marked destructive
  so M09 confirmation tokens bind actor, target, action and revision and revalidate authorization
  on consumption.
- `M12PresentationBindings` delegates immutable requests to an injected application facade. The
  facade owns execution-time authorization, audit, cooldown/rate-limit policy and Phase 1–3 use
  cases; the adapter contains no progression policy.
- `M12GuiPages` registers asynchronous, paginated M09 page definitions. M09 supplies loading,
  empty, error, stale-load, stale-click, replay, navigation and lifecycle handling.
- `M12PaperProjection` is a Java 21 owner-thread boundary for localized messages, action bars,
  titles, sounds, XP-bar feedback, inventory views, reward/level/prestige/currency feedback and
  cleanup. It consumes committed semantic intents and performs no SQL or progression calculation.

## Threading and failure rules

Repositories and page loaders execute on bounded non-owner executors. Only the final inventory or
feedback mutation crosses `M12PaperProjection`, which rejects off-owner-thread invocation. Failed
loads remain typed M09 error states; rejected authorization, stale revisions and duplicate actions
fail closed and do not reach the M12 mutation facade.

## Deferred

Final M12 integration certification is closed in Phase 5. Statistics (M15), PlaceholderAPI (M16),
replay (M17), Atlas (M18), distributed/proxy behavior (M19/M20), external providers (M21), legacy
compatibility (M22), and quests/achievements/battle pass/cosmetics remain with their owners.
