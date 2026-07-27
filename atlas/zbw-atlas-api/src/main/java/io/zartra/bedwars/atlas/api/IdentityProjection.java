package io.zartra.bedwars.atlas.api;

import java.util.Objects;
import java.util.Optional;

/** Immutable identity projection that is anonymous unless explicitly authorized. */
public final class IdentityProjection {
    private final AtlasCaseId caseId;
    private final AnonymizedIdentity anonymousIdentity;
    private final String identityVaultReference;

    private IdentityProjection(final AtlasCaseId caseId,
                               final AnonymizedIdentity anonymousIdentity,
                               final String identityVaultReference) {
        this.caseId = Objects.requireNonNull(caseId, "caseId");
        this.anonymousIdentity = Objects.requireNonNull(anonymousIdentity, "anonymousIdentity");
        this.identityVaultReference = identityVaultReference;
    }

    /** Creates the mandatory default community projection. */
    public static IdentityProjection anonymous(final AtlasCaseId caseId,
                                               final AnonymizedIdentity identity) {
        return new IdentityProjection(caseId, identity, null);
    }

    /** Creates a restricted projection after explicit authorization, using only an opaque vault key. */
    public IdentityProjection reveal(final IdentityRevealRequest request,
                                     final String identityVaultReference) {
        Objects.requireNonNull(request, "request");
        if (!caseId.equals(request.caseId()) || !request.authorized()) {
            throw new SecurityException("identity reveal was not authorized for this case");
        }
        if (identityVaultReference == null || identityVaultReference.trim().isEmpty()
                || identityVaultReference.length() > 160
                || !identityVaultReference.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException("identityVaultReference must be an opaque token");
        }
        return new IdentityProjection(caseId, anonymousIdentity, identityVaultReference);
    }

    /** Returns target case identity. */ public AtlasCaseId caseId() { return caseId; }
    /** Returns the community-safe alias. */
    public AnonymizedIdentity anonymousIdentity() { return anonymousIdentity; }
    /** Returns whether an authorized vault reference is present. */
    public boolean revealed() { return identityVaultReference != null; }
    /** Returns an opaque identity-vault lookup key, never raw identity data. */
    public Optional<String> identityVaultReference() {
        return Optional.ofNullable(identityVaultReference);
    }
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IdentityProjection)) {
            return false;
        }
        IdentityProjection that = (IdentityProjection) other;
        return caseId.equals(that.caseId)
                && anonymousIdentity.equals(that.anonymousIdentity)
                && Objects.equals(identityVaultReference, that.identityVaultReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, anonymousIdentity, identityVaultReference);
    }
}
