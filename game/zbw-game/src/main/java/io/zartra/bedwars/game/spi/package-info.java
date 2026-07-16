/**
 * Asynchronous platform-neutral ports used by the M08 game application.
 *
 * <p>Implementations must never complete blocking storage or network work on a Minecraft owner
 * thread. Projection implementations are the exception: they are invoked only by their owner
 * thread and must fail closed when called elsewhere.</p>
 */
package io.zartra.bedwars.game.spi;
