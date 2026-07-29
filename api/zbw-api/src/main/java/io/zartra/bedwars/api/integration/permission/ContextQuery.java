package io.zartra.bedwars.api.integration.permission;

import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable, bounded permission/meta query context. */
public final class ContextQuery {
    private static final int MAX_CONTEXTS = 32;
    private final PlayerId playerId;
    private final Map<String, String> contexts;
    private final Instant deadline;

    /**
     * Creates a context query.
     *
     * @param playerId player identity
     * @param contexts sanitized context key/value pairs
     * @param deadline query deadline
     */
    public ContextQuery(final PlayerId playerId, final Map<String, String> contexts,
                        final Instant deadline) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        Objects.requireNonNull(contexts, "contexts");
        if (contexts.size() > MAX_CONTEXTS) {
            throw new IllegalArgumentException("too many contexts");
        }
        TreeMap<String, String> copy = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : contexts.entrySet()) {
            if (!safe(entry.getKey(), 64) || !safe(entry.getValue(), 128)) {
                throw new IllegalArgumentException("context values must be sanitized");
            }
            copy.put(entry.getKey(), entry.getValue());
        }
        this.contexts = Collections.unmodifiableMap(copy);
    }

    private static boolean safe(final String value, final int limit) {
        return value != null && !value.isEmpty() && value.length() <= limit
                && value.matches("[A-Za-z0-9_.:-]+");
    }

    /** @return player identity */
    public PlayerId playerId() { return playerId; }
    /** @return sorted immutable contexts */
    public Map<String, String> contexts() { return contexts; }
    /** @return query deadline */
    public Instant deadline() { return deadline; }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof ContextQuery)) { return false; }
        ContextQuery that = (ContextQuery) other;
        return playerId.equals(that.playerId) && contexts.equals(that.contexts)
                && deadline.equals(that.deadline);
    }

    @Override public int hashCode() { return Objects.hash(playerId, contexts, deadline); }
}
