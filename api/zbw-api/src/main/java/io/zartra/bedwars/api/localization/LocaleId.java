package io.zartra.bedwars.api.localization;

import java.util.Locale;

/** Immutable normalized language or language-region identifier. */
public final class LocaleId implements Comparable<LocaleId> {
    private final String value;

    private LocaleId(final String value) {
        if (value == null || !value.matches("[a-z]{2,8}(?:-[A-Z]{2}|-[0-9]{3})?")) {
            throw new IllegalArgumentException("Locale must use language or language-region form");
        }
        this.value = value;
    }

    /** @return normalized locale such as {@code en} or {@code it-IT} */
    public static LocaleId parse(final String value) {
        if (value == null) { throw new IllegalArgumentException("Locale must not be null"); }
        final String[] parts = value.replace('_', '-').split("-");
        if (parts.length < 1 || parts.length > 2) {
            throw new IllegalArgumentException("Locale must use language or language-region form");
        }
        final String normalized = parts.length == 1
                ? parts[0].toLowerCase(Locale.ROOT)
                : parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
        return new LocaleId(normalized);
    }
    /** @return normalized identifier */ public String value() { return value; }
    @Override public int compareTo(final LocaleId other) {
        if (other == null) { throw new NullPointerException("other"); }
        return value.compareTo(other.value);
    }
    @Override public String toString() { return value; }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) {
        return this == other || other instanceof LocaleId && value.equals(((LocaleId) other).value);
    }
}
