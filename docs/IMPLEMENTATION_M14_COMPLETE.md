# M14 Complete Implementation

Requirement IDs: `ZBW-PROG-006`, `ZBW-PROG-007`, `ZBW-PROG-008`, `ZBW-PROG-014`.

M14 completes the cosmetics, player-profile and calendar/campaign implementation over the existing progression, M12 entitlement/reward, M03 authorization and M09 presentation boundaries. `M14Runtime` resolves ownership from M12 entitlements, protects duplicate/idempotent loadout persistence through the established repository port, rejects disabled/unavailable definitions, and safely falls back for absent definitions. `M14ProfileRuntime` uses immutable revisioned replacements and the M12 persistence boundary; its visibility predicate consumes caller-supplied, authorized relationship facts. `M14CampaignRuntime` validates campaign windows and exposes existing M12 reward references without delivering rewards itself.

The Paper adapter is presentation-only: command bindings defer authorization, validation, confirmation and audit context to the established command operation; GUI pages use the established asynchronous query/page state contract; Paper feedback is owner-thread restricted and validates per-intent entity/particle budgets. No statistics, PlaceholderAPI, replay, moderation, provider, distributed or compatibility feature was added.

The catalogue/configuration remains versioned and immutable. Publishing or activating content remains governed by the approved catalogue and original-content provenance process; no third-party cosmetic asset is packaged by this milestone.
