package io.zartra.bedwars.party.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.PartyId;
import io.zartra.bedwars.api.identity.PlayerId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.party.Party;
import io.zartra.bedwars.party.PartyMigrationPolicy;
import io.zartra.bedwars.party.PartyRepository;
import io.zartra.bedwars.party.PartyState;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.sqlite.SQLiteDataSource;

final class JdbcPartyRepositoryTest {
    private static final Instant NOW = Instant.parse("2026-07-29T10:00:00Z");
    @TempDir Path temporaryDirectory;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    @Test
    void roundTripRestartAndOptimisticConflict() {
        SQLiteDataSource source = source("party.db");
        PartySchemaMigrator migrator = new PartySchemaMigrator();
        assertTrue(migrator.migrate(source, executor).toCompletableFuture().join());
        assertFalse(migrator.migrate(source, executor).toCompletableFuture().join());

        JdbcPartyRepository firstRepository = new JdbcPartyRepository(source, executor);
        PlayerId leader = player(1);
        PlayerId member = player(2);
        Party created = Party.create(PartyId.of(uuid(10)), leader).activate()
                .invite(leader, member, NOW, NOW.plusSeconds(60))
                .accept(member, NOW.plusSeconds(1));
        assertTrue(firstRepository.create(created).toCompletableFuture().join());

        JdbcPartyRepository restarted = new JdbcPartyRepository(source, executor);
        Party loaded = restarted.find(created.partyId()).toCompletableFuture().join()
                .orElseThrow(AssertionError::new);
        assertEquals(created.state(), loaded.state());
        assertEquals(created.members(), loaded.members());
        assertEquals(created.revision(), loaded.revision());
        assertEquals(created.partyId(), restarted.findByMember(member)
                .toCompletableFuture().join().orElseThrow(AssertionError::new).partyId());

        Party updated = loaded.removeMember(member);
        assertEquals(PartyRepository.SaveResult.UPDATED,
                restarted.save(updated, loaded.revision()).toCompletableFuture().join());
        assertEquals(PartyRepository.SaveResult.CONFLICT,
                restarted.save(updated.withPrivacy(updated.privacy()), loaded.revision())
                        .toCompletableFuture().join());
    }

    @Test
    void duplicateMemberRollsBackWholeParty() {
        SQLiteDataSource source = source("duplicate.db");
        new PartySchemaMigrator().migrate(source, executor).toCompletableFuture().join();
        JdbcPartyRepository repository = new JdbcPartyRepository(source, executor);
        PlayerId shared = player(1);
        Party first = Party.create(PartyId.of(uuid(20)), shared);
        Party second = Party.create(PartyId.of(uuid(21)), shared);
        assertTrue(repository.create(first).toCompletableFuture().join());
        assertFalse(repository.create(second).toCompletableFuture().join());
        assertFalse(repository.find(second.partyId()).toCompletableFuture().join().isPresent());
    }

    @Test
    void migratingPartyRecoversWithoutSplitAuthority() {
        SQLiteDataSource source = source("recovery.db");
        new PartySchemaMigrator().migrate(source, executor).toCompletableFuture().join();
        JdbcPartyRepository repository = new JdbcPartyRepository(source, executor);
        Party party = Party.create(PartyId.of(uuid(30)), player(1)).activate()
                .beginMigration(ProviderId.of("parties", "alessiodp"),
                        new PartyMigrationPolicy());
        assertTrue(repository.create(party).toCompletableFuture().join());

        Party recovered = new JdbcPartyRepository(source, executor).find(party.partyId())
                .toCompletableFuture().join().orElseThrow(AssertionError::new);
        assertEquals(PartyState.MIGRATING, recovered.state());
        assertEquals(ProviderId.of("parties", "alessiodp"),
                recovered.migrationTarget().orElseThrow(AssertionError::new));
    }

    @Test
    void malformedPersistenceDataFailsClosed() throws SQLException {
        SQLiteDataSource source = source("malformed.db");
        new PartySchemaMigrator().migrate(source, executor).toCompletableFuture().join();
        Party party = Party.create(PartyId.of(uuid(40)), player(1));
        JdbcPartyRepository repository = new JdbcPartyRepository(source, executor);
        assertTrue(repository.create(party).toCompletableFuture().join());
        execute(source, "UPDATE parties SET state='BROKEN' WHERE party_id='"
                + party.partyId() + "'");

        CompletionException failure = assertThrows(CompletionException.class,
                () -> repository.find(party.partyId()).toCompletableFuture().join());
        assertTrue(failure.getCause() instanceof PartyPersistenceException);
    }

    private SQLiteDataSource source(final String name) {
        SQLiteDataSource source = new SQLiteDataSource();
        source.setUrl("jdbc:sqlite:" + temporaryDirectory.resolve(name));
        return source;
    }

    private static void execute(final SQLiteDataSource source, final String sql)
            throws SQLException {
        try (Connection connection = source.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static PlayerId player(final int value) { return PlayerId.of(uuid(value)); }

    private static UUID uuid(final int value) { return new UUID(0, value); }
}
