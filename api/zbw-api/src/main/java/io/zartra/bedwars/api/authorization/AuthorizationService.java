package io.zartra.bedwars.api.authorization;

/**
 * Central authorization port.
 *
 * <p>Implementations must be thread-safe, exact-node, default-deny and free of platform role
 * assumptions. Expected denial is a decision, not an exception. New implementations must retain
 * this contract for the supported API major version; deprecated nodes remain aliases for at least
 * one major version.</p>
 */
public interface AuthorizationService {
    /** @return deterministic authorization decision for the complete request */
    AuthorizationDecision authorize(AuthorizationRequest request);
}
