package io.zartra.bedwars.sdk.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.extension.Extension;
import io.zartra.bedwars.api.migration.MigrationApi;
import java.time.Duration;
import java.util.Collections;
import org.junit.jupiter.api.Test;

class ExampleMigrationExtensionTest {
    @Test
    void exampleUsesPublicLifecycleAndMigrationContracts() {
        final ExampleMigrationExtension extension = new ExampleMigrationExtension();
        assertEquals(Extension.State.RUNNING,
                extension.start().toCompletableFuture().join().requireValue());
        assertEquals(Extension.State.DRAINING,
                extension.drain(Duration.ofSeconds(1)).toCompletableFuture().join().requireValue());
        assertEquals(Extension.State.STOPPED,
                extension.stop().toCompletableFuture().join().requireValue());
        assertEquals(MigrationApi.ConversionState.MAPPED,
                extension.convert(new MigrationApi.Record("hello", "example-greeting",
                        Collections.singletonMap("message-key", "example.hello"))).state());
        assertTrue(extension.metadata().providedCapabilities().values().size() == 1);
        assertThrows(IllegalArgumentException.class, () -> extension.drain(Duration.ZERO));
    }
}
