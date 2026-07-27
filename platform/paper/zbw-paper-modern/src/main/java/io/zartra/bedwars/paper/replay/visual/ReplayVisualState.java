package io.zartra.bedwars.paper.replay.visual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Immutable, bounded visual state at one playback cursor. */
public final class ReplayVisualState {
    private final int eventIndex;
    private final Map<String, VisualEntityState> entities;
    private final List<VisualMatchEvent> importantEvents;

    /** Creates a stable visual projection. */
    public ReplayVisualState(final int eventIndex,
                             final Map<String, VisualEntityState> entities,
                             final List<VisualMatchEvent> importantEvents) {
        if (eventIndex < -1) { throw new IllegalArgumentException("eventIndex must be at least -1"); }
        Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(importantEvents, "importantEvents");
        final Map<String, VisualEntityState> entityCopy =
                new TreeMap<String, VisualEntityState>();
        for (Map.Entry<String, VisualEntityState> entry : entities.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("visual entities cannot contain null");
            }
            entityCopy.put(entry.getKey(), entry.getValue());
        }
        if (importantEvents.contains(null)) {
            throw new IllegalArgumentException("visual events cannot contain null");
        }
        this.eventIndex = eventIndex;
        this.entities = Collections.unmodifiableMap(entityCopy);
        this.importantEvents = Collections.unmodifiableList(
                new ArrayList<VisualMatchEvent>(importantEvents));
    }

    /** @return empty visual state before the first event */
    public static ReplayVisualState empty() {
        return new ReplayVisualState(-1, Collections.<String, VisualEntityState>emptyMap(),
                Collections.<VisualMatchEvent>emptyList());
    }

    /** @return inclusive playback event index */ public int eventIndex() { return eventIndex; }
    /** @return immutable entity map in identity order */ public Map<String, VisualEntityState> entities() {
        return entities;
    }
    /** @return bounded important events in replay order */ public List<VisualMatchEvent> importantEvents() {
        return importantEvents;
    }

    @Override public boolean equals(final Object other) {
        if (!(other instanceof ReplayVisualState)) { return false; }
        final ReplayVisualState value = (ReplayVisualState) other;
        return eventIndex == value.eventIndex && entities.equals(value.entities)
                && importantEvents.equals(value.importantEvents);
    }

    @Override public int hashCode() {
        return Objects.hash(eventIndex, entities, importantEvents);
    }
}
