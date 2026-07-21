# M12 Phase 4 API

Phase 4 adds no Bukkit type to a Java 8 API and does not change the Phase 1–3 progression model.

The additive public surfaces are:

- `PresentationActions.Catalog.m12()` and `throughM12()` — immutable action definitions with
  command path, M09 page, central permission, confirmation policy and Requirement IDs.
- `M12PresentationBindings` — Java 21 composition adapter from M09 actions to M12 operations.
- `M12GuiPages` — Java 21 registration adapter for asynchronous M09 page loaders.
- `M12PaperProjection` — Java 21 owner-thread projection port and immutable semantic feedback.

Application operations remain async. Null or malformed boundaries fail immediately. Operational
failure, denial, conflict and timeout use `PresentationActions.Response`; Paper translation never
throws a generic application error across the M12 boundary. Existing Java 8 API signatures remain
the compatibility baseline and the new catalog methods are strictly additive.
