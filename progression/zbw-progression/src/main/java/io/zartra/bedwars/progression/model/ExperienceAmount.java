package io.zartra.bedwars.progression.model;

/** Non-negative bounded experience amount. */
public final class ExperienceAmount implements Comparable<ExperienceAmount> {
    private final long value;
    private ExperienceAmount(final long value) {
        if (value < 0) { throw new IllegalArgumentException("experience must be non-negative"); }
        this.value = value;
    }
    /** @return validated experience */ public static ExperienceAmount of(final long value) { return new ExperienceAmount(value); }
    /** @return zero experience */ public static ExperienceAmount zero() { return new ExperienceAmount(0); }
    /** @return numeric value */ public long value() { return value; }
    /** @return exact sum */ public ExperienceAmount plus(final ExperienceAmount other) { return new ExperienceAmount(Math.addExact(value, other.value)); }
    @Override public int compareTo(final ExperienceAmount other) { return Long.compare(value, other.value); }
    @Override public int hashCode() { return Long.valueOf(value).hashCode(); }
    @Override public boolean equals(final Object other) { return other instanceof ExperienceAmount && value == ((ExperienceAmount) other).value; }
}
