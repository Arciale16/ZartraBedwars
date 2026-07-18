package io.zartra.bedwars.shop.generator;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.api.identity.MatchId;
import io.zartra.bedwars.api.identity.ResourceId;
import java.time.Instant;
import java.util.Objects;

/** Immutable delivery unit with a stable key that prevents duplicate resource creation. */
public final class GeneratorBatch {
    private final IdempotencyKey key;
    private final MatchId matchId;
    private final DefinitionId generatorId;
    private final ResourceId resource;
    private final long sequence;
    private final int amount;
    private final Instant generatedAt;

    GeneratorBatch(final IdempotencyKey key, final MatchId matchId, final DefinitionId generatorId,
                   final ResourceId resource, final long sequence, final int amount,
                   final Instant generatedAt) {
        if (sequence < 1L || amount < 1) { throw new IllegalArgumentException("sequence and amount must be positive"); }
        this.key = Objects.requireNonNull(key, "key");
        this.matchId = Objects.requireNonNull(matchId, "matchId");
        this.generatorId = Objects.requireNonNull(generatorId, "generatorId");
        this.resource = Objects.requireNonNull(resource, "resource");
        this.sequence = sequence;
        this.amount = amount;
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
    }
    /** @return exactly-once delivery key */ public IdempotencyKey key() { return key; }
    /** @return owning match */ public MatchId matchId() { return matchId; }
    /** @return source generator */ public DefinitionId generatorId() { return generatorId; }
    /** @return generated resource */ public ResourceId resource() { return resource; }
    /** @return monotonic per-generator sequence */ public long sequence() { return sequence; }
    /** @return generated units */ public int amount() { return amount; }
    /** @return logical generation instant */ public Instant generatedAt() { return generatedAt; }
}
