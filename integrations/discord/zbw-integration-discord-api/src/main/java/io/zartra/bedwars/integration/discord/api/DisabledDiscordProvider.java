package io.zartra.bedwars.integration.discord.api;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.extension.ExtensionMetadata;
import io.zartra.bedwars.api.extension.MinecraftVersion;
import io.zartra.bedwars.api.identity.ExtensionId;
import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.provider.Provider;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.api.version.ApiVersions;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.api.version.VersionRange;
import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Built-in disabled Discord provider used when Discord is absent, unconfigured or disabled.
 *
 * <p>It performs no I/O, starts no thread, owns no mutable lifecycle state and exposes no provider
 * capability. Lifecycle calls complete synchronously as {@link LifecycleState#STOPPED}; delivery
 * is rejected by policy without affecting gameplay or callers.</p>
 */
public final class DisabledDiscordProvider implements DiscordProvider {
    private static final SemanticVersion VERSION = SemanticVersion.parse("1.0.0");
    private final TimeSource timeSource;
    private final Descriptor descriptor;
    private final ExtensionMetadata metadata;

    /** @param timeSource injected wall-clock used only for health observation metadata */
    public DisabledDiscordProvider(final TimeSource timeSource) {
        this.timeSource = Objects.requireNonNull(timeSource, "timeSource");
        descriptor = Provider.Descriptor.of(ProviderId.of("zartra", "discord/disabled"),
                VERSION, CapabilitySet.empty());
        metadata = ExtensionMetadata.builder()
                .id(ExtensionId.of("zartra", "discord-disabled-provider"))
                .displayName("Zartra Discord Disabled Provider")
                .version(VERSION)
                .entrypoint(DisabledDiscordProvider.class.getName())
                .apiVersions(ApiVersions.SUPPORTED)
                .productVersions(VersionRange.parse("[0.1.0,1.0.0)"))
                .minecraftVersions(ExtensionMetadata.MinecraftRange.inclusive(
                        MinecraftVersion.parse("1.8.8"), MinecraftVersion.parse("1.21.11")))
                .dependencies(Collections.<ExtensionMetadata.Dependency>emptyList())
                .requiredApis(CapabilitySet.empty())
                .providedCapabilities(CapabilitySet.empty())
                .permissions(Collections.<ExtensionMetadata.PermissionNode>emptyList())
                .configurationKeys(Collections.singletonList(
                        ExtensionMetadata.ConfigurationKey.of("integrations.discord.enabled")))
                .build();
    }

    @Override public Descriptor descriptor() { return descriptor; }
    @Override public Health health() {
        return Health.of(HealthStatus.DISABLED, timeSource.now(), "discord.disabled");
    }
    @Override public CompletionStage<Result<LifecycleState>> start() { return stopped(); }
    @Override public CompletionStage<Result<LifecycleState>> drain(final Duration deadline) {
        Objects.requireNonNull(deadline, "deadline");
        if (deadline.isNegative()) { throw new IllegalArgumentException("deadline must not be negative"); }
        return stopped();
    }
    @Override public CompletionStage<Result<LifecycleState>> stop() { return stopped(); }
    @Override public CompletionStage<Result<DeliveryResult>> deliver(
            final DiscordEventEnvelope<? extends DiscordEventEnvelope.Payload> envelope) {
        Objects.requireNonNull(envelope, "envelope");
        return CompletableFuture.completedFuture(Result.success(DeliveryResult.of(
                Classification.REJECTED_BY_POLICY, "discord.disabled")));
    }
    @Override public ExtensionMetadata extensionMetadata() { return metadata; }
    private static CompletionStage<Result<LifecycleState>> stopped() {
        return CompletableFuture.completedFuture(Result.success(LifecycleState.STOPPED));
    }
}
