package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

/** Immutable validated connection, pool and timeout settings with redacted diagnostics. */
public final class SqlStorageConfiguration {
    private final EngineKind engineKind;
    private final String jdbcUrl;
    private final String username;
    private final char[] password;
    private final int maximumPoolSize;
    private final Duration connectionTimeout;
    private final Duration queryTimeout;

    private SqlStorageConfiguration(final EngineKind engineKind, final String jdbcUrl,
                                    final String username, final char[] password,
                                    final int maximumPoolSize, final Duration connectionTimeout,
                                    final Duration queryTimeout) {
        this.engineKind = Objects.requireNonNull(engineKind, "engineKind");
        validateUrl(engineKind, jdbcUrl);
        if (maximumPoolSize < 1 || maximumPoolSize > 64) {
            throw new IllegalArgumentException("maximumPoolSize must be between 1 and 64");
        }
        if (engineKind == EngineKind.SQLITE && maximumPoolSize != 1) {
            throw new IllegalArgumentException("SQLite requires a single-connection serialized writer");
        }
        requirePositive(connectionTimeout, "connectionTimeout");
        requirePositive(queryTimeout, "queryTimeout");
        this.jdbcUrl = jdbcUrl;
        this.username = username == null ? "" : username;
        this.password = password == null ? new char[0] : Arrays.copyOf(password, password.length);
        this.maximumPoolSize = maximumPoolSize;
        this.connectionTimeout = connectionTimeout;
        this.queryTimeout = queryTimeout;
    }

    /** @return validated configuration */
    public static SqlStorageConfiguration of(final EngineKind engineKind, final String jdbcUrl,
                                             final String username, final char[] password,
                                             final int maximumPoolSize,
                                             final Duration connectionTimeout,
                                             final Duration queryTimeout) {
        return new SqlStorageConfiguration(engineKind, jdbcUrl, username, password,
                maximumPoolSize, connectionTimeout, queryTimeout);
    }

    private static void validateUrl(final EngineKind kind, final String url) {
        final String prefix;
        switch (kind) {
            case SQLITE:
                prefix = "jdbc:sqlite:";
                break;
            case MYSQL:
                prefix = "jdbc:mysql:";
                break;
            case MARIADB:
                prefix = "jdbc:mariadb:";
                break;
            default: throw new IllegalArgumentException("unsupported SQL engine");
        }
        if (url == null || !url.startsWith(prefix) || url.indexOf('\n') >= 0 || url.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("jdbcUrl does not match the selected engine");
        }
    }

    private static void requirePositive(final Duration value, final String name) {
        if (value == null || value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    /** @return engine kind */ public EngineKind engineKind() { return engineKind; }
    /** @return JDBC URL; operators must treat it as sensitive */ public String jdbcUrl() { return jdbcUrl; }
    /** @return database username */ public String username() { return username; }
    /** @return defensive transient secret copy */ public char[] password() { return Arrays.copyOf(password, password.length); }
    /** @return bounded pool size */ public int maximumPoolSize() { return maximumPoolSize; }
    /** @return pool connection timeout */ public Duration connectionTimeout() { return connectionTimeout; }
    /** @return statement timeout */ public Duration queryTimeout() { return queryTimeout; }
    /** @return redacted diagnostic string */
    @Override public String toString() {
        return "SqlStorageConfiguration{engine=" + engineKind + ", pool=" + maximumPoolSize
                + ", connectionTimeout=" + connectionTimeout + ", queryTimeout=" + queryTimeout + '}';
    }
}
