# M03 localization foundation

M03 provides a platform-neutral catalog service for `ZBW-UX-005`. A catalog is an immutable mapping from `MessageKey` to neutral templates and belongs to a normalized `LocaleId`. Catalog replacement is atomic. Duplicate keys, malformed locale IDs, malformed placeholders and missing required catalog entries are rejected before publication.

Locale resolution is deterministic: explicit player preference, server locale, configured fallback locale. Changing a player preference affects the next lookup without restart. The service reports whether fallback was used and provides a completeness report against a required-key set.

Parameters are named, typed and immutable. Duplicate names and `null` values are rejected. Text values are escaped before interpolation; callers cannot inject formatting or click/hover behavior through a parameter. Missing or extra parameters produce a typed validation failure instead of a partially rendered message.

The M03 codec imports and exports a deterministic UTF-8 line representation through `Reader`/string boundaries. It does no filesystem access. A later adapter may persist catalogs only if it preserves the same validation, ordering and rollback rules.

M03 does not claim Adventure/MiniMessage, RGB/gradient, plural rules, click/hover, Minecraft packet or legacy-client rendering. Those rendering capabilities and the documented 1.8 fallbacks remain M22. Commands and language/editor GUIs remain M09/M22. The neutral foundation ensures those adapters share the same keys, locale selection and safe parameters.
