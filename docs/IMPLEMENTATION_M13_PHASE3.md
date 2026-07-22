# M13 Phase 3 implementation

**Final milestone status:** VERIFIED and completed on 2026-07-22. This document is the final M13 presentation/runtime-integration reference; Phase 1 and Phase 2 remain the foundation and runtime checkpoint references.

**Requirements:** ZBW-PROG-009..013, ZBW-ARC-002, ZBW-ARC-005..006, ZBW-UX-001..006, ZBW-SEC-001..004, ZBW-QA-001..007.

Phase 3 extends the M09 command and GUI frameworks. `PresentationActions.Catalog.m13()` contributes 17 actions to the existing command tree and parity-page registry. `M13PresentationBindings` delegates every invocation to application operations, where M03 authorization is rechecked at execution time and actor, target, revision and correlation ID form the audit context. Six mutation paths require a single-use M09 confirmation token.

`M13GuiPages` contributes asynchronous pages through `zbw-ui-api` and `zbw-ui-paper`. M09 query, page-state and session revisions supply bounded pagination, filter/search input, loading/empty/error states, duplicate-click rejection and stale-view protection. `M13PaperProjection` is a Java 21 closed adapter with no business logic. Every feedback effect, inventory open and cleanup is owner-thread guarded; storage, evaluation and delivery remain on bounded application workers.

Runtime flow is `M08 event -> M13EventAdapter -> M13ProjectionEngine -> durable completion -> M13RewardIntent -> M12 reward delivery -> M13PaperProjection feedback`. Durable claims and stable reward keys reject duplicates. Persisted state supports reconnect/restart recovery; expiry and definition validation reject expired/invalid content, and M12 retains failed rewards for idempotent retry.

No M14+, statistics, PlaceholderAPI or replay scope is included.

## Final closure evidence

The clean Java 8 and Java 21 reactors pass. The Java 21 quality reactor passes Checkstyle, SpotBugs and every configured JaCoCo threshold; strict JavaDoc covers 460 Java 8 and 67 Java 21 sources. The current M13 API checkpoint preserves the immutable Phase 1 baseline. Governance, the deterministic 672-row dashboard, all three 100% traceability/coverage validators, both dependency/licence/SBOM locks and the targeted Paper presentation suite pass. The Phase 3 implementation checkpoint is `2b22e83bb22102719003c2cd1485165b4a21958f`.

M14 is next. M15 owns statistics, M16 owns PlaceholderAPI and M17 owns replay; distributed, provider and compatibility milestones retain their existing boundaries.
