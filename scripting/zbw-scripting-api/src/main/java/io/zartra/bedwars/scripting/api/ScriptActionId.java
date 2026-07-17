package io.zartra.bedwars.scripting.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable identity of an entry point inside a declarative action graph. */
public final class ScriptActionId implements Comparable<ScriptActionId> {
    private final DefinitionId value;

    private ScriptActionId(final DefinitionId value) {
        this.value = Objects.requireNonNull(value, "value");
    }

    /** @return an identity in {@code namespace:action/path} form */
    public static ScriptActionId of(final String namespace, final String path) {
        return new ScriptActionId(DefinitionId.of(namespace, "action/" + path));
    }

    /** @return a parsed identity whose path begins with {@code action/} */
    public static ScriptActionId parse(final String serialized) {
        final DefinitionId parsed = DefinitionId.parse(serialized);
        if (!parsed.path().startsWith("action/") || parsed.path().length() == 7) {
            throw new IllegalArgumentException("action identity must use the action/ path");
        }
        return new ScriptActionId(parsed);
    }

    /** @return underlying public definition identity */
    public DefinitionId value() { return value; }

    @Override public int compareTo(final ScriptActionId other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) {
        return this == other || other instanceof ScriptActionId
                && value.equals(((ScriptActionId) other).value);
    }
    @Override public String toString() { return value.toString(); }
}
