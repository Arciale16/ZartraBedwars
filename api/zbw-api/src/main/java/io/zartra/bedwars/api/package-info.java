/**
 * Stable, platform-independent ZartraBedWars public API root.
 *
 * <p>Public packages target Java 8 bytecode and never expose Bukkit, Paper, NMS, proxy, Redis,
 * database, filesystem or runtime-configuration types. Parameters and return values are non-null
 * unless explicitly documented. Expected failures use typed results. API compatibility follows
 * Semantic Versioning and the deprecation policy documented by {@code ApiVersions}.</p>
 */
package io.zartra.bedwars.api;
