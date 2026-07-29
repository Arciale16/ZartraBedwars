package io.zartra.bedwars.compat.v1_17_19;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class Paper117To119CompatibilityAdaptersTest {
    @Test void exactFixturesRemainDeterministic() {
        final List<CompatibilityAdapter> adapters = Paper117To119CompatibilityAdapters.all(
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        assertEquals(3, adapters.size());
        assertEquals("1.17.1", adapters.get(0).runtimeClaim().minecraftVersion());
        assertEquals("1.19.4", adapters.get(2).runtimeClaim().minecraftVersion());
    }
}
