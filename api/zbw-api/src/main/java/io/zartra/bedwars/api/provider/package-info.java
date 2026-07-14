/**
 * Provider-neutral SPI contracts.
 *
 * <p>Providers use constructor injection, declare capabilities before start and have explicit
 * asynchronous start, drain and stop phases. They are never discovered through global mutable
 * state or a service locator.</p>
 */
package io.zartra.bedwars.api.provider;
