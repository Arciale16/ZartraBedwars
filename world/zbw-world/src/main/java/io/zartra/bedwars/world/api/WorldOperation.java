package io.zartra.bedwars.world.api;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.TaskId;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Immutable request for one bounded world lifecycle operation. */
public final class WorldOperation {
    private final TaskId operationId;
    private final CorrelationId correlationId;
    private final Type type;
    private final WorldKey target;
    private final WorldKey source;
    private final Duration timeout;

    private WorldOperation(final TaskId operationId, final CorrelationId correlationId,
                           final Type type, final WorldKey target, final WorldKey source,
                           final Duration timeout) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.type = Objects.requireNonNull(type, "type");
        this.target = Objects.requireNonNull(target, "target");
        this.source = source;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if ((type == Type.CLONE || type == Type.RESET) && source == null) {
            throw new IllegalArgumentException(type + " requires a source template");
        }
        if ((type == Type.LOAD || type == Type.UNLOAD) && source != null) {
            throw new IllegalArgumentException(type + " does not accept a source template");
        }
        if (target.equals(source)) { throw new IllegalArgumentException("source and target must differ"); }
    }

    /** @return request with explicit deterministic identities */
    public static WorldOperation of(final TaskId operationId, final CorrelationId correlationId,
                                    final Type type, final WorldKey target, final WorldKey source,
                                    final Duration timeout) {
        return new WorldOperation(operationId, correlationId, type, target, source, timeout);
    }
    /** @return new collision-resistant request */
    public static WorldOperation create(final Type type, final WorldKey target,
                                        final WorldKey source, final Duration timeout) {
        return of(TaskId.random(), CorrelationId.random(), type, target, source, timeout);
    }
    /** @return operation identity */ public TaskId operationId() { return operationId; }
    /** @return correlation identity */ public CorrelationId correlationId() { return correlationId; }
    /** @return operation type */ public Type type() { return type; }
    /** @return target world */ public WorldKey target() { return target; }
    /** @return optional source template */ public Optional<WorldKey> source() { return Optional.ofNullable(source); }
    /** @return total operation deadline */ public Duration timeout() { return timeout; }

    /** Supported M06 world lifecycle operation. */ public enum Type { LOAD, CLONE, RESET, UNLOAD }
}
