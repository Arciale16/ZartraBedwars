/**
 * Secret resolution, zeroization, redaction and diagnostic export controls.
 *
 * <p>Resolved material is leased as a character array to a callback and cleared immediately after
 * use. Diagnostics are allowlist-only, reject sensitive classifications and apply seeded redaction.
 * No logger, environment access, protected-file access or global resolver exists in this package.</p>
 */
package io.zartra.bedwars.config.secret;
