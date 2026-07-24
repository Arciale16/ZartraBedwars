# M09 GUI framework

The neutral UI model defines stable page, component and session IDs; immutable page/query/component
state; supported interactions; localized labels/lore; and accessibility metadata. `UiFramework`
uses a bounded session registry and implements open/load, navigation, back, refresh, close, expiry,
state retention, stale-revision rejection and click-nonce deduplication. Page loaders return
`CompletionStage` values and expose loading, ready, empty and error states.

`AdminDashboard` generates searchable, filterable, sortable and paginated administration entries
from the shared action catalogue. `PresentationParity` rejects a missing, duplicate or divergent
command/page mapping. `PaperGuiAdapter` renders inventories, translates clicks, rejects stale
inventories, cancels drags, cleans close state and confines all inventory mutation to its owner-thread
port. It contains no arena, lobby or game rule.

Fallback interaction is part of component accessibility metadata: every actionable component has
plain-language description, keyboard/chat equivalent and Bedrock-safe interaction intent. M22 owns
legacy runtime rendering certification; M09 certifies the primary Paper 1.21.1 mapping only.
## M13 extension inventory

M13 adds 17 objective, quest, achievement, challenge, battle-pass, reward-claim and administration parity pages through the existing registry. Their canonical IDs and permissions are generated in `build/m13-command-inventory.json`; behavior and states are specified in `M13_GUI.md`. Requirements: ZBW-PROG-009..013 and ZBW-UX-001..006.
