package io.zartra.bedwars.replay.ingestion;

import io.zartra.bedwars.game.model.MatchTransition;
import io.zartra.bedwars.replay.api.ReplaySession;

/** M08 ordered match-transition adapter; it never owns match lifecycle. */
final class MatchReplayEventAdapter implements ReplaySourceEventAdapter<MatchTransition> {
    private final ReplayEventIngestion converter = new ReplayEventIngestion();
    @Override public Class<MatchTransition> sourceType() { return MatchTransition.class; }
    @Override public boolean duplicate(final MatchTransition sourceEvent,
                                       final ReplaySession session, final String sourceEventId) {
        return sourceEvent.duplicate() || session.timeline().contains(sourceEventId + ":0");
    }
    @Override public ReplaySession ingest(final ReplaySession session,
                                          final MatchTransition sourceEvent,
                                          final String sourceEventId) {
        return converter.ingestGame(session, sourceEvent, sourceEventId);
    }
}
