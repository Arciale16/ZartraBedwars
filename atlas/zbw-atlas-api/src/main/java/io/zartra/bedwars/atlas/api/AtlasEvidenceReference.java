package io.zartra.bedwars.atlas.api;

import io.zartra.bedwars.replay.api.ReplayId;
import java.util.Objects;
import java.util.Optional;

/** Immutable reference to evidence owned by replay or another authoritative source. */
public final class AtlasEvidenceReference {
    /** Supported evidence-reference families. */
    public enum Type { REPLAY_SEGMENT, REPORT, INTERNAL_SIGNAL }

    private final AtlasEvidenceId evidenceId;
    private final Type type;
    private final ReplayId replayId;
    private final String externalReference;
    private final long startMillis;
    private final long endMillis;

    private AtlasEvidenceReference(final AtlasEvidenceId evidenceId, final Type type,
                                   final ReplayId replayId, final String externalReference,
                                   final long startMillis, final long endMillis) {
        if (startMillis < 0 || endMillis < startMillis) {
            throw new IllegalArgumentException("evidence time range is invalid");
        }
        this.evidenceId = Objects.requireNonNull(evidenceId, "evidenceId");
        this.type = Objects.requireNonNull(type, "type");
        this.replayId = replayId;
        this.externalReference = externalReference;
        this.startMillis = startMillis;
        this.endMillis = endMillis;
    }

    /** References an interval in an M17 replay without copying or owning its payload. */
    public static AtlasEvidenceReference replay(final AtlasEvidenceId evidenceId,
                                                final ReplayId replayId,
                                                final long startMillis,
                                                final long endMillis) {
        return new AtlasEvidenceReference(evidenceId, Type.REPLAY_SEGMENT,
                Objects.requireNonNull(replayId, "replayId"), null, startMillis, endMillis);
    }

    /** References an opaque report record held by its authoritative provider. */
    public static AtlasEvidenceReference report(final AtlasEvidenceId evidenceId,
                                                final String reference) {
        return external(evidenceId, Type.REPORT, reference);
    }

    /** References an opaque internal-signal record held by its authoritative provider. */
    public static AtlasEvidenceReference internalSignal(final AtlasEvidenceId evidenceId,
                                                        final String reference) {
        return external(evidenceId, Type.INTERNAL_SIGNAL, reference);
    }

    private static AtlasEvidenceReference external(final AtlasEvidenceId evidenceId,
                                                   final Type type, final String reference) {
        if (reference == null || reference.trim().isEmpty() || reference.length() > 160
                || !reference.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("external evidence reference must be an opaque token");
        }
        return new AtlasEvidenceReference(evidenceId, type, null, reference, 0, 0);
    }

    /** Returns Atlas-local evidence identity. */ public AtlasEvidenceId evidenceId() { return evidenceId; }
    /** Returns the evidence family. */ public Type type() { return type; }
    /** Returns the M17 replay identity when this is replay evidence. */
    public Optional<ReplayId> replayId() { return Optional.ofNullable(replayId); }
    /** Returns the opaque provider reference for non-replay evidence. */
    public Optional<String> externalReference() { return Optional.ofNullable(externalReference); }
    /** Returns inclusive replay start offset. */ public long startMillis() { return startMillis; }
    /** Returns inclusive replay end offset. */ public long endMillis() { return endMillis; }

    @Override public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (!(other instanceof AtlasEvidenceReference)) { return false; }
        final AtlasEvidenceReference that = (AtlasEvidenceReference) other;
        return startMillis == that.startMillis && endMillis == that.endMillis
                && evidenceId.equals(that.evidenceId) && type == that.type
                && Objects.equals(replayId, that.replayId)
                && Objects.equals(externalReference, that.externalReference);
    }
    @Override public int hashCode() {
        return Objects.hash(evidenceId, type, replayId, externalReference, startMillis, endMillis);
    }
}
