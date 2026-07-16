package io.zartra.bedwars.game.addon;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.game.model.PlayerStateSnapshot;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Bounded exactly-once policy for arena-owned items crossing a configured loss boundary. */
public final class AntiDropPolicy {
    private final int fenceCapacity;
    private final Set<IdempotencyKey> processed = new HashSet<IdempotencyKey>();
    private final Deque<IdempotencyKey> order = new ArrayDeque<IdempotencyKey>();

    /** Creates a bounded capture fence. */
    public AntiDropPolicy(final int fenceCapacity) {
        if (fenceCapacity < 16 || fenceCapacity > 100000) {
            throw new IllegalArgumentException("fence capacity must be between 16 and 100000");
        }
        this.fenceCapacity = fenceCapacity;
    }

    /** Resolves a capture atomically; duplicate and stale observations never redeliver. */
    public synchronized Outcome capture(final Observation observation, final Rules rules,
                                        final boolean recipientHasCapacity) {
        Objects.requireNonNull(observation, "observation");
        Objects.requireNonNull(rules, "rules");
        if (processed.contains(observation.captureId)) { return Outcome.of(Status.DUPLICATE, null); }
        remember(observation.captureId);
        if (!observation.lossBoundary || observation.pickedUp || observation.resetting
                || observation.age.compareTo(rules.maximumAge) > 0
                || !rules.allowedItems.contains(observation.item.itemId())
                || "true".equals(observation.item.metadata().get("zartra.synthetic"))) {
            return Outcome.of(Status.FILTERED, null);
        }
        final PlayerId recipient = choose(observation, rules);
        if (recipient == null) { return Outcome.of(Status.NO_RECIPIENT, null); }
        return Outcome.of(recipientHasCapacity ? Status.DELIVER : Status.PENDING_GRANT, recipient);
    }

    /** @return current bounded fence size for diagnostics */
    public synchronized int fenceSize() { return processed.size(); }

    private void remember(final IdempotencyKey key) {
        processed.add(key);
        order.addLast(key);
        if (order.size() > fenceCapacity) { processed.remove(order.removeFirst()); }
    }

    private static PlayerId choose(final Observation observation, final Rules rules) {
        for (Recipient candidate : rules.recipients) {
            final PlayerId selected;
            switch (candidate) {
                case OWNER:
                    selected = observation.owner;
                    break;
                case KILLER:
                    selected = observation.killer;
                    break;
                case TEAMMATE:
                    selected = observation.teammate;
                    break;
                case NEAREST:
                    selected = observation.nearest;
                    break;
                default: selected = null;
            }
            if (selected != null && !observation.disconnected.contains(selected)) { return selected; }
        }
        return null;
    }

    /** Immutable event translation with captured item identity and race facts. */
    public static final class Observation {
        private final IdempotencyKey captureId;
        private final PlayerStateSnapshot.Item item;
        private final PlayerId owner;
        private final PlayerId killer;
        private final PlayerId teammate;
        private final PlayerId nearest;
        private final Set<PlayerId> disconnected;
        private final Duration age;
        private final boolean lossBoundary;
        private final boolean pickedUp;
        private final boolean resetting;
        /** Creates a capture observation. Nullable candidates mean unavailable. */
        public Observation(final IdempotencyKey captureId, final PlayerStateSnapshot.Item item,
                           final PlayerId owner, final PlayerId killer,
                           final PlayerId teammate, final PlayerId nearest,
                           final Set<PlayerId> disconnected, final Duration age,
                           final boolean lossBoundary, final boolean pickedUp,
                           final boolean resetting) {
            this.captureId = Objects.requireNonNull(captureId, "captureId");
            this.item = Objects.requireNonNull(item, "item");
            this.owner = owner;
            this.killer = killer;
            this.teammate = teammate;
            this.nearest = nearest;
            this.disconnected = Collections.unmodifiableSet(new HashSet<PlayerId>(
                    Objects.requireNonNull(disconnected, "disconnected")));
            this.age = Objects.requireNonNull(age, "age");
            if (age.isNegative()) { throw new IllegalArgumentException("capture age cannot be negative"); }
            this.lossBoundary = lossBoundary;
            this.pickedUp = pickedUp;
            this.resetting = resetting;
        }
    }

    /** Immutable item filters and recipient priority. */
    public static final class Rules {
        private final Set<DefinitionId> allowedItems;
        private final java.util.List<Recipient> recipients;
        private final Duration maximumAge;
        /** Creates validated capture rules. */
        public Rules(final Set<DefinitionId> allowedItems,
                     final java.util.List<Recipient> recipients, final Duration maximumAge) {
            if (allowedItems == null || allowedItems.isEmpty() || allowedItems.contains(null)
                    || recipients == null || recipients.isEmpty() || recipients.contains(null)
                    || new HashSet<Recipient>(recipients).size() != recipients.size()
                    || maximumAge == null || maximumAge.isNegative()
                    || maximumAge.compareTo(Duration.ofMinutes(10)) > 0) {
                throw new IllegalArgumentException("anti-drop rules are invalid");
            }
            this.allowedItems = Collections.unmodifiableSet(new HashSet<DefinitionId>(allowedItems));
            this.recipients = Collections.unmodifiableList(new java.util.ArrayList<Recipient>(recipients));
            this.maximumAge = maximumAge;
        }
    }

    /** Recipient resolution options. */
    public enum Recipient { /** Original owner. */ OWNER, /** Killer. */ KILLER, /** Eligible team member. */ TEAMMATE, /** Nearest eligible player. */ NEAREST }
    /** Capture dispositions. */
    public enum Status { /** Apply inventory delivery. */ DELIVER, /** Persist recoverable grant. */ PENDING_GRANT, /** Already fenced. */ DUPLICATE, /** Rejected by filters/race. */ FILTERED, /** No live eligible recipient. */ NO_RECIPIENT }
    /** Immutable capture result and optional recipient. */
    public static final class Outcome {
        private final Status status;
        private final PlayerId recipient;
        private Outcome(final Status status, final PlayerId recipient) {
            this.status = status;
            this.recipient = recipient;
        }
        private static Outcome of(final Status status, final PlayerId player) { return new Outcome(status, player); }
        /** @return typed disposition */ public Status status() { return status; }
        /** @return resolved recipient when present */ public Optional<PlayerId> recipient() { return Optional.ofNullable(recipient); }
    }
}
