package io.zartra.bedwars.integration.placeholderapi.api;

import java.util.Objects;

/**
 * Result of a placeholder resolution.
 *
 * @param value resolved placeholder value.
 * @param found whether a concrete value was produced.
 * @param fallback whether fallback handling was used.
 */
public final class PlaceholderResult {

    private final String value;
    private final boolean found;
    private final boolean fallback;

    private PlaceholderResult(final String value, final boolean found, final boolean fallback) {
        this.value = value;
        this.found = found;
        this.fallback = fallback;
    }

    public static PlaceholderResult found(final String value) {
        return new PlaceholderResult(normalize(value), true, false);
    }

    public static PlaceholderResult fallback(final String value) {
        return new PlaceholderResult(normalize(value), false, true);
    }

    public static PlaceholderResult unavailable(final String fallback) {
        return new PlaceholderResult(normalize(fallback), false, true);
    }

    private static String normalize(final String value) {
        return value == null ? "-" : value;
    }

    public String value() {
        return value;
    }

    public boolean found() {
        return found;
    }

    public boolean fallback() {
        return fallback;
    }

    public static void require(final String value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }
        if (value.isEmpty()) {
            throw new IllegalArgumentException("value must not be empty");
        }
    }

    @Override
    public String toString() {
        return value + "[found=" + found + ",fallback=" + fallback + "]";
    }

    public PlaceholderResult requireAvailable(final String context) {
        Objects.requireNonNull(context, "context");
        if (!found) {
            throw new IllegalStateException("placeholder unavailable: " + context);
        }
        return this;
    }
}
