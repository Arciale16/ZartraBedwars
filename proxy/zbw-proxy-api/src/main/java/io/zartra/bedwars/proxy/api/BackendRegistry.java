package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Bounded, thread-safe ephemeral backend registry. SQL and domain modules remain authoritative. */
public final class BackendRegistry {
    /** Maximum retained backend states. */
    public static final int MAX_BACKENDS = 1024;
    private final Map<BackendId, Entry> entries = new LinkedHashMap<BackendId, Entry>();

    /** Registers a boot epoch, rejecting stale or repeated instances. */
    public synchronized void register(final BackendRegistration registration) {
        Objects.requireNonNull(registration, "registration");
        Entry current = entries.get(registration.backendId());
        if (current != null) {
            registration.epoch().requireNewerThan(current.registration.epoch());
        } else if (entries.size() >= MAX_BACKENDS) {
            throw new IllegalStateException("backend registry capacity exceeded");
        }
        entries.put(registration.backendId(), new Entry(registration, null));
    }

    /** Applies a heartbeat only to its current backend epoch. */
    public synchronized boolean heartbeat(final Heartbeat heartbeat, final Instant now) {
        Objects.requireNonNull(heartbeat, "heartbeat");
        Objects.requireNonNull(now, "now");
        Entry current = entries.get(heartbeat.backendId());
        if (current == null || !current.registration.epoch().equals(heartbeat.epoch())
                || heartbeat.isExpiredAt(now)) {
            return false;
        }
        BackendStatus status = heartbeat.health().state() == HealthSnapshot.State.UNHEALTHY
                ? BackendStatus.UNHEALTHY : current.registration.status();
        BackendRegistration updated = BackendRegistration.of(current.registration.backendId(),
                current.registration.epoch(), current.registration.capabilities(), status,
                current.registration.registeredAt());
        entries.put(heartbeat.backendId(), new Entry(updated, heartbeat));
        return true;
    }

    /** Changes lifecycle state for the current epoch. */
    public synchronized boolean status(final BackendId id, final InstanceEpoch epoch,
            final BackendStatus status) {
        Entry current = entries.get(Objects.requireNonNull(id, "id"));
        if (current == null || !current.registration.epoch().equals(epoch)) {
            return false;
        }
        BackendRegistration updated = BackendRegistration.of(id, epoch,
                current.registration.capabilities(), Objects.requireNonNull(status, "status"),
                current.registration.registeredAt());
        entries.put(id, new Entry(updated, current.heartbeat));
        return true;
    }

    /** Expires bounded stale state and marks current instances offline. */
    public synchronized void expire(final Instant now) {
        Objects.requireNonNull(now, "now");
        for (Map.Entry<BackendId, Entry> item : new ArrayList<Map.Entry<BackendId, Entry>>(entries.entrySet())) {
            Entry value = item.getValue();
            if (value.heartbeat == null || value.heartbeat.isExpiredAt(now)) {
                status(item.getKey(), value.registration.epoch(), BackendStatus.OFFLINE);
            }
        }
    }

    /** Returns deterministic registration order. */
    public synchronized List<BackendRegistration> registrations() {
        List<BackendRegistration> values = new ArrayList<BackendRegistration>();
        for (Entry entry : entries.values()) {
            values.add(entry.registration);
        }
        Collections.sort(values, (left, right) -> left.backendId().compareTo(right.backendId()));
        return Collections.unmodifiableList(values);
    }

    /** Returns latest capacity or an empty snapshot marker. */
    public synchronized int available(final BackendId id) {
        Entry entry = entries.get(id);
        return entry == null || entry.heartbeat == null ? 0 : entry.heartbeat.capacity().available();
    }

    private static final class Entry {
        private final BackendRegistration registration;
        private final Heartbeat heartbeat;
        private Entry(final BackendRegistration registration, final Heartbeat heartbeat) {
            this.registration = registration;
            this.heartbeat = heartbeat;
        }
    }
}
