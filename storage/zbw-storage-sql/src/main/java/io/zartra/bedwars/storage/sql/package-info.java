/**
 * JDBC/Hikari SQL adapters for the platform-neutral storage API.
 *
 * <p>Every operation blocks and therefore belongs on a bounded storage worker. SQL is confined to
 * this package; statements use bound parameters, configured query timeouts and typed failures.
 * SQLite is single-JVM/single-writer. MySQL and MariaDB are authoritative for scalable topology.</p>
 */
package io.zartra.bedwars.storage.sql;
