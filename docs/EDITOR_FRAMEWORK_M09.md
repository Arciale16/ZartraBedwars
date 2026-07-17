# M09 editor framework

`EditorFramework<D>` manages presentation-neutral typed drafts with a base revision and immutable
history. An injected policy owns serialization, validation, preview, application and migration;
the framework owns session bounds, expiry, optimistic conflict detection, cancel, undo/redo,
import/export, duplicate, reset and rollback mechanics.

Apply validates and previews before invoking the policy and rejects a stale repository revision.
Import data is length-bounded and schema-migrated before replacement. M07 arena/setup policies are
adapters to this contract; no arena validation, team rule, match state or item rule is embedded here.
