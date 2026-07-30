package io.zartra.bedwars.compat.v1_11;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class Paper111CompatibilityAdapterTest {
    @Test void exactVersionAndMappingsAreStable() {
        final CompatibilityAdapter adapter = Paper111CompatibilityAdapter.create(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        assertEquals("1.11.2", adapter.runtimeClaim().minecraftVersion());
        assertEquals(10, adapter.mappings().mappings().size());
    }
}
