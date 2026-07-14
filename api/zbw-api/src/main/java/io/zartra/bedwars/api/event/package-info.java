/**
 * Immutable event primitives and listener decisions.
 *
 * <p>Event payloads and metadata are immutable. Listener order is explicit and stable within one
 * dispatch. Cancellation is expressed as an immutable decision rather than mutating the event.
 * Runtime dispatch is introduced after M02; consumers must honor the thread context carried by
 * each event and must never block an owner-thread callback.</p>
 */
package io.zartra.bedwars.api.event;
