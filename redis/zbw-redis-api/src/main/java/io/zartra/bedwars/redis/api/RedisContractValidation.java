package io.zartra.bedwars.redis.api;

import java.util.Objects;

final class RedisContractValidation {
    private RedisContractValidation() {
    }

    static String token(final String value, final String name) {
        final String checked = Objects.requireNonNull(value, name);
        if (!checked.matches("[a-z0-9][a-z0-9._-]{0,63}")) {
            throw new IllegalArgumentException(name + " must be a lowercase Redis-safe token");
        }
        return checked;
    }

    static String opaque(final String value, final String name) {
        final String checked = Objects.requireNonNull(value, name);
        if (!checked.matches("[A-Za-z0-9._-]{1,128}")) {
            throw new IllegalArgumentException(name + " must be an opaque identifier");
        }
        return checked;
    }
}
