/**
 * Serialized M08 match application services.
 *
 * <p>Services contain no platform objects. Storage operations return stages and are expected to
 * execute on bounded off-thread infrastructure. Callers must serialize commands per match; the
 * service additionally rejects overlapping persistence to prevent owner-thread blocking.</p>
 */
package io.zartra.bedwars.game.application;
