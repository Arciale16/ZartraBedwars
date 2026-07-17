/**
 * Platform-neutral Java 8 command and presentation-extension contracts.
 *
 * <p>Types in this package never expose Bukkit, Paper or storage objects. Parsing,
 * authorization and dispatch perform bounded in-memory work; feature executors return
 * completion stages and must use the M05 bounded scheduler for blocking work.</p>
 */
package io.zartra.bedwars.command.api;
