package io.zartra.bedwars.compat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

final class CompatibilityRuntimeTest {
    private static final String SHA =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test void selectorRejectsUnsupportedAndDuplicateExactRuntime() {
        final CompatibilityAdapter adapter = adapter("1.8.8");
        final CompatibilityAdapterSelector selector =
                new CompatibilityAdapterSelector(Collections.singletonList(adapter));
        assertEquals(adapter, selector.select(adapter.runtimeClaim()));
        assertThrows(IllegalStateException.class, () -> selector.select(
                claim("1.9.4")));
        assertThrows(IllegalStateException.class, () ->
                new CompatibilityAdapterSelector(java.util.Arrays.asList(adapter, adapter))
                        .select(adapter.runtimeClaim()));
    }

    @Test void fallbackAndPacketIsolationAreExplicit() {
        final CompatibilityAdapter adapter = adapter("1.8.8");
        final SemanticKey material = adapter.mappings().mappings().keySet().iterator().next();
        final CompatibilityOutcome outcome = adapter.resolve(material);
        assertEquals(CompatibilityOutcome.State.FALLBACK, outcome.state());
        assertTrue(outcome.gameplayPreserved());
        assertFalse(outcome.decorativeSuppression());
        assertEquals("zartra:compat/fallback/material", outcome.reason().toString());
        assertThrows(NullPointerException.class, () -> adapter.resolve(null));
    }

    @Test void lifecycleActivatesPresentationAfterSelectionAndCleansUp() {
        final CompatibilityAdapter adapter = adapter("1.8.8");
        final CompatibilityRuntimeLifecycle lifecycle = new CompatibilityRuntimeLifecycle(
                new CompatibilityAdapterSelector(Collections.singletonList(adapter)));
        final AtomicInteger sequence = new AtomicInteger();
        final CompatibilityPresentationBootstrap<String, String> bootstrap =
                new CompatibilityPresentationBootstrap<String, String>(
                        lifecycle, "commands", "ui",
                        new CompatibilityPresentationBootstrap.Presentation<String, String>() {
                            @Override public java.util.concurrent.CompletionStage<Void> activate(
                                    final CompatibilityAdapter selected,
                                    final String commands, final String ui) {
                                assertEquals(ProviderId.of("zartra", "fixture"),
                                        selected.descriptor().id());
                                assertEquals("commands", commands);
                                assertEquals("ui", ui);
                                sequence.compareAndSet(0, 1);
                                return CompletableFuture.completedFuture(null);
                            }
                            @Override public java.util.concurrent.CompletionStage<Void> deactivate() {
                                sequence.compareAndSet(1, 2);
                                return CompletableFuture.completedFuture(null);
                            }
                        });
        bootstrap.start(adapter.runtimeClaim()).toCompletableFuture().join();
        assertEquals(CompatibilityRuntimeLifecycle.State.RUNNING, bootstrap.state());
        bootstrap.stop().toCompletableFuture().join();
        assertEquals(2, sequence.get());
        assertEquals(CompatibilityRuntimeLifecycle.State.STOPPED, bootstrap.state());
        assertEquals(null, lifecycle.active());
    }

    @Test void completeMappingsRequireEveryKindAndSafeValues() {
        assertThrows(IllegalArgumentException.class, () ->
                CompatibilityMappings.complete("profile", "only-one"));
        assertThrows(IllegalArgumentException.class, () ->
                CompatibilityMappings.complete("Bad Prefix", values()));
        final List<CompatibilityMapping> mappings =
                CompatibilityMappings.complete("fixture", values());
        assertEquals(SemanticKey.Kind.values().length, mappings.size());
        assertEquals(1, CompatibilityMappingKeys.of(mappings,
                SemanticKey.Kind.SCHEDULER).size());
    }

    private static CompatibilityAdapter adapter(final String version) {
        final List<CompatibilityMapping> mappings =
                CompatibilityMappings.complete("fixture", values());
        return new VersionedCompatibilityAdapter(claim(version),
                ProviderId.of("zartra", "fixture"), mappings,
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.MATERIAL),
                CompatibilityMappingKeys.of(mappings, SemanticKey.Kind.PARTICLE),
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
    }

    private static CompatibilityAdapter.RuntimeClaim claim(final String version) {
        return new CompatibilityAdapter.RuntimeClaim("Paper", version, "1", SHA);
    }

    private static String[] values() {
        return new String[] {
            "MATERIAL", "ITEM", "METADATA", "SOUND", "PARTICLE",
            "TEXT", "ENTITY", "PACKET", "GUI", "SCHEDULER"
        };
    }
}
