/**
 * Platform-neutral shop application policies.
 *
 * <p>Services are immutable and contain no global state. They coordinate central authorization,
 * deterministic validation and a single atomic inventory transaction port. Platform adapters are
 * responsible only for owner-thread translation and must not recreate purchase rules.</p>
 */
package io.zartra.bedwars.shop.application;
