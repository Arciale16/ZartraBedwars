package io.zartra.bedwars.content.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class ModeBalanceCatalogTest {
    @Test
    void loadsAndSimulatesDeterministically() {
        final DefinitionId id = DefinitionId.of("zbw", "mode/standard");
        final ModeBalanceCatalog.Profile profile = new ModeBalanceCatalog.Profile(
                id, Collections.singletonMap(
                        DefinitionId.of("zbw", "timing/generator"), 30));
        final ModeBalanceCatalog catalog =
                new ModeBalanceCatalog(1, Collections.singletonList(profile));
        assertEquals(1, catalog.version());
        assertEquals(30, catalog.require(id).values().values().iterator().next());
        final ModeBalanceCatalog.GoldenResult result = catalog.simulate(
                id, value -> value.values().values().iterator().next(), 100);
        assertTrue(result.success());
        assertEquals(30, result.score());
    }

    @Test
    void rejectsInvalidAndNondeterministicProfiles() {
        final DefinitionId id = DefinitionId.of("zbw", "mode/standard");
        assertThrows(IllegalArgumentException.class,
                () -> new ModeBalanceCatalog(0, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> new ModeBalanceCatalog.Profile(id, Collections.emptyMap()));
        final ModeBalanceCatalog.Profile profile = new ModeBalanceCatalog.Profile(
                id, Collections.singletonMap(
                        DefinitionId.of("zbw", "timing/generator"), 30));
        assertThrows(IllegalArgumentException.class,
                () -> new ModeBalanceCatalog(1, java.util.Arrays.asList(profile, profile)));
        final ModeBalanceCatalog catalog =
                new ModeBalanceCatalog(1, Collections.singletonList(profile));
        final AtomicLong value = new AtomicLong();
        assertEquals("nondeterministic",
                catalog.simulate(id, ignored -> value.incrementAndGet(), 100).code());
        assertEquals("score_out_of_bounds",
                catalog.simulate(id, ignored -> 101, 100).code());
        assertThrows(IllegalArgumentException.class,
                () -> catalog.require(DefinitionId.of("zbw", "mode/missing")));
    }
}
