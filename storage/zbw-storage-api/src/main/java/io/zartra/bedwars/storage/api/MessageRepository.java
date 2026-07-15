package io.zartra.bedwars.storage.api;

import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.result.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Transactional outbox/inbox port providing duplicate-safe business outcomes. */
public interface MessageRepository {
    /** @return true when newly enqueued; false when the operation was already enqueued */
    Result<Boolean> enqueue(UnitOfWork unitOfWork, MessageEnvelope envelope);
    /** @return bounded ordered messages claimed for one dispatcher lease */
    Result<List<MessageEnvelope>> claim(UnitOfWork unitOfWork, Instant now,
                                        int maximum, Duration lease);
    /** Marks a claimed outbox operation delivered atomically. */
    Result<Boolean> acknowledge(UnitOfWork unitOfWork, IdempotencyKey operationId);
    /** @return true only for the first inbox receipt of the operation */
    Result<Boolean> receive(UnitOfWork unitOfWork, MessageEnvelope envelope);

    /** Creates a defensive immutable message batch and enforces its configured bound. */
    final class Batches {
        private Batches() { }
        /** @return immutable copy no larger than {@code maximum} */
        public static List<MessageEnvelope> bounded(final List<MessageEnvelope> source,
                                                    final int maximum) {
            if (source == null) { throw new NullPointerException("source"); }
            if (maximum < 1 || source.size() > maximum) {
                throw new IllegalArgumentException("message batch exceeds its positive bound");
            }
            return Collections.unmodifiableList(new ArrayList<MessageEnvelope>(source));
        }
    }
}
