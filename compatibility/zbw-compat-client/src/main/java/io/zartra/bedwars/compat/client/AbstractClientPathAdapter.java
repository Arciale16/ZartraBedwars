package io.zartra.bedwars.compat.client;

import io.zartra.bedwars.api.identity.DefinitionId;
import io.zartra.bedwars.compat.api.CompatibilityAdapter;
import io.zartra.bedwars.compat.api.CompatibilityMapping;
import io.zartra.bedwars.compat.api.CompatibilityOutcome;
import io.zartra.bedwars.compat.api.SemanticKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Shared fail-closed server-capability and parity evaluation. */
abstract class AbstractClientPathAdapter implements ClientPathAdapter {
    private static final Map<ClientFeature, Set<SemanticKey.Kind>> REQUIREMENTS =
            requirements();
    private final ClientPath path;
    private final List<ClientProvider> providers;

    AbstractClientPathAdapter(final ClientPath path,
                              final ClientProvider... providers) {
        this.path = Objects.requireNonNull(path, "path");
        this.providers = Arrays.asList(providers.clone());
    }

    @Override public final ClientPath path() { return path; }

    @Override
    public final boolean available(final ClientProviderInventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        if (!inventory.supports(path)) {
            return false;
        }
        for (ClientProvider provider : providers) {
            if (!inventory.probe(provider).exactlyCompatible()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final ClientCompatibilityReport evaluate(
            final CompatibilityAdapter serverAdapter,
            final ClientSession session) {
        Objects.requireNonNull(serverAdapter, "serverAdapter");
        if (Objects.requireNonNull(session, "session").path() != path) {
            throw new IllegalArgumentException("session path does not match adapter");
        }
        final List<ClientFeatureOutcome> outcomes =
                new ArrayList<ClientFeatureOutcome>();
        for (ClientFeature feature : ClientFeature.values()) {
            if (!serverSupports(serverAdapter, REQUIREMENTS.get(feature))) {
                outcomes.add(outcome(feature,
                        ClientFeatureOutcome.State.BLOCKED,
                        "server-capability-missing", false, false));
            } else {
                outcomes.add(usable(feature, session));
            }
        }
        return new ClientCompatibilityReport(
                path, runtimeLabel(serverAdapter), outcomes);
    }

    abstract ClientFeatureOutcome usable(ClientFeature feature, ClientSession session);

    final ClientFeatureOutcome outcome(
            final ClientFeature feature,
            final ClientFeatureOutcome.State state,
            final String reason,
            final boolean preserved,
            final boolean suppressed) {
        return new ClientFeatureOutcome(feature, state,
                DefinitionId.of("zartra", "compat/client/" + reason),
                preserved, suppressed);
    }

    private static boolean serverSupports(
            final CompatibilityAdapter adapter,
            final Set<SemanticKey.Kind> requiredKinds) {
        for (SemanticKey.Kind kind : requiredKinds) {
            boolean kindSupported = false;
            for (Map.Entry<SemanticKey, CompatibilityMapping> entry
                    : adapter.mappings().mappings().entrySet()) {
                if (entry.getKey().kind() == kind) {
                    final CompatibilityOutcome resolved =
                            adapter.resolve(entry.getKey());
                    if (resolved.gameplayPreserved()) {
                        kindSupported = true;
                        break;
                    }
                }
            }
            if (!kindSupported) {
                return false;
            }
        }
        return true;
    }

    private static String runtimeLabel(final CompatibilityAdapter adapter) {
        final CompatibilityAdapter.RuntimeClaim claim = adapter.runtimeClaim();
        return claim.platform() + "/" + claim.minecraftVersion()
                + "@" + claim.build();
    }

    private static Map<ClientFeature, Set<SemanticKey.Kind>> requirements() {
        final Map<ClientFeature, Set<SemanticKey.Kind>> result =
                new EnumMap<ClientFeature, Set<SemanticKey.Kind>>(ClientFeature.class);
        result.put(ClientFeature.GUI, kinds(SemanticKey.Kind.USER_INTERFACE));
        result.put(ClientFeature.SHOP, kinds(SemanticKey.Kind.USER_INTERFACE,
                SemanticKey.Kind.ITEM, SemanticKey.Kind.METADATA));
        result.put(ClientFeature.SPECTATOR, kinds(SemanticKey.Kind.USER_INTERFACE,
                SemanticKey.Kind.ENTITY, SemanticKey.Kind.PACKET));
        result.put(ClientFeature.REPLAY_ACCESS, kinds(
                SemanticKey.Kind.USER_INTERFACE, SemanticKey.Kind.ENTITY,
                SemanticKey.Kind.PACKET));
        result.put(ClientFeature.HOTBAR, kinds(SemanticKey.Kind.ITEM,
                SemanticKey.Kind.METADATA, SemanticKey.Kind.USER_INTERFACE));
        result.put(ClientFeature.TEXT, kinds(SemanticKey.Kind.TEXT));
        result.put(ClientFeature.SOUND, kinds(SemanticKey.Kind.SOUND));
        result.put(ClientFeature.PARTICLE, kinds(SemanticKey.Kind.PARTICLE));
        result.put(ClientFeature.ENTITY_DISPLAY, kinds(
                SemanticKey.Kind.ENTITY, SemanticKey.Kind.PACKET));
        result.put(ClientFeature.INPUT, kinds(
                SemanticKey.Kind.USER_INTERFACE, SemanticKey.Kind.PACKET));
        return result;
    }

    private static Set<SemanticKey.Kind> kinds(
            final SemanticKey.Kind first,
            final SemanticKey.Kind... remaining) {
        final EnumSet<SemanticKey.Kind> result = EnumSet.of(first);
        result.addAll(Arrays.asList(remaining));
        return result;
    }
}
