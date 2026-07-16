package io.zartra.bedwars.game.model;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Deterministic configurable timeline for timeout, sudden-death, dragon, border and custom events. */
public final class GamePhasePolicy {
    private final List<ScheduledEvent> schedule;
    private final Set<DefinitionId> fired = new HashSet<DefinitionId>();

    /** Creates a strictly ordered event schedule with unique IDs and offsets. */
    public GamePhasePolicy(final List<ScheduledEvent> schedule) {
        final List<ScheduledEvent> copy = new ArrayList<ScheduledEvent>(
                Objects.requireNonNull(schedule, "schedule"));
        if (copy.isEmpty() || copy.contains(null)) {
            throw new IllegalArgumentException("phase schedule cannot be empty");
        }
        Collections.sort(copy);
        final Set<DefinitionId> ids = new HashSet<DefinitionId>();
        Duration previous = null;
        for (ScheduledEvent event : copy) {
            if (!ids.add(event.id) || previous != null && previous.equals(event.offset)) {
                throw new IllegalArgumentException("phase event IDs and offsets must be unique");
            }
            previous = event.offset;
        }
        this.schedule = Collections.unmodifiableList(copy);
    }

    /** Emits every newly due event exactly once in schedule order. */
    public synchronized List<ScheduledEvent> due(final Instant startedAt, final Instant now) {
        final Duration elapsed = Duration.between(Objects.requireNonNull(startedAt, "startedAt"),
                Objects.requireNonNull(now, "now"));
        if (elapsed.isNegative()) { throw new IllegalArgumentException("now precedes match start"); }
        final List<ScheduledEvent> due = new ArrayList<ScheduledEvent>();
        for (ScheduledEvent event : schedule) {
            if (event.offset.compareTo(elapsed) <= 0 && fired.add(event.id)) { due.add(event); }
        }
        return Collections.unmodifiableList(due);
    }

    /** Resets the exactly-once fence when an arena is safely reused. */
    public synchronized void reset() { fired.clear(); }
    /** @return immutable configured schedule */ public List<ScheduledEvent> schedule() { return schedule; }

    /** Event types implemented generically in M08; mode-specific effects remain M10. */
    public enum Type { /** Match timeout. */ TIMEOUT, /** Escalation phase. */ SUDDEN_DEATH, /** Dragon spawn intent. */ DRAGON, /** Border shrink intent. */ BORDER, /** Configured extension event. */ CUSTOM }

    /** Immutable scheduled semantic event. */
    public static final class ScheduledEvent implements Comparable<ScheduledEvent> {
        private final DefinitionId id;
        private final Type type;
        private final Duration offset;
        private final DefinitionId payload;
        /** Creates one positive bounded timeline event. */
        public ScheduledEvent(final DefinitionId id, final Type type, final Duration offset,
                              final DefinitionId payload) {
            if (offset == null || offset.isNegative() || offset.compareTo(Duration.ofHours(6)) > 0) {
                throw new IllegalArgumentException("event offset is invalid");
            }
            this.id = Objects.requireNonNull(id, "id");
            this.type = Objects.requireNonNull(type, "type");
            this.offset = offset;
            this.payload = payload;
            if ((type == Type.CUSTOM) != (payload != null)) {
                throw new IllegalArgumentException("only custom events require a payload");
            }
        }
        /** @return stable event identity */ public DefinitionId id() { return id; }
        /** @return semantic event type */ public Type type() { return type; }
        /** @return elapsed-match trigger offset */ public Duration offset() { return offset; }
        /** @return custom payload when applicable */ public Optional<DefinitionId> payload() { return Optional.ofNullable(payload); }
        @Override public int compareTo(final ScheduledEvent other) { return offset.compareTo(other.offset); }
    }
}
