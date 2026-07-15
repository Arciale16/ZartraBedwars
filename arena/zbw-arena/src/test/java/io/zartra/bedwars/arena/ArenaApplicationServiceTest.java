package io.zartra.bedwars.arena;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.arena.application.ArenaApplicationService;
import io.zartra.bedwars.arena.application.ArenaFailures;
import io.zartra.bedwars.arena.application.ArenaPolicy;
import io.zartra.bedwars.arena.model.ArenaBundle;
import io.zartra.bedwars.arena.model.ArenaDefinition;
import io.zartra.bedwars.arena.spi.ArenaRepository;
import io.zartra.bedwars.arena.validation.ArenaValidation;
import io.zartra.bedwars.world.api.WorldKey;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ArenaApplicationServiceTest {
    private MemoryPorts ports;
    private ArenaApplicationService service;
    private CorrelationId correlation;

    @BeforeEach void setUp() {
        ports = new MemoryPorts();
        service = service(ports, 10);
        correlation = CorrelationId.random();
    }

    @Test void crudRenameDuplicateAndListPreserveIdentityRules() {
        final ArenaBundle original = ArenaTestFixture.complete();
        final ArenaRepository.Record created = service.create(original, ArenaTestFixture.actor(),
                correlation).requireValue();
        assertEquals(1, created.revision());
        assertEquals(created, service.find(original.arenaId(), ArenaTestFixture.actor(),
                correlation).requireValue().get());
        assertEquals(1, service.list(ArenaTestFixture.actor(), correlation).requireValue().size());

        final ArenaRepository.Record renamed = service.rename(original.arenaId(), 1, "Renamed",
                ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(original.arenaId(), renamed.bundle().arenaId());
        assertEquals(original.mapId(), renamed.bundle().mapId());
        assertEquals("Renamed", renamed.bundle().map().displayName());

        final ArenaRepository.Record duplicate = service.duplicate(original.arenaId(), 2,
                "Independent", WorldKey.of("independent_world"), ArenaTestFixture.actor(),
                correlation).requireValue();
        assertNotEquals(original.arenaId(), duplicate.bundle().arenaId());
        assertNotEquals(original.mapId(), duplicate.bundle().mapId());
        assertEquals(original.arena().teams(), duplicate.bundle().arena().teams());
        assertEquals(ArenaDefinition.Status.DISABLED, duplicate.bundle().arena().status());
        assertEquals(2, service.list(ArenaTestFixture.actor(), correlation).requireValue().size());

        assertTrue(service.delete(duplicate.bundle().arenaId(), 1, ArenaTestFixture.actor(),
                correlation).requireValue());
        assertFalse(service.delete(duplicate.bundle().arenaId(), 1, ArenaTestFixture.actor(),
                correlation).requireValue());
        assertTrue(ports.audits.size() >= 8);
        assertTrue(ports.events.size() >= 6);
    }

    @Test void enablePromotesLastKnownGoodAndRestoreRollsBackLaterRename() {
        service.create(ArenaTestFixture.complete(), ArenaTestFixture.actor(), correlation);
        final ArenaRepository.Record enabled = service.enable(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals(ArenaDefinition.Status.ENABLED, enabled.bundle().arena().status());
        assertTrue(enabled.lastKnownGood().isPresent());
        final ArenaRepository.Record renamed = service.rename(ArenaTestFixture.ARENA_ID, 2,
                "Changed", ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals("Changed", renamed.bundle().arena().displayName());
        final ArenaRepository.Record restored = service.restoreLastKnownGood(
                ArenaTestFixture.ARENA_ID, 3, ArenaTestFixture.actor(), correlation).requireValue();
        assertEquals("Original", restored.bundle().arena().displayName());
        assertTrue(service.validate(ArenaTestFixture.ARENA_ID, ArenaTestFixture.actor(),
                correlation).requireValue().mayEnable());
    }

    @Test void invalidArenaCannotEnableAndStaleRevisionCannotMutate() {
        final ArenaBundle source = ArenaTestFixture.complete();
        final ArenaBundle invalid = new ArenaBundle(source.arena().toBuilder()
                .teams(Collections.emptyList()).build(), source.map());
        service.create(invalid, ArenaTestFixture.actor(), correlation);
        assertEquals(ArenaFailures.INVALID, service.enable(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.CONFLICT, service.rename(ArenaTestFixture.ARENA_ID, 2,
                "Stale", ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.NOT_FOUND, service.restoreLastKnownGood(
                io.zartra.bedwars.api.identity.ArenaId.random(), 1, ArenaTestFixture.actor(),
                correlation).error().orElse(ArenaFailures.NOT_FOUND));
    }

    @Test void authorizationCancellationCapacityAndConflictsAreDefaultSafe() {
        ports.allow = false;
        assertEquals(ArenaFailures.FORBIDDEN, service.create(ArenaTestFixture.complete(),
                ArenaTestFixture.actor(), correlation).error().get());
        ports.allow = true;
        ports.cancel = true;
        assertEquals(ArenaFailures.FORBIDDEN, service.create(ArenaTestFixture.complete(),
                ArenaTestFixture.actor(), correlation).error().get());
        ports.cancel = false;
        service = service(ports, 1);
        assertTrue(service.create(ArenaTestFixture.complete(), ArenaTestFixture.actor(),
                correlation).isSuccess());
        final Result<ArenaRepository.Record> duplicate = service.duplicate(
                ArenaTestFixture.ARENA_ID, 1, "Capacity", WorldKey.of("capacity_world"),
                ArenaTestFixture.actor(), correlation);
        assertEquals(ArenaFailures.CAPACITY, duplicate.error().get());
        assertEquals(ArenaFailures.CAPACITY, service.create(ArenaTestFixture.complete(),
                ArenaTestFixture.actor(), correlation).error().get());
    }

    @Test void repositoryFailuresMissingRecordsAndEveryAuthorizationSurfaceFailClosed() {
        ports.failArenaLists = true;
        assertEquals(ArenaFailures.INVALID, service.create(ArenaTestFixture.complete(),
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.INVALID, service.list(ArenaTestFixture.actor(), correlation)
                .error().get());
        ports.failArenaLists = false;
        ports.failArenaReads = true;
        assertEquals(ArenaFailures.INVALID, service.find(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.INVALID, service.rename(ArenaTestFixture.ARENA_ID, 1,
                "Failure", ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.INVALID, service.restoreLastKnownGood(
                ArenaTestFixture.ARENA_ID, 1, ArenaTestFixture.actor(), correlation).error().get());
        ports.failArenaReads = false;
        ports.failArenaWrites = true;
        assertEquals(ArenaFailures.INVALID, service.delete(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).error().get());
        ports.failArenaWrites = false;

        assertFalse(service.find(ArenaTestFixture.ARENA_ID, ArenaTestFixture.actor(), correlation)
                .requireValue().isPresent());
        assertEquals(ArenaFailures.NOT_FOUND, service.validate(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), correlation).error().get());
        assertThrows(IllegalArgumentException.class, () -> service.rename(
                ArenaTestFixture.ARENA_ID, 0, "Invalid", ArenaTestFixture.actor(), correlation));
        service.create(ArenaTestFixture.complete(), ArenaTestFixture.actor(), correlation);
        assertEquals(ArenaFailures.CONFLICT, service.create(ArenaTestFixture.complete(),
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.CONFLICT, service.restoreLastKnownGood(
                ArenaTestFixture.ARENA_ID, 2, ArenaTestFixture.actor(), correlation).error().get());

        ports.allow = false;
        assertEquals(ArenaFailures.FORBIDDEN, service.find(ArenaTestFixture.ARENA_ID,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.FORBIDDEN, service.list(ArenaTestFixture.actor(), correlation)
                .error().get());
        assertEquals(ArenaFailures.FORBIDDEN, service.delete(ArenaTestFixture.ARENA_ID, 1,
                ArenaTestFixture.actor(), correlation).error().get());
        assertEquals(ArenaFailures.FORBIDDEN, service.restoreLastKnownGood(
                ArenaTestFixture.ARENA_ID, 1, ArenaTestFixture.actor(), correlation).error().get());
    }

    private static ArenaApplicationService service(final MemoryPorts ports,
                                                   final int maximumArenas) {
        return new ArenaApplicationService(ports, ports, new ArenaValidation.DefaultValidator(),
                ArenaPolicy.of(maximumArenas, 10, Duration.ofSeconds(30),
                        Duration.ofSeconds(5)), ports, ports, ports,
                TimeSource.FixedTimeSource.at(ArenaTestFixture.NOW));
    }
}
