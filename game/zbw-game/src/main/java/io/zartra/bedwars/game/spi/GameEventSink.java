package io.zartra.bedwars.game.spi;

import io.zartra.bedwars.game.model.MatchTransition;

/** Ordered event boundary invoked only after an authoritative transition is persisted. */
public interface GameEventSink {
    /** Publishes immutable facts; implementations must isolate consumer failures. */
    void publish(MatchTransition transition);
}
