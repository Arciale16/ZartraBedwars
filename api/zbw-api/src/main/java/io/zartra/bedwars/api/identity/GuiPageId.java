package io.zartra.bedwars.api.identity;

/** Immutable namespaced identity for a GUI page contract. */
public final class GuiPageId extends NamespacedIdentifier {
    private GuiPageId(final String namespace, final String path) { super(namespace, path); }
    /** @return typed GUI page ID @throws IdentifierFormatException if either component is invalid */
    public static GuiPageId of(final String namespace, final String path) { return new GuiPageId(namespace, path); }
    /** @return parsed GUI page ID @throws IdentifierFormatException if malformed */
    public static GuiPageId parse(final String value) {
        final String[] parts = split(value);
        return of(parts[0], parts[1]);
    }
}
