package io.zartra.bedwars.redis.security;


import io.zartra.bedwars.redis.api.OperationId;

import io.zartra.bedwars.redis.api.SchemaVersion;

import java.time.Instant;

import java.util.Arrays;

import java.util.Objects;

import java.util.UUID;


/** Immutable authenticated Redis record with bounded payload and privacy-safe headers. */
public final class AuthenticatedRedisEnvelope {
    /** Redis record payload ceiling. */ public static final int MAX_PAYLOAD_BYTES = 256 * 1024;

    private final UUID messageId;

    private final OperationId operationId;

    private final String installation;

    private final String environment;

    private final String audience;

    private final SchemaVersion schema;

    private final Instant issuedAt;

    private final Instant deadline;

    private final byte[] nonce;

    private final String keyId;

    private final byte[] payload;

    private final byte[] signature;

    private AuthenticatedRedisEnvelope(final Builder builder, final byte[] signature) {
        messageId = Objects.requireNonNull(builder.messageId, "messageId");

        operationId = Objects.requireNonNull(builder.operationId, "operationId");

        installation = token(builder.installation, "installation");
 environment = token(builder.environment, "environment");
 audience = token(builder.audience, "audience");

        schema = Objects.requireNonNull(builder.schema, "schema");
 issuedAt = Objects.requireNonNull(builder.issuedAt, "issuedAt");
 deadline = Objects.requireNonNull(builder.deadline, "deadline");

        if (!deadline.isAfter(issuedAt)) { throw new IllegalArgumentException("deadline must follow issue time");
 }
        nonce = copy(builder.nonce, 16, 16, "nonce");
 payload = copy(builder.payload, 0, MAX_PAYLOAD_BYTES, "payload");

        keyId = token(builder.keyId, "keyId");
 this.signature = signature == null ? new byte[0] : copy(signature, 32, 32, "signature");

    }
    /** Starts an envelope builder. */ public static Builder builder() { return new Builder();
 }
    /** Returns a copy carrying the supplied signature. */ public AuthenticatedRedisEnvelope signed(final byte[] value) { return new AuthenticatedRedisEnvelope(toBuilder(), value);
 }
    /** Returns canonical bytes excluding signature. */
    public byte[] canonicalBytes() {
        final String header = messageId + "\n" + operationId + "\n" + installation + "\n" + environment + "\n" + audience + "\n" + schema + "\n" + issuedAt.toEpochMilli() + "\n" + deadline.toEpochMilli() + "\n" + keyId + "\n" + payload.length + "\n";

        final byte[] prefix = header.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        final byte[] value = new byte[prefix.length + nonce.length + payload.length];

        System.arraycopy(prefix, 0, value, 0, prefix.length);
 System.arraycopy(nonce, 0, value, prefix.length, nonce.length);
 System.arraycopy(payload, 0, value, prefix.length + nonce.length, payload.length);

        return value;

    }
    /** Returns message ID. */ public UUID messageId() { return messageId;
 }
    /** Returns operation ID. */ public OperationId operationId() { return operationId;
 }
    /** Returns installation. */ public String installation() { return installation;
 }
    /** Returns environment. */ public String environment() { return environment;
 }
    /** Returns audience. */ public String audience() { return audience;
 }
    /** Returns schema. */ public SchemaVersion schema() { return schema;
 }
    /** Returns issue instant. */ public Instant issuedAt() { return issuedAt;
 }
    /** Returns deadline. */ public Instant deadline() { return deadline;
 }
    /** Returns nonce copy. */ public byte[] nonce() { return Arrays.copyOf(nonce, nonce.length);
 }
    /** Returns key ID. */ public String keyId() { return keyId;
 }
    /** Returns payload copy. */ public byte[] payload() { return Arrays.copyOf(payload, payload.length);
 }
    /** Returns signature copy. */ public byte[] signature() { return Arrays.copyOf(signature, signature.length);
 }
    private Builder toBuilder() { return builder().messageId(messageId).operationId(operationId).installation(installation).environment(environment).audience(audience).schema(schema).issuedAt(issuedAt).deadline(deadline).nonce(nonce).keyId(keyId).payload(payload);
 }
    private static String token(final String value, final String name) { if (value == null || !value.matches("[A-Za-z0-9._-]{1,64}")) { throw new IllegalArgumentException("invalid " + name);
 } return value;
 }
    private static byte[] copy(final byte[] value, final int min, final int max, final String name) { if (value == null || value.length < min || value.length > max) { throw new IllegalArgumentException("invalid " + name);
 } return Arrays.copyOf(value, value.length);
 }
    /** Mutable construction helper consumed once into an immutable envelope. */
    public static final class Builder {
        private UUID messageId;
 private OperationId operationId;
 private String installation;
 private String environment;
 private String audience;
 private SchemaVersion schema;
 private Instant issuedAt;
 private Instant deadline;
 private byte[] nonce;
 private String keyId;
 private byte[] payload;

        /** Sets message ID. */ public Builder messageId(final UUID value) { messageId = value;
 return this;
 }
        /** Sets operation ID. */ public Builder operationId(final OperationId value) { operationId = value;
 return this;
 }
        /** Sets installation. */ public Builder installation(final String value) { installation = value;
 return this;
 }
        /** Sets environment. */ public Builder environment(final String value) { environment = value;
 return this;
 }
        /** Sets audience. */ public Builder audience(final String value) { audience = value;
 return this;
 }
        /** Sets schema. */ public Builder schema(final SchemaVersion value) { schema = value;
 return this;
 }
        /** Sets issue time. */ public Builder issuedAt(final Instant value) { issuedAt = value;
 return this;
 }
        /** Sets deadline. */ public Builder deadline(final Instant value) { deadline = value;
 return this;
 }
        /** Sets nonce. */ public Builder nonce(final byte[] value) { nonce = value == null ? null : Arrays.copyOf(value, value.length);
 return this;
 }
        /** Sets key ID. */ public Builder keyId(final String value) { keyId = value;
 return this;
 }
        /** Sets payload. */ public Builder payload(final byte[] value) { payload = value == null ? null : Arrays.copyOf(value, value.length);
 return this;
 }
        /** Builds an unsigned envelope for signing. */ public AuthenticatedRedisEnvelope build() { return new AuthenticatedRedisEnvelope(this, null);
 }
    }
}
