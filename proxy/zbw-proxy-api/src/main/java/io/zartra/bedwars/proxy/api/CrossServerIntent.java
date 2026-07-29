package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

/** Immutable owner-issued cross-server intent containing only privacy-safe references. */
public final class CrossServerIntent {
    /** Maximum opaque attributes. */ public static final int MAX_ATTRIBUTES = 32;
    /** Maximum private-game roster references. */ public static final int MAX_ROSTER = 100;
    private final UUID operationId;
    private final CrossServerFlowType type;
    private final String subjectReference;
    private final String audience;
    private final String ownerReference;
    private final SortedMap<String, String> attributes;
    private final SortedSet<String> rosterReferences;
    private final Instant requestedAt;
    private final Instant deadline;

    private CrossServerIntent(final UUID operationId, final CrossServerFlowType type,
            final String subjectReference, final String audience, final String ownerReference,
            final Map<String, String> attributes, final Collection<String> rosterReferences,
            final Instant requestedAt, final Instant deadline) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.type = Objects.requireNonNull(type, "type");
        this.subjectReference = ProxyContractValidation.token(subjectReference, "subjectReference");
        this.audience = ProxyContractValidation.token(audience, "audience");
        this.ownerReference = ProxyContractValidation.token(ownerReference, "ownerReference");
        if (attributes == null || attributes.size() > MAX_ATTRIBUTES) {
            throw new IllegalArgumentException("invalid attribute count");
        }
        TreeMap<String, String> attributeCopy = new TreeMap<String, String>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            attributeCopy.put(ProxyContractValidation.token(entry.getKey(), "attributeKey"),
                    ProxyContractValidation.token(entry.getValue(), "attributeValue"));
        }
        this.attributes = Collections.unmodifiableSortedMap(attributeCopy);
        if (rosterReferences == null || rosterReferences.size() > MAX_ROSTER) {
            throw new IllegalArgumentException("invalid roster count");
        }
        TreeSet<String> rosterCopy = new TreeSet<String>();
        for (String reference : rosterReferences) {
            rosterCopy.add(ProxyContractValidation.token(reference, "rosterReference"));
        }
        if (type == CrossServerFlowType.PRIVATE_GAME && rosterCopy.isEmpty()) {
            throw new IllegalArgumentException("private game requires roster references");
        }
        this.rosterReferences = Collections.unmodifiableSortedSet(rosterCopy);
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        if (!deadline.isAfter(requestedAt) || deadline.isAfter(requestedAt.plusSeconds(30))) {
            throw new IllegalArgumentException("intent deadline must be within 30 seconds");
        }
    }

    /** Creates a bounded owner-issued intent. */
    public static CrossServerIntent of(final UUID operationId, final CrossServerFlowType type,
            final String subjectReference, final String audience, final String ownerReference,
            final Map<String, String> attributes, final Collection<String> rosterReferences,
            final Instant requestedAt, final Instant deadline) {
        return new CrossServerIntent(operationId, type, subjectReference, audience, ownerReference,
                attributes, rosterReferences, requestedAt, deadline);
    }
    /** Returns idempotency/operation ID. */ public UUID operationId() { return operationId; }
    /** Returns workflow family. */ public CrossServerFlowType type() { return type; }
    /** Returns opaque subject. */ public String subjectReference() { return subjectReference; }
    /** Returns exact destination audience. */ public String audience() { return audience; }
    /** Returns owner decision reference. */ public String ownerReference() { return ownerReference; }
    /** Returns immutable opaque attributes. */ public SortedMap<String, String> attributes() { return attributes; }
    /** Returns immutable private-game roster references. */ public SortedSet<String> rosterReferences() { return rosterReferences; }
    /** Returns request time. */ public Instant requestedAt() { return requestedAt; }
    /** Returns deadline. */ public Instant deadline() { return deadline; }
    /** Tests expiry. */ public boolean expiredAt(final Instant now) { return !Objects.requireNonNull(now, "now").isBefore(deadline); }
}
