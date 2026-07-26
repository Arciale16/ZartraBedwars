package io.zartra.bedwars.replay.ingestion;

import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.replay.api.ReplayState;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Stateless serial ingestion coordinator for existing M08/M11/M12 immutable facts.
 * The caller owns per-replay serialization; rejected inputs never mutate the session.
 */
public final class ReplayIngestionService {
    private final List<ReplaySourceEventAdapter<?>> adapters;

    /** Creates the fixed built-in adapter chain in deterministic source order. */
    public ReplayIngestionService() {
        this.adapters = Collections.unmodifiableList(Arrays.<ReplaySourceEventAdapter<?>>asList(
                new MatchReplayEventAdapter(),
                new SettlementReplayEventAdapter(),
                new ProjectionReplayEventAdapter()));
    }

    /**
     * Validates and ingests one source fact. Unsupported source classes are rejected
     * explicitly so extension producers cannot silently alter the replay format.
     */
    public ReplayIngestionResult ingest(final ReplaySession session, final Object sourceEvent,
                                        final String sourceEventId) {
        final ReplaySession current = Objects.requireNonNull(session, "session");
        if (current.state() != ReplayState.RECORDING) {
            return ReplayIngestionResult.of(ReplayIngestionResult.Status.INVALID_STATE,
                    current, "session-not-recording");
        }
        if (sourceEvent == null || sourceEventId == null || sourceEventId.trim().isEmpty()
                || sourceEventId.length() > 128) {
            return ReplayIngestionResult.of(ReplayIngestionResult.Status.MALFORMED,
                    current, "invalid-source-envelope");
        }
        for (ReplaySourceEventAdapter<?> adapter : adapters) {
            if (adapter.sourceType().isInstance(sourceEvent)) {
                return ingestWithAdapter(current, sourceEvent, sourceEventId, adapter);
            }
        }
        return ReplayIngestionResult.of(ReplayIngestionResult.Status.UNSUPPORTED,
                current, "unsupported-source-type");
    }

    private <T> ReplayIngestionResult ingestWithAdapter(
            final ReplaySession session, final Object sourceEvent, final String sourceEventId,
            final ReplaySourceEventAdapter<T> adapter) {
        final T typedEvent = adapter.sourceType().cast(sourceEvent);
        if (adapter.duplicate(typedEvent, session, sourceEventId)) {
            return ReplayIngestionResult.of(ReplayIngestionResult.Status.DUPLICATE,
                    session, "duplicate-source-event");
        }
        try {
            final ReplaySession updated = adapter.ingest(session, typedEvent, sourceEventId);
            if (updated.timeline().nextSequence() == session.timeline().nextSequence()) {
                return ReplayIngestionResult.of(ReplayIngestionResult.Status.UNSUPPORTED,
                        session, "source-produced-no-events");
            }
            return ReplayIngestionResult.of(ReplayIngestionResult.Status.ACCEPTED,
                    updated, "accepted");
        } catch (IllegalArgumentException malformed) {
            return ReplayIngestionResult.of(ReplayIngestionResult.Status.MALFORMED,
                    session, "malformed-source-event");
        } catch (IllegalStateException invalidState) {
            return ReplayIngestionResult.of(ReplayIngestionResult.Status.INVALID_STATE,
                    session, "session-transition-rejected");
        }
    }
}
