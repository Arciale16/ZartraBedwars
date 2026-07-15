package io.zartra.bedwars.api.recovery;

import io.zartra.bedwars.api.event.ApiEvent;
import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.failure.FailureReport;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.result.Result;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Platform-neutral recovery markers, persistence port, ordered steps and public event. */
public final class Recovery {
    private Recovery() { throw new AssertionError("No instances"); }
    /** Monotonic recovery state. */
    public enum State {
        /** Failure detected and completion fenced. */ DETECTED,
        /** Runtime mutation admission stopped. */ QUIESCED,
        /** Players safely routed by a later platform step. */ PLAYERS_ROUTED,
        /** Authoritative data reconciled idempotently. */ RECONCILED,
        /** Recovery completed once. */ RECOVERED,
        /** Automated recovery stopped for explicit administrator action. */ MANUAL_REQUIRED
    }
    /** Durable compare-and-set recovery marker. */
    public static final class Marker {
        private final MatchId matchId;
        private final IdempotencyKey idempotencyKey;
        private final State state;
        private final long revision;
        private final Instant updatedAt;
        /** Creates a validated marker. */
        public Marker(final MatchId matchId, final IdempotencyKey idempotencyKey,
                      final State state, final long revision, final Instant updatedAt) {
            if (revision < 0) { throw new IllegalArgumentException("revision cannot be negative"); }
            this.matchId = Objects.requireNonNull(matchId, "matchId");
            this.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey");
            this.state = Objects.requireNonNull(state, "state");
            this.revision = revision;
            this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        }
        /** @return match identity */ public MatchId matchId() { return matchId; }
        /** @return duplicate-completion fence */ public IdempotencyKey idempotencyKey() { return idempotencyKey; }
        /** @return recovery state */ public State state() { return state; }
        /** @return compare-and-set revision */ public long revision() { return revision; }
        /** @return last transition instant */ public Instant updatedAt() { return updatedAt; }
        /** @return next immutable revision */
        public Marker advance(final State nextState, final Instant instant) {
            Objects.requireNonNull(nextState, "nextState");
            if (state == State.RECOVERED || state == State.MANUAL_REQUIRED) {
                throw new IllegalStateException("terminal recovery marker cannot advance");
            }
            return new Marker(matchId, idempotencyKey, nextState, revision + 1L, instant);
        }
    }
    /** Persistence port implemented by a later storage composition adapter. */
    public interface MarkerStore {
        /**
         * Persists with compare-and-set semantics.
         * @param marker next marker
         * @param expectedPreviousRevision minus one only for first creation
         * @return persisted marker or conflict failure
         */
        Result<Marker> save(Marker marker, long expectedPreviousRevision);
    }
    /** One idempotent ordered recovery step. */
    public interface Step {
        /** @return stable step ID */ DefinitionId id();
        /** @return next monotonic state or structured failure */
        Result<State> execute(Marker current);
    }
    /** Immutable final recovery result. */
    public static final class Report {
        private final Marker marker;
        private final List<FailureReport> failures;
        /** Creates a report. */
        public Report(final Marker marker, final List<FailureReport> failures) {
            this.marker = Objects.requireNonNull(marker, "marker");
            final List<FailureReport> copy = new ArrayList<FailureReport>(
                    Objects.requireNonNull(failures, "failures"));
            if (copy.contains(null)) { throw new IllegalArgumentException("failures cannot contain null"); }
            this.failures = Collections.unmodifiableList(copy);
        }
        /** @return final persisted marker */ public Marker marker() { return marker; }
        /** @return isolated failures */ public List<FailureReport> failures() { return failures; }
        /** @return whether automated recovery completed */ public boolean recovered() {
            return marker.state() == State.RECOVERED && failures.isEmpty();
        }
    }
    /** Immutable post-transition public recovery event. */
    public static final class Event implements ApiEvent {
        private final EventMetadata metadata;
        private final Marker marker;
        /** Creates an event whose thread context is declared in metadata. */
        public Event(final EventMetadata metadata, final Marker marker) {
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            this.marker = Objects.requireNonNull(marker, "marker");
        }
        @Override public EventMetadata metadata() { return metadata; }
        /** @return committed marker snapshot */ public Marker marker() { return marker; }
    }
}
