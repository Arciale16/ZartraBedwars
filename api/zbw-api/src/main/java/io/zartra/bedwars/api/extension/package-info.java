/**
 * Versioned extension metadata, validation and lifecycle contracts.
 *
 * <p>Extensions communicate only through public contracts. Metadata is validated before code is
 * loaded; incompatible or malformed extensions remain inactive. Lifecycle methods are
 * asynchronous, constructor-injected and never receive a service locator.</p>
 */
package io.zartra.bedwars.api.extension;
