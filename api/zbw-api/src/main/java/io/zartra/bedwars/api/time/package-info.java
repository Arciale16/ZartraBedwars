/**
 * Injectable wall-clock and monotonic-time abstractions.
 *
 * <p>Domain and application code receives these contracts through constructor injection. It must
 * not read platform clocks directly when behavior is time-dependent.</p>
 */
package io.zartra.bedwars.api.time;
