package io.zartra.bedwars.arena.setup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Immutable marker-discovery proposal that cannot mutate a session without explicit apply. */
public final class MarkerProposal {
    private final SetupSessionId sessionId;
    private final long draftRevision;
    private final List<SetupMutation> mutations;

    /** Creates a bounded proposal tied to the inspected draft revision. */
    public MarkerProposal(final SetupSessionId sessionId, final long draftRevision,
                          final List<SetupMutation> mutations) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        if (draftRevision < 0L) { throw new IllegalArgumentException("draftRevision is negative"); }
        this.draftRevision = draftRevision;
        final List<SetupMutation> copy = new ArrayList<SetupMutation>(
                Objects.requireNonNull(mutations, "mutations"));
        if (copy.isEmpty() || copy.size() > 256 || copy.contains(null)) {
            throw new IllegalArgumentException("proposal requires one to 256 mutations");
        }
        this.mutations = Collections.unmodifiableList(copy);
    }
    /** @return inspected session */ public SetupSessionId sessionId() { return sessionId; }
    /** @return inspected draft revision */ public long draftRevision() { return draftRevision; }
    /** @return proposed mutations in deterministic apply order */ public List<SetupMutation> mutations() { return mutations; }
    /** @return whether explicit apply may target this session revision */
    public boolean matches(final SetupSession session) {
        return sessionId.equals(session.id()) && draftRevision == session.draftRevision();
    }
}
