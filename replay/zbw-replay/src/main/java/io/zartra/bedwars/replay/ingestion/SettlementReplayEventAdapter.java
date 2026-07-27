package io.zartra.bedwars.replay.ingestion;

import io.zartra.bedwars.replay.api.ReplaySession;
import io.zartra.bedwars.shop.api.PurchaseOutcome;

/** M11 successful settlement adapter; retries remain idempotent. */
final class SettlementReplayEventAdapter implements ReplaySourceEventAdapter<PurchaseOutcome> {
    private final ReplayEventIngestion converter = new ReplayEventIngestion();
    @Override public Class<PurchaseOutcome> sourceType() { return PurchaseOutcome.class; }
    @Override public boolean duplicate(final PurchaseOutcome sourceEvent,
                                       final ReplaySession session, final String sourceEventId) {
        return sourceEvent.duplicate() || session.timeline().contains(sourceEventId);
    }
    @Override public ReplaySession ingest(final ReplaySession session,
                                          final PurchaseOutcome sourceEvent,
                                          final String sourceEventId) {
        return converter.ingestShop(session, sourceEvent, sourceEventId);
    }
}
