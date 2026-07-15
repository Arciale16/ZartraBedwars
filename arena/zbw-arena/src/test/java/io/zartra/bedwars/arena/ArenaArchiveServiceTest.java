package io.zartra.bedwars.arena;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.application.ArenaArchiveService;
import io.zartra.bedwars.arena.application.ArenaFailures;
import io.zartra.bedwars.arena.archive.ArenaArchive;
import io.zartra.bedwars.arena.archive.ArenaArchiveCodec;
import io.zartra.bedwars.arena.archive.CanonicalArenaArchiveCodec;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.model.MapDefinition;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import java.util.Collections;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArenaArchiveServiceTest {
    private MemoryPorts ports;
    private ArenaArchiveService service;
    private CanonicalArenaArchiveCodec codec;

    @BeforeEach void setUp() {
        ports = new MemoryPorts();
        ports.save(new ArenaRepository.SaveRequest(ArenaTestFixture.complete(), 0, true));
        codec = new CanonicalArenaArchiveCodec();
        service = new ArenaArchiveService(ports, ports, codec,
                new ArenaValidation.DefaultValidator(), ports, ports, ports,
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW));
    }

    @Test void canonicalArchiveRoundTripsEveryAtomicFieldDeterministically() {
        final ArenaBundle bundle = ArenaTestFixture.complete();
        final ArenaArchive first = codec.encode(ArenaTestFixture.id("archive/one"), bundle,
                ArenaTestFixture.NOW).requireValue();
        final ArenaArchive second = codec.encode(ArenaTestFixture.id("archive/two"), bundle,
                ArenaTestFixture.NOW).requireValue();
        assertArrayEquals(first.payload(), second.payload());
        assertEquals(first.sha256(), second.sha256());
        assertEquals(bundle, codec.decode(first).requireValue());
        final byte[] defensive = first.payload();
        defensive[0] = 0;
        assertTrue(first.payload()[0] != 0);
    }

    @Test void envelopeRejectsTamperOversizeUnsupportedAndTrailingData() {
        final ArenaArchive archive = codec.encode(ArenaTestFixture.id("archive/valid"),
                ArenaTestFixture.complete(), ArenaTestFixture.NOW).requireValue();
        final byte[] changed = archive.payload();
        changed[changed.length - 1] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> ArenaArchive.verified(
                archive.archiveId(), archive.arenaId(), archive.mapId(), 1, archive.createdAt(),
                changed, archive.sha256()));
        assertThrows(IllegalArgumentException.class, () -> ArenaArchive.create(
                archive.archiveId(), archive.arenaId(), archive.mapId(), 1, archive.createdAt(),
                new byte[ArenaArchive.MAXIMUM_BYTES + 1]));
        final ArenaArchive unsupported = ArenaArchive.create(archive.archiveId(),
                archive.arenaId(), archive.mapId(), 2, archive.createdAt(), archive.payload());
        assertEquals(ArenaFailures.ARCHIVE, codec.decode(unsupported).error().get());
        final byte[] trailing = Arrays.copyOf(archive.payload(), archive.payload().length + 1);
        final ArenaArchive bad = ArenaArchive.create(archive.archiveId(), archive.arenaId(),
                archive.mapId(), 1, archive.createdAt(), trailing);
        assertEquals(ArenaFailures.ARCHIVE, codec.decode(bad).error().get());
    }

    @Test void minimalArchiveCoversAbsentOptionalFieldsAndRejectsEnvelopeIdentityDrift() {
        final ArenaDefinition arena = ArenaDefinition.builder(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.MAP_ID, "Minimal", ArenaTestFixture.NOW)
                .modes(Collections.singleton(ArenaTestFixture.id("mode/standard")))
                .playerLimits(1, 1, 1).revision(0, ArenaTestFixture.NOW).build();
        final MapDefinition map = new MapDefinition(ArenaTestFixture.MAP_ID, "Minimal",
                ArenaTestFixture.NOW, ArenaTestFixture.NOW, 0,
                ArenaTestFixture.id("template/minimal"), ArenaTestFixture.id("group/default"),
                ArenaTestFixture.id("author/test"), "",
                Collections.singleton(ArenaTestFixture.id("mode/standard")), 1, 1,
                Collections.emptySet(), Collections.emptyMap(),
                ArenaTestFixture.id("validation/pending"));
        final ArenaArchive archive = codec.encode(ArenaTestFixture.id("archive/minimal"),
                new ArenaBundle(arena, map), ArenaTestFixture.NOW).requireValue();
        assertEquals(arena, codec.decode(archive).requireValue().arena());
        final byte[] wrongMagic = archive.payload();
        wrongMagic[0] = 0;
        final ArenaArchive invalidMagic = ArenaArchive.create(archive.archiveId(),
                archive.arenaId(), archive.mapId(), 1, archive.createdAt(), wrongMagic);
        assertEquals(ArenaFailures.ARCHIVE, codec.decode(invalidMagic).error().get());
        final ArenaArchive wrongArena = ArenaArchive.create(archive.archiveId(),
                io.zartra.bedwars.api.identity.ArenaId.random(), archive.mapId(), 1,
                archive.createdAt(), archive.payload());
        assertEquals(ArenaFailures.ARCHIVE, codec.decode(wrongArena).error().get());
        final ArenaArchive wrongMap = ArenaArchive.create(archive.archiveId(), archive.arenaId(),
                io.zartra.bedwars.api.identity.MapId.random(), 1, archive.createdAt(),
                archive.payload());
        assertEquals(ArenaFailures.ARCHIVE, codec.decode(wrongMap).error().get());
        final ArenaArchive truncated = ArenaArchive.create(archive.archiveId(), archive.arenaId(),
                archive.mapId(), 1, archive.createdAt(), Arrays.copyOf(archive.payload(), 6));
        assertEquals(ArenaFailures.ARCHIVE, codec.decode(truncated).error().get());
    }

    @Test void exportBackupRestoreAndImportUseRevisionAndIdentityRules() {
        final CorrelationId correlation = CorrelationId.random();
        final ArenaArchive exported = service.exportArena(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), correlation).requireValue();
        final ArenaArchive backup = service.backup(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(ArenaTestFixture.ARENA_ID, backup.arenaId());
        final ArenaRepository.Record restored = service.restoreBackup(backup.archiveId(),
                ArenaTestFixture.ARENA_ID, 1, ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(2, restored.revision());
        assertEquals(ArenaFailures.CONFLICT, service.restore(exported, 1,
                ArenaTestFixture.actor(), correlation).error().get());

        final MemoryPorts importedPorts = new MemoryPorts();
        final ArenaArchiveService importer = new ArenaArchiveService(importedPorts, importedPorts,
                codec, new ArenaValidation.DefaultValidator(), importedPorts, importedPorts,
                importedPorts, TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW));
        assertEquals(ArenaTestFixture.ARENA_ID, importer.importArena(exported,
                ArenaTestFixture.actor(), correlation).requireValue().bundle().arenaId());
        assertEquals(ArenaFailures.CONFLICT, importer.importArena(exported,
                ArenaTestFixture.actor(), correlation).error().get());
    }

    @Test void archiveAuthorizationAndMissingBackupFailClosed() {
        ports.allow = false;
        assertEquals(ArenaFailures.FORBIDDEN, service.exportArena(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), CorrelationId.random()).error().get());
        ports.allow = true;
        assertEquals(ArenaFailures.NOT_FOUND, service.restoreBackup(
                ArenaTestFixture.id("archive/missing"), ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), CorrelationId.random()).error().get());
    }

    @Test void portCodecTransitionAndRestoreFailuresAreNeverReportedAsSuccess() {
        final CorrelationId correlation = CorrelationId.random();
        ports.failArenaReads = true;
        assertEquals(ArenaFailures.INVALID, service.exportArena(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.INVALID, service.backup(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), correlation).error().get());
        ports.failArenaReads = false;
        ports.failArchiveWrites = true;
        assertEquals(ArenaFailures.INVALID, service.backup(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), correlation).error().get());
        ports.failArchiveWrites = false;
        ports.failArchiveReads = true;
        assertEquals(ArenaFailures.INVALID, service.restoreBackup(
                ArenaTestFixture.id("archive/unavailable"), ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).error().get());
        ports.failArchiveReads = false;

        final ArenaArchive valid = codec.encode(ArenaTestFixture.id("archive/valid_restore"),
                ArenaTestFixture.complete(), ArenaTestFixture.NOW).requireValue();
        assertThrows(IllegalArgumentException.class, () -> service.restore(valid, 0,
                ArenaTestFixture.actor(), correlation));
        ports.allow = false;
        assertEquals(ArenaFailures.FORBIDDEN, service.importArena(valid,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.FORBIDDEN, service.restore(valid, 1,
                ArenaTestFixture.actor(), correlation).error().get());
        ports.allow = true;

        final ArenaArchive unsupported = ArenaArchive.create(valid.archiveId(), valid.arenaId(),
                valid.mapId(), 2, valid.createdAt(), valid.payload());
        assertEquals(ArenaFailures.ARCHIVE, service.importArena(unsupported,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.ARCHIVE, service.restore(unsupported, 1,
                ArenaTestFixture.actor(), correlation).error().get());

        final ArenaBundle source = ArenaTestFixture.complete();
        final ArenaBundle invalidEnabled = new ArenaBundle(source.arena().toBuilder()
                .teams(Collections.emptyList()).status(ArenaDefinition.Status.ENABLED)
                .build(), source.map());
        final ArenaArchive invalid = codec.encode(ArenaTestFixture.id("archive/invalid_enabled"),
                invalidEnabled, ArenaTestFixture.NOW).requireValue();
        assertEquals(ArenaFailures.INVALID, service.importArena(invalid,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.INVALID, service.restore(invalid, 1,
                ArenaTestFixture.actor(), correlation).error().get());
    }

    @Test void encodingFailureIsPropagatedByExportAndBackup() {
        final ArenaArchiveCodec failing = new ArenaArchiveCodec() {
            @Override public io.zartra.bedwars.api.result.Result<ArenaArchive> encode(
                    final io.zartra.bedwars.api.identity.DefinitionId archiveId,
                    final ArenaBundle bundle, final java.time.Instant createdAt) {
                return io.zartra.bedwars.api.result.Result.failure(ArenaFailures.ARCHIVE);
            }
            @Override public io.zartra.bedwars.api.result.Result<ArenaBundle> decode(
                    final ArenaArchive archive) {
                return io.zartra.bedwars.api.result.Result.failure(ArenaFailures.ARCHIVE);
            }
        };
        final ArenaArchiveService failingService = new ArenaArchiveService(ports, ports, failing,
                new ArenaValidation.DefaultValidator(), ports, ports, ports,
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW));
        assertEquals(ArenaFailures.ARCHIVE, failingService.exportArena(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), CorrelationId.random()).error().get());
        assertEquals(ArenaFailures.ARCHIVE, failingService.backup(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), CorrelationId.random()).error().get());
    }
}
