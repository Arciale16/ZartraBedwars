package io.zartra.bedwars.compat.v1_12_16;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityAdapterSelector;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class Paper112To1164CompatibilityAdaptersTest {
    @Test void everyExactFixtureHasOneCompleteAdapter() {
        final List<CompatibilityAdapter> adapters =
                Paper112To1164CompatibilityAdapters.all(
                        TimeSource.FixedTimeSource.at(Instant.EPOCH));
        assertEquals(4, adapters.size());
        final CompatibilityAdapterSelector selector = new CompatibilityAdapterSelector(adapters);
        for (CompatibilityAdapter adapter : adapters) {
            assertEquals(adapter, selector.select(adapter.runtimeClaim()));
            assertEquals(10, adapter.mappings().mappings().size());
        }
    }
}
