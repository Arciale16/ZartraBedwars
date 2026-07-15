/**
 * Atomic targeted reload coordination.
 *
 * <p>Candidate documents validate before preparation. All participant changes prepare before any
 * application, and an apply failure rolls every prepared change back in reverse order while the
 * published last-known-good snapshot remains unchanged. Calls are synchronous and serialized.</p>
 */
package io.zartra.bedwars.config.reload;
