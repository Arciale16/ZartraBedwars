package io.zartra.bedwars.proxy.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Bounded duplicate/stale filter for owner-issued resource and item-rotation notifications. */
public final class VersionPropagationTracker {
    /** Maximum tracked owner streams. */ public static final int MAX_STREAMS = 5000;
    private final Map<String, Long> versions = new LinkedHashMap<String, Long>();

    /** Accepts only a strictly newer owner version. */
    public synchronized boolean accept(final DomainVersionNotification notification) {
        Objects.requireNonNull(notification, "notification");
        String key = notification.trackingKey();
        Long current = versions.get(key);
        if (current != null && notification.version() <= current.longValue()) { return false; }
        if (current == null && versions.size() >= MAX_STREAMS) {
            throw new IllegalStateException("version tracking capacity exceeded");
        }
        versions.put(key, Long.valueOf(notification.version()));
        return true;
    }

    /** Returns the latest accepted version, or zero when absent. */
    public synchronized long version(final DomainVersionNotification.Family family,
            final String ownerReference) {
        String key = Objects.requireNonNull(family, "family").name() + ":"
                + ProxyContractValidation.token(ownerReference, "ownerReference");
        Long value = versions.get(key);
        return value == null ? 0L : value.longValue();
    }
}
