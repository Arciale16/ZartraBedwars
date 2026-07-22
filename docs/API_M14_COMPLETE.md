# M14 Complete API

Requirement IDs: `ZBW-PROG-006`, `ZBW-PROG-007`, `ZBW-PROG-008`, `ZBW-PROG-014`.

`M14Runtime` is the Java 8-neutral cosmetic ownership and loadout service. It receives an `M14Catalog`, `M14Configuration`, `CosmeticStateRepository`, and existing `EntitlementRepository`. Callers retain transaction ownership and provide `RecordRevision`, `IdempotencyKey`, and `AuditMetadata` for mutation.

`M14ProfileRuntime` reads and saves `ProfileSettings` through `ProfileSettingsRepository`, preserving whole immutable snapshots and optimistic revisions. `mayView` is a policy helper; command/UI adapters must perform M03 authorization before calling it.

`M14CampaignRuntime` reports `ACTIVE`, `NOT_STARTED`, `EXPIRED`, or `NOT_FOUND`. An active `CalendarCampaign` carries only `RewardId` references for the M12 reward engine.

`PresentationActions.Catalog.m14()` is the additive command/permission/page catalogue. `M14PresentationBindings`, `M14GuiPages`, and `M14PaperProjection` are Java 21 Paper adapters. The projection accepts only committed feedback intents and enforces owner-thread plus effect budgets; it contains no progression, entitlement, reward, or persistence policy.
