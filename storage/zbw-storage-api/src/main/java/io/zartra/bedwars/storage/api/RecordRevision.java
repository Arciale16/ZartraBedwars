package io.zartra.bedwars.storage.api;

/** Non-negative optimistic-concurrency revision. */
public final class RecordRevision implements Comparable<RecordRevision> {
    private final long value;

    private RecordRevision(final long value) {
        if (value < 0) { throw new IllegalArgumentException("revision must be non-negative"); }
        this.value = value;
    }

    /** @return validated revision */ public static RecordRevision of(final long value) { return new RecordRevision(value); }
    /** @return initial absent-record revision */ public static RecordRevision initial() { return new RecordRevision(0); }
    /** @return numeric revision */ public long value() { return value; }
    /** @return next revision @throws IllegalStateException on overflow */
    public RecordRevision next() {
        if (value == Long.MAX_VALUE) { throw new IllegalStateException("revision exhausted"); }
        return new RecordRevision(value + 1);
    }
    @Override public int compareTo(final RecordRevision other) { return Long.compare(value, other.value); }
    @Override public int hashCode() { return Long.valueOf(value).hashCode(); }
    @Override public boolean equals(final Object other) {
        return other instanceof RecordRevision && value == ((RecordRevision) other).value;
    }
    @Override public String toString() { return Long.toString(value); }
}
