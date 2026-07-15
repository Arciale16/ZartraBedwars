package io.zartra.bedwars.arena.setup;

import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/** Immutable two-phase preview bound to an exact session and draft revision. */
public final class SetupPreview {
    private final SetupSessionId sessionId;
    private final long draftRevision;
    private final ArenaBundle bundle;
    private final ArenaValidation.Report validation;
    private final String baseFingerprint;
    private final String fingerprint;

    /** Creates a revision-bound preview with a deterministic content fingerprint. */
    public SetupPreview(final SetupSessionId sessionId, final long draftRevision,
                        final ArenaBundle bundle, final ArenaValidation.Report validation) {
        this(sessionId, draftRevision, bundle, bundle, validation);
    }

    /** @return a candidate preview cryptographically bound to its unchanged base draft */
    public static SetupPreview candidate(final SetupSessionId sessionId, final long draftRevision,
                                         final ArenaBundle base, final ArenaBundle candidate,
                                         final ArenaValidation.Report validation) {
        return new SetupPreview(sessionId, draftRevision, base, candidate, validation);
    }

    private SetupPreview(final SetupSessionId sessionId, final long draftRevision,
                         final ArenaBundle base, final ArenaBundle bundle,
                         final ArenaValidation.Report validation) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        if (draftRevision < 0L) { throw new IllegalArgumentException("draftRevision is negative"); }
        this.draftRevision = draftRevision;
        this.baseFingerprint = fingerprint(sessionId, draftRevision,
                Objects.requireNonNull(base, "base"));
        this.bundle = Objects.requireNonNull(bundle, "bundle");
        this.validation = Objects.requireNonNull(validation, "validation");
        this.fingerprint = fingerprint(sessionId, draftRevision, bundle);
    }
    private static String fingerprint(final SetupSessionId sessionId, final long revision,
                                      final ArenaBundle bundle) {
        final String material = sessionId + ":" + revision + ":" + bundle.arenaId() + ":"
                + bundle.mapId() + ":" + bundle.arena().version() + ":" + bundle.hashCode();
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(material.getBytes(StandardCharsets.UTF_8));
            final StringBuilder result = new StringBuilder(64);
            for (byte value : digest) { result.append(String.format("%02x", value & 0xff)); }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
    /** @return session identity */ public SetupSessionId sessionId() { return sessionId; }
    /** @return exact draft revision */ public long draftRevision() { return draftRevision; }
    /** @return immutable preview bundle */ public ArenaBundle bundle() { return bundle; }
    /** @return complete validation report */ public ArenaValidation.Report validation() { return validation; }
    /** @return deterministic non-secret preview fingerprint */ public String fingerprint() { return fingerprint; }
    /** @return whether this preview still exactly matches a session */
    public boolean matches(final SetupSession session) {
        return sessionId.equals(session.id()) && draftRevision == session.draftRevision()
                && baseFingerprint.equals(fingerprint(sessionId, draftRevision, session.draft()));
    }

    /** @return a derived candidate retaining the original base-draft integrity binding */
    public SetupPreview withBundle(final ArenaBundle value,
                                   final ArenaValidation.Report report) {
        return new SetupPreview(sessionId, draftRevision, baseFingerprint,
                Objects.requireNonNull(value, "value"), Objects.requireNonNull(report, "report"));
    }

    private SetupPreview(final SetupSessionId sessionId, final long draftRevision,
                         final String baseFingerprint, final ArenaBundle bundle,
                         final ArenaValidation.Report validation) {
        this.sessionId = sessionId;
        this.draftRevision = draftRevision;
        this.baseFingerprint = baseFingerprint;
        this.bundle = bundle;
        this.validation = validation;
        this.fingerprint = fingerprint(sessionId, draftRevision, bundle);
    }
}
