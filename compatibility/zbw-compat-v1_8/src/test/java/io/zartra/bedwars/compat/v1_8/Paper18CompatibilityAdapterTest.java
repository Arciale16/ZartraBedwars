package io.zartra.bedwars.compat.v1_8;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class Paper18CompatibilityAdapterTest {
    @Test void privateFixtureDigestIsRequiredAndMappingsAreComplete() {
        final CompatibilityAdapter adapter = Paper18CompatibilityAdapter.create(
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        assertEquals("1.8.8", adapter.runtimeClaim().minecraftVersion());
        assertEquals(10, adapter.mappings().mappings().size());
    }
}
