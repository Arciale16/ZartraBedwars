package io.zartra.bedwars.compat.modern;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityAdapterSelector;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ModernCompatibilityAdaptersTest {
    @Test void everyLockedModernFixtureSelectsExactlyOnce() {
        final List<CompatibilityAdapter> adapters = ModernCompatibilityAdapters.all(
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
        assertEquals(10, adapters.size());
        final CompatibilityAdapterSelector selector = new CompatibilityAdapterSelector(adapters);
        for (CompatibilityAdapter adapter : adapters) {
            assertEquals(adapter, selector.select(adapter.runtimeClaim()));
        }
    }
}
