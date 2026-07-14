/**
 * Provider-neutral Discord API and SPI contracts.
 *
 * <p>The Minecraft process can run with no Discord provider. Contracts expose only scoped,
 * canonical and immutable data; no Discord SDK object, bot token, secret, mutable game object or
 * game-state mutation appears here. All provider I/O is asynchronous and failure-isolated.</p>
 */
package io.zartra.bedwars.integration.discord.api;
