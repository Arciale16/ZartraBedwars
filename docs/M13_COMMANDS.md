# M13 commands

**Requirements:** ZBW-PROG-009..013, ZBW-UX-001, ZBW-SEC-001..004.

Player paths are `/zbw objectives view`, `/zbw quests active|completed`, `/zbw achievements view`, `/zbw challenges view`, `/zbw battlepass view|tiers`, and `/zbw rewards claim`.

Administration paths are `/zbw admin progression inspect|audit`, `/zbw admin quest assign|remove`, `/zbw admin achievement grant|inspect`, `/zbw admin challenge modify`, and `/zbw admin season inspect|manage`.

All paths use M09 parsing/dispatch, typed validation, execution-time M03 permission checks, correlation/audit context and bounded deadlines. Claim, assign, remove, grant, modify and season-management mutations require a one-time confirmation token. `build/m13-command-inventory.json` is canonical and `tools/validation/m13_inventories.py` rejects drift.
