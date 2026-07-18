package io.zartra.bedwars.shop.generator;

/** Platform boundary for idempotently applying a generated resource batch. */
public interface ResourceDeliveryPort {
    /** Delivery result. RETRY leaves the batch queued without loss. */
    enum Result { DELIVERED, ALREADY_DELIVERED, RETRY }
    /**
     * Applies a batch atomically. Implementations must remember {@link GeneratorBatch#key()} and
     * return {@code ALREADY_DELIVERED} for retries of a completed delivery.
     * @param configuration immutable routing and delivery policy
     * @param batch immutable generated batch
     * @return non-null delivery result
     */
    Result deliver(GeneratorConfiguration configuration, GeneratorBatch batch);
}
