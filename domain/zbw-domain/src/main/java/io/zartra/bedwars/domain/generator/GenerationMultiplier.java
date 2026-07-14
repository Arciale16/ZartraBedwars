package io.zartra.bedwars.domain.generator;

import java.math.BigDecimal;
import java.util.Objects;

/** Immutable non-negative multiplier for a resource generation interval or yield policy. */
public final class GenerationMultiplier implements Comparable<GenerationMultiplier> {
    /** Neutral multiplier. */
    public static final GenerationMultiplier NORMAL = new GenerationMultiplier(BigDecimal.ONE);
    private final BigDecimal value;

    private GenerationMultiplier(final BigDecimal value) {
        final BigDecimal normalized = Objects.requireNonNull(value, "value").stripTrailingZeros();
        if (normalized.signum() < 0 || normalized.precision() > 34 || Math.abs(normalized.scale()) > 12) {
            throw new IllegalArgumentException("Generation multiplier must be non-negative and bounded to 34 digits/12 scale");
        }
        this.value = normalized;
    }

    /** @return validated multiplier */
    public static GenerationMultiplier of(final BigDecimal value) { return value.compareTo(BigDecimal.ONE) == 0 ? NORMAL : new GenerationMultiplier(value); }
    /** @return parsed multiplier @throws IllegalArgumentException when malformed, negative or unbounded */
    public static GenerationMultiplier parse(final String value) {
        if (value == null) { throw new IllegalArgumentException("Generation multiplier must not be null"); }
        try {
            return of(new BigDecimal(value));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Generation multiplier is malformed", exception);
        }
    }
    /** @return canonical decimal value */ public BigDecimal value() { return value; }
    @Override public int compareTo(final GenerationMultiplier other) { return value.compareTo(other.value); }
    @Override public String toString() { return value.toPlainString(); }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) { return this == other || other instanceof GenerationMultiplier && value.equals(((GenerationMultiplier) other).value); }
}
