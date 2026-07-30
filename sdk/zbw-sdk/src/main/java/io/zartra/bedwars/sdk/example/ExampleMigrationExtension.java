package io.zartra.bedwars.sdk.example;

import io.zartra.bedwars.api.capability.CapabilitySet;
import io.zartra.bedwars.api.extension.Extension;
import io.zartra.bedwars.api.extension.ExtensionMetadata;
import io.zartra.bedwars.api.extension.MinecraftVersion;
import io.zartra.bedwars.api.identity.CapabilityId;
import io.zartra.bedwars.api.identity.ExtensionId;
import io.zartra.bedwars.api.migration.MigrationApi;
import io.zartra.bedwars.api.result.Result;
import io.zartra.bedwars.api.version.SemanticVersion;
import io.zartra.bedwars.api.version.VersionRange;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Compiled SDK example using public API contracts only.
 *
 * <p>The example converts a neutral greeting record. Real extensions receive data projections from
 * the host and must not open arbitrary files or mutate feature state directly.</p>
 */
public final class ExampleMigrationExtension implements Extension, MigrationApi.Provider {
    private static final ExtensionMetadata METADATA = ExtensionMetadata.builder()
            .id(ExtensionId.of("example", "migration"))
            .displayName("Example Migration Extension")
            .version(SemanticVersion.parse("1.0.0"))
            .entrypoint(ExampleMigrationExtension.class.getName())
            .apiVersions(VersionRange.parse("[1.0.0,2.0.0)"))
            .productVersions(VersionRange.parse("[0.1.0,1.0.0)"))
            .minecraftVersions(ExtensionMetadata.MinecraftRange.inclusive(
                    MinecraftVersion.parse("1.8.8"), MinecraftVersion.parse("1.21.11")))
            .providedCapabilities(CapabilitySet.of(Collections.singleton(
                    CapabilityId.of("example", "migration-provider"))))
            .build();

    @Override public ExtensionMetadata metadata() { return METADATA; }
    @Override public CompletionStage<Result<State>> start() {
        return CompletableFuture.completedFuture(Result.success(State.RUNNING));
    }
    @Override public CompletionStage<Result<State>> drain(final Duration deadline) {
        if (deadline == null || deadline.isNegative() || deadline.isZero()) {
            throw new IllegalArgumentException("deadline must be positive");
        }
        return CompletableFuture.completedFuture(Result.success(State.DRAINING));
    }
    @Override public CompletionStage<Result<State>> stop() {
        return CompletableFuture.completedFuture(Result.success(State.STOPPED));
    }
    @Override public String id() { return "example:migration"; }
    @Override public boolean supports(final String sourceKind) {
        return "example-greeting".equals(sourceKind);
    }
    @Override public MigrationApi.Conversion convert(final MigrationApi.Record source) {
        if (!supports(source.kind()) || !source.attributes().containsKey("message-key")) {
            return new MigrationApi.Conversion(MigrationApi.ConversionState.UNSUPPORTED,
                    Collections.<MigrationApi.Record>emptyList(), "unsupported-example-record");
        }
        final Map<String, String> attributes = new TreeMap<String, String>();
        attributes.put("message-key", source.attributes().get("message-key"));
        return new MigrationApi.Conversion(MigrationApi.ConversionState.MAPPED,
                Collections.singletonList(new MigrationApi.Record(
                        "example/" + source.id(), "native-greeting", attributes)),
                "example-mapped");
    }
}
