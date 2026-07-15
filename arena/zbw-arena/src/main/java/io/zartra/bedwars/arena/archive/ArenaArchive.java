package io.zartra.bedwars.arena.archive;

import io.zartra.bedwars.api.identity.ArenaId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.MapId;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/** Immutable integrity-checked archive envelope containing no world path or executable content. */
public final class ArenaArchive {
    /** Maximum encoded metadata archive size. */ public static final int MAXIMUM_BYTES = 4 * 1024 * 1024;
    private final DefinitionId archiveId;
    private final ArenaId arenaId;
    private final MapId mapId;
    private final int schemaVersion;
    private final Instant createdAt;
    private final byte[] payload;
    private final String sha256;

    private ArenaArchive(final DefinitionId archiveId, final ArenaId arenaId, final MapId mapId,
                         final int schemaVersion, final Instant createdAt, final byte[] payload,
                         final String expectedSha256) {
        this.archiveId = Objects.requireNonNull(archiveId, "archiveId");
        this.arenaId = Objects.requireNonNull(arenaId, "arenaId");
        this.mapId = Objects.requireNonNull(mapId, "mapId");
        if (schemaVersion < 1) { throw new IllegalArgumentException("schemaVersion must be positive"); }
        this.schemaVersion = schemaVersion;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        if (payload == null || payload.length == 0 || payload.length > MAXIMUM_BYTES) {
            throw new IllegalArgumentException("archive payload is empty or exceeds the size limit");
        }
        this.payload = Arrays.copyOf(payload, payload.length);
        this.sha256 = digest(this.payload);
        if (expectedSha256 != null && !constantTime(this.sha256, expectedSha256)) {
            throw new IllegalArgumentException("archive checksum mismatch");
        }
    }

    /** @return a new envelope computing its checksum */
    public static ArenaArchive create(final DefinitionId archiveId, final ArenaId arenaId,
                                      final MapId mapId, final int schemaVersion,
                                      final Instant createdAt, final byte[] payload) {
        return new ArenaArchive(archiveId, arenaId, mapId, schemaVersion, createdAt, payload, null);
    }
    /** @return an imported envelope only when the supplied checksum matches */
    public static ArenaArchive verified(final DefinitionId archiveId, final ArenaId arenaId,
                                        final MapId mapId, final int schemaVersion,
                                        final Instant createdAt, final byte[] payload,
                                        final String sha256) {
        return new ArenaArchive(archiveId, arenaId, mapId, schemaVersion, createdAt, payload,
                Objects.requireNonNull(sha256, "sha256"));
    }
    private static String digest(final byte[] value) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
            final StringBuilder result = new StringBuilder(64);
            for (byte part : digest) { result.append(String.format("%02x", part & 0xff)); }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }
    private static boolean constantTime(final String first, final String second) {
        return MessageDigest.isEqual(first.getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                second.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
    }
    /** @return archive identity */ public DefinitionId archiveId() { return archiveId; }
    /** @return preserved source arena identity */ public ArenaId arenaId() { return arenaId; }
    /** @return preserved source map identity */ public MapId mapId() { return mapId; }
    /** @return format schema */ public int schemaVersion() { return schemaVersion; }
    /** @return capture time */ public Instant createdAt() { return createdAt; }
    /** @return defensive encoded payload */ public byte[] payload() { return Arrays.copyOf(payload, payload.length); }
    /** @return lower-case SHA-256 integrity checksum */ public String sha256() { return sha256; }
}
