package io.zartra.bedwars.api.failure;

/** Thread-safe destination for structured operational failures. */
public interface FailureSink {
    /**
     * Publishes one already-sanitized failure without blocking an owner thread.
     *
     * @param report non-null immutable failure
     */
    void publish(FailureReport report);
}
