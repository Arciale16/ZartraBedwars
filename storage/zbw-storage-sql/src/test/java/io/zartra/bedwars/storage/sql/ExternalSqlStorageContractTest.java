package io.zartra.bedwars.storage.sql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.event.EventMetadata;
import io.zartra.bedwars.api.identity.CorrelationId;
import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.EventId;
import io.zartra.bedwars.api.identity.EventTypeId;
import io.zartra.bedwars.api.identity.IdempotencyKey;
import io.zartra.bedwars.storage.api.MessageEnvelope;
import io.zartra.bedwars.storage.api.RecordKey;
import io.zartra.bedwars.storage.api.RecordRevision;
import io.zartra.bedwars.storage.api.StorageEngine.EngineKind;
import io.zartra.bedwars.storage.api.StoredRecord;
import io.zartra.bedwars.storage.api.TransactionOptions;
import io.zartra.bedwars.storage.api.UnitOfWork;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/** MySQL and MariaDB Testcontainers contracts using only operator-audited digest references. */
final class ExternalSqlStorageContractTest {
    private static final Instant NOW = Instant.parse("2026-07-15T00:00:00Z");

    @Test void mysqlContract() throws Exception {
        final DockerImageName image = image("ZBW_TEST_MYSQL_IMAGE");
        try (MySQLContainer<?> container = new MySQLContainer<>(image)
                .withDatabaseName("zartra").withUsername("zartra").withPassword("test-secret")) {
            container.start();
            runContract(container, EngineKind.MYSQL);
        }
    }

    @Test void mariaDbContract() throws Exception {
        final DockerImageName image = image("ZBW_TEST_MARIADB_IMAGE");
        try (MariaDBContainer<?> container = new MariaDBContainer<>(image)
                .withDatabaseName("zartra").withUsername("zartra").withPassword("test-secret")) {
            container.start();
            runContract(container, EngineKind.MARIADB);
        }
    }

    private static DockerImageName image(final String variable) {
        final String reference = System.getenv(variable);
        Assumptions.assumeTrue(reference != null && reference.matches("[^@]+@sha256:[0-9a-f]{64}"),
                variable + " must contain an audited digest reference");
        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker-compatible Testcontainers runtime is required");
        return DockerImageName.parse(reference);
    }

    private static void runContract(final JdbcDatabaseContainer<?> container,
                                    final EngineKind kind) throws Exception {
        final JdbcStorageEngine engine = JdbcStorageEngine.open(SqlStorageConfiguration.of(
                kind, container.getJdbcUrl(), container.getUsername(),
                container.getPassword().toCharArray(), 4, Duration.ofSeconds(10),
                Duration.ofSeconds(3))).requireValue();
        final RecordKey key = key("external");
        try (UnitOfWork transaction = write(engine)) {
            assertEquals(1L, engine.records().save(transaction, record(key),
                    RecordRevision.initial()).requireValue().revision().value());
            assertTrue(engine.messages().enqueue(transaction, envelope("external")).requireValue());
            assertFalse(engine.messages().enqueue(transaction, envelope("external")).requireValue());
            transaction.commit();
        }
        try (UnitOfWork read = engine.begin(options(TransactionOptions.AccessMode.READ_ONLY)).requireValue()) {
            assertTrue(engine.records().find(read, key).requireValue().isPresent());
            read.rollback();
        }

        final ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            final Future<Boolean> left = workers.submit(insert(engine, "left"));
            final Future<Boolean> right = workers.submit(insert(engine, "right"));
            assertTrue(left.get());
            assertTrue(right.get());
        } finally {
            workers.shutdownNow();
        }
        assertTrue(engine.poolHealth().total() <= 4);
        engine.close();
    }

    private static Callable<Boolean> insert(final JdbcStorageEngine engine, final String id) {
        return new Callable<Boolean>() {
            @Override public Boolean call() {
                try (UnitOfWork transaction = write(engine)) {
                    final boolean success = engine.records().save(transaction, record(key(id)),
                            RecordRevision.initial()).isSuccess();
                    if (success) { transaction.commit(); }
                    return success;
                }
            }
        };
    }
    private static UnitOfWork write(final JdbcStorageEngine engine) {
        return engine.begin(options(TransactionOptions.AccessMode.READ_WRITE)).requireValue();
    }
    private static TransactionOptions options(final TransactionOptions.AccessMode mode) {
        return TransactionOptions.of(mode, Duration.ofSeconds(3), 3);
    }
    private static RecordKey key(final String id) {
        return RecordKey.of(DefinitionId.of("zartra", "contract"), DefinitionId.of("zartra", id));
    }
    private static StoredRecord record(final RecordKey key) {
        return StoredRecord.of(key, RecordRevision.initial(), 1, new byte[] {1}, NOW);
    }
    private static MessageEnvelope envelope(final String id) {
        return MessageEnvelope.of(IdempotencyKey.of("zartra", id), EventMetadata.of(
                EventId.of(UUID.nameUUIDFromBytes(id.getBytes(java.nio.charset.StandardCharsets.UTF_8))),
                EventTypeId.of("zartra", "contract"), CorrelationId.of(UUID.fromString(
                        "00000000-0000-0000-0000-000000000001")), NOW, 1, 1,
                EventMetadata.ThreadContext.APPLICATION_WORKER), new byte[] {2}, NOW);
    }
}
