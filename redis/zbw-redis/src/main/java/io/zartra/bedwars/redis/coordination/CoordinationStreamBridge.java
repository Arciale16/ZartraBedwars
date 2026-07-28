package io.zartra.bedwars.redis.coordination;

import io.zartra.bedwars.redis.RedisStreamProcessor;
import io.zartra.bedwars.redis.api.StreamCursor;
import io.zartra.bedwars.redis.api.StreamRecord;
import java.util.List;
import java.util.Objects;

/** Ordered M04-envelope/M19-stream consumer for coordination events. */
public final class CoordinationStreamBridge {
    /** Infrastructure callback; it must not execute domain or Paper logic. */
    public interface Handler {
        /** @return true only when infrastructure processing completed */
        boolean handle(CoordinationEvent event, VersionedCoordinationBridge.Result result);
    }

    private final RedisStreamProcessor streamProcessor;
    private final CoordinationEventCodec codec;
    private final VersionedCoordinationBridge versions;

    /** Creates a stream bridge from the existing bounded primitives. */
    public CoordinationStreamBridge(final RedisStreamProcessor streamProcessor,
                                    final CoordinationEventCodec codec,
                                    final VersionedCoordinationBridge versions) {
        this.streamProcessor = Objects.requireNonNull(streamProcessor, "streamProcessor");
        this.codec = Objects.requireNonNull(codec, "codec");
        this.versions = Objects.requireNonNull(versions, "versions");
    }

    /**
     * Sorts by stream ID, validates schema, rejects duplicates/stale versions and advances on success.
     */
    public StreamCursor consume(final StreamCursor cursor, final List<StreamRecord> records,
                                final Handler handler) {
        Objects.requireNonNull(handler, "handler");
        return streamProcessor.process(cursor, records, record -> {
            final CoordinationEvent event;
            try {
                event = codec.decode(record.envelope().payload());
            } catch (IllegalArgumentException malformed) {
                return false;
            }
            return handler.handle(event, versions.accept(event));
        });
    }
}
