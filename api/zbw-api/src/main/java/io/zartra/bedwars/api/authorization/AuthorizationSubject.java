package io.zartra.bedwars.api.authorization;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable authenticated subject presented to the authorization boundary. */
public final class AuthorizationSubject {
    private final Kind kind;
    private final DefinitionId id;

    private AuthorizationSubject(final Kind kind, final DefinitionId id) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.id = Objects.requireNonNull(id, "id");
    }

    /** @return authenticated subject with a stable typed identity */
    public static AuthorizationSubject of(final Kind kind, final DefinitionId id) {
        return new AuthorizationSubject(kind, id);
    }
    /** @return subject category */ public Kind kind() { return kind; }
    /** @return stable identity, never a display name */ public DefinitionId id() { return id; }
    @Override public int hashCode() { return Objects.hash(kind, id); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AuthorizationSubject)) { return false; }
        final AuthorizationSubject that = (AuthorizationSubject) other;
        return kind == that.kind && id.equals(that.id);
    }

    /** Supported authenticated subject categories. */
    public enum Kind {
        /** A Minecraft player identity. */ PLAYER,
        /** The local console identity. */ CONSOLE,
        /** An authenticated extension identity. */ EXTENSION,
        /** An authenticated internal or external service identity. */ SERVICE
    }
}
