package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.result.ApiError;
import java.sql.SQLException;

/** Internal SQL exception classification without leaking vendor messages. */
final class SqlErrors {
    static final ApiError CLOSED = permanent("storage.closed", "storage.error.closed");
    static final ApiError INVALID_TRANSACTION = permanent("storage.invalid_transaction", "storage.error.transaction");
    static final ApiError CONFLICT = permanent("storage.conflict", "storage.error.conflict");
    static final ApiError READ_ONLY = permanent("storage.read_only", "storage.error.read_only");

    private SqlErrors() { }

    static ApiError classify(final SQLException exception) {
        final String state = exception.getSQLState();
        if (state != null && (state.startsWith("08") || state.equals("40001") || state.equals("40P01"))) {
            return retryable("storage.transient", "storage.error.transient");
        }
        if (state != null && state.startsWith("23")) { return CONFLICT; }
        return permanent("storage.database", "storage.error.database");
    }

    static boolean duplicate(final SQLException exception) {
        final String state = exception.getSQLState();
        return (state != null && state.startsWith("23"))
                || exception.getErrorCode() == 1062 || exception.getErrorCode() == 19;
    }

    private static ApiError permanent(final String path, final String message) {
        return ApiError.of(DefinitionId.of("zartra", path), message, ApiError.RetryDisposition.PERMANENT);
    }
    private static ApiError retryable(final String path, final String message) {
        return ApiError.of(DefinitionId.of("zartra", path), message, ApiError.RetryDisposition.RETRYABLE);
    }
}
