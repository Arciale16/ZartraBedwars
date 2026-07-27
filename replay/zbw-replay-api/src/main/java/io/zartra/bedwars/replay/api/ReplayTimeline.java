package io.zartra.bedwars.replay.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable ordered replay event stream with idempotent duplicate handling. */
public final class ReplayTimeline {
    private final List<ReplayEvent> events;
    private final Set<String> eventIds;

    private ReplayTimeline(final List<ReplayEvent> events) {
        this.events = Collections.unmodifiableList(events);
        final Set<String> ids = new HashSet<String>();
        for (ReplayEvent event : events) { ids.add(event.eventId()); }
        this.eventIds = Collections.unmodifiableSet(ids);
    }

    /** Returns an empty timeline. */
    public static ReplayTimeline empty() { return new ReplayTimeline(Collections.<ReplayEvent>emptyList()); }

    /**
     * Appends the next event. The same event identity is an idempotent no-op; gaps,
     * sequence conflicts and time regression are rejected deterministically.
     */
    public ReplayTimeline append(final ReplayEvent event) {
        Objects.requireNonNull(event, "event");
        if (eventIds.contains(event.eventId())) { return this; }
        final long expected = events.size();
        if (event.sequence() != expected) {
            throw new IllegalArgumentException("expected sequence " + expected);
        }
        if (!events.isEmpty() && event.offsetMillis() < events.get(events.size() - 1).offsetMillis()) {
            throw new IllegalArgumentException("event time cannot regress");
        }
        final List<ReplayEvent> copy = new ArrayList<ReplayEvent>(events);
        copy.add(event);
        return new ReplayTimeline(copy);
    }

    /** Returns immutable ordered events. */ public List<ReplayEvent> events() { return events; }
    /** Returns the next required sequence. */ public long nextSequence() { return events.size(); }
    /** Returns whether an event identity was already accepted. */
    public boolean contains(final String eventId) { return eventIds.contains(eventId); }
}
