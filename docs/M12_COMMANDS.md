# M12 command surfaces

The normative generated inventory is `build/m12-command-inventory.json`; `docs/COMMANDS.md` is its
human-readable through-M12 projection. Phase 4 adds 17 `/zbw` actions:

- Player: progression overview, level, XP, prestige, balances, reward list/claim and unlocks.
- Admin: progression inspect/grant-XP/adjust/recalculate, reward inspect/grant/failures, and
  currency inspect/ledger.

All dispatch uses M09. M03 authorization is checked when executing, not merely while rendering or
tab-completing. Claim/grant/adjust/recalculate actions additionally require M09 confirmation,
idempotency and audit correlation. No operator-only bypass exists.
