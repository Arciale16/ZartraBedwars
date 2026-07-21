package io.zartra.bedwars.progression.objective;

import io.zartra.bedwars.progression.model.AuditMetadata;
import java.time.Instant;
import java.util.Objects;

/** Stateless deterministic evaluator for versioned objective definitions. */
public final class ObjectiveExecutionEngine {
    /** Evaluates one event without performing persistence or side effects. */
    public Evaluation evaluate(final ObjectiveDefinition definition,
                               final ObjectiveRuntimeState current,
                               final ObjectiveEvent event, final boolean repeatable,
                               final Instant now) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(now, "now");
        requireCompatible(definition, current, event);
        if (current.lastEvent().isPresent()
                && current.lastEvent().get().equals(event.idempotencyKey())) {
            return new Evaluation(current, false, false, true, false);
        }
        if (current.expiresAt().isPresent() && !now.isBefore(current.expiresAt().get())) {
            return new Evaluation(copy(current, current.value(), current.completionCount(),
                    ObjectiveRuntimeState.Status.EXPIRED, event, now), false, true, false, true);
        }
        if (current.status() != ObjectiveRuntimeState.Status.ACTIVE) {
            return new Evaluation(current, false,
                    current.status() == ObjectiveRuntimeState.Status.EXPIRED, false, false);
        }
        if (!matches(definition, event)) {
            return new Evaluation(current, false, false, false, false);
        }
        final long accumulated = safeAdd(current.value(), event.amount());
        final boolean completed = accumulated >= definition.target();
        final long value = completed && repeatable ? accumulated % definition.target()
                : Math.min(accumulated, definition.target());
        final long completions = completed
                ? Math.addExact(current.completionCount(), accumulated / definition.target())
                : current.completionCount();
        final ObjectiveRuntimeState.Status status = completed && !repeatable
                ? ObjectiveRuntimeState.Status.COMPLETED : ObjectiveRuntimeState.Status.ACTIVE;
        return new Evaluation(copy(current, value, completions, status, event, now),
                completed, false, false, true);
    }

    private static void requireCompatible(final ObjectiveDefinition definition,
                                          final ObjectiveRuntimeState current,
                                          final ObjectiveEvent event) {
        if (!definition.id().equals(current.objectiveId())
                || definition.version() != current.definitionVersion()
                || !current.playerId().equals(event.playerId())) {
            throw new IllegalArgumentException("definition, state and event do not identify one projection");
        }
    }

    private static boolean matches(final ObjectiveDefinition definition, final ObjectiveEvent event) {
        if (!definition.eventType().equals(event.type())) { return false; }
        for (ObjectiveFilter filter : definition.filters()) {
            final String key = filter.dimension().name().toLowerCase(java.util.Locale.ROOT);
            if (!filter.expectedValue().equals(event.attributes().get(key))) { return false; }
        }
        return true;
    }

    private static long safeAdd(final long left, final long right) {
        try { return Math.addExact(left, right); }
        catch (ArithmeticException overflow) { return Long.MAX_VALUE; }
    }

    private static ObjectiveRuntimeState copy(final ObjectiveRuntimeState current,
                                              final long value, final long completions,
                                              final ObjectiveRuntimeState.Status status,
                                              final ObjectiveEvent event, final Instant now) {
        final AuditMetadata audit = new AuditMetadata(current.audit().actor(),
                event.audit().correlationId(), current.audit().createdAt(), now);
        return new ObjectiveRuntimeState(current.objectiveId(), current.playerId(),
                current.definitionVersion(), value, completions, status,
                Math.addExact(current.revision(), 1), java.util.Optional.of(event.idempotencyKey()),
                current.expiresAt(), audit);
    }

    /** Immutable evaluation result. */
    public static final class Evaluation {
        private final ObjectiveRuntimeState state;
        private final boolean completed;
        private final boolean expired;
        private final boolean duplicate;
        private final boolean changed;
        private Evaluation(final ObjectiveRuntimeState state, final boolean completed,
                           final boolean expired, final boolean duplicate, final boolean changed) {
            this.state = state;
            this.completed = completed;
            this.expired = expired;
            this.duplicate = duplicate;
            this.changed = changed;
        }
        /** @return resulting state */ public ObjectiveRuntimeState state() { return state; }
        /** @return whether this event completed at least one cycle */ public boolean completed() { return completed; }
        /** @return whether expiration was observed */ public boolean expired() { return expired; }
        /** @return whether the event was already applied */ public boolean duplicate() { return duplicate; }
        /** @return whether persistence is required */ public boolean changed() { return changed; }
    }
}
