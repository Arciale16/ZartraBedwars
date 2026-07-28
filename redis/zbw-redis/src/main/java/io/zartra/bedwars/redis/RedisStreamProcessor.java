package io.zartra.bedwars.redis;


import io.zartra.bedwars.redis.api.DeduplicationKey;

import io.zartra.bedwars.redis.api.StreamCursor;

import io.zartra.bedwars.redis.api.StreamRecord;

import java.util.ArrayList;

import java.util.Collections;

import java.util.Comparator;

import java.util.List;

import java.util.Objects;


/** Deterministic ordered duplicate-safe stream batch processor. */
public final class RedisStreamProcessor {
    /** Processing callback;
 it must remain infrastructure-only and nonblocking. */ public interface Handler { boolean handle(StreamRecord record);
 }
    private final RedisDeduplicationStore dedupe;

    private final int retryBudget;

    /** Creates a processor with a finite retry budget. */ public RedisStreamProcessor(final RedisDeduplicationStore dedupe, final int retryBudget) { this.dedupe = Objects.requireNonNull(dedupe, "dedupe");
 if (retryBudget < 0 || retryBudget > 3) { throw new IllegalArgumentException("retry budget outside 0..3");
 } this.retryBudget = retryBudget;
 }
    /** Sorts by stream ID, rejects old records, deduplicates and advances only after success. */
    public StreamCursor process(final StreamCursor cursor, final List<StreamRecord> input, final Handler handler) {
        Objects.requireNonNull(cursor, "cursor");
 Objects.requireNonNull(handler, "handler");

        final List<StreamRecord> records = new ArrayList<StreamRecord>(Objects.requireNonNull(input, "input"));

        Collections.sort(records, new Comparator<StreamRecord>() {
            @Override public int compare(final StreamRecord left, final StreamRecord right) {
                return left.id().compareTo(right.id());
            }
        });

        StreamCursor result = cursor;

        for (StreamRecord record : records) {
            if (record.id().compareTo(result.lastConsumed()) <= 0) { continue;
 }
            final DeduplicationKey key = DeduplicationKey.of(cursor.stream().namespace(), io.zartra.bedwars.redis.api.OperationId.parse(java.util.UUID.nameUUIDFromBytes(record.envelope().operationId().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString()));

            if (!dedupe.record(key)) { result = result.advance(record.id());
 continue;
 }
            boolean accepted = false;

            for (int attempt = 0; attempt <= retryBudget && !accepted; attempt++) { accepted = handler.handle(record);
 }
            if (!accepted) { break;
 }
            result = result.advance(record.id());

        }
        return result;

    }
}
