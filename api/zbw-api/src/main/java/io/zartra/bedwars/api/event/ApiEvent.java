package io.zartra.bedwars.api.event;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Base contract for an immutable public event payload. */
public interface ApiEvent {
    /** @return immutable metadata captured before publication */
    EventMetadata metadata();

    /** Marker for an event whose pre-commit listeners may return a cancellation decision. */
    interface Cancellable extends ApiEvent {
        /**
         * @return stable policy ID describing what a cancellation prevents; cancellation never
         * rewinds a transition that has already committed
         */
        DefinitionId cancellationPolicy();
    }

    /** Listener contract. Implementations must obey the event's documented thread context. */
    interface Listener<E extends ApiEvent> {
        /**
         * Handles one immutable event.
         *
         * @param event event payload
         * @return continue or cancel decision; cancel is valid only for {@link Cancellable}
         */
        Decision onEvent(E event);
    }

    /** Immutable listener decision. */
    final class Decision {
        private static final Decision CONTINUE = new Decision(null);
        private final DefinitionId cancellationReason;
        private Decision(final DefinitionId cancellationReason) { this.cancellationReason = cancellationReason; }
        /** @return shared immutable continue decision */
        public static Decision proceed() { return CONTINUE; }
        /** @return cancellation decision with a stable, localizable reason code */
        public static Decision cancel(final DefinitionId reason) {
            return new Decision(Objects.requireNonNull(reason, "reason"));
        }
        /** @return whether dispatch should stop and the pending transition should not commit */
        public boolean isCancellation() { return cancellationReason != null; }
        /** @return reason code, or {@code null} only for {@link #proceed()} */
        public DefinitionId cancellationReasonOrNull() { return cancellationReason; }
    }
}
