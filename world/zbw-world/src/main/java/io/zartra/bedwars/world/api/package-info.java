/**
 * Java 8 platform-neutral world-provider contracts.
 *
 * <p>Providers expose bounded plans made of explicit worker and owner-thread steps. Platform
 * objects and filesystem paths stay inside provider implementations. Expected failures use typed
 * step and operation outcomes; public contracts reject null values.</p>
 */
package io.zartra.bedwars.world.api;
