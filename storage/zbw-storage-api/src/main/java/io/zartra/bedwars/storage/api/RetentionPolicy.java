package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.time.Duration;
import java.util.Objects;

/** Immutable retention class and deletion deadline. */
public final class RetentionPolicy {
    private final DefinitionId retentionClass;
    private final Duration retention;
    private final Duration deletionDeadline;

    private RetentionPolicy(final DefinitionId retentionClass, final Duration retention,
                            final Duration deletionDeadline) {
        if (retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("retention must be positive");
        }
        if (deletionDeadline == null || deletionDeadline.isNegative() || deletionDeadline.isZero()) {
            throw new IllegalArgumentException("deletionDeadline must be positive");
        }
        this.retentionClass = Objects.requireNonNull(retentionClass, "retentionClass");
        this.retention = retention;
        this.deletionDeadline = deletionDeadline;
    }

    /** @return validated retention policy */
    public static RetentionPolicy of(final DefinitionId retentionClass, final Duration retention,
                                     final Duration deletionDeadline) {
        return new RetentionPolicy(retentionClass, retention, deletionDeadline);
    }
    /** @return typed class identity */ public DefinitionId retentionClass() { return retentionClass; }
    /** @return normal retention duration */ public Duration retention() { return retention; }
    /** @return maximum deletion completion duration */ public Duration deletionDeadline() { return deletionDeadline; }
}
