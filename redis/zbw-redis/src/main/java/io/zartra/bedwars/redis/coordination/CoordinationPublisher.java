package io.zartra.bedwars.redis.coordination;

import io.zartra.bedwars.redis.LettuceRedisAdapter;
import io.zartra.bedwars.redis.api.RedisKey;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

/** Non-authoritative publisher for disposable coordination notifications. */
public final class CoordinationPublisher {
    private final LettuceRedisAdapter adapter;
    private final CoordinationEventCodec codec;

    /** Creates a publisher over the bounded Lettuce adapter. */
    public CoordinationPublisher(final LettuceRedisAdapter adapter,
                                 final CoordinationEventCodec codec) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.codec = Objects.requireNonNull(codec, "codec");
    }

    /**
     * Publishes a notification. Failure means consumers recover from their durable owner.
     *
     * @param channel namespace-validated channel
     * @param event immutable notification
     * @return subscriber count
     */
    public CompletionStage<Long> publish(final RedisKey channel, final CoordinationEvent event) {
        return adapter.publish(channel, codec.encode(event));
    }
}
