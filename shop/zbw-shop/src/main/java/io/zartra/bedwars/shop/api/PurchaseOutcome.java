package io.zartra.bedwars.shop.api;

import java.time.Instant;
import java.util.Objects;

/** Immutable applied or duplicate-safe successful purchase outcome. */
public final class PurchaseOutcome {
    private final PurchaseQuote quote;
    private final boolean duplicate;
    private final Instant observedAt;

    /** Creates a successful outcome. */
    public PurchaseOutcome(final PurchaseQuote quote, final boolean duplicate, final Instant observedAt) {
        this.quote = Objects.requireNonNull(quote, "quote");
        this.duplicate = duplicate;
        this.observedAt = Objects.requireNonNull(observedAt, "observedAt");
    }
    /** @return committed quote */ public PurchaseQuote quote() { return quote; }
    /** @return whether the operation had already committed */ public boolean duplicate() { return duplicate; }
    /** @return local observation instant */ public Instant observedAt() { return observedAt; }
}
