package io.zartra.bedwars.progression.challenge;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Durable active-window and completion state for one challenge. */
public final class ChallengeProgress {
    /** Challenge lifecycle. */
    public enum Status { /** Not started. */ AVAILABLE, /** Timer running. */ ACTIVE, /** Objective met. */ COMPLETED, /** Timer elapsed. */ EXPIRED }
    private final ChallengeId challengeId;
    private final PlayerProgressionId playerId;
    private final int definitionVersion;
    private final Status status;
    private final Instant activatedAt;
    private final Instant expiresAt;
    private final long revision;
    private final Optional<IdempotencyKey> lastEvent;

    /** Creates a validated challenge snapshot. */
    public ChallengeProgress(final ChallengeId challengeId, final PlayerProgressionId playerId,
                             final int definitionVersion, final Status status,
                             final Instant activatedAt, final Instant expiresAt,
                             final long revision, final Optional<IdempotencyKey> lastEvent) {
        this.challengeId = Objects.requireNonNull(challengeId, "challengeId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        if (definitionVersion < 1 || revision < 0) {
            throw new IllegalArgumentException("invalid challenge version or revision");
        }
        this.definitionVersion = definitionVersion;
        this.status = Objects.requireNonNull(status, "status");
        this.activatedAt = Objects.requireNonNull(activatedAt, "activatedAt");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
        if (!expiresAt.isAfter(activatedAt)) { throw new IllegalArgumentException("invalid challenge window"); }
        this.revision = revision;
        this.lastEvent = Objects.requireNonNull(lastEvent, "lastEvent");
    }
    /** @return challenge identity */ public ChallengeId challengeId() { return challengeId; }
    /** @return player identity */ public PlayerProgressionId playerId() { return playerId; }
    /** @return definition version */ public int definitionVersion() { return definitionVersion; }
    /** @return lifecycle status */ public Status status() { return status; }
    /** @return activation instant */ public Instant activatedAt() { return activatedAt; }
    /** @return expiration instant */ public Instant expiresAt() { return expiresAt; }
    /** @return optimistic revision */ public long revision() { return revision; }
    /** @return last applied event */ public Optional<IdempotencyKey> lastEvent() { return lastEvent; }
}
