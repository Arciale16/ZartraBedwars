package io.zartra.bedwars.integration.placeholderapi.api;

import java.util.Objects;

/**
 * Typed identifier for a registered placeholder.
 *
 * @param value the identifier value in lowercase snake_case.
 */
public final class PlaceholderId {

    private final String value;

    private PlaceholderId(final String value) {
        this.value = value;
    }

    public static PlaceholderId of(final String value) {
        final String normalized = Objects.requireNonNull(value, "value").trim().toLowerCase();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("placeholder id must not be empty");
        }
        if (normalized.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("placeholder id must not contain spaces");
        }
        return new PlaceholderId(normalized);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlaceholderId)) {
            return false;
        }
        final PlaceholderId that = (PlaceholderId) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
