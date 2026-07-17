/**
 * Platform-neutral, immutable references to validated declarative action graphs.
 *
 * <p>This package defines metadata only. Execution, scheduling, platform access and persistence
 * are outside this Java 8 API boundary. References are safe to retain across threads; an eventual
 * engine must resolve them against an immutable registry snapshot and reject unknown versions.</p>
 */
package io.zartra.bedwars.scripting.api;
