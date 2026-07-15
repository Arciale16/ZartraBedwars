/**
 * Central exact-node authorization policies.
 *
 * <p>No role or parent-node grant is inferred. Migration aliases resolve one-to-one to canonical
 * nodes and cannot form chains or wildcards. Decisions are default-deny and are sent to an injected
 * audit sink without player display names or secret data.</p>
 */
package io.zartra.bedwars.config.authorization;
