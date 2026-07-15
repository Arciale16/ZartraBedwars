package io.zartra.bedwars.arena.spi;

import io.zartra.bedwars.arena.application.ArenaAuditRecord;

/** Thread-safe destination for already-sanitized arena authorization and mutation audit facts. */
public interface ArenaAuditSink {
    /** Publishes one complete audit record without exposing platform objects or secrets. */
    void publish(ArenaAuditRecord record);
}
