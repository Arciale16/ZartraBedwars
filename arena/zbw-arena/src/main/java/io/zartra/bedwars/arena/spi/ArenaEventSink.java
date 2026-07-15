package io.zartra.bedwars.arena.spi;

import io.zartra.bedwars.api.event.ApiEvent;
import io.zartra.bedwars.arena.application.ArenaEvents;

/** Event publication boundary with explicit pre-commit cancellation and immutable post-commit flow. */
public interface ArenaEventSink {
    /** @return listener decision before persistence; implementations must be bounded and non-blocking */
    ApiEvent.Decision before(ArenaEvents.BeforeChange event);
    /** Publishes an immutable committed fact; listener failure cannot roll back state. */
    void after(ArenaEvents.Changed event);
}
