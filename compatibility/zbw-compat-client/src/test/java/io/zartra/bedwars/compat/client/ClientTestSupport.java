package io.zartra.bedwars.compat.client;

import io.zartra.bedwars.api.identity.ProviderId;
import io.zartra.bedwars.api.time.TimeSource;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.CompatibilityMappings;
import io.zartra.bedwars.compat.api.VersionedCompatibilityAdapter;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/** Deterministic M22 test fixtures for ZBW-INT-010 and ZBW-COMPAT-004..008. */
final class ClientTestSupport {
    static final String SHA =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private ClientTestSupport() { throw new AssertionError("No instances"); }

    static CompatibilityAdapter server() {
        return server(CompatibilityMappings.complete(
                "test", "STONE", "ITEM", "NBT", "SOUND", "PARTICLE",
                "TEXT", "ENTITY", "PACKET", "GUI", "SCHEDULER"));
    }

    static CompatibilityAdapter server(
            final List<CompatibilityMapping> mappings) {
        return new VersionedCompatibilityAdapter(
                new CompatibilityAdapter.RuntimeClaim(
                        "Paper", "1.21.1", "build133", SHA),
                ProviderId.of("zartra", "test_compat"), mappings,
                Collections.emptySet(), Collections.emptySet(),
                TimeSource.FixedTimeSource.at(Instant.EPOCH));
    }

    static ClientProviderInventory allProviders() {
        return ClientProviderInventory.of(java.util.Arrays.asList(
                ClientProviderProbe.present(ClientProvider.VIAVERSION, "5.4.2"),
                ClientProviderProbe.present(ClientProvider.VIABACKWARDS, "5.4.2"),
                ClientProviderProbe.present(ClientProvider.VIAREWIND, "4.0.6"),
                ClientProviderProbe.present(ClientProvider.GEYSER, "2.7.0"),
                ClientProviderProbe.present(ClientProvider.FLOODGATE, "2.2.4")));
    }

    static ClientSession session(final ClientPath path) {
        if (path == ClientPath.GEYSER_FLOODGATE) {
            return new ClientSession("session_1234", path,
                    ClientSession.Edition.BEDROCK,
                    ClientSession.InputMode.CONTROLLER, 776);
        }
        return new ClientSession("session_1234", path,
                ClientSession.Edition.JAVA,
                ClientSession.InputMode.JAVA_NATIVE, 47);
    }
}
