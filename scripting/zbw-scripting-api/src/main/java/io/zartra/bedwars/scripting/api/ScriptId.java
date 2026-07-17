package io.zartra.bedwars.scripting.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable identity of one versioned declarative action graph. */
public final class ScriptId implements Comparable<ScriptId> {
    private final DefinitionId value;

    private ScriptId(final DefinitionId value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    /** @return an identity in {@code namespace:script/path} form */
    public static ScriptId of(final String namespace, final String path) {
        return new ScriptId(DefinitionId.of(namespace, "script/" + path));
    }

    /** @return a parsed identity whose path begins with {@code script/} */
    public static ScriptId parse(final String serialized) {
        final DefinitionId parsed = DefinitionId.parse(serialized);
        if (!parsed.path().startsWith("script/") || parsed.path().length() == 7) {
            throw new IllegalArgumentException("script identity must use the script/ path");
        }
        return new ScriptId(parsed);
    }

    /** @return underlying public definition identity */
    public DefinitionId value() {
        return value;
    }

    @Override public int compareTo(final ScriptId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) {
        return this == other || other instanceof ScriptId
                && value.equals(((ScriptId) other).value);
    }
    @Override public String toString() { return value.toString(); }
}
