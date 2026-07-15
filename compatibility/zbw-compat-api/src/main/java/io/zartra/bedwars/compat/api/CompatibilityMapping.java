package io.zartra.bedwars.compat.api;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Objects;

/** Immutable adapter-private renderer selection described without a platform class. */
public final class CompatibilityMapping {
    private final SemanticKey semanticKey;
    private final DefinitionId rendererId;
    private final String platformValue;

    /** Creates a validated mapping. Platform values are diagnostic-safe enum or key names. */
    public CompatibilityMapping(final SemanticKey semanticKey, final DefinitionId rendererId,
                                final String platformValue) {
        this.semanticKey = Objects.requireNonNull(semanticKey, "semanticKey");
        this.rendererId = Objects.requireNonNull(rendererId, "rendererId");
        if (platformValue == null || !platformValue.matches("[A-Za-z0-9_.:/-]{1,128}")) {
            throw new IllegalArgumentException("platformValue must be a safe bounded key");
        }
        this.platformValue = platformValue;
    }

    /** @return semantic intent */ public SemanticKey semanticKey() { return semanticKey; }
    /** @return adapter-owned renderer identity */ public DefinitionId rendererId() { return rendererId; }
    /** @return safe platform mapping key, never a platform object */ public String platformValue() { return platformValue; }

    @Override public int hashCode() { return Objects.hash(semanticKey, rendererId, platformValue); }
    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof CompatibilityMapping)) { return false; }
        final CompatibilityMapping that = (CompatibilityMapping) other;
        return semanticKey.equals(that.semanticKey) && rendererId.equals(that.rendererId)
                && platformValue.equals(that.platformValue);
    }
}
