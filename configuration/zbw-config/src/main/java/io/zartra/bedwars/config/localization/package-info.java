/**
 * Immutable locale catalogs, fallback selection and deterministic import/export.
 *
 * <p>Catalog switching is atomic and requires completeness against the configured fallback.
 * Rendering substitutes typed scalar parameters as escaped neutral text; Minecraft component,
 * plural and legacy rendering remain behind later version-specific presentation adapters.</p>
 */
package io.zartra.bedwars.config.localization;
