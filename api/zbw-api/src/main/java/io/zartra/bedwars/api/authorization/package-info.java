/**
 * Platform-neutral authorization contracts.
 *
 * <p>Callers must ask {@link io.zartra.bedwars.api.authorization.AuthorizationService} for every
 * protected action. Implementations are thread-safe, default-deny and return immutable decisions.
 * No role name, platform permission object or mutable global state is part of this API.</p>
 */
package io.zartra.bedwars.api.authorization;
