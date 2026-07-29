package io.zartra.bedwars.proxy.api;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/** Immutable signed transport bytes. Payload is opaque until authentication succeeds. */
public final class SignedProxyMessage {
    /** Maximum authenticated proxy message size (64 KiB). */
    public static final int MAX_PAYLOAD_BYTES = 65536;
    private final ProtocolVersion protocol;
    private final String environment;
    private final String audience;
    private final String keyId;
    private final String nonce;
    private final Instant issuedAt;
    private final Instant deadline;
    private final byte[] payload;
    private final byte[] signature;

    private SignedProxyMessage(final ProtocolVersion protocol, final String environment,
            final String audience, final String keyId, final String nonce,
            final Instant issuedAt, final Instant deadline, final byte[] payload,
            final byte[] signature) {
        this.protocol = Objects.requireNonNull(protocol, "protocol");
        this.environment = ProxyContractValidation.token(environment, "environment");
        this.audience = ProxyContractValidation.token(audience, "audience");
        this.keyId = ProxyContractValidation.token(keyId, "keyId");
        this.nonce = ProxyContractValidation.token(nonce, "nonce");
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.deadline = Objects.requireNonNull(deadline, "deadline");
        if (!deadline.isAfter(issuedAt)) {
            throw new IllegalArgumentException("deadline must follow issue time");
        }
        if (payload == null || payload.length == 0 || payload.length > MAX_PAYLOAD_BYTES) {
            throw new IllegalArgumentException("invalid payload size");
        }
        if (signature == null || signature.length != ProxyMessageSecurity.SIGNATURE_BYTES) {
            throw new IllegalArgumentException("invalid signature size");
        }
        this.payload = payload.clone();
        this.signature = signature.clone();
    }

    /** Creates signed bytes after caller-side signing. */
    public static SignedProxyMessage of(final ProtocolVersion protocol, final String environment,
            final String audience, final String keyId, final String nonce,
            final Instant issuedAt, final Instant deadline, final byte[] payload,
            final byte[] signature) {
        return new SignedProxyMessage(protocol, environment, audience, keyId, nonce,
                issuedAt, deadline, payload, signature);
    }
    /** Returns protocol. */ public ProtocolVersion protocol() { return protocol; }
    /** Returns environment. */ public String environment() { return environment; }
    /** Returns audience. */ public String audience() { return audience; }
    /** Returns key ID. */ public String keyId() { return keyId; }
    /** Returns nonce. */ public String nonce() { return nonce; }
    /** Returns issue time. */ public Instant issuedAt() { return issuedAt; }
    /** Returns deadline. */ public Instant deadline() { return deadline; }
    /** Returns a payload copy; call only after security verification. */ public byte[] payload() { return payload.clone(); }
    /** Returns a signature copy. */ public byte[] signature() { return signature.clone(); }
    @Override public boolean equals(final Object other) {
        if (!(other instanceof SignedProxyMessage)) { return false; }
        SignedProxyMessage value = (SignedProxyMessage) other;
        return protocol.equals(value.protocol) && environment.equals(value.environment)
                && audience.equals(value.audience) && keyId.equals(value.keyId)
                && nonce.equals(value.nonce) && issuedAt.equals(value.issuedAt)
                && deadline.equals(value.deadline) && Arrays.equals(payload, value.payload)
                && Arrays.equals(signature, value.signature);
    }
    @Override public int hashCode() {
        int result = Objects.hash(protocol, environment, audience, keyId, nonce, issuedAt, deadline);
        return 31 * (31 * result + Arrays.hashCode(payload)) + Arrays.hashCode(signature);
    }
}
