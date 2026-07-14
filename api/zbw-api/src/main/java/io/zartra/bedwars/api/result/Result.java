package io.zartra.bedwars.api.result;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Immutable success-or-error result.
 *
 * @param <T> non-null success value type
 */
public final class Result<T> {
    private final T value;
    private final ApiError error;

    private Result(final T value, final ApiError error) {
        this.value = value;
        this.error = error;
    }

    /** @return successful result containing a non-null value */
    public static <T> Result<T> success(final T value) {
        return new Result<T>(Objects.requireNonNull(value, "value"), null);
    }

    /** @return failed result containing a typed error */
    public static <T> Result<T> failure(final ApiError error) {
        return new Result<T>(null, Objects.requireNonNull(error, "error"));
    }

    /** @return whether this result contains a value */
    public boolean isSuccess() { return error == null; }
    /** @return whether this result contains an error */
    public boolean isFailure() { return error != null; }
    /** @return optional success value */
    public Optional<T> value() { return Optional.ofNullable(value); }
    /** @return optional typed error */
    public Optional<ApiError> error() { return Optional.ofNullable(error); }

    /**
     * Maps a successful value without changing an existing error.
     *
     * @param mapper non-null, side-effect-free mapping function
     * @param <R> mapped type
     * @return mapped immutable result
     */
    public <R> Result<R> map(final Function<? super T, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return isSuccess() ? Result.success(mapper.apply(value)) : Result.failure(error);
    }

    /**
     * Returns the success value or throws an exception for a programmer-level misuse.
     * Boundary code should inspect {@link #isFailure()} instead.
     *
     * @return success value
     * @throws ResultAccessException when this result is a failure
     */
    public T requireValue() {
        if (isFailure()) {
            throw new ResultAccessException(error);
        }
        return value;
    }

    @Override public int hashCode() { return Objects.hash(value, error); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof Result)) { return false; }
        final Result<?> that = (Result<?>) other;
        return Objects.equals(value, that.value) && Objects.equals(error, that.error);
    }

    /** Typed exception for incorrect direct access to a failed result. */
    public static final class ResultAccessException extends IllegalStateException {
        private static final long serialVersionUID = 1L;
        private final String errorCode;
        private final String messageKey;
        private final ApiError.RetryDisposition retryDisposition;
        private ResultAccessException(final ApiError error) {
            super("Result is a failure: " + error.code());
            this.errorCode = error.code().toString();
            this.messageKey = error.messageKey();
            this.retryDisposition = error.retryDisposition();
        }
        /** @return the typed error that prevented value access */
        public ApiError error() { return ApiError.of(DefinitionId.parse(errorCode), messageKey, retryDisposition); }
    }
}
