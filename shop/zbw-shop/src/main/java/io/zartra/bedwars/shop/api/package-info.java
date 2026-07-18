/**
 * Immutable Java 8 shop, tender, purchase and user-data contracts.
 *
 * <p>Types in this package have no Bukkit, storage, filesystem or runtime-configuration
 * dependency. Calls that inspect or mutate a live inventory obey the owner-thread rules declared
 * by their port. Expected purchase rejection is represented as data and never as a generic
 * exception. New optional fields may be added within an API major version; semantic changes use a
 * new catalog revision or API major version.</p>
 */
package io.zartra.bedwars.shop.api;
