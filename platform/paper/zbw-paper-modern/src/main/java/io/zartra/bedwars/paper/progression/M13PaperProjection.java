package io.zartra.bedwars.paper.progression;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Objects;

/** Owner-thread-only Paper projection for committed M13 lifecycle and reward feedback. */
public final class M13PaperProjection {
    private final OwnerThread ownerThread;
    private final Platform platform;

    /** Creates a closed projection boundary with no objective or reward policy. */
    public M13PaperProjection(final OwnerThread ownerThread, final Platform platform) {
        this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    /** Renders a committed semantic intent through configured title/action-bar/sound/particles. */
    public void feedback(final Feedback feedback) {
        requireOwner();
        platform.feedback(Objects.requireNonNull(feedback, "feedback"));
    }

    /** Opens an inventory already rendered by zbw-ui-paper. */
    public void open(final PlayerId player, final Object inventory) {
        requireOwner();
        platform.open(Objects.requireNonNull(player, "player"), Objects.requireNonNull(inventory, "inventory"));
    }

    /** Removes M13 transient effects on close, disconnect, match end or shutdown. */
    public void clear(final PlayerId player) {
        requireOwner();
        platform.clear(Objects.requireNonNull(player, "player"));
    }

    private void requireOwner() {
        if (!ownerThread.isOwnerThread()) {
            throw new IllegalStateException("M13 Paper mutation attempted off owner thread");
        }
    }

    /** Owner-thread predicate supplied by the Paper composition root. */
    public interface OwnerThread { /** @return true only on the owning Paper thread */ boolean isOwnerThread(); }
    /** Platform renderer; implementations perform presentation only. */
    public interface Platform {
        /** Applies title/action-bar/message, sound and optional particles. */ void feedback(Feedback feedback);
        /** Opens one rendered inventory. */ void open(PlayerId player, Object inventory);
        /** Clears transient state. */ void clear(PlayerId player);
    }

    /** Immutable bounded feedback produced after durable completion or delivery. */
    public record Feedback(PlayerId player, Kind kind, String messageKey, String soundKey,
                           String particleKey, float progress) {
        /** Validates safe localization and Bukkit enum keys without resolving platform types. */
        public Feedback {
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
            if (!Float.isFinite(progress) || progress < 0F || progress > 1F) {
                throw new IllegalArgumentException("progress outside unit interval");
            }
        }
    }

    /** M13 feedback categories selected by the neutral runtime. */
    public enum Kind { OBJECTIVE, QUEST, ACHIEVEMENT, CHALLENGE, BATTLE_PASS, REWARD }
}
