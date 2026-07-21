package io.zartra.bedwars.progression.model;

import java.util.Objects;

/** Immutable persistent-currency definition. */
public final class CurrencyDefinition {
    private final CurrencyId id;
    private final String displayName;
    private final long maximumBalance;
    private final boolean enabled;

    /** Creates a persistent currency definition. */
    public CurrencyDefinition(final CurrencyId id, final String displayName,
                              final long maximumBalance, final boolean enabled) {
        this.id = Objects.requireNonNull(id, "id");
        if (displayName == null || displayName.trim().isEmpty() || displayName.length() > 64) { throw new IllegalArgumentException("displayName must contain 1..64 characters"); }
        if (maximumBalance < 0) { throw new IllegalArgumentException("maximumBalance must be non-negative"); }
        this.displayName = displayName;
        this.maximumBalance = maximumBalance;
        this.enabled = enabled;
    }
    /** @return currency identity */ public CurrencyId id() { return id; }
    /** @return display label */ public String displayName() { return displayName; }
    /** @return inclusive balance ceiling */ public long maximumBalance() { return maximumBalance; }
    /** @return whether new transactions may use this currency */ public boolean enabled() { return enabled; }
}
