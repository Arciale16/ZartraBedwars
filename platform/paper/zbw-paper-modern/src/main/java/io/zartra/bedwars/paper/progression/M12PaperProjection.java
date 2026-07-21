package io.zartra.bedwars.paper.progression;

import io.zartra.bedwars.api.identity.PlayerId;
import java.util.Objects;

/** Java 21 owner-thread projection of committed M12 feedback and reward intents. */
public final class M12PaperProjection {
    private final OwnerThread ownerThread;
    private final Platform platform;

    /** Creates a projection that cannot mutate Paper off its owning thread. */
    public M12PaperProjection(final OwnerThread ownerThread, final Platform platform) {
        this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
        this.platform = Objects.requireNonNull(platform, "platform");
    }

    /** Projects localized progression feedback without performing progression calculations. */
    public void feedback(final Feedback feedback) {
        requireOwner();
        platform.feedback(Objects.requireNonNull(feedback, "feedback"));
    }

    /** Opens an already loaded M09 inventory page. */
    public void open(final PlayerId player, final Object inventoryView) {
        requireOwner();
        platform.open(Objects.requireNonNull(player, "player"),
                Objects.requireNonNull(inventoryView, "inventoryView"));
    }

    /** Clears M12-owned transient projections when a player or match lifecycle ends. */
    public void clear(final PlayerId player) {
        requireOwner();
        platform.clear(Objects.requireNonNull(player, "player"));
    }

    private void requireOwner() {
        if (!ownerThread.isOwnerThread()) {
            throw new IllegalStateException("M12 Paper mutation attempted off owner thread");
        }
    }

    /** Owner-thread predicate supplied by the Paper bootstrap. */
    public interface OwnerThread {
        /** @return whether the current thread owns the affected Paper state */
        boolean isOwnerThread();
    }

    /** Closed platform translation boundary; it contains no progression or reward policy. */
    public interface Platform {
        /** Renders one configured feedback intent. */
        void feedback(Feedback feedback);
        /** Opens one M09-rendered inventory view. */
        void open(PlayerId player, Object inventoryView);
        /** Clears action bars, XP projections and pending inventory state owned by M12. */
        void clear(PlayerId player);
    }

    /** Immutable semantic feedback intent produced only after an M12 operation commits. */
    public static final class Feedback {
        private final PlayerId player;
        private final Kind kind;
        private final String messageKey;
        private final String soundKey;
        private final float progress;

        /** Creates bounded localized feedback with an optional configured sound. */
        public Feedback(final PlayerId player, final Kind kind, final String messageKey,
                        final String soundKey, final float progress) {
            this.player = Objects.requireNonNull(player, "player");
            this.kind = Objects.requireNonNull(kind, "kind");
            if (messageKey == null || !messageKey.matches("[a-z0-9][a-z0-9._-]{1,127}")) {
                throw new IllegalArgumentException("invalid message key");
            }
            if (soundKey != null && !soundKey.matches("[A-Z][A-Z0-9_]{1,95}")) {
                throw new IllegalArgumentException("invalid sound key");
            }
            if (!Float.isFinite(progress) || progress < 0F || progress > 1F) {
                throw new IllegalArgumentException("progress outside unit interval");
            }
            this.messageKey = messageKey;
            this.soundKey = soundKey;
            this.progress = progress;
        }

        /** @return target player */ public PlayerId player() { return player; }
        /** @return projection kind */ public Kind kind() { return kind; }
        /** @return localization key */ public String messageKey() { return messageKey; }
        /** @return optional configured sound enum key */ public String soundKey() { return soundKey; }
        /** @return normalized XP progress, used only for XP-bar feedback */ public float progress() { return progress; }
    }

    /** Supported feedback projections with identical neutral semantics. */
    public enum Kind {
        /** Chat message. */ MESSAGE,
        /** Action bar. */ ACTION_BAR,
        /** Title and subtitle. */ TITLE,
        /** XP progress bar. */ XP_BAR,
        /** Reward claim effect. */ REWARD,
        /** Level or prestige transition. */ TRANSITION,
        /** Persistent-currency mutation. */ CURRENCY
    }
}
