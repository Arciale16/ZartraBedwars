package io.zartra.bedwars.api.scheduler;

/** Cooperative cancellation signal safe to read from any thread. */
public interface CancellationToken {
    /** @return whether cancellation or the declared deadline has been requested */
    boolean isCancellationRequested();
}
