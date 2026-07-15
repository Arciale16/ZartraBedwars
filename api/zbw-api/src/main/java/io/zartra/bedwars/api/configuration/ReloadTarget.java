package io.zartra.bedwars.api.configuration;

/** Safe targeted reload classifications used by option metadata and reload plans. */
public enum ReloadTarget {
    /** Core startup settings; changes require restart. */ CORE,
    /** Localized message catalogs. */ MESSAGES,
    /** GUI definitions. */ GUI,
    /** Shop and balance definitions. */ SHOP,
    /** Quest and progression definitions. */ QUESTS,
    /** Cosmetic definitions. */ COSMETICS,
    /** Optional integration settings. */ INTEGRATIONS,
    /** Placeholder definitions. */ PLACEHOLDERS,
    /** Arena and map definitions that explicitly support safe reload. */ ARENAS,
    /** Authorization policy. */ PERMISSIONS,
    /** Compatibility declaration data; adapter replacement still requires restart. */ COMPATIBILITY,
    /** Security policy; secret material itself is never in configuration snapshots. */ SECURITY
}
