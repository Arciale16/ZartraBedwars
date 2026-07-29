package io.zartra.bedwars.proxy.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

/** Immutable owner-module routing decision; the proxy never derives eligibility or match rules. */
public final class OwnerHandoff {
    private final UUID operationId;
    private final String decisionReference;
    private final long decisionVersion;
    private final boolean authorized;
    private final SortedSet<String> requiredCapabilities;

    private OwnerHandoff(final UUID operationId, final String decisionReference,
            final long decisionVersion, final boolean authorized,
            final Collection<String> requiredCapabilities) {
        this.operationId = Objects.requireNonNull(operationId, "operationId");
        this.decisionReference = ProxyContractValidation.token(decisionReference, "decisionReference");
        if (decisionVersion < 1) { throw new IllegalArgumentException("decisionVersion must be positive"); }
        this.decisionVersion = decisionVersion;
        this.authorized = authorized;
        if (requiredCapabilities == null || requiredCapabilities.isEmpty()) {
            throw new IllegalArgumentException("required capabilities missing");
        }
        TreeSet<String> copy = new TreeSet<String>();
        for (String capability : requiredCapabilities) {
            copy.add(ProxyContractValidation.token(capability, "capability"));
        }
        this.requiredCapabilities = Collections.unmodifiableSortedSet(copy);
    }
    /** Creates an owner decision. */
    public static OwnerHandoff of(final UUID operationId, final String decisionReference,
            final long decisionVersion, final boolean authorized,
            final Collection<String> requiredCapabilities) {
        return new OwnerHandoff(operationId, decisionReference, decisionVersion,
                authorized, requiredCapabilities);
    }
    /** Returns operation ID. */ public UUID operationId() { return operationId; }
    /** Returns opaque owner decision. */ public String decisionReference() { return decisionReference; }
    /** Returns monotonic owner version. */ public long decisionVersion() { return decisionVersion; }
    /** Returns whether the owner authorized routing. */ public boolean authorized() { return authorized; }
    /** Returns immutable capabilities. */ public SortedSet<String> requiredCapabilities() { return requiredCapabilities; }
    /** Verifies binding to an intent. */ public boolean appliesTo(final CrossServerIntent intent) { return operationId.equals(Objects.requireNonNull(intent, "intent").operationId()); }
}
