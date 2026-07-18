package io.zartra.bedwars.paper.game;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.shop.generator.GeneratorBatch;
import io.zartra.bedwars.shop.item.ItemActionRequest;
import io.zartra.bedwars.shop.item.UtilityItemDefinition;
import io.zartra.bedwars.shop.upgrade.TeamEffectIntent;
import java.util.Objects;

/** Java 21 owner-thread translation of already validated M11 application intents. */
public final class M11PaperProjection {
    private final OwnerThread ownerThread;
    private final Platform platform;
    /** Creates a closed Paper projection with no feature policy. */
    public M11PaperProjection(final OwnerThread ownerThread, final Platform platform) {
        this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
        this.platform = Objects.requireNonNull(platform, "platform");
    }
    /** Projects a committed generator batch to its configured semantic delivery. */
    public void deliver(final GeneratorBatch batch) {
        requireOwner();
        platform.deliver(Objects.requireNonNull(batch, "batch"));
    }
    /** Applies one validated team-upgrade, trap or forge intent. */
    public void apply(final TeamEffectIntent intent) {
        requireOwner();
        platform.apply(Objects.requireNonNull(intent, "intent"));
    }
    /** Applies one validated utility action and its bounded semantic parameters. */
    public boolean apply(final DefinitionId effect, final UtilityItemDefinition definition,
                         final ItemActionRequest request) {
        requireOwner();
        return platform.apply(Objects.requireNonNull(effect, "effect"),
                Objects.requireNonNull(definition, "definition"),
                Objects.requireNonNull(request, "request"));
    }
    /** Clears every M11-owned inventory, block, entity and effect projection for a player. */
    public void clear(final PlayerId playerId) {
        requireOwner();
        platform.clear(Objects.requireNonNull(playerId, "playerId"));
    }
    private void requireOwner() {
        if (!ownerThread.isOwnerThread()) {
            throw new IllegalStateException("M11 Paper mutation attempted off owner thread");
        }
    }
    /** Owner-thread predicate supplied by the Paper bootstrap. */
    public interface OwnerThread { /** @return whether current thread owns mutation */ boolean isOwnerThread(); }
    /** Closed platform mutation surface containing no M11 business rules. */
    public interface Platform {
        /** Delivers one committed generator batch. */ void deliver(GeneratorBatch batch);
        /** Applies one team effect intent. */ void apply(TeamEffectIntent intent);
        /** Applies one utility effect. */ boolean apply(DefinitionId effect,
                                                         UtilityItemDefinition definition,
                                                         ItemActionRequest request);
        /** Clears player-owned M11 projections. */ void clear(PlayerId playerId);
    }
}
