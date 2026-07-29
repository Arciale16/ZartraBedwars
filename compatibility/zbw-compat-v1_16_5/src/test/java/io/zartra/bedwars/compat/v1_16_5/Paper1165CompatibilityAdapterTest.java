package io.zartra.bedwars.compat.v1_16_5;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import java.time.Instant;
import org.junit.jupiter.api.Test;

final class Paper1165CompatibilityAdapterTest {
    @Test void lockedFixtureAndMappingsAreStable() {
        final CompatibilityAdapter adapter = Paper1165CompatibilityAdapter.create(
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        assertEquals(Paper1165CompatibilityAdapter.SERVER_SHA256,
                adapter.runtimeClaim().sha256());
        assertEquals(10, adapter.mappings().mappings().size());
    }
}
