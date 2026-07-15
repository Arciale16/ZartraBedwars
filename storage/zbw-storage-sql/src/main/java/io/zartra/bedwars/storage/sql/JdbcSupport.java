package io.zartra.bedwars.storage.sql;

import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.sql.SQLException;

/** Shared validation and bounded deadlock-retry mechanics for JDBC repositories. */
final class JdbcSupport {
    private JdbcSupport() { }

    static JdbcUnitOfWork require(final UnitOfWork unitOfWork, final boolean write) {
        if (!(unitOfWork instanceof JdbcUnitOfWork)) {
            throw new IllegalArgumentException("unitOfWork belongs to a different storage engine");
        }
        final JdbcUnitOfWork jdbc = (JdbcUnitOfWork) unitOfWork;
        jdbc.connection();
        if (write && jdbc.options().accessMode() != TransactionOptions.AccessMode.READ_WRITE) {
            throw new IllegalStateException("write attempted in read-only transaction");
        }
        return jdbc;
    }

    static int timeoutSeconds(final JdbcUnitOfWork unitOfWork) {
        final long milliseconds = unitOfWork.options().timeout().toMillis();
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, (milliseconds + 999L) / 1000L));
    }

    static <T> Result<T> execute(final JdbcUnitOfWork unitOfWork,
                                 final SqlOperation<T> operation) {
        int attempt = 0;
        while (true) {
            try {
                return operation.execute();
            } catch (SQLException exception) {
                if (!retryable(exception) || attempt >= unitOfWork.options().deadlockRetries()) {
                    return Result.failure(SqlErrors.classify(exception));
                }
                attempt++;
            }
        }
    }

    private static boolean retryable(final SQLException exception) {
        final String state = exception.getSQLState();
        return "40001".equals(state) || "40P01".equals(state)
                || exception.getErrorCode() == 1205 || exception.getErrorCode() == 1213;
    }

    /** One JDBC operation that may produce a typed success result. */
    interface SqlOperation<T> {
        /** @return operation result @throws SQLException for classified database failures */
        Result<T> execute() throws SQLException;
    }
}
