/**
 * Deterministic, backup-first configuration migrations.
 *
 * <p>Migration steps are pure document transformations. A backup port is invoked before the first
 * step, and any failure returns the unchanged source document. Filesystem persistence belongs to a
 * later composition adapter and is not exposed here.</p>
 */
package io.zartra.bedwars.config.migration;
