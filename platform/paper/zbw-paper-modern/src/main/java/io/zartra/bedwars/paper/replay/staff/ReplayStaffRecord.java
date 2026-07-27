package io.zartra.bedwars.paper.replay.staff;

import io.zartra.bedwars.replay.api.ReplaySession;
import java.util.Objects;

/** Immutable staff-visible replay projection including moderation mark state. */
public final class ReplayStaffRecord {
    private final ReplaySession session;
    private final boolean marked;

    /** Creates a staff record around one immutable replay session. */
    public ReplayStaffRecord(final ReplaySession session, final boolean marked) {
        this.session = Objects.requireNonNull(session, "session");
        this.marked = marked;
    }

    /** @return immutable replay session and ordered events */ public ReplaySession session() {
        return session;
    }
    /** @return whether staff marked this replay for review */ public boolean marked() {
        return marked;
    }
    /** @return deterministic replay duration */
    public long durationMillis() {
        return session.timeline().events().isEmpty() ? 0L
                : session.timeline().events().get(
                        session.timeline().events().size() - 1).offsetMillis();
    }
}
