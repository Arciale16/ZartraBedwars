package io.zartra.bedwars.replay.playback;

import io.zartra.bedwars.replay.api.ReplayEvent;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Version-neutral event applier retaining the latest immutable semantic attributes.
 *
 * <p>Rendering adapters may supply a richer applier without coupling playback to Paper.</p>
 */
public final class AttributeReplayEventApplier implements ReplayEventApplier {
    private static final String EVENT_ID = "_replay.event_id";
    private static final String EVENT_SOURCE = "_replay.source";
    private static final String EVENT_TYPE = "_replay.type";

    @Override
    public ReplaySnapshot apply(final ReplaySnapshot snapshot, final ReplayEvent event) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(event, "event");
        if (event.sequence() != snapshot.cursor().nextEventIndex()) {
            throw new IllegalArgumentException("event sequence does not follow snapshot cursor");
        }
        final Map<String, String> values = new TreeMap<String, String>(snapshot.values());
        values.putAll(event.attributes());
        values.put(EVENT_ID, event.eventId());
        values.put(EVENT_SOURCE, event.source().name());
        values.put(EVENT_TYPE, event.type());
        final TimelinePosition position =
                new TimelinePosition((int) event.sequence(), event.offsetMillis());
        return new ReplaySnapshot(new PlaybackCursor(position), values);
    }
}
