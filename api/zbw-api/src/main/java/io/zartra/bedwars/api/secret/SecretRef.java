package io.zartra.bedwars.api.secret;

import java.util.Locale;
import java.util.Objects;

/** Immutable reference to secret material held outside normal exported configuration. */
public final class SecretRef {
    private final Source source;
    private final String key;

    private SecretRef(final Source source, final String key) {
        this.source = Objects.requireNonNull(source, "source");
        if (key == null || !key.matches("[A-Za-z0-9][A-Za-z0-9_./-]{1,127}")) {
            throw new IllegalArgumentException("Invalid secret reference key");
        }
        this.key = key;
    }

    /** @return parsed {@code source:key} reference */
    public static SecretRef parse(final String value) {
        if (value == null) { throw new IllegalArgumentException("Secret reference must not be null"); }
        final int separator = value.indexOf(':');
        if (separator < 1 || separator != value.lastIndexOf(':') || separator == value.length() - 1) {
            throw new IllegalArgumentException("Secret reference must contain one source separator");
        }
        final Source source;
        try {
            source = Source.valueOf(value.substring(0, separator).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unsupported secret reference source", exception);
        }
        return new SecretRef(source, value.substring(separator + 1));
    }

    /** @return secret source class */ public Source source() { return source; }
    /** @return non-secret locator key */ public String key() { return key; }
    @Override public String toString() { return source.name().toLowerCase(Locale.ROOT) + ':' + key; }
    @Override public int hashCode() { return Objects.hash(source, key); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof SecretRef)) { return false; }
        final SecretRef that = (SecretRef) other;
        return source == that.source && key.equals(that.key);
    }

    /** Approved secret locator classes in resolution priority order. */
    public enum Source {
        /** Approved external secret-provider locator. */ PROVIDER,
        /** Protected environment-variable locator. */ ENVIRONMENT,
        /** Explicitly approved protected file locator; normal config files are forbidden. */ PROTECTED_FILE
    }
}
