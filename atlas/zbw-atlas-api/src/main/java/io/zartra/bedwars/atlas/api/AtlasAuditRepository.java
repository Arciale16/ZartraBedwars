package io.zartra.bedwars.atlas.api;

import java.util.concurrent.CompletionStage;

/** Asynchronous append-only Atlas audit event boundary. */
public interface AtlasAuditRepository {
    /** Appends one immutable event exactly once by its event metadata identity. */
    CompletionStage<Boolean> append(AtlasEvent event);
}
