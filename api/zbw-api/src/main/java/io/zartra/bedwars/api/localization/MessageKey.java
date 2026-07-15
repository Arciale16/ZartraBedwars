package io.zartra.bedwars.api.localization;

import java.util.Objects;

/** Immutable stable message-catalog key. */
public final class MessageKey implements Comparable<MessageKey> {
    private final String value;

    private MessageKey(final String value) {
        if (value == null || !value.matches("[a-z0-9][a-z0-9_.-]{2,191}")) {
            throw new IllegalArgumentException("Invalid message key");
        }
        this.value = value;
    }
    /** @return validated message key */ public static MessageKey of(final String value) { return new MessageKey(value); }
    /** @return canonical key */ public String value() { return value; }
    @Override public int compareTo(final MessageKey other) {
        return value.compareTo(Objects.requireNonNull(other, "other").value);
    }
    @Override public String toString() { return value; }
    @Override public int hashCode() { return value.hashCode(); }
    @Override public boolean equals(final Object other) {
        return this == other || other instanceof MessageKey && value.equals(((MessageKey) other).value);
    }
}
