package io.zartra.bedwars.progression.pass;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.progression.model.PlayerProgressionId;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;

/** Deterministic free-track battle-pass XP and tier projection policy. */
public final class BattlePassRuntime {
    /** Creates a zero-XP player season while the season is active. */
    public SeasonProgress start(final BattlePassDefinition definition,
                                final PlayerProgressionId playerId, final Instant now) {
        requireActive(definition, now);
        return new SeasonProgress(definition.id(), playerId, definition.version(), 0, 0,
                java.util.Collections.<Integer>emptySet(), 0, Optional.empty(), now);
    }

    /** Adds positive season XP exactly once and recalculates the unlocked tier. */
    public SeasonProgress addExperience(final BattlePassDefinition definition,
                                        final SeasonProgress current, final long amount,
                                        final IdempotencyKey key, final Instant now) {
        requireCompatible(definition, current);
        requireActive(definition, now);
        if (amount < 1) { throw new IllegalArgumentException("amount must be positive"); }
        if (current.lastEvent().isPresent() && current.lastEvent().get().equals(key)) { return current; }
        final long experience;
        try { experience = Math.addExact(current.experience(), amount); }
        catch (ArithmeticException overflow) { throw new IllegalArgumentException("season XP overflow", overflow); }
        int tier = 0;
        for (BattlePassDefinition.Tier candidate : definition.tiers()) {
            if (experience >= candidate.requiredXp()) { tier = candidate.number(); }
        }
        return new SeasonProgress(current.seasonId(), current.playerId(), current.definitionVersion(),
                experience, tier, current.claimedFreeTiers(), Math.addExact(current.revision(), 1),
                Optional.of(Objects.requireNonNull(key, "key")), now);
    }

    /** Marks one free-track tier claimed; reward delivery remains owned by M12. */
    public SeasonProgress claimFreeTier(final BattlePassDefinition definition,
                                        final SeasonProgress current, final int tier,
                                        final IdempotencyKey key, final Instant now) {
        requireCompatible(definition, current);
        if (tier < 1 || tier > current.tier()) { throw new IllegalArgumentException("tier is not unlocked"); }
        if (!now.isBefore(definition.graceEndsAt())) { throw new IllegalStateException("claim grace has ended"); }
        if (current.claimedFreeTiers().contains(tier)) { return current; }
        final LinkedHashSet<Integer> claimed = new LinkedHashSet<Integer>(current.claimedFreeTiers());
        claimed.add(tier);
        return new SeasonProgress(current.seasonId(), current.playerId(), current.definitionVersion(),
                current.experience(), current.tier(), claimed, Math.addExact(current.revision(), 1),
                Optional.of(Objects.requireNonNull(key, "key")), now);
    }

    private static void requireActive(final BattlePassDefinition definition, final Instant now) {
        if (now.isBefore(definition.startsAt()) || !now.isBefore(definition.endsAt())) {
            throw new IllegalStateException("season is not active");
        }
    }
    private static void requireCompatible(final BattlePassDefinition definition,
                                          final SeasonProgress current) {
        if (!definition.id().equals(current.seasonId())
                || definition.version() != current.definitionVersion()) {
            throw new IllegalArgumentException("definition does not match season progress");
        }
    }
}
