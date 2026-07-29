package io.zartra.bedwars.compat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.time.TimeSource;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

final class CompatibilityRuntimeFailureTest {
    private static final String SHA =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test void adapterCoversSupportedFallbackDegradedAndUnsupportedOutcomes() {
        final List<CompatibilityMapping> mappings =
                CompatibilityMappings.complete("coverage", values());
        final Set<SemanticKey> fallback = CompatibilityMappingKeys.of(
                mappings, SemanticKey.Kind.MATERIAL);
        final Set<SemanticKey> degraded = CompatibilityMappingKeys.of(
                mappings, SemanticKey.Kind.PARTICLE);
        final VersionedCompatibilityAdapter adapter = adapter(mappings, fallback, degraded);
        assertEquals(CompatibilityOutcome.State.FALLBACK,
                adapter.resolve(key(mappings, SemanticKey.Kind.MATERIAL)).state());
        final CompatibilityOutcome particle =
                adapter.resolve(key(mappings, SemanticKey.Kind.PARTICLE));
        assertEquals(CompatibilityOutcome.State.DEGRADED, particle.state());
        assertTrue(particle.decorativeSuppression());
        assertEquals(CompatibilityOutcome.State.SUPPORTED,
                adapter.resolve(key(mappings, SemanticKey.Kind.SCHEDULER)).state());
        assertEquals(CompatibilityOutcome.State.UNSUPPORTED, adapter.resolve(
                SemanticKey.of(SemanticKey.Kind.SOUND,
                        DefinitionId.of("zartra", "sound/missing"))).state());
    }

    @Test void adapterLifecycleAndValidationBranchesAreFailClosed() {
        final List<CompatibilityMapping> mappings =
                CompatibilityMappings.complete("coverage", values());
        final VersionedCompatibilityAdapter adapter = adapter(
                mappings, Collections.<SemanticKey>emptySet(),
                Collections.<SemanticKey>emptySet());
        assertEquals(Provider.HealthStatus.DISABLED, adapter.health().status());
        adapter.start().toCompletableFuture().join();
        assertEquals(Provider.HealthStatus.HEALTHY, adapter.health().status());
        adapter.drain(Duration.ofSeconds(1)).toCompletableFuture().join();
        adapter.stop().toCompletableFuture().join();
        assertThrows(IllegalArgumentException.class, () -> adapter.drain(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> adapter.drain(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> adapter(
                mappings, Collections.singleton(SemanticKey.of(
                        SemanticKey.Kind.TEXT, DefinitionId.of("zartra", "missing"))),
                Collections.<SemanticKey>emptySet()));
        final SemanticKey material = key(mappings, SemanticKey.Kind.MATERIAL);
        assertThrows(IllegalArgumentException.class, () -> adapter(
                mappings, Collections.singleton(material), Collections.singleton(material)));
    }

    @Test void selectorAndMappingInputsRejectAmbiguityAndMutation() {
        final List<CompatibilityMapping> mappings =
                CompatibilityMappings.complete("coverage", values());
        final VersionedCompatibilityAdapter adapter = adapter(
                mappings, Collections.<SemanticKey>emptySet(),
                Collections.<SemanticKey>emptySet());
        assertThrows(NullPointerException.class, () -> new CompatibilityAdapterSelector(null));
        assertThrows(IllegalArgumentException.class, () ->
                new CompatibilityAdapterSelector(Arrays.asList(adapter, null)));
        final CompatibilityAdapterSelector selector =
                new CompatibilityAdapterSelector(Collections.singletonList(adapter));
        assertThrows(NullPointerException.class, () -> selector.select(null));
        assertThrows(UnsupportedOperationException.class, () -> selector.adapters().clear());
        assertThrows(NullPointerException.class, () ->
                CompatibilityMappingKeys.of(null, SemanticKey.Kind.TEXT));
        assertThrows(NullPointerException.class, () ->
                CompatibilityMappingKeys.of(mappings, (SemanticKey.Kind[]) null));
        assertThrows(IllegalArgumentException.class, () ->
                CompatibilityMappings.complete(null, values()));
        assertThrows(IllegalArgumentException.class, () ->
                CompatibilityMappings.complete("coverage", (String[]) null));
        assertThrows(IllegalArgumentException.class, () ->
                CompatibilityMappings.complete("INVALID PREFIX", values()));
        final String[] invalid = values();
        invalid[0] = "unsafe value";
        assertThrows(IllegalArgumentException.class, () ->
                CompatibilityMappings.complete("coverage", invalid));
    }

    @Test void presentationFailureAlwaysStopsSelectedAdapter() {
        final List<CompatibilityMapping> mappings =
                CompatibilityMappings.complete("coverage", values());
        final VersionedCompatibilityAdapter adapter = adapter(
                mappings, Collections.<SemanticKey>emptySet(),
                Collections.<SemanticKey>emptySet());
        final CompatibilityRuntimeLifecycle lifecycle = new CompatibilityRuntimeLifecycle(
                new CompatibilityAdapterSelector(Collections.singletonList(adapter)));
        final CompatibilityPresentationBootstrap<String, String> bootstrap =
                new CompatibilityPresentationBootstrap<String, String>(
                        lifecycle, "commands", "ui",
                        new CompatibilityPresentationBootstrap.Presentation<String, String>() {
                            @Override public java.util.concurrent.CompletionStage<Void> activate(
                                    final CompatibilityAdapter ignored,
                                    final String commands, final String ui) {
                                throw new IllegalStateException("activation rejected");
                            }
                            @Override public java.util.concurrent.CompletionStage<Void> deactivate() {
                                return CompletableFuture.completedFuture(null);
                            }
                        });
        assertThrows(CompletionException.class, () ->
                bootstrap.start(adapter.runtimeClaim()).toCompletableFuture().join());
        assertEquals(CompatibilityRuntimeLifecycle.State.STOPPED, lifecycle.state());
        bootstrap.stop().toCompletableFuture().join();
        assertEquals(CompatibilityRuntimeLifecycle.State.STOPPED, lifecycle.state());
    }

    @Test void lifecycleRejectsSecondConcurrentActivation() {
        final List<CompatibilityMapping> mappings =
                CompatibilityMappings.complete("coverage", values());
        final VersionedCompatibilityAdapter adapter = adapter(
                mappings, Collections.<SemanticKey>emptySet(),
                Collections.<SemanticKey>emptySet());
        final CompatibilityRuntimeLifecycle lifecycle = new CompatibilityRuntimeLifecycle(
                new CompatibilityAdapterSelector(Collections.singletonList(adapter)));
        lifecycle.start(adapter.runtimeClaim()).toCompletableFuture().join();
        assertThrows(IllegalStateException.class, () -> lifecycle.start(adapter.runtimeClaim()));
        lifecycle.stop().toCompletableFuture().join();
    }

    private static VersionedCompatibilityAdapter adapter(
            final List<CompatibilityMapping> mappings,
            final Set<SemanticKey> fallback,
            final Set<SemanticKey> degraded) {
        return new VersionedCompatibilityAdapter(
                new CompatibilityAdapter.RuntimeClaim("Paper", "1.8.8", "1", SHA),
                ProviderId.of("zartra", "coverage"), mappings, fallback, degraded,
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
    }

    private static SemanticKey key(final List<CompatibilityMapping> mappings,
                                   final SemanticKey.Kind kind) {
        for (CompatibilityMapping mapping : mappings) {
            if (mapping.semanticKey().kind() == kind) {
                return mapping.semanticKey();
            }
        }
        throw new AssertionError("missing key " + kind);
    }

    private static String[] values() {
        return new String[] {
            "MATERIAL", "ITEM", "METADATA", "SOUND", "PARTICLE",
            "TEXT", "ENTITY", "PACKET", "GUI", "SCHEDULER"
        };
    }
}
