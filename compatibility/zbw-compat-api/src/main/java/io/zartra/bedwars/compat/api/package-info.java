/**
 * Platform-neutral Java 8 compatibility contracts.
 *
 * <p>Callers exchange semantic keys and immutable outcomes only. Platform enum, packet, entity,
 * scheduler and GUI classes are adapter-private. Registry activation is bounded in-memory work
 * and never performs I/O. Provider lifecycle callbacks follow the public provider threading and
 * error contracts.</p>
 */
package io.zartra.bedwars.compat.api;
