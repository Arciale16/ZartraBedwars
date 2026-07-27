package io.zartra.bedwars.replay.api;

import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.PlayerId;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/** Immutable identity, timing, participant and format metadata (ZBW-REPLAY-001). */
public final class ReplayMetadata {
    private final ReplayId replayId;
    private final MatchId matchId;
    private final Instant createdAt;
    private final int formatVersion;
    private final Set<PlayerId> participants;
    private final boolean protectedEvidence;

    /** Creates validated metadata without payload/provider concerns. */
    public ReplayMetadata(final ReplayId replayId, final MatchId matchId, final Instant createdAt,
                          final int formatVersion, final Set<PlayerId> participants,
                          final boolean protectedEvidence) {
        if (formatVersion < 1) { throw new IllegalArgumentException("formatVersion must be positive"); }
        final Set<PlayerId> copy = new LinkedHashSet<PlayerId>(
                Objects.requireNonNull(participants, "participants"));
        if (copy.contains(null)) { throw new IllegalArgumentException("participants cannot contain null"); }
        this.replayId = Objects.requireNonNull(replayId, "replayId");
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.formatVersion = formatVersion;
        this.participants = Collections.unmodifiableSet(copy);
        this.protectedEvidence = protectedEvidence;
    }

    /** Returns replay identity. */ public ReplayId replayId() { return replayId; }
    /** Returns source match identity. */ public MatchId matchId() { return matchId; }
    /** Returns creation instant. */ public Instant createdAt() { return createdAt; }
    /** Returns independently versioned replay format. */ public int formatVersion() { return formatVersion; }
    /** Returns an immutable participant set. */ public Set<PlayerId> participants() { return participants; }
    /** Returns whether ordinary participant visibility is forbidden. */
    public boolean protectedEvidence() { return protectedEvidence; }
}
