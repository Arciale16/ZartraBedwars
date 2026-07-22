# M13 Phase 3 API

**Requirements:** ZBW-PROG-009..013, ZBW-ARC-003..006, ZBW-UX-001, ZBW-SEC-001..004.

`PresentationActions.Catalog.m13()` returns the immutable 17-action extension; `throughM13()` returns all 174 actions. Definitions expose stable action ID, command path, GUI page ID, exact permission, confirmation flag and Requirement IDs.

`M13PresentationBindings.create(Operations)` returns immutable exact bindings. `Operations.execute` receives the authenticated subject, protected target, optimistic revision, correlation ID, validated arguments and surface. Implementations reauthorize through M03 immediately before execution and audit privileged mutations.

`M13GuiPages.create(ViewProvider)` returns 17 asynchronous page definitions. Queries carry page/filter/search state; results carry revision and loading, ready, empty or error status. M09 rejects stale loads/clicks, wrong viewers and duplicate clicks.

`M13PaperProjection` requires an `OwnerThread` and closed `Platform` renderer. Feedback supports OBJECTIVE, QUEST, ACHIEVEMENT, CHALLENGE, BATTLE_PASS and REWARD with validated localization, sound and particle keys. It performs no persistence or progression calculation.
