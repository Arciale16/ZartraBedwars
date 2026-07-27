package io.zartra.bedwars.replay.ingestion;

import io.zartra.bedwars.progression.projection.ProgressionEventInput;
import io.zartra.bedwars.replay.api.ReplaySession;

/** M12 immutable projection-event adapter using the authoritative event identity. */
final class ProjectionReplayEventAdapter
        implements ReplaySourceEventAdapter<ProgressionEventInput> {
    private final ReplayEventIngestion converter = new ReplayEventIngestion();
    @Override public Class<ProgressionEventInput> sourceType() {
        return ProgressionEventInput.class;
    }
    @Override public boolean duplicate(final ProgressionEventInput sourceEvent,
                                       final ReplaySession session, final String sourceEventId) {
        return session.timeline().contains(sourceEvent.metadata().eventId().toString());
    }
    @Override public ReplaySession ingest(final ReplaySession session,
                                          final ProgressionEventInput sourceEvent,
                                          final String sourceEventId) {
        return converter.ingestProgression(session, sourceEvent);
    }
}
