package io.zartra.bedwars.progression.achievement;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Durable achievement tier and visibility state. */
public final class AchievementProgress {
    private final AchievementId achievementId;
    private final PlayerProgressionId playerId;
    private final int definitionVersion;
    private final int tier;
    private final long value;
    private final boolean discovered;
    private final long revision;
    private final Optional<IdempotencyKey> lastEvent;
    private final Instant updatedAt;

    /** Creates a validated achievement snapshot. */
    public AchievementProgress(final AchievementId achievementId,
                               final PlayerProgressionId playerId, final int definitionVersion,
                               final int tier, final long value, final boolean discovered,
                               final long revision, final Optional<IdempotencyKey> lastEvent,
                               final Instant updatedAt) {
        this.achievementId = Objects.requireNonNull(achievementId, "achievementId");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        if (definitionVersion < 1 || tier < 0 || value < 0 || revision < 0) {
            throw new IllegalArgumentException("invalid achievement counters");
        }
        this.definitionVersion = definitionVersion;
        this.tier = tier;
        this.value = value;
        this.discovered = discovered;
        this.revision = revision;
        this.lastEvent = Objects.requireNonNull(lastEvent, "lastEvent");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }
    /** @return achievement identity */ public AchievementId achievementId() { return achievementId; }
    /** @return player identity */ public PlayerProgressionId playerId() { return playerId; }
    /** @return definition version */ public int definitionVersion() { return definitionVersion; }
    /** @return highest completed tier */ public int tier() { return tier; }
    /** @return shared objective value */ public long value() { return value; }
    /** @return whether hidden progress is revealed */ public boolean discovered() { return discovered; }
    /** @return optimistic revision */ public long revision() { return revision; }
    /** @return last applied event */ public Optional<IdempotencyKey> lastEvent() { return lastEvent; }
    /** @return update time */ public Instant updatedAt() { return updatedAt; }
}
