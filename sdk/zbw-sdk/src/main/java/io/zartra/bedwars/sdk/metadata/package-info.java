/**
 * Deterministic, platform-free extension metadata reader and validator.
 *
 * <p>The SDK accepts the restricted UTF-8 key/value metadata format through a {@code Reader}; it does not open files,
 * inspect classes or load extension code. This keeps validation reusable in tools and CI.</p>
 */
package io.zartra.bedwars.sdk.metadata;
