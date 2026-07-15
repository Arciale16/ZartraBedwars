/**
 * Platform-neutral durable-storage contracts.
 *
 * <p>All operations are synchronous and potentially blocking. Callers must invoke them only from
 * bounded storage workers, never a Minecraft owner/tick thread. Implementations return typed
 * results for expected failures, reject {@code null}, and keep implementation resources private.
 * Contracts are Java 8 compatible and evolve additively; removals require a major API version.</p>
 */
package io.zartra.bedwars.storage.api;
