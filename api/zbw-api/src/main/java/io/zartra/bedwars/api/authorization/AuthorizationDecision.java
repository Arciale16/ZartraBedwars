package io.zartra.bedwars.api.authorization;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable, secret-free authorization outcome suitable for structured audit. */
public final class AuthorizationDecision {
    private final boolean allowed;
    private final DefinitionId reason;

    private AuthorizationDecision(final boolean allowed, final DefinitionId reason) {
        this.allowed = allowed;
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /** @return allowed decision with a stable policy reason */
    public static AuthorizationDecision allow(final DefinitionId reason) {
        return new AuthorizationDecision(true, reason);
    }
    /** @return denied decision with a stable policy reason */
    public static AuthorizationDecision deny(final DefinitionId reason) {
        return new AuthorizationDecision(false, reason);
    }
    /** @return whether the exact requested action is authorized */ public boolean isAllowed() { return allowed; }
    /** @return stable, localized-by-caller policy reason */ public DefinitionId reason() { return reason; }
    @Override public int hashCode() { return Objects.hash(allowed, reason); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AuthorizationDecision)) { return false; }
        final AuthorizationDecision that = (AuthorizationDecision) other;
        return allowed == that.allowed && reason.equals(that.reason);
    }
}
