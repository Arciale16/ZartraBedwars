package io.zartra.bedwars.paper.progression;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Objects;

/** Owner-thread-only, budgeted Paper projection for committed M14 cosmetic/profile/campaign intents. */
public final class M14PaperProjection {
    private final OwnerThread ownerThread;
    private final Platform platform;
    private final Budget budget;
    /** Creates a presentation-only projection. */
    public M14PaperProjection(final OwnerThread ownerThread, final Platform platform, final Budget budget) {
        this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
        this.platform = Objects.requireNonNull(platform, "platform");
        this.budget = Objects.requireNonNull(budget, "budget");
    }
    /** Renders an already committed semantic intent on the owning Paper thread. */
    public void feedback(final Feedback feedback) {
        requireOwner();
        final Feedback value = Objects.requireNonNull(feedback, "feedback");
        if (value.particleCount() > budget.maximumParticles() || value.entityCount() > budget.maximumEntities()) {
            throw new IllegalArgumentException("M14 effect budget exceeded");
        }
        platform.feedback(value);
    }
    /** Opens a rendered inventory on the owner thread. */
    public void open(final PlayerId player, final Object inventory) {
        requireOwner();
        platform.open(Objects.requireNonNull(player, "player"), Objects.requireNonNull(inventory, "inventory"));
    }
    /** Clears transient effects on close, disconnect, match end or shutdown. */
    public void clear(final PlayerId player) {
        requireOwner();
        platform.clear(Objects.requireNonNull(player, "player"));
    }
    private void requireOwner() {
        if (!ownerThread.isOwnerThread()) {
            throw new IllegalStateException("M14 Paper mutation attempted off owner thread");
        }
    }
    /** Owner-thread predicate supplied by the composition root. */
    public interface OwnerThread {
        /** @return true only on the owner thread */
        boolean isOwnerThread();
    }

    /** Platform renderer performs no M14 business decision. */
    public interface Platform {
        /** Renders feedback. */
        void feedback(Feedback feedback);

        /** Opens UI. */
        void open(PlayerId player, Object inventory);

        /** Clears effects. */
        void clear(PlayerId player);
    }
    /** Per-intent effect budget. */ public record Budget(int maximumParticles, int maximumEntities) {
        /** Validates bounded budgets. */ public Budget {
            if (maximumParticles < 0 || maximumEntities < 0) {
                throw new IllegalArgumentException("negative budget");
            }
        }
    }
    /** Committed feedback intent. */ public record Feedback(PlayerId player, Kind kind, String messageKey,
                                                              String soundKey, String particleKey, int particleCount,
                                                              int entityCount) {
        /** Validates platform-safe identifiers and bounded counts. */ public Feedback {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(kind, "kind");
            if (messageKey == null || !messageKey.matches("[a-z0-9][a-z0-9._-]{1,127}")) {
                throw new IllegalArgumentException("invalid message key");
            }
            if (soundKey != null && !soundKey.matches("[A-Z][A-Z0-9_]{1,95}")) {
                throw new IllegalArgumentException("invalid sound key");
            }
            if (particleKey != null && !particleKey.matches("[A-Z][A-Z0-9_]{1,95}")) {
                throw new IllegalArgumentException("invalid particle key");
            }
            if (particleCount < 0 || entityCount < 0) {
                throw new IllegalArgumentException("negative effect count");
            }
        }
    }
    /** M14 feedback categories. */ public enum Kind { COSMETIC, PROFILE, CAMPAIGN }
}
