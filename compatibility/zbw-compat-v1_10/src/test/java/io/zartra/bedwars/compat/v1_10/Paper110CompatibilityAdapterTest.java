package io.zartra.bedwars.compat.v1_10;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class Paper110CompatibilityAdapterTest {
    @Test void exactVersionAndMappingsAreStable() {
        final CompatibilityAdapter adapter = Paper110CompatibilityAdapter.create(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        assertEquals("1.10.2", adapter.runtimeClaim().minecraftVersion());
        assertEquals(10, adapter.mappings().mappings().size());
    }
}
