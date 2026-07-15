/**
 * Bounded world lifecycle orchestration over the M05 scheduler and owner dispatcher.
 *
 * <p>Worker steps may perform filesystem work; owner steps may mutate world/entity state. The
 * orchestrator never creates an executor, never performs I/O and drains through the M05 lifecycle
 * contract.</p>
 */
package io.zartra.bedwars.world.orchestration;
