# M13 GUI inventory

**Requirements:** ZBW-PROG-009..013, ZBW-UX-001..006, ZBW-ARC-005..006.

Player pages cover objectives, active/completed quest lists and details, achievements, challenges, battle-pass season and tiers, and reward claim. Administration pages cover progression inspection, quest management, achievement inspection/grant, challenge mutation, season management and audit history. The 17 canonical parity page IDs are generated under `gui_page` in `build/m13-command-inventory.json`.

Pages use bounded page numbers, filter/search queries, accessible primary/keyboard interactions, asynchronous loading, explicit loading/empty/error states and immutable revisions. Late loads, wrong-viewer clicks, stale revisions and duplicate nonces are rejected. Mutations use the same action, M03 permission and M09 confirmation flow as commands; M12 idempotency protects claims.
