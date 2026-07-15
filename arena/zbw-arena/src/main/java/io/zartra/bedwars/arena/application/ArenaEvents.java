package io.zartra.bedwars.arena.application;

import io.zartra.bedwars.api.authorization.AuthorizationSubject;
import io.zartra.bedwars.api.event.ApiEvent;
import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Public immutable pre-change and post-change event payloads for M07 lifecycle operations. */
public final class ArenaEvents {
    private ArenaEvents() { throw new AssertionError("No instances"); }

    /** Cancellable event published before an authorized mutation is committed. */
    public static final class BeforeChange implements ApiEvent.Cancellable {
        private final EventMetadata metadata;
        private final ArenaOperation operation;
        private final ArenaId arenaId;
        private final AuthorizationSubject actor;
        /** Creates an immutable pre-change event. */
        public BeforeChange(final EventMetadata metadata, final ArenaOperation operation,
                            final ArenaId arenaId, final AuthorizationSubject actor) {
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            this.operation = Objects.requireNonNull(operation, "operation");
            this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
            this.actor = Objects.requireNonNull(actor, "actor");
        }
        @Override public EventMetadata metadata() { return metadata; }
        /** @return pending operation */ public ArenaOperation operation() { return operation; }
        /** @return target arena */ public ArenaId arenaId() { return arenaId; }
        /** @return authenticated actor */ public AuthorizationSubject actor() { return actor; }
        @Override public DefinitionId cancellationPolicy() { return DefinitionId.of("zartra", "arena/pre_commit"); }
    }

    /** Immutable post-commit event; listeners cannot rewind committed state. */
    public static final class Changed implements ApiEvent {
        private final EventMetadata metadata;
        private final ArenaOperation operation;
        private final ArenaId arenaId;
        private final long revision;
        /** Creates an immutable post-change event. */
        public Changed(final EventMetadata metadata, final ArenaOperation operation,
                       final ArenaId arenaId, final long revision) {
            this.metadata = Objects.requireNonNull(metadata, "metadata");
            this.operation = Objects.requireNonNull(operation, "operation");
            this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
            if (revision < 1L) { throw new IllegalArgumentException("revision must be positive"); }
            this.revision = revision;
        }
        @Override public EventMetadata metadata() { return metadata; }
        /** @return completed operation */ public ArenaOperation operation() { return operation; }
        /** @return target arena */ public ArenaId arenaId() { return arenaId; }
        /** @return committed durable revision */ public long revision() { return revision; }
    }
}
