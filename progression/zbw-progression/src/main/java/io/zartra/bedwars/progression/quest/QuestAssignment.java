package io.zartra.bedwars.progression.quest;

import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.time.Instant;
import java.util.Objects;

/** Immutable lifecycle snapshot for one player's quest assignment. */
public final class QuestAssignment {
    /** Assignment lifecycle state. */
    public enum Status {
        /** Requirements are unmet. */ LOCKED, /** Progress may be accumulated. */ ACTIVE,
        /** Objective is complete. */ COMPLETED, /** Rewards were claimed. */ CLAIMED,
        /** Assignment expired. */ EXPIRED, /** Player abandoned it. */ ABANDONED
    }

    private final QuestId questId;
    private final PlayerProgressionId playerId;
    private final Status status;
    private final Instant assignedAt;
    private final Instant expiresAt;
    private final long revision;

    /** Creates a validated assignment snapshot. */
    public QuestAssignment(final QuestId questId, final PlayerProgressionId playerId,
                           final Status status, final Instant assignedAt,
                           final Instant expiresAt, final long revision) {
        this.questId = Objects.requireNonNull(questId, "questId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.status = Objects.requireNonNull(status, "status");
        this.assignedAt = Objects.requireNonNull(assignedAt, "assignedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(assignedAt)) {
            throw new IllegalArgumentException("expiresAt must follow assignedAt");
        }
        if (revision < 0) { throw new IllegalArgumentException("revision must not be negative"); }
        this.revision = revision;
    }

    /** @return quest identity */ public QuestId questId() { return questId; }
    /** @return player identity */ public PlayerProgressionId playerId() { return playerId; }
    /** @return lifecycle status */ public Status status() { return status; }
    /** @return assignment time */ public Instant assignedAt() { return assignedAt; }
    /** @return expiration time */ public Instant expiresAt() { return expiresAt; }
    /** @return optimistic revision */ public long revision() { return revision; }
}
