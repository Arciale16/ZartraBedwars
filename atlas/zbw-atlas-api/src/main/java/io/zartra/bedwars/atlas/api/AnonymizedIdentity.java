package io.zartra.bedwars.atlas.api;

import java.util.Objects;

/** Stable per-case alias containing no name, UUID, rank or social metadata. */
public final class AnonymizedIdentity {
    private final String alias;

    /** Creates a validated opaque display alias. */
    public AnonymizedIdentity(final String alias) {
        if (alias == null || !alias.matches("CASE-[A-Z0-9]{6,24}")) {
            throw new IllegalArgumentException("alias must match CASE-[A-Z0-9]{6,24}");
        }
        this.alias = alias;
    }

    /** Returns the community-safe alias. */ public String alias() { return alias; }
    @Override public boolean equals(final Object other) {
        return other instanceof AnonymizedIdentity
                && alias.equals(((AnonymizedIdentity) other).alias);
    }
    @Override public int hashCode() { return Objects.hash(alias); }
    @Override public String toString() { return alias; }
}
