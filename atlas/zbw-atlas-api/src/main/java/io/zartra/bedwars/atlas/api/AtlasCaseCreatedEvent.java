package io.zartra.bedwars.atlas.api;

import io.zartra.bedwars.api.event.EventMetadata;
import java.util.Objects;

/** Immutable post-commit event emitted after a new Atlas case is persisted. */
public final class AtlasCaseCreatedEvent implements AtlasEvent {
    private final EventMetadata metadata;
    private final AtlasCase atlasCase;

    /** Creates a case-created event snapshot. */
    public AtlasCaseCreatedEvent(final EventMetadata metadata, final AtlasCase atlasCase) {
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.atlasCase = Objects.requireNonNull(atlasCase, "atlasCase");
    }
    @Override public EventMetadata metadata() { return metadata; }
    /** Returns the immutable created case. */ public AtlasCase atlasCase() { return atlasCase; }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AtlasCaseCreatedEvent)) { return false; }
        final AtlasCaseCreatedEvent that = (AtlasCaseCreatedEvent) other;
        return metadata.equals(that.metadata) && atlasCase.equals(that.atlasCase);
    }
    @Override public int hashCode() { return Objects.hash(metadata, atlasCase); }
}
